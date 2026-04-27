package com.dove.krx.infrastructure.adapter;

import com.dove.krxcalllog.application.service.KrxDailyDataAuditor;
import com.dove.krxcalllog.domain.entity.KrxDailyData;
import com.dove.krxcalllog.domain.enums.KrxDailyDataStatus;
import com.dove.krxcalllog.domain.repository.KrxDailyDataRepository;
import com.dove.krx.infrastructure.client.KrxDailyPriceResponse;
import com.dove.krx.infrastructure.client.KrxStockClient;
import com.dove.stockprice.application.port.DailyPriceFetcher;
import com.dove.market.domain.enums.MarketType;
import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KrxDailyPriceFetcherTest {

    @Mock
    private KrxStockClient krxStockClient;
    @Mock
    private KrxDailyDataRepository krxDailyDataRepository;
    @Mock
    private KrxDailyDataAuditor krxDailyDataAuditor;

    private final LocalDate targetDate = LocalDate.of(2026, 4, 17);
    private final String testAuthKey = "test-api-key";
    private final ZoneId kst = ZoneId.of("Asia/Seoul");

    private KrxDailyPriceFetcher provider;

    private void setupProvider(Instant now) {
        Clock clock = Clock.fixed(now, kst);
        provider = new KrxDailyPriceFetcher(krxStockClient, krxDailyDataRepository, krxDailyDataAuditor, clock);
        ReflectionTestUtils.setField(provider, "krxApiAuthKey", testAuthKey);
    }

    @BeforeEach
    void setUp() {
        // Default: well past confirmation time (2026-04-19 09:00 KST)
        setupProvider(LocalDate.of(2026, 4, 25).atStartOfDay(kst).toInstant());
    }

    private KrxDailyPriceResponse successResponse() {
        KrxDailyPriceResponse.Data data = new KrxDailyPriceResponse.Data(
                "20260417", "004560", "BNG스틸", "KOSPI", "-",
                "8910", "0", "0", "8660", "8910", "8650",
                "138442", "0", "0", "0"
        );
        return new KrxDailyPriceResponse(List.of(data));
    }

    private KrxDailyPriceResponse malformedResponse() {
        KrxDailyPriceResponse.Data data = new KrxDailyPriceResponse.Data(
                "20260417", "004560", "BNG스틸", "KOSPI", "-",
                "8910", "NOT_A_NUMBER", "NOT_A_NUMBER", "NOT_A_NUMBER", "NOT_A_NUMBER", "NOT_A_NUMBER",
                "NOT_A_NUMBER", "NOT_A_NUMBER", "NOT_A_NUMBER", "NOT_A_NUMBER"
        );
        return new KrxDailyPriceResponse(List.of(data));
    }

    private FeignException.Unauthorized buildUnauthorized() {
        return new FeignException.Unauthorized("Unauthorized",
                Request.create(Request.HttpMethod.GET, "/test",
                        Collections.emptyMap(), Request.Body.empty(), new RequestTemplate()),
                new byte[0], Collections.emptyMap());
    }

    private FeignException.ServiceUnavailable buildServiceUnavailable() {
        return new FeignException.ServiceUnavailable("Service Unavailable",
                Request.create(Request.HttpMethod.GET, "/test",
                        Collections.emptyMap(), Request.Body.empty(), new RequestTemplate()),
                new byte[0], Collections.emptyMap());
    }

    @Test
    @DisplayName("KOSPI 데이터 있음 → Success + SUCCESS 감사 저장")
    void shouldReturnSuccessWhenDataPresent() {
        when(krxStockClient.getDailyKospiStockInfo(eq(testAuthKey), eq(targetDate))).thenReturn(successResponse());

        DailyPriceFetcher.Outcome outcome = provider.fetchDailyMarketData(MarketType.KOSPI, targetDate);

        assertThat(outcome).isInstanceOf(DailyPriceFetcher.Outcome.Success.class);
        assertThat(((DailyPriceFetcher.Outcome.Success) outcome).stocks()).hasSize(1);
        ArgumentCaptor<KrxDailyData> captor = ArgumentCaptor.forClass(KrxDailyData.class);
        verify(krxDailyDataRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(KrxDailyDataStatus.SUCCESS);
    }

    @Test
    @DisplayName("빈 응답 + 확정 시점 지남 → Holiday")
    void shouldReturnHolidayWhenEmptyAndPastConfirmation() {
        setupProvider(LocalDate.of(2026, 4, 25).atStartOfDay(kst).toInstant());
        when(krxStockClient.getDailyKospiStockInfo(eq(testAuthKey), eq(targetDate)))
                .thenReturn(new KrxDailyPriceResponse(Collections.emptyList()));

        DailyPriceFetcher.Outcome outcome = provider.fetchDailyMarketData(MarketType.KOSPI, targetDate);

        assertThat(outcome).isInstanceOf(DailyPriceFetcher.Outcome.Holiday.class);
    }

    @Test
    @DisplayName("빈 응답 + 확정 시점 전 → RetryLater UNCERTAIN with confirmation time")
    void shouldReturnRetryLaterUncertainWhenEmptyBeforeConfirmation() {
        // now = 2026-04-18 08:59 KST (before 2026-04-19 09:00 confirmation for targetDate 2026-04-17)
        Instant beforeConfirmation = LocalDate.of(2026, 4, 18).atTime(8, 59).atZone(kst).toInstant();
        setupProvider(beforeConfirmation);
        when(krxStockClient.getDailyKospiStockInfo(eq(testAuthKey), eq(targetDate)))
                .thenReturn(new KrxDailyPriceResponse(Collections.emptyList()));

        DailyPriceFetcher.Outcome outcome = provider.fetchDailyMarketData(MarketType.KOSPI, targetDate);

        assertThat(outcome).isInstanceOf(DailyPriceFetcher.Outcome.RetryLater.class);
        DailyPriceFetcher.Outcome.RetryLater rl = (DailyPriceFetcher.Outcome.RetryLater) outcome;
        assertThat(rl.reason()).isEqualTo(DailyPriceFetcher.Reason.UNCERTAIN);
        Instant expectedRetryAt = LocalDate.of(2026, 4, 19).atTime(9, 0).atZone(kst).toInstant();
        assertThat(rl.nextRetryAt()).isEqualTo(expectedRetryAt);
    }

    @Test
    @DisplayName("응답 body null → PermanentFail PARSE_FAILED + 감사 저장")
    void shouldReturnPermanentFailParseOnNullBody() {
        when(krxStockClient.getDailyKospiStockInfo(eq(testAuthKey), eq(targetDate))).thenReturn(null);

        DailyPriceFetcher.Outcome outcome = provider.fetchDailyMarketData(MarketType.KOSPI, targetDate);

        assertThat(outcome).isInstanceOf(DailyPriceFetcher.Outcome.PermanentFail.class);
        assertThat(((DailyPriceFetcher.Outcome.PermanentFail) outcome).reason())
                .isEqualTo(DailyPriceFetcher.Reason.PARSE_FAILED);
        verify(krxDailyDataAuditor, times(1)).recordFailure(any());
    }

    @Test
    @DisplayName("파싱 실패 → PermanentFail PARSE_FAILED + BODY_ERROR 감사")
    void shouldReturnPermanentFailParseOnMalformed() {
        when(krxStockClient.getDailyKospiStockInfo(eq(testAuthKey), eq(targetDate))).thenReturn(malformedResponse());

        DailyPriceFetcher.Outcome outcome = provider.fetchDailyMarketData(MarketType.KOSPI, targetDate);

        assertThat(outcome).isInstanceOf(DailyPriceFetcher.Outcome.PermanentFail.class);
        assertThat(((DailyPriceFetcher.Outcome.PermanentFail) outcome).reason())
                .isEqualTo(DailyPriceFetcher.Reason.PARSE_FAILED);
        ArgumentCaptor<KrxDailyData> captor = ArgumentCaptor.forClass(KrxDailyData.class);
        verify(krxDailyDataAuditor).recordFailure(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(KrxDailyDataStatus.BODY_ERROR);
    }

    @Test
    @DisplayName("FeignException.Unauthorized → PermanentFail AUTH_FAILED + AUTH_FAILED 감사")
    void shouldReturnPermanentFailAuthOnUnauthorized() {
        when(krxStockClient.getDailyKospiStockInfo(eq(testAuthKey), eq(targetDate)))
                .thenThrow(buildUnauthorized());

        DailyPriceFetcher.Outcome outcome = provider.fetchDailyMarketData(MarketType.KOSPI, targetDate);

        assertThat(outcome).isInstanceOf(DailyPriceFetcher.Outcome.PermanentFail.class);
        assertThat(((DailyPriceFetcher.Outcome.PermanentFail) outcome).reason())
                .isEqualTo(DailyPriceFetcher.Reason.AUTH_FAILED);
        ArgumentCaptor<KrxDailyData> captor = ArgumentCaptor.forClass(KrxDailyData.class);
        verify(krxDailyDataAuditor).recordFailure(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(KrxDailyDataStatus.API_AUTH_FAILED);
    }

    @Test
    @DisplayName("일반 FeignException(5xx) → RetryLater TRANSIENT + API_FAILED 감사")
    void shouldReturnRetryLaterTransientOnFeignException() {
        Instant now = Instant.parse("2026-04-25T00:00:00Z");
        setupProvider(now);
        when(krxStockClient.getDailyKospiStockInfo(eq(testAuthKey), eq(targetDate)))
                .thenThrow(buildServiceUnavailable());

        DailyPriceFetcher.Outcome outcome = provider.fetchDailyMarketData(MarketType.KOSPI, targetDate);

        assertThat(outcome).isInstanceOf(DailyPriceFetcher.Outcome.RetryLater.class);
        DailyPriceFetcher.Outcome.RetryLater rl = (DailyPriceFetcher.Outcome.RetryLater) outcome;
        assertThat(rl.reason()).isEqualTo(DailyPriceFetcher.Reason.TRANSIENT);
        assertThat(rl.nextRetryAt()).isEqualTo(now.plusSeconds(300));
        ArgumentCaptor<KrxDailyData> captor = ArgumentCaptor.forClass(KrxDailyData.class);
        verify(krxDailyDataAuditor).recordFailure(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(KrxDailyDataStatus.API_FAILED);
    }

    @Test
    @DisplayName("KONEX 시장 조회 → KONEX 엔드포인트 호출")
    void shouldCallKonexEndpoint() {
        when(krxStockClient.getDailyKonexStockInfo(eq(testAuthKey), eq(targetDate))).thenReturn(successResponse());

        DailyPriceFetcher.Outcome outcome = provider.fetchDailyMarketData(MarketType.KONEX, targetDate);

        assertThat(outcome).isInstanceOf(DailyPriceFetcher.Outcome.Success.class);
        verify(krxStockClient, times(1)).getDailyKonexStockInfo(eq(testAuthKey), eq(targetDate));
        verify(krxStockClient, never()).getDailyKospiStockInfo(any(), any());
    }
}
