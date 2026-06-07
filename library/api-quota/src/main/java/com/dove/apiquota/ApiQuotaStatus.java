package com.dove.apiquota;

/**
 * API 호출 한도 현황 스냅샷.
 *
 * <p>DAILY: used=오늘 호출 횟수, limit=일일 한도, lastLimitAt=마지막 서버 거부 시각(nullable).
 * <p>PER_SECOND: used=현재 사용 중인 슬롯, limit=초당 최대 동시 요청 수, lastLimitAt=null.
 */
public record ApiQuotaStatus(
        String name,
        QuotaType type,
        int used,
        int limit,
        String lastLimitAt
) {
    public int remaining() {
        return Math.max(0, limit - used);
    }
}
