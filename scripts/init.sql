-- ============================================================================
-- 데이터베이스 초기화 스크립트
--
-- 단일 스키마 'DOVE_LAB' 를 사용한다.
-- 스키마 생성 및 사용자 권한 부여는 docker-compose 환경변수
-- (MYSQL_DATABASE, MYSQL_USER, MYSQL_PASSWORD) 가 자동 처리한다.
--
-- 운영 환경 최초 설정은 아래 SQL 을 root 로 실행한다:
--   CREATE SCHEMA IF NOT EXISTS DOVE_LAB
--     CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
--   CREATE USER IF NOT EXISTS 'dove_app'@'%' IDENTIFIED BY '<strong-password>';
--   GRANT SELECT, INSERT, UPDATE, DELETE ON DOVE_LAB.*            TO 'dove_app'@'%';
--   GRANT CREATE, ALTER, DROP, INDEX, REFERENCES ON DOVE_LAB.*   TO 'dove_app'@'%';
--   FLUSH PRIVILEGES;
-- ============================================================================

SET NAMES utf8mb4;

-- ============================================================================
-- 테이블 초기화 (의존 순서 역순)
-- ============================================================================

DROP TABLE IF EXISTS USER_FEATURE_DISPLAY;
DROP TABLE IF EXISTS USER_MODULE_DISPLAY;
DROP TABLE IF EXISTS USER_SUB_MENU_GRANT;
DROP TABLE IF EXISTS USER_FEATURE_GRANT;
DROP TABLE IF EXISTS CREDENTIAL;
DROP TABLE IF EXISTS INVITE_CODE;
DROP TABLE IF EXISTS MEMBER;
DROP TABLE IF EXISTS MARKET_TRADING_DATE;
DROP TABLE IF EXISTS DAILY_STOCK_PRICE;
DROP TABLE IF EXISTS TECHNICAL_INDICATOR;
DROP TABLE IF EXISTS STOCK;
DROP TABLE IF EXISTS SEARCH_FILTER;
DROP TABLE IF EXISTS STOCK_SET;
DROP TABLE IF EXISTS INDICATOR_PRESET;

-- ============================================================================
-- 주식 도메인
-- ============================================================================


CREATE TABLE STOCK (
    MARKET_TYPE  VARCHAR(10)  NOT NULL COMMENT '시장 타입',
    CODE         VARCHAR(20)  NOT NULL COMMENT '종목 코드 (단축코드/티커, 예: 005930)',
    NAME         VARCHAR(100) NOT NULL COMMENT '한글 종목명',
    ISIN_CODE    VARCHAR(12)           COMMENT 'ISIN 코드 (KR + 종류코드 1자 + 티커 6자 + 체크디짓 3자, 예: KR7005930003)',
    LISTING_DATE DATE         NOT NULL COMMENT '최초 상장일 (KRX LIST_DD 기준)',
    CREATED_AT   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'DB 등록 일시',
    PRIMARY KEY (MARKET_TYPE, CODE),
    INDEX IDX_STOCK_ISIN_CODE (ISIN_CODE)
) COMMENT='종목 마스터';

CREATE TABLE DAILY_STOCK_PRICE (
    MARKET_TYPE VARCHAR(10) NOT NULL  COMMENT '시장 타입',
    STOCK_CODE  VARCHAR(20) NOT NULL  COMMENT '종목 코드',
    TRADE_DATE  DATE        NOT NULL  COMMENT '거래일',
    VOLUME      BIGINT               COMMENT '거래량 (거래정지 시 NULL)',
    OPEN_PRICE  BIGINT               COMMENT '시가 (거래정지 시 NULL)',
    CLOSE_PRICE BIGINT      NOT NULL  COMMENT '종가',
    LOW_PRICE   BIGINT               COMMENT '저가 (거래정지 시 NULL)',
    HIGH_PRICE  BIGINT               COMMENT '고가 (거래정지 시 NULL)',
    PRIMARY KEY (MARKET_TYPE, STOCK_CODE, TRADE_DATE),
    INDEX IDX_DAILY_STOCK_PRICE_MARKET_TRADE_DATE (MARKET_TYPE, TRADE_DATE)
) COMMENT='일별 주가';


CREATE TABLE MARKET_TRADING_DATE (
    MARKET_TYPE VARCHAR(10) NOT NULL COMMENT '시장 타입',
    TRADE_DATE  DATE        NOT NULL COMMENT '거래일',
    IS_OPEN     BOOLEAN     NOT NULL COMMENT '개장 여부',
    PRIMARY KEY (MARKET_TYPE, TRADE_DATE)
) COMMENT='시장별 개장/휴장 날짜';

CREATE TABLE TECHNICAL_INDICATOR (
    MARKET_TYPE     VARCHAR(10) NOT NULL COMMENT '시장 타입',
    STOCK_CODE      VARCHAR(20) NOT NULL COMMENT '종목 코드',
    TRADE_DATE      DATE        NOT NULL COMMENT '거래일',
    INDICATOR_NAME  VARCHAR(30) NOT NULL COMMENT '지표명',
    INDICATOR_VALUE DOUBLE      NOT NULL COMMENT '지표값',
    CALCULATED_AT   DATETIME    NOT NULL COMMENT '계산 일시',
    PRIMARY KEY (MARKET_TYPE, STOCK_CODE, TRADE_DATE, INDICATOR_NAME),
    INDEX IDX_TECHNICAL_INDICATOR_CALCULATED_AT (CALCULATED_AT),
    INDEX IDX_TI_MARKET_INDICATOR_DATE          (MARKET_TYPE, INDICATOR_NAME, TRADE_DATE),
    INDEX IDX_TI_MARKET_DATE                    (MARKET_TYPE, TRADE_DATE)
) COMMENT='기술적 지표 계산 결과';

CREATE TABLE STOCK_SET (
    ID         BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '고유 ID',
    MEMBER_ID  BIGINT       NOT NULL COMMENT '소유 회원 ID',
    NAME       VARCHAR(100) NOT NULL COMMENT '세트명',
    CODES      TEXT         NOT NULL COMMENT '종목 코드 목록 (JSON)',
    CREATED_AT DATETIME     NOT NULL COMMENT '생성 일시',
    UPDATED_AT DATETIME     NOT NULL COMMENT '수정 일시',
    UNIQUE KEY UK_STOCK_SET_MEMBER_NAME (MEMBER_ID, NAME),
    INDEX IDX_STOCK_SET_MEMBER_ID (MEMBER_ID)
) COMMENT='사용자 정의 종목 세트';

CREATE TABLE SEARCH_FILTER (
    ID                   BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '고유 ID',
    MEMBER_ID            BIGINT       NOT NULL COMMENT '소유 회원 ID',
    NAME                 VARCHAR(100) NOT NULL COMMENT '필터명',
    DATE_RULE            VARCHAR(20)  NOT NULL DEFAULT 'LATEST' COMMENT '날짜 기준',
    MARKETS              VARCHAR(50)  NOT NULL DEFAULT 'KOSPI,KOSDAQ,KONEX' COMMENT '대상 시장',
    EXPRESSION           TEXT         NOT NULL COMMENT '필터 표현식 (JSON)',
    INCLUDE_STOCK_SET_ID BIGINT                COMMENT '포함 종목 세트 ID',
    EXCLUDE_STOCK_SET_ID BIGINT                COMMENT '제외 종목 세트 ID',
    DISPLAY_ORDER        INT          NOT NULL DEFAULT 0 COMMENT '표시 순서',
    CREATED_AT           DATETIME     NOT NULL COMMENT '생성 일시',
    UPDATED_AT           DATETIME     NOT NULL COMMENT '수정 일시',
    UNIQUE KEY UK_SEARCH_FILTER_MEMBER_NAME (MEMBER_ID, NAME),
    INDEX IDX_SEARCH_FILTER_MEMBER_ID (MEMBER_ID)
) COMMENT='사용자 정의 검색 필터';

CREATE TABLE INDICATOR_PRESET (
    ID            BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '고유 ID',
    MEMBER_ID     BIGINT       NOT NULL COMMENT '소유 회원 ID',
    NAME          VARCHAR(100) NOT NULL COMMENT '프리셋명',
    ITEMS         TEXT         NOT NULL COMMENT '지표 항목 목록 (JSON)',
    PANEL_ORDER   VARCHAR(300)          COMMENT '패널 순서',
    DISPLAY_ORDER INT          NOT NULL DEFAULT 0 COMMENT '표시 순서',
    CREATED_AT    DATETIME     NOT NULL COMMENT '생성 일시',
    UPDATED_AT    DATETIME     NOT NULL COMMENT '수정 일시',
    UNIQUE KEY UK_INDICATOR_PRESET_MEMBER_NAME (MEMBER_ID, NAME),
    INDEX IDX_INDICATOR_PRESET_MEMBER_ID (MEMBER_ID)
) COMMENT='지표 프리셋';

-- ============================================================================
-- 사용자 도메인
-- ============================================================================

CREATE TABLE MEMBER (
    ID         BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '회원 고유 ID',
    EMAIL      VARCHAR(100) NOT NULL COMMENT '이메일 (식별자)',
    NAME       VARCHAR(50)  NOT NULL COMMENT '표시명',
    ROLE       VARCHAR(20)  NOT NULL COMMENT '권한 (USER/ADMIN/ROOT)',
    CREATED_AT DATETIME     NOT NULL COMMENT '가입 일시',
    UNIQUE KEY UK_MEMBER_EMAIL (EMAIL)
) COMMENT='회원 신원/프로필';

-- ============================================================================
-- 인증 도메인
-- ============================================================================

CREATE TABLE CREDENTIAL (
    MEMBER_ID       BIGINT       NOT NULL PRIMARY KEY COMMENT '회원 ID',
    USERNAME        VARCHAR(50)  NOT NULL COMMENT '로그인 식별자',
    PASSWORD_HASH   VARCHAR(255) NOT NULL COMMENT 'BCrypt 해시',
    LAST_LOGIN_AT   DATETIME              COMMENT '마지막 성공 로그인 시각',
    FAILED_ATTEMPTS INT          NOT NULL DEFAULT 0 COMMENT '연속 로그인 실패 횟수',
    LOCKED_UNTIL            DATETIME              COMMENT '계정 잠금 해제 시각',
    PASSWORD_RESET_REQUIRED TINYINT(1) NOT NULL DEFAULT 0 COMMENT '비밀번호 초기화 필요 여부 (1=다음 로그인 시 변경 강제)',
    UNIQUE KEY UK_CREDENTIAL_USERNAME (USERNAME)
) COMMENT='회원 자격증명 — 비밀번호 해시 격리';

CREATE TABLE INVITE_CODE (
    ID         BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '초대 코드 고유 ID',
    VERSION    BIGINT       NOT NULL DEFAULT 0 COMMENT '낙관적 락 버전',
    CODE       VARCHAR(64)  NOT NULL COMMENT '초대 코드 값 (UUID 기반)',
    ROLE       VARCHAR(20)  NOT NULL COMMENT '발급 대상 역할 (USER/ADMIN)',
    EXPIRES_AT DATETIME     NOT NULL COMMENT '만료 일시',
    USED_AT    DATETIME              COMMENT '사용 일시',
    CREATED_BY VARCHAR(50)  NOT NULL COMMENT '발급자 username',
    CREATED_AT DATETIME     NOT NULL COMMENT '발급 일시',
    UNIQUE KEY UK_INVITE_CODE (CODE),
    INDEX IDX_INVITE_CODE_EXPIRES_AT (EXPIRES_AT)
) COMMENT='초대 코드 — 사전 승인 기반 회원가입';

-- ============================================================================
-- 사용자 기능 권한 및 메뉴 표시 설정
-- ============================================================================

CREATE TABLE USER_FEATURE_GRANT (
    ID           BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '기능 부여 고유 ID',
    USER_ID      BIGINT       NOT NULL COMMENT '회원 ID',
    FEATURE_CODE VARCHAR(50)  NOT NULL COMMENT '기능 코드',
    ACTIVE       TINYINT(1)   NOT NULL COMMENT '활성 여부 (0=회수됨)',
    GRANTED_AT   DATETIME     NOT NULL COMMENT '최초 부여 일시',
    GRANTED_BY   BIGINT                COMMENT '부여한 관리자 회원 ID (NULL=시스템 자동)',
    UPDATED_AT   DATETIME     NOT NULL COMMENT '마지막 상태 변경 일시',
    UNIQUE KEY UK_USER_FEATURE_GRANT (USER_ID, FEATURE_CODE),
    INDEX IDX_USER_FEATURE_GRANT_USER_ID (USER_ID)
) COMMENT='사용자 기능 부여 기록';

CREATE TABLE USER_SUB_MENU_GRANT (
    ID            BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '하위 메뉴 부여 고유 ID',
    USER_ID       BIGINT      NOT NULL COMMENT '회원 ID',
    SUB_MENU_CODE VARCHAR(50) NOT NULL COMMENT '하위 메뉴 코드',
    ACTIVE        TINYINT(1)  NOT NULL COMMENT '활성 여부 (0=회수됨)',
    GRANTED_AT    DATETIME    NOT NULL COMMENT '최초 부여 일시',
    GRANTED_BY    BIGINT               COMMENT '부여한 관리자 회원 ID (NULL=기능 부여 시 자동)',
    UPDATED_AT    DATETIME    NOT NULL COMMENT '마지막 상태 변경 일시',
    UNIQUE KEY UK_USER_SUB_MENU_GRANT (USER_ID, SUB_MENU_CODE),
    INDEX IDX_USER_SUB_MENU_GRANT_USER_ID (USER_ID)
) COMMENT='사용자 하위 메뉴 부여 기록';

CREATE TABLE USER_MODULE_DISPLAY (
    ID            BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '모듈 표시 설정 고유 ID',
    USER_ID       BIGINT      NOT NULL COMMENT '회원 ID',
    MODULE_CODE   VARCHAR(50) NOT NULL COMMENT '모듈 코드',
    DISPLAY_ORDER INT         NOT NULL COMMENT '메뉴 표시 순서',
    HIDDEN        TINYINT(1)  NOT NULL DEFAULT 0 COMMENT '숨김 여부',
    UNIQUE KEY UK_USER_MODULE_DISPLAY (USER_ID, MODULE_CODE),
    INDEX IDX_USER_MODULE_DISPLAY_USER_ID (USER_ID)
) COMMENT='사용자 모듈 메뉴 표시 설정';

CREATE TABLE USER_FEATURE_DISPLAY (
    ID            BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '기능 표시 설정 고유 ID',
    USER_ID       BIGINT      NOT NULL COMMENT '회원 ID',
    FEATURE_CODE  VARCHAR(50) NOT NULL COMMENT '기능 코드',
    DISPLAY_ORDER INT         NOT NULL COMMENT '모듈 내 표시 순서',
    HIDDEN        TINYINT(1)  NOT NULL DEFAULT 0 COMMENT '숨김 여부',
    UNIQUE KEY UK_USER_FEATURE_DISPLAY (USER_ID, FEATURE_CODE),
    INDEX IDX_USER_FEATURE_DISPLAY_USER_ID (USER_ID)
) COMMENT='사용자 기능 메뉴 표시 설정';
