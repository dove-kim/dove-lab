package com.dove.stockcollection.domain.enums;

/**
 * 수집 작업 상태.
 */
public enum CollectionStatus {
    PENDING,  // 접수됨, 아직 시작 전
    RUNNING,  // 진행 중
    DONE,     // 정상 완료
    FAILED    // 에러로 중단
}
