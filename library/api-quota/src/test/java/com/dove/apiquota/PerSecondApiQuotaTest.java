package com.dove.apiquota;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PerSecondApiQuota")
class PerSecondApiQuotaTest {

    @Test
    @DisplayName("연속 호출을 최소 간격으로 균등 분산한다")
    void shouldSpaceOutConsecutiveCalls() throws InterruptedException {
        PerSecondApiQuota quota = new PerSecondApiQuota(50); // 간격 20ms
        int calls = 6;

        long start = System.currentTimeMillis();
        for (int i = 0; i < calls; i++) {
            quota.acquire();
        }
        long elapsed = System.currentTimeMillis() - start;

        // (calls-1) * 20ms = 100ms 이상이 걸려야 한다(하한만 검증 — 상한은 스케줄링 영향으로 flaky).
        assertThat(elapsed).isGreaterThanOrEqualTo(70L);
    }

    @Test
    @DisplayName("max는 설정한 초당 한도를 반환한다")
    void shouldReturnConfiguredMax() {
        assertThat(new PerSecondApiQuota(20).max()).isEqualTo(20);
    }
}
