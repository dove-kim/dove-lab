package com.dove.apiquota;

import java.util.concurrent.locks.ReentrantLock;

/**
 * ReentrantLock 기반 단일 프로세스 초당 요청 수 제한기 (호출 간격을 균등 분산).
 */
public class PerSecondApiQuota {

    private final int maxPerSecond;
    private final long intervalMs;
    private final ReentrantLock lock = new ReentrantLock(true); // fair FIFO
    private long lastAcquireMs = 0;

    public PerSecondApiQuota(int maxPerSecond) {
        this.maxPerSecond = maxPerSecond;
        this.intervalMs = Math.max(1, 1000L / maxPerSecond);
    }

    /**
     * 요청 슬롯을 획득한다.
     *
     * @throws InterruptedException 대기 중 인터럽트 발생 시
     */
    public void acquire() throws InterruptedException {
        lock.lockInterruptibly();
        try {
            long now = System.currentTimeMillis();
            long nextAllowed = lastAcquireMs + intervalMs;
            if (nextAllowed > now) Thread.sleep(nextAllowed - now);
            // idle 기간 누적 방지: 현재 시각 vs 예약 시각 중 더 늦은 값으로 전진
            lastAcquireMs = Math.max(now, nextAllowed);
        } finally {
            lock.unlock();
        }
    }

    public int max() {
        return maxPerSecond;
    }
}
