package com.dove.kis.quota;

import feign.FeignException;

/**
 * KIS API 오류 코드와 재시도 가능 여부 판정.
 */
public final class KisErrorCodes {

    /** 초당 호출 한도 초과. */
    public static final String RATE_LIMIT = "EGW00201";
    /** 일시 게이트웨이 오류. */
    public static final String TRANSIENT_GATEWAY = "EGW00316";

    private KisErrorCodes() {
    }

    /**
     * 재시도하면 회복 가능한 KIS 일시 오류(초당 한도·게이트웨이)인지 여부.
     */
    public static boolean isTransient(FeignException e) {
        if (e.status() != 500) return false;
        String body = e.contentUTF8();
        return body.contains(RATE_LIMIT) || body.contains(TRANSIENT_GATEWAY);
    }
}
