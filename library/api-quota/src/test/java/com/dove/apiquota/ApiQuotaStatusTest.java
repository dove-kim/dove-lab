package com.dove.apiquota;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ApiQuotaStatus")
class ApiQuotaStatusTest {

    private ApiQuotaStatus status(int used, int limit) {
        return new ApiQuotaStatus("KIS", QuotaType.DAILY, used, limit, null);
    }

    @Test
    @DisplayName("remaining은 limit - used를 반환한다")
    void shouldReturnRemaining() {
        assertThat(status(30, 100).remaining()).isEqualTo(70);
    }

    @Test
    @DisplayName("used가 limit 이상이면 remaining은 0 (음수 아님)")
    void shouldClampRemainingToZero() {
        assertThat(status(100, 100).remaining()).isZero();
        assertThat(status(150, 100).remaining()).isZero();
    }
}
