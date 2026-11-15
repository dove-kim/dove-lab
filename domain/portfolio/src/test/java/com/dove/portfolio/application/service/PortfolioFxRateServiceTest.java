package com.dove.portfolio.application.service;

import com.dove.portfolio.application.port.FxQuote;
import com.dove.portfolio.application.port.FxRatePort;
import com.dove.portfolio.domain.entity.PortfolioFxRate;
import com.dove.portfolio.domain.repository.PortfolioFxRateRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 환율 갱신 서비스를 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class PortfolioFxRateServiceTest {

    private static final LocalDate FALLBACK = LocalDate.of(2024, 3, 1);

    @Mock
    PortfolioFxRateRepository repository;

    @Mock
    FxRatePort fxRatePort;

    @InjectMocks
    PortfolioFxRateService service;

    @Nested
    @DisplayName("구간 갱신")
    class RefreshFrom {
        @Test
        @DisplayName("저장된 이력이 없으면 지정 시작일부터 조회해 전부 저장한다")
        void shouldBackfillFromFallbackWhenEmpty() {
            when(repository.findTopByIdCurrencyOrderByIdRateDateDesc("USD")).thenReturn(Optional.empty());
            when(fxRatePort.fetchSeriesToKrw(eq("USD"), eq(FALLBACK), any())).thenReturn(List.of(
                    new FxQuote("USD", new BigDecimal("1335.62"), LocalDate.of(2024, 3, 1)),
                    new FxQuote("USD", new BigDecimal("1332.23"), LocalDate.of(2024, 3, 4))));

            int saved = service.refreshFrom(FALLBACK);

            assertThat(saved).isEqualTo(2);
            verify(repository, never()).findTopByIdCurrencyAndIdRateDateLessThanEqualOrderByIdRateDateDesc(any(), any());
        }

        @Test
        @DisplayName("저장된 이력이 있으면 그 다음날부터 조회한다")
        void shouldResumeFromDayAfterLastSaved() {
            PortfolioFxRate last = PortfolioFxRate.create("USD", new BigDecimal("1300"), LocalDate.of(2026, 11, 10));
            when(repository.findTopByIdCurrencyOrderByIdRateDateDesc("USD")).thenReturn(Optional.of(last));
            when(fxRatePort.fetchSeriesToKrw(eq("USD"), eq(LocalDate.of(2026, 11, 11)), any()))
                    .thenReturn(List.of(new FxQuote("USD", new BigDecimal("1310"), LocalDate.of(2026, 11, 11))));

            int saved = service.refreshFrom(FALLBACK);

            assertThat(saved).isEqualTo(1);
            ArgumentCaptor<PortfolioFxRate> captor = ArgumentCaptor.forClass(PortfolioFxRate.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getRateDate()).isEqualTo(LocalDate.of(2026, 11, 11));
            assertThat(captor.getValue().getRate()).isEqualByComparingTo("1310");
        }

        @Test
        @DisplayName("오늘까지 이미 받았으면 외부를 조회하지 않는다")
        void shouldSkipWhenAlreadyUpToDate() {
            PortfolioFxRate last = PortfolioFxRate.create("USD", new BigDecimal("1300"), LocalDate.now());
            when(repository.findTopByIdCurrencyOrderByIdRateDateDesc("USD")).thenReturn(Optional.of(last));

            int saved = service.refreshFrom(FALLBACK);

            assertThat(saved).isZero();
            verify(fxRatePort, never()).fetchSeriesToKrw(eq("USD"), any(), any());
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("조회 결과가 비면 저장하지 않는다")
        void shouldSaveNothingWhenSeriesEmpty() {
            when(repository.findTopByIdCurrencyOrderByIdRateDateDesc("USD")).thenReturn(Optional.empty());
            when(fxRatePort.fetchSeriesToKrw(eq("USD"), eq(FALLBACK), any())).thenReturn(List.of());

            int saved = service.refreshFrom(FALLBACK);

            assertThat(saved).isZero();
            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("단건 갱신")
    class RefreshOne {
        @Test
        @DisplayName("조회된 환율을 해당 고시일로 저장한다")
        void shouldSaveWhenFetched() {
            when(fxRatePort.fetchRateToKrw("USD"))
                    .thenReturn(Optional.of(new FxQuote("USD", new BigDecimal("1494.09"), LocalDate.of(2026, 7, 13))));
            when(repository.findById(any())).thenReturn(Optional.empty());

            boolean updated = service.refreshOne("USD");

            assertThat(updated).isTrue();
            ArgumentCaptor<PortfolioFxRate> captor = ArgumentCaptor.forClass(PortfolioFxRate.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getCurrency()).isEqualTo("USD");
            assertThat(captor.getValue().getRateDate()).isEqualTo(LocalDate.of(2026, 7, 13));
            assertThat(captor.getValue().getRate()).isEqualByComparingTo("1494.09");
        }

        @Test
        @DisplayName("같은 고시일이 이미 있으면 값만 덮어쓴다")
        void shouldUpdateWhenSameRateDateExists() {
            PortfolioFxRate existing = PortfolioFxRate.create("USD", new BigDecimal("1400"), LocalDate.of(2026, 7, 13));
            when(fxRatePort.fetchRateToKrw("USD"))
                    .thenReturn(Optional.of(new FxQuote("USD", new BigDecimal("1500"), LocalDate.of(2026, 7, 13))));
            when(repository.findById(any())).thenReturn(Optional.of(existing));

            boolean updated = service.refreshOne("USD");

            assertThat(updated).isTrue();
            assertThat(existing.getRate()).isEqualByComparingTo("1500");
            verify(repository, never()).save(existing);
        }

        @Test
        @DisplayName("조회 실패하면 저장하지 않는다")
        void shouldSkipWhenFetchFails() {
            when(fxRatePort.fetchRateToKrw("USD")).thenReturn(Optional.empty());

            boolean updated = service.refreshOne("USD");

            assertThat(updated).isFalse();
            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("환율 조회")
    class Rates {
        @Test
        @DisplayName("통화별 최신 환율을 원화 맵으로 반환한다")
        void shouldReturnLatestRatesByCurrency() {
            PortfolioFxRate last = PortfolioFxRate.create("USD", new BigDecimal("1332.23"), LocalDate.of(2024, 3, 4));
            when(repository.findTopByIdCurrencyOrderByIdRateDateDesc("USD")).thenReturn(Optional.of(last));

            var rates = service.ratesByCurrency();

            assertThat(rates.get("USD")).isEqualByComparingTo("1332.23");
            assertThat(rates.get("KRW")).isEqualByComparingTo("1");
        }

        @Test
        @DisplayName("이력으로 일자별 조회 테이블을 만든다")
        void shouldBuildRateTable() {
            when(repository.findByIdCurrencyIn(any())).thenReturn(List.of(
                    PortfolioFxRate.create("USD", new BigDecimal("1335.62"), LocalDate.of(2024, 3, 1)),
                    PortfolioFxRate.create("USD", new BigDecimal("1332.23"), LocalDate.of(2024, 3, 4))));

            FxRateTable table = service.rateTable();

            assertThat(table.rateOn("USD", LocalDate.of(2024, 3, 3))).isEqualByComparingTo("1335.62");
        }
    }
}
