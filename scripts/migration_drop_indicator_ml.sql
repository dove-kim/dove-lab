-- INDICATOR_ML capability 폐기: 강제·게이팅에 쓰인 적 없는 죽은 권한.
-- Capability enum에서 제거하기 전에 실행해야 한다(남은 grant 행이 있으면 @Enumerated(STRING) 로딩 시
-- 'No enum constant' 예외로 grant 조회가 깨진다). 멱등 — 없으면 0행 삭제.
DELETE FROM MEMBER_CAPABILITY_GRANT WHERE CAPABILITY = 'INDICATOR_ML';
