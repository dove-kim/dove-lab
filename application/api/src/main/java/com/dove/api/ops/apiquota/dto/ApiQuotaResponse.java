package com.dove.api.ops.apiquota.dto;

import com.dove.apiquota.ApiQuotaStatus;
import com.dove.apiquota.QuotaType;

import java.util.List;

/**
 * API 사용량(쿼터) 응답.
 *
 * @param quotas 쿼터 항목 목록
 */
public record ApiQuotaResponse(List<QuotaEntry> quotas) {

    /**
     * 쿼터 항목 한 건.
     *
     * @param name       쿼터 이름
     * @param type       쿼터 유형(DAILY/PER_SECOND)
     * @param used       사용량
     * @param limit      한도
     * @param remaining  잔여량
     * @param lastLimitAt 마지막 한도 도달 시각
     */
    public record QuotaEntry(
            String name,
            String type,
            int used,
            int limit,
            int remaining,
            String lastLimitAt
    ) {
        public static QuotaEntry from(ApiQuotaStatus s) {
            return new QuotaEntry(
                    s.name(),
                    s.type() == QuotaType.DAILY ? "DAILY" : "PER_SECOND",
                    s.used(),
                    s.limit(),
                    s.remaining(),
                    s.lastLimitAt()
            );
        }
    }
}
