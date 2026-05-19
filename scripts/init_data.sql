-- init_data.sql
-- 로컬 개발용 사용자 시드 데이터
-- 실행 전제: init.sql 이 먼저 실행되어 스키마가 생성되어 있어야 함
-- 비밀번호: 모든 계정 공통 "1234"
-- BCrypt hash (strength=10) for "1234":
--   $2a$10$ozV6pKNkhik1CIzh24.LoeKFxUcePT0wkTzJeh9dKKMsx544dUxq2
--
-- | username | name   | role  |
-- |----------|--------|-------|
-- | admin    | Admin  | ROOT  |
-- | manager  | 관리자 | ADMIN |
-- | alice    | Alice  | USER  |
-- | bob      | Bob    | USER  |
-- | charlie  | Charlie| USER  |
--
-- ※ AdminInitializer(init.admin.username/password 환경변수)도 반드시 1234로 맞출 것.
--   앱 재시작 시 환경변수 비밀번호가 다르면 admin 해시를 덮어씀.

SET NAMES utf8mb4;

INSERT IGNORE INTO MEMBER (EMAIL, NAME, ROLE, CREATED_AT)
VALUES
    ('admin@dove.local',   'Admin',  'ROOT',  NOW()),
    ('manager@dove.local', '관리자', 'ADMIN', NOW()),
    ('alice@dove.local',   'Alice',  'USER',  NOW()),
    ('bob@dove.local',     'Bob',    'USER',  NOW()),
    ('charlie@dove.local', 'Charlie','USER',  NOW());

INSERT IGNORE INTO CREDENTIAL (MEMBER_ID, USERNAME, PASSWORD_HASH, FAILED_ATTEMPTS)
SELECT ID, 'admin', '$2a$10$ozV6pKNkhik1CIzh24.LoeKFxUcePT0wkTzJeh9dKKMsx544dUxq2', 0
FROM MEMBER WHERE EMAIL = 'admin@dove.local';

INSERT IGNORE INTO CREDENTIAL (MEMBER_ID, USERNAME, PASSWORD_HASH, FAILED_ATTEMPTS)
SELECT ID, 'manager', '$2a$10$ozV6pKNkhik1CIzh24.LoeKFxUcePT0wkTzJeh9dKKMsx544dUxq2', 0
FROM MEMBER WHERE EMAIL = 'manager@dove.local';

INSERT IGNORE INTO CREDENTIAL (MEMBER_ID, USERNAME, PASSWORD_HASH, FAILED_ATTEMPTS)
SELECT ID, 'alice', '$2a$10$ozV6pKNkhik1CIzh24.LoeKFxUcePT0wkTzJeh9dKKMsx544dUxq2', 0
FROM MEMBER WHERE EMAIL = 'alice@dove.local';

INSERT IGNORE INTO CREDENTIAL (MEMBER_ID, USERNAME, PASSWORD_HASH, FAILED_ATTEMPTS)
SELECT ID, 'bob', '$2a$10$ozV6pKNkhik1CIzh24.LoeKFxUcePT0wkTzJeh9dKKMsx544dUxq2', 0
FROM MEMBER WHERE EMAIL = 'bob@dove.local';

INSERT IGNORE INTO CREDENTIAL (MEMBER_ID, USERNAME, PASSWORD_HASH, FAILED_ATTEMPTS)
SELECT ID, 'charlie', '$2a$10$ozV6pKNkhik1CIzh24.LoeKFxUcePT0wkTzJeh9dKKMsx544dUxq2', 0
FROM MEMBER WHERE EMAIL = 'charlie@dove.local';
