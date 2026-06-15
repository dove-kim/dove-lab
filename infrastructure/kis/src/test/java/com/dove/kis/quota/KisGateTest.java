package com.dove.kis.quota;

import com.dove.apiquota.RateLimiter;
import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("KisGate")
class KisGateTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    @Mock
    private RateLimiter rateLimiter;

    private KisGate gate(int maxConcurrent, int maxRetries) {
        return new KisGate(rateLimiter, maxConcurrent, maxRetries, TIMEOUT);
    }

    private static Request request() {
        return Request.create(Request.HttpMethod.GET, "/x",
                Collections.emptyMap(), Request.Body.empty(), new RequestTemplate());
    }

    /** status 500 + 지정 본문을 가진 FeignException. */
    private static FeignException serverError(String body) {
        return new FeignException.InternalServerError(
                body, request(), body.getBytes(StandardCharsets.UTF_8), Collections.emptyMap());
    }

    @SuppressWarnings("unchecked")
    private static Supplier<String> mockCall() {
        return mock(Supplier.class);
    }

    @Test
    @DisplayName("정상 호출은 재시도 없이 결과를 반환한다")
    void shouldReturnWithoutRetry() throws InterruptedException {
        given(rateLimiter.tryAcquire(any())).willReturn(true);
        Supplier<String> call = mockCall();
        given(call.get()).willReturn("ok");

        assertThat(gate(4, 2).call(call)).isEqualTo("ok");
        verify(call, times(1)).get();
    }

    @Nested
    @DisplayName("일시오류 재시도")
    class TransientRetry {

        @Test
        @DisplayName("EGW00201(초당 한도)은 재시도 후 성공한다")
        void shouldRetryRateLimitThenSucceed() throws InterruptedException {
            given(rateLimiter.tryAcquire(any())).willReturn(true);
            Supplier<String> call = mockCall();
            given(call.get()).willThrow(serverError("err EGW00201 err")).willReturn("ok");

            assertThat(gate(4, 1).call(call)).isEqualTo("ok");
            verify(call, times(2)).get();
        }

        @Test
        @DisplayName("EGW00316(게이트웨이 일시오류)도 재시도한다")
        void shouldRetryGatewayTransientThenSucceed() throws InterruptedException {
            given(rateLimiter.tryAcquire(any())).willReturn(true);
            Supplier<String> call = mockCall();
            given(call.get()).willThrow(serverError("err EGW00316 err")).willReturn("ok");

            assertThat(gate(4, 1).call(call)).isEqualTo("ok");
            verify(call, times(2)).get();
        }

        @Test
        @DisplayName("재시도 한도를 넘으면 마지막 예외를 던진다")
        void shouldThrowAfterMaxRetries() throws InterruptedException {
            given(rateLimiter.tryAcquire(any())).willReturn(true);
            Supplier<String> call = mockCall();
            given(call.get()).willThrow(serverError("err EGW00201 err"));

            assertThatThrownBy(() -> gate(4, 1).call(call)).isInstanceOf(FeignException.class);
            verify(call, times(2)).get(); // 최초 + 1회 재시도
        }
    }

    @Test
    @DisplayName("일시오류가 아닌 오류는 재시도 없이 즉시 전파한다")
    void shouldNotRetryNonTransient() throws InterruptedException {
        given(rateLimiter.tryAcquire(any())).willReturn(true);
        Supplier<String> call = mockCall();
        given(call.get()).willThrow(serverError("err OTHER_CODE err"));

        assertThatThrownBy(() -> gate(4, 2).call(call)).isInstanceOf(FeignException.class);
        verify(call, times(1)).get();
    }

    @Nested
    @DisplayName("rate 슬롯 획득 타임아웃")
    class RateSlotTimeout {

        @Test
        @DisplayName("타임아웃 후 슬롯을 받으면 재시도해 성공한다")
        void shouldRetryThenSucceedWhenSlotEventuallyAcquired() throws InterruptedException {
            given(rateLimiter.tryAcquire(any())).willReturn(false).willReturn(true);
            Supplier<String> call = mockCall();
            given(call.get()).willReturn("ok");

            assertThat(gate(4, 1).call(call)).isEqualTo("ok");
            verify(call, times(1)).get(); // 슬롯 받은 뒤 1회만 호출
        }

        @Test
        @DisplayName("재시도 한도까지 슬롯을 못 받으면 호출 없이 예외를 던진다")
        void shouldThrowWithoutCallingWhenSlotNeverAcquired() throws InterruptedException {
            given(rateLimiter.tryAcquire(any())).willReturn(false);
            Supplier<String> call = mockCall();

            assertThatThrownBy(() -> gate(4, 1).call(call))
                    .isInstanceOf(KisRateLimitException.class);
            verify(call, never()).get(); // KIS를 한 번도 부르지 않음 (율제한 위반 없음)
        }
    }
}
