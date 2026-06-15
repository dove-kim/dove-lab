package com.dove.apiquota;

import java.time.Duration;

/**
 * API 호출 횟수 제한기.
 * 슬롯을 획득할 때까지 제한 시간 안에서 블로킹한다 (가상 스레드에서 호출하는 것을 전제로 한다).
 */
public interface RateLimiter {

    /**
     * 제한 시간 안에 호출 슬롯을 획득한다.
     *
     * @param timeout 최대 대기 시간
     * @return 시간 내 획득에 성공하면 true, 시간 초과면 false
     * @throws InterruptedException 대기 중 인터럽트 발생 시
     */
    boolean tryAcquire(Duration timeout) throws InterruptedException;
}
