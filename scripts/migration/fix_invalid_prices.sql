-- 비정상 주가 정리 + 거래일 캘린더 보정 + 지표 재계산 트리거.
--
-- 배경
--   KIS 국내주식기간별시세가 일부 과거 구간(1986~2008)에 종가 0 이하(음수 포함)를 내려주어 그대로 저장됐다.
--   음수 7,425행 중 5,733행은 정확히 -2147483648(Integer.MIN_VALUE)이며 모두 수정주가(PRICE_TYPE=1)다.
--   원주가에는 음수가 없어 수집 로직이 아니라 외부 응답이 원인이다.
--   PriceCollectionService에 종가 양수 검증(DailyCandle.hasValidPrices)을 추가해 재발하지 않는다.
--   이 스크립트는 그 이전에 쌓인 오염분을 정리한다.
--
-- 보존 대상 (지우지 않는다)
--   시·고·저가가 0이고 종가만 양수인 행 68,278건은 거래정지일의 정상 기록이다.
--   따라서 삭제 조건은 "종가 0 이하 또는 시·고·저가 음수"로 한정한다.
--
-- 동작
--   1) 비정상 가격 행과 그에 대응하는 지표 행을 제거 (약 8,662행 / 175그룹)
--   2) 영향 그룹의 지표 커서를 삭제 → 다음 지표 배치가 그 그룹을 처음부터 재계산(saveAll이 PK upsert)
--   3) 주가에 있으나 캘린더에 없는 거래일을 보정
--
-- 실행: mysql -h <host> -P <port> -u <writer> -p DOVE_LAB < fix_invalid_prices.sql
-- 재실행 안전.
--
-- 이 스크립트로 해결되지 않는 잔여 건 (외부 재수집 필요 — ROOT 백필 API)
--   a) 원주가 1985-12~1987-11 누락: 해당 구간 EXCHANGE=0의 원주가는 125거래일뿐인데
--      수정주가는 581거래일이다. 원주가 재수집이 필요하다.
--   b) 2009-03-20 주가 누락 7종목 (그날 1,829종목, 앞뒤 1,835~1,836)
--        KOSDAQ(1): 065180
--        KOSPI(0) : 081200, 084240, 094950, 099210, 101380, 102000

-- ── 0) 정리 전 현황 ──────────────────────────────────────────────────────────
SELECT 'before' AS phase, PRICE_TYPE, COUNT(*) AS bad_rows
  FROM STOCK_PRICE
 WHERE CLOSE_PRICE <= 0 OR OPEN_PRICE < 0 OR HIGH_PRICE < 0 OR LOW_PRICE < 0
 GROUP BY PRICE_TYPE;

-- ── 1) 대상 키 보존 ─────────────────────────────────────────────────────────
-- 가격 행을 지우면 대상을 알 수 없으므로 먼저 키를 담아두고, 이후 삭제는 전부 PK 조인으로 수행한다.
DROP TEMPORARY TABLE IF EXISTS TMP_BAD_PRICE;
CREATE TEMPORARY TABLE TMP_BAD_PRICE (
    TICKER     VARCHAR(20) NOT NULL,
    EXCHANGE   TINYINT     NOT NULL,
    PRICE_TYPE TINYINT     NOT NULL,
    TRADE_DATE DATE        NOT NULL,
    PRIMARY KEY (TICKER, EXCHANGE, PRICE_TYPE, TRADE_DATE)
);

INSERT INTO TMP_BAD_PRICE (TICKER, EXCHANGE, PRICE_TYPE, TRADE_DATE)
SELECT TICKER, EXCHANGE, PRICE_TYPE, TRADE_DATE
  FROM STOCK_PRICE
 WHERE CLOSE_PRICE <= 0 OR OPEN_PRICE < 0 OR HIGH_PRICE < 0 OR LOW_PRICE < 0;

-- ── 2) 지표 행 제거 (가격보다 먼저) ─────────────────────────────────────────
DELETE f
  FROM STOCK_FEATURE_DAILY f
  JOIN TMP_BAD_PRICE b
    ON b.TICKER = f.TICKER AND b.EXCHANGE = f.EXCHANGE
   AND b.PRICE_TYPE = f.PRICE_TYPE AND b.TRADE_DATE = f.TRADE_DATE;

-- ── 3) 비정상 가격 행 제거 ──────────────────────────────────────────────────
DELETE p
  FROM STOCK_PRICE p
  JOIN TMP_BAD_PRICE b
    ON b.TICKER = p.TICKER AND b.EXCHANGE = p.EXCHANGE
   AND b.PRICE_TYPE = p.PRICE_TYPE AND b.TRADE_DATE = p.TRADE_DATE;

-- ── 4) 지표 커서 삭제 → 다음 배치가 해당 그룹을 재계산 ──────────────────────
DELETE c
  FROM INDICATOR_CURSOR c
  JOIN (SELECT DISTINCT TICKER, EXCHANGE, PRICE_TYPE FROM TMP_BAD_PRICE) g
    ON g.TICKER = c.TICKER AND g.EXCHANGE = c.EXCHANGE AND g.PRICE_TYPE = c.PRICE_TYPE;

-- ── 5) 거래일 캘린더 보정 ───────────────────────────────────────────────────
-- 원주가가 비어 있는 과거 구간이 있어 두 가격유형의 거래일을 모두 반영한다.
INSERT INTO EXCHANGE_TRADING_DATE (EXCHANGE, TRADE_DATE, PRICES_SYNCED)
SELECT 'KRX', d.TRADE_DATE, 1
  FROM (
      SELECT DISTINCT TRADE_DATE
        FROM STOCK_PRICE
       WHERE EXCHANGE IN (0, 1, 2)
  ) d
ON DUPLICATE KEY UPDATE PRICES_SYNCED = VALUES(PRICES_SYNCED);

-- ── 6) 정리 후 확인 ─────────────────────────────────────────────────────────
SELECT 'after_bad_prices' AS phase, COUNT(*) AS bad_rows
  FROM STOCK_PRICE
 WHERE CLOSE_PRICE <= 0 OR OPEN_PRICE < 0 OR HIGH_PRICE < 0 OR LOW_PRICE < 0;

SELECT 'halt_rows_kept' AS phase, COUNT(*) AS rows_kept
  FROM STOCK_PRICE
 WHERE CLOSE_PRICE > 0 AND OPEN_PRICE = 0 AND HIGH_PRICE = 0 AND LOW_PRICE = 0;

SELECT 'recompute_groups' AS phase, COUNT(DISTINCT TICKER, EXCHANGE, PRICE_TYPE) AS groups
  FROM TMP_BAD_PRICE;

SELECT 'calendar_missing' AS phase, COUNT(*) AS missing_dates
  FROM (
      SELECT DISTINCT TRADE_DATE FROM STOCK_PRICE WHERE EXCHANGE IN (0, 1, 2)
  ) p
  LEFT JOIN EXCHANGE_TRADING_DATE e ON e.TRADE_DATE = p.TRADE_DATE
 WHERE e.TRADE_DATE IS NULL;

DROP TEMPORARY TABLE IF EXISTS TMP_BAD_PRICE;
