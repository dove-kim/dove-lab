package com.dove.stockcollection.application.service;

import java.util.function.IntConsumer;

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

    /**
     * total을 캡처하면서 delegate로 위임하는 래퍼를 반환한다.
     * 다음 단계의 offset 계산에 사용한다.
     *
     * @param delegate        실제 진행률을 기록할 대상
     * @param onTotalCapture  캡처된 total을 받을 콜백
     */
    static CollectionProgress capturing(CollectionProgress delegate, IntConsumer onTotalCapture) {
        return new CollectionProgress() {
            public void onTotal(int total) { onTotalCapture.accept(total); delegate.onTotal(total); }
            public void onProgress(int done) { delegate.onProgress(done); }
        };
    }

    /**
     * done·total에 offset을 더해 delegate로 위임하는 래퍼를 반환한다.
     * 2단계 작업의 진행률을 이어서 표시할 때 사용한다.
     *
     * @param delegate 실제 진행률을 기록할 대상
     * @param offset   이전 단계에서 완료된 작업 수
     */
    static CollectionProgress offset(CollectionProgress delegate, int offset) {
        return new CollectionProgress() {
            public void onTotal(int total) { delegate.onTotal(offset + total); }
            public void onProgress(int done) { delegate.onProgress(offset + done); }
        };
    }
}
