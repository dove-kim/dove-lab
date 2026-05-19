package com.dove.screening.domain.enums;

public enum DateRule {
    LATEST,        // 데이터가 있는 가장 최신 거래일 자동 선택
    SPECIFIC_DATE, // 직접 날짜 지정
    PREV_1D,       // 기준일 이전 1 거래일
    PREV_3D, // 기준일 이전 3 거래일
    PREV_5D, // 기준일 이전 5 거래일
    PREV_10D // 기준일 이전 10 거래일
}
