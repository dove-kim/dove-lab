package com.dove.krxcalllog.domain.enums;

/** KRX API 호출 결과 상태. 성공/인증실패/API실패/응답파싱오류 등을 구분한다. */
public enum KrxDailyDataStatus {
    SUCCESS,
    API_AUTH_FAILED,
    API_FAILED,
    BODY_NULL,
    BODY_ERROR,
    UNSUPPORTED_MARKET_TYPE
}
