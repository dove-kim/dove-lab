package com.dove.krx;

public enum DayStatus {
    /** 개장 — listings·prices 모두 채워짐 */
    OPEN,
    /** 휴장 확정 — 구간 내 다른 날짜의 주가로 역추론 */
    CLOSED,
    /** 미확정 — T+1 정책으로 아직 데이터 미제공 또는 오류. 커서 미전진 */
    UNCONFIRMED
}
