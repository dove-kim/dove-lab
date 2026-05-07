-- 로컬 테스트용 시드 데이터
-- 실행 전제: init.sql 이 먼저 실행되어 스키마가 생성되어 있어야 함
--
-- 종목별 가격 패턴 (DATEDIFF 기반 수식으로 뚜렷한 패턴 생성):
--   005930 삼성전자     - 꾸준한 상승 추세 (2년)
--   000660 SK하이닉스  - V형 하락→반등  (10개월)
--   035420 NAVER        - 횡보 (박스권 진동) (10개월)
--   293490 카카오게임즈 - ∩형 상승→하락  (10개월)
--   247540 에코프로비엠 - 꾸준한 하락 추세 (8개월)
--   999901 딥테크에이아이 - 급등 후 조정    (4개월)
--   999902 미래모빌리티   - 꾸준한 상승     (3개월)
--   005380 현대차       - 완만한 상승 (정지 4개월 전)
--   086520 에코프로     - 강한 상승   (정지 6개월 전)
--   999903 한일정밀     - 거래 후 3개월 정지 → 재개 (중간 데이터 공백)

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
INSERT INTO STOCK (MARKET_TYPE, CODE, NAME, TRADING_STATUS, LISTING_DATE) VALUES
  ('KOSPI',  '005930', '삼성전자',       'ACTIVE',     '1975-06-11'),
  ('KOSPI',  '005380', '현대차',         'SUSPENDED',  '1987-03-09'),
  ('KOSPI',  '000660', 'SK하이닉스',     'ACTIVE',     '1996-12-26'),
  ('KOSPI',  '035420', 'NAVER',          'ACTIVE',     '2002-10-29'),
  ('KOSDAQ', '086520', '에코프로',       'SUSPENDED',  '2007-11-22'),
  ('KOSDAQ', '247540', '에코프로비엠',   'ACTIVE',     '2019-03-15'),
  ('KOSDAQ', '293490', '카카오게임즈',   'ACTIVE',     '2020-09-10'),
  ('KOSPI',  '373220', 'LG에너지솔루션', 'SUSPENDED',  '2022-01-27'),
  ('KOSDAQ', '999901', '딥테크에이아이', 'ACTIVE',     DATE_SUB(CURDATE(), INTERVAL 4  MONTH)),
  ('KOSPI',  '999902', '미래모빌리티',   'ACTIVE',     DATE_SUB(CURDATE(), INTERVAL 3  MONTH)),
  ('KOSPI',  '999903', '한일정밀',       'ACTIVE',     '2010-05-15');

-- ─────────────────────────────────────────
-- 2. 개장일 (2년 전 ~ 어제, 주말 제외)
-- ─────────────────────────────────────────
INSERT INTO MARKET_TRADING_DATE (MARKET_TYPE, TRADE_DATE, IS_OPEN)
WITH RECURSIVE date_range AS (
  SELECT DATE_SUB(CURDATE(), INTERVAL 2 YEAR) AS d
  UNION ALL
  SELECT DATE_ADD(d, INTERVAL 1 DAY) FROM date_range WHERE d < DATE_SUB(CURDATE(), INTERVAL 1 DAY)
),
trading_days AS (
  SELECT d FROM date_range WHERE DAYOFWEEK(d) NOT IN (1, 7)
)
SELECT m.market_type, td.d, TRUE
FROM trading_days td
CROSS JOIN (SELECT 'KOSPI'  AS market_type
            UNION ALL SELECT 'KOSDAQ'
            UNION ALL SELECT 'KONEX') m;

-- ─────────────────────────────────────────
-- 3. 종목 상장 이력 (각 종목 상장일 ~ 어제)
-- ─────────────────────────────────────────
INSERT INTO STOCK_LISTED_DATE (MARKET_TYPE, CODE, DATE)
WITH RECURSIVE date_range AS (
  SELECT DATE_SUB(CURDATE(), INTERVAL 2 YEAR) AS d
  UNION ALL
  SELECT DATE_ADD(d, INTERVAL 1 DAY) FROM date_range WHERE d < DATE_SUB(CURDATE(), INTERVAL 1 DAY)
),
trading_days AS (
  SELECT d FROM date_range WHERE DAYOFWEEK(d) NOT IN (1, 7)
),
stocks AS (
  SELECT 'KOSPI'  AS mkt, '005930' AS code, DATE_SUB(CURDATE(), INTERVAL 2  YEAR)  AS start_d UNION ALL
  SELECT 'KOSPI',  '000660',                DATE_SUB(CURDATE(), INTERVAL 10 MONTH)            UNION ALL
  SELECT 'KOSPI',  '035420',                DATE_SUB(CURDATE(), INTERVAL 10 MONTH)            UNION ALL
  SELECT 'KOSDAQ', '293490',                DATE_SUB(CURDATE(), INTERVAL 10 MONTH)            UNION ALL
  SELECT 'KOSPI',  '005380',                DATE_SUB(CURDATE(), INTERVAL 10 MONTH)            UNION ALL
  SELECT 'KOSDAQ', '086520',                DATE_SUB(CURDATE(), INTERVAL 10 MONTH)            UNION ALL
  SELECT 'KOSPI',  '373220',                DATE_SUB(CURDATE(), INTERVAL 2  YEAR)             UNION ALL
  SELECT 'KOSDAQ', '247540',                DATE_SUB(CURDATE(), INTERVAL 8  MONTH)            UNION ALL
  SELECT 'KOSDAQ', '999901',                DATE_SUB(CURDATE(), INTERVAL 4  MONTH)            UNION ALL
  SELECT 'KOSPI',  '999902',                DATE_SUB(CURDATE(), INTERVAL 3  MONTH)            UNION ALL
  SELECT 'KOSPI',  '999903',                DATE_SUB(CURDATE(), INTERVAL 8  MONTH)
)
SELECT s.mkt, s.code, td.d
FROM stocks s
JOIN trading_days td ON td.d >= s.start_d;

-- ─────────────────────────────────────────
-- 4. 일별 주가
--
--  공식: base × (1 + DATEDIFF × drift) × (1 + amp × SIN(DATEDIFF × freq + phase)) × noise
--
--  drift : 캘린더일당 추세 변화율
--  amp   : 사인파 진폭 (음수면 V형, 양수면 ∩형 또는 단순 파동)
--  freq  : 사인파 주파수 (π/300 ≈ 0.01047 → 300일에 반 주기, 즉 V/∩ 형태)
--  phase : 사인파 초기 위상
--
--  패턴별 설계:
--   삼성전자   drift=+0.00025  amp=0.10  freq=0.07   → 10개월 추세 +7.5% + 단기 파동
--   SK하이닉스 drift=+0.00015  amp=-0.32 freq=0.01047 → V형 (바닥 -30%, 회복 +36k)
--   NAVER      drift≈0         amp=0.14  freq=0.105  → 60일 주기 박스권 ±14%
--   카카오게임즈drift=-0.00030  amp=+0.35 freq=0.01047 → ∩형 (피크 +24.5k, 이후 하락)
--   에코프로비엠drift=-0.00090  amp=0.10  freq=0.07   → 8개월 -22% 하락 추세
--   딥테크에이아이 drift=+0.00200 amp=+0.25 freq=0.02618 → 2개월 +40% 급등 후 -12% 조정
--   미래모빌리티  drift=+0.00150 amp=0.10  freq=0.14   → 3개월 +14% 꾸준 상승
--   현대차     drift=+0.00040  amp=0.11  freq=0.07   → 완만한 상승 (정지)
--   에코프로   drift=+0.00190  amp=0.25  freq=0.038  → 강한 상승 (정지)
-- ─────────────────────────────────────────
INSERT INTO DAILY_STOCK_PRICE (MARKET_TYPE, STOCK_CODE, TRADE_DATE, VOLUME, OPEN_PRICE, CLOSE_PRICE, LOW_PRICE, HIGH_PRICE)
WITH RECURSIVE date_range AS (
  SELECT DATE_SUB(CURDATE(), INTERVAL 2 YEAR) AS d
  UNION ALL
  SELECT DATE_ADD(d, INTERVAL 1 DAY) FROM date_range WHERE d < DATE_SUB(CURDATE(), INTERVAL 1 DAY)
),
trading_days AS (
  SELECT d FROM date_range WHERE DAYOFWEEK(d) NOT IN (1, 7)
),
stocks AS (
  SELECT
    'KOSPI' AS mkt, '005930' AS code,
    60000   AS base,
    DATE_SUB(CURDATE(), INTERVAL 2  YEAR)  AS start_date,
    DATE_SUB(CURDATE(), INTERVAL 1  DAY)   AS end_date,
    0.00025 AS drift, 0.10 AS amp, 0.07    AS freq, 0.0 AS phase,
    3000000 AS vol_base

  UNION ALL SELECT 'KOSPI', '000660', 130000,
    DATE_SUB(CURDATE(), INTERVAL 10 MONTH),
    DATE_SUB(CURDATE(), INTERVAL 1  DAY),
    0.00015, -0.32, 0.01047, 0.0, 1500000

  UNION ALL SELECT 'KOSPI', '035420', 195000,
    DATE_SUB(CURDATE(), INTERVAL 10 MONTH),
    DATE_SUB(CURDATE(), INTERVAL 1  DAY),
    0.00005, 0.14, 0.105, 0.0, 500000

  UNION ALL SELECT 'KOSDAQ', '293490', 19000,
    DATE_SUB(CURDATE(), INTERVAL 10 MONTH),
    DATE_SUB(CURDATE(), INTERVAL 1  DAY),
    -0.00030, 0.35, 0.01047, 0.0, 2000000

  UNION ALL SELECT 'KOSPI', '005380', 220000,
    DATE_SUB(CURDATE(), INTERVAL 10 MONTH),
    DATE_SUB(CURDATE(), INTERVAL 4  MONTH),
    0.00040, 0.11, 0.07, 1.1, 800000

  UNION ALL SELECT 'KOSDAQ', '086520', 72000,
    DATE_SUB(CURDATE(), INTERVAL 10 MONTH),
    DATE_SUB(CURDATE(), INTERVAL 6  MONTH),
    0.00190, 0.25, 0.038, 0.4, 1200000

  UNION ALL SELECT 'KOSDAQ', '247540', 95000,
    DATE_SUB(CURDATE(), INTERVAL 8  MONTH),
    DATE_SUB(CURDATE(), INTERVAL 1  DAY),
    -0.00090, 0.10, 0.07, 0.0, 700000

  UNION ALL SELECT 'KOSDAQ', '999901', 10000,
    DATE_SUB(CURDATE(), INTERVAL 4  MONTH),
    DATE_SUB(CURDATE(), INTERVAL 1  DAY),
    0.00200, 0.25, 0.02618, 0.0, 5000000

  UNION ALL SELECT 'KOSPI', '999902', 45000,
    DATE_SUB(CURDATE(), INTERVAL 3  MONTH),
    DATE_SUB(CURDATE(), INTERVAL 1  DAY),
    0.00150, 0.10, 0.14, 0.0, 1000000

  -- 한일정밀: 8~5개월 전 거래 → 5~2개월 전 정지(데이터 없음) → 2개월 전 재개
  UNION ALL SELECT 'KOSPI', '999903', 42000,
    DATE_SUB(CURDATE(), INTERVAL 8  MONTH),
    DATE_SUB(CURDATE(), INTERVAL 5  MONTH),
    0.00020, 0.12, 0.07, 0.0, 350000

  UNION ALL SELECT 'KOSPI', '999903', 36000,
    DATE_SUB(CURDATE(), INTERVAL 2  MONTH),
    DATE_SUB(CURDATE(), INTERVAL 1  DAY),
    -0.00010, 0.09, 0.11, 1.5, 280000
),
raw AS (
  SELECT
    s.mkt, s.code, td.d,
    -- 종가
    GREATEST(100, ROUND(
      s.base
      * (1.0 + DATEDIFF(td.d, s.start_date) * s.drift)
      * (1.0 + s.amp * SIN(DATEDIFF(td.d, s.start_date) * s.freq + s.phase))
      * (1.0 + (CAST(CRC32(CONCAT(s.code, td.d, 'c')) AS SIGNED) % 61 - 30) * 0.001)
    , -2)) AS cp,
    -- 시가 (종가와 약간 다른 위상)
    GREATEST(100, ROUND(
      s.base
      * (1.0 + DATEDIFF(td.d, s.start_date) * s.drift)
      * (1.0 + s.amp * SIN(DATEDIFF(td.d, s.start_date) * s.freq + s.phase + 0.2))
      * (1.0 + (CAST(CRC32(CONCAT(s.code, td.d, 'o')) AS SIGNED) % 61 - 30) * 0.001)
    , -2)) AS op,
    -- 거래량 (종목별 기준량 + 랜덤)
    s.vol_base + (CRC32(CONCAT(s.code, td.d, 'v')) % (s.vol_base * 3)) AS vol
  FROM stocks s
  JOIN trading_days td ON td.d >= s.start_date AND td.d <= s.end_date
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
  ('KOSPI',  DATE_SUB(CURDATE(), INTERVAL 1 DAY)),
  ('KOSDAQ', DATE_SUB(CURDATE(), INTERVAL 1 DAY)),
  ('KONEX',  DATE_SUB(CURDATE(), INTERVAL 1 DAY));
