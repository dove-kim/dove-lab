package com.dove.krx.quota;

/**
 * 로컬 카운트가 일일 한도에 도달해 호출을 차단함.
 */
public class KrxDailyQuotaExceededException extends RuntimeException {
    public KrxDailyQuotaExceededException() {
        super("KRX_DAILY_QUOTA_EXCEEDED");
    }
}
