package com.dove.apiquota;

/**
 * API 호출 한도 현황을 제공하는 컴포넌트.
 */
public interface QuotaStatusProvider {

    /**
     * 현재 호출 한도 현황 스냅샷을 반환한다.
     */
    ApiQuotaStatus getStatus();
}
