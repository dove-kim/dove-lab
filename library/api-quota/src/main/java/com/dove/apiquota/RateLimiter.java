package com.dove.apiquota;

/**
 * API 호출 횟수 제한기.
 * 슬롯을 획득할 때까지 블로킹한다 (가상 스레드에서 호출하는 것을 전제로 한다).
 */
public interface RateLimiter {

    /**
     * 호출 슬롯을 획득한다.
     *
     * @throws InterruptedException 대기 중 인터럽트 발생 시
     */
    void acquire() throws InterruptedException;
}
