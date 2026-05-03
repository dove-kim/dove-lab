-- 로컬 테스트용 시드 데이터 (2025-07-01 ~ 2026-04-25)
-- 실행 전제: init.sql 이 먼저 실행되어 스키마가 생성되어 있어야 함
SET NAMES utf8mb4;
USE DOVE_STOCK;

-- ─────────────────────────────────────────
-- 0. 기존 데이터 초기화
-- ─────────────────────────────────────────
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE DAILY_STOCK_PRICE;
TRUNCATE TABLE STOCK_LISTED_DATE;
TRUNCATE TABLE MARKET_TRADING_DATE;
TRUNCATE TABLE MARKET_DATA_CURSOR;
TRUNCATE TABLE STOCK;
SET FOREIGN_KEY_CHECKS = 1;

-- ─────────────────────────────────────────
-- 1. 종목 마스터
-- ─────────────────────────────────────────
-- [정상] 전체 기간 주가 존재
-- [중간정지] 일부 기간 주가 후 거래정지
-- [완전정지] 상장은 되어 있으나 주가 데이터 없음
-- [신규상장] 50일차부터 주가 존재
INSERT INTO STOCK (MARKET_TYPE, CODE, NAME, TRADING_STATUS) VALUES
  ('KOSPI',  '005930', '삼성전자',       'ACTIVE'),     -- 정상
  ('KOSPI',  '000660', 'SK하이닉스',      'ACTIVE'),     -- 정상
  ('KOSPI',  '035420', 'NAVER',          'ACTIVE'),     -- 정상
  ('KOSDAQ', '293490', '카카오게임즈',     'ACTIVE'),     -- 정상
  ('KOSDAQ', '247540', '에코프로비엠',     'ACTIVE'),     -- 신규상장 (50일차~)
  ('KOSPI',  '005380', '현대차',          'SUSPENDED'),  -- 중간정지 (~120일차)
  ('KOSDAQ', '086520', '에코프로',        'SUSPENDED'),  -- 중간정지 (~80일차)
  ('KOSPI',  '373220', 'LG에너지솔루션',   'SUSPENDED');  -- 완전정지 (주가 없음)

-- ─────────────────────────────────────────
-- 2. 개장일
--    제외 공휴일:
--      2025-08-15 광복절
--      2025-10-03 개천절, 2025-10-06~07 추석연휴, 2025-10-09 한글날
--      2025-12-25 크리스마스
--      2026-01-01 신정, 2026-01-27~29 설날연휴
-- ─────────────────────────────────────────
INSERT INTO MARKET_TRADING_DATE (MARKET_TYPE, TRADE_DATE, IS_OPEN)
WITH RECURSIVE date_range AS (
  SELECT DATE('2025-07-01') AS d
  UNION ALL
  SELECT DATE_ADD(d, INTERVAL 1 DAY) FROM date_range WHERE d < '2026-04-25'
),
trading_days AS (
  SELECT d FROM date_range
  WHERE DAYOFWEEK(d) NOT IN (1, 7)
    AND d NOT IN (
      '2025-08-15',
      '2025-10-03','2025-10-06','2025-10-07','2025-10-09',
      '2025-12-25',
      '2026-01-01','2026-01-27','2026-01-28','2026-01-29'
    )
)
SELECT m.market_type, td.d, TRUE
FROM trading_days td
CROSS JOIN (SELECT 'KOSPI'  AS market_type
            UNION ALL SELECT 'KOSDAQ'
            UNION ALL SELECT 'KONEX') m;

-- ─────────────────────────────────────────
-- 3. 종목 상장 이력
--    거래정지 종목도 상장 이력은 전체 기간 존재
--    신규상장(에코프로비엠)은 50일차부터
-- ─────────────────────────────────────────
INSERT INTO STOCK_LISTED_DATE (MARKET_TYPE, CODE, DATE)
WITH RECURSIVE date_range AS (
  SELECT DATE('2025-07-01') AS d
  UNION ALL
  SELECT DATE_ADD(d, INTERVAL 1 DAY) FROM date_range WHERE d < '2026-04-25'
),
trading_days AS (
  SELECT d, ROW_NUMBER() OVER (ORDER BY d) AS rn
  FROM date_range
  WHERE DAYOFWEEK(d) NOT IN (1, 7)
    AND d NOT IN (
      '2025-08-15',
      '2025-10-03','2025-10-06','2025-10-07','2025-10-09',
      '2025-12-25',
      '2026-01-01','2026-01-27','2026-01-28','2026-01-29'
    )
),
stocks AS (
  SELECT 'KOSPI'  AS mkt, '005930' AS code, 1  AS from_rn UNION ALL
  SELECT 'KOSPI',  '000660', 1  UNION ALL
  SELECT 'KOSPI',  '035420', 1  UNION ALL
  SELECT 'KOSDAQ', '293490', 1  UNION ALL
  SELECT 'KOSPI',  '005380', 1  UNION ALL  -- 거래정지지만 상장 이력은 전체
  SELECT 'KOSDAQ', '086520', 1  UNION ALL  -- 거래정지지만 상장 이력은 전체
  SELECT 'KOSPI',  '373220', 1  UNION ALL  -- 완전정지지만 상장 이력은 전체
  SELECT 'KOSDAQ', '247540', 50             -- 신규상장: 50일차부터
)
SELECT s.mkt, s.code, td.d
FROM stocks s
JOIN trading_days td ON td.rn >= s.from_rn;

-- ─────────────────────────────────────────
-- 4. 일별 주가
--    base_price ±8% 범위, 100원 단위 반올림
--    거래정지 종목: 정지일 이전까지만
--    신규상장 종목: 50일차부터
--    LG에너지솔루션: 행 없음
-- ─────────────────────────────────────────
INSERT INTO DAILY_STOCK_PRICE (MARKET_TYPE, STOCK_CODE, TRADE_DATE, VOLUME, OPEN_PRICE, CLOSE_PRICE, LOW_PRICE, HIGH_PRICE)
WITH RECURSIVE date_range AS (
  SELECT DATE('2025-07-01') AS d
  UNION ALL
  SELECT DATE_ADD(d, INTERVAL 1 DAY) FROM date_range WHERE d < '2026-04-25'
),
trading_days AS (
  SELECT d, ROW_NUMBER() OVER (ORDER BY d) AS rn
  FROM date_range
  WHERE DAYOFWEEK(d) NOT IN (1, 7)
    AND d NOT IN (
      '2025-08-15',
      '2025-10-03','2025-10-06','2025-10-07','2025-10-09',
      '2025-12-25',
      '2026-01-01','2026-01-27','2026-01-28','2026-01-29'
    )
),
stocks AS (
  --                               mkt       code     base    from  to
  SELECT 'KOSPI'  AS mkt, '005930' AS code,  60000 AS base, 1   AS from_rn, 9999 AS to_rn UNION ALL
  SELECT 'KOSPI',  '000660', 170000, 1,    9999 UNION ALL
  SELECT 'KOSPI',  '035420', 190000, 1,    9999 UNION ALL
  SELECT 'KOSDAQ', '293490',  15000, 1,    9999 UNION ALL
  SELECT 'KOSPI',  '005380', 220000, 1,     120 UNION ALL  -- 120일차까지
  SELECT 'KOSDAQ', '086520',  72000, 1,      80 UNION ALL  -- 80일차까지
  SELECT 'KOSDAQ', '247540',  95000, 50,   9999             -- 50일차부터
  -- LG에너지솔루션(373220): 의도적으로 제외
),
raw AS (
  SELECT
    s.mkt, s.code, td.d, s.base,
    ROUND(s.base * (0.92 + (CRC32(CONCAT(s.code, td.d, 'c')) % 160) * 0.001), -2) AS cp,
    ROUND(s.base * (0.92 + (CRC32(CONCAT(s.code, td.d, 'o')) % 160) * 0.001), -2) AS op,
    500000 + (CRC32(CONCAT(s.code, td.d, 'v')) % 9500000)                          AS vol
  FROM stocks s
  JOIN trading_days td ON td.rn >= s.from_rn AND td.rn <= s.to_rn
)
SELECT
  mkt, code, d, vol, op, cp,
  ROUND(LEAST(op, cp)    * (0.970 + (CRC32(CONCAT(code, d, 'l')) % 20) * 0.001), -2),
  ROUND(GREATEST(op, cp) * (1.010 + (CRC32(CONCAT(code, d, 'h')) % 20) * 0.001), -2)
FROM raw;

-- ─────────────────────────────────────────
-- 5. 시장 데이터 커서
-- ─────────────────────────────────────────
INSERT INTO MARKET_DATA_CURSOR (MARKET_TYPE, LAST_PROCESSED_DATE) VALUES
  ('KOSPI',  '2026-04-25'),
  ('KOSDAQ', '2026-04-25'),
  ('KONEX',  '2026-04-25');
