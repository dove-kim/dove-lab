-- INDICATOR_PRESET에 차트 오버레이(OVERLAY) 컬럼 추가 (운영 증분 배포용, 멱등)
-- information_schema 가드로 컬럼이 이미 있으면 ALTER를 건너뛴다.
SET @ddl := (
    SELECT IF(
        EXISTS(
            SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME   = 'INDICATOR_PRESET'
              AND COLUMN_NAME  = 'OVERLAY'
        ),
        'SELECT 1',
        'ALTER TABLE INDICATOR_PRESET ADD COLUMN OVERLAY JSON NULL COMMENT ''차트 오버레이 설정 (JSON: {signalModelId, signalThreshold, seriesMetricIds})'' AFTER PANEL_ORDER'
    )
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
