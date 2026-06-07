package com.dove.stockcollection.application.service;

/**
 * 수집 진행 상황 리스너. 기록 방식은 구현이 결정한다.
 */
public interface CollectionProgress {

    /**
     * 전체 작업 수가 확정됐을 때 1회 호출.
     */
    void onTotal(int total);

    /**
     * 작업 1건 완료마다 호출 (현재까지 완료 수).
     */
    void onProgress(int done);

    /**
     * 수정주가 재조회 대상 수가 확정됐을 때 1회 호출. 기본 no-op.
     */
    default void onAdjustedTotal(int total) {}

    /**
     * 수정주가 재조회 1건 완료마다 호출 (현재까지 완료 수). 기본 no-op.
     */
    default void onAdjustedProgress(int done) {}

    /**
     * 진행 상황을 기록하지 않는 no-op 구현.
     */
    CollectionProgress NOOP = new CollectionProgress() {
        public void onTotal(int total) {}
        public void onProgress(int done) {}
    };
}
