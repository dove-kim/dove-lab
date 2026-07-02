package com.dove.fundamental.domain.enums;

/**
 * DART 정기보고서 코드.
 */
public enum ReportCode {

    Q1("11013"),
    HALF("11012"),
    Q3("11014"),
    ANNUAL("11011");

    private final String code;

    ReportCode(String code) {
        this.code = code;
    }

    /**
     * DART API 보고서 코드 문자열.
     */
    public String code() {
        return code;
    }

    /**
     * DART 코드 문자열에 대응하는 보고서 종류를 반환한다.
     *
     * @throws IllegalArgumentException 알 수 없는 코드인 경우
     */
    public static ReportCode fromCode(String code) {
        for (ReportCode rc : values()) {
            if (rc.code.equals(code)) {
                return rc;
            }
        }
        throw new IllegalArgumentException("UNKNOWN_REPORT_CODE: " + code);
    }
}
