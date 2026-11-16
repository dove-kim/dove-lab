package com.dove.portfolio.application.service;

import com.dove.portfolio.application.port.OverseasClose;
import com.dove.portfolio.application.port.OverseasPricePort;
import com.dove.portfolio.domain.entity.PortfolioHolding;
import com.dove.portfolio.domain.entity.PortfolioQuote;
import com.dove.portfolio.domain.enums.PortfolioMarket;
import com.dove.portfolio.domain.repository.PortfolioHoldingRepository;
import com.dove.portfolio.domain.repository.PortfolioQuoteRepository;
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
 * 해외 종가 이력 서비스를 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class PortfolioQuoteServiceTest {

    private static final LocalDate FALLBACK = LocalDate.of(2024, 3, 1);

    @Mock
    PortfolioHoldingRepository holdingRepository;

    @Mock
    PortfolioQuoteRepository quoteRepository;

    @Mock
    OverseasPricePort overseasPricePort;

    @InjectMocks
    PortfolioQuoteService service;

    private PortfolioHolding holding(PortfolioMarket market, String ticker) {
        return PortfolioHolding.create(1L, 1L, ticker, market, ticker, "tester");
    }

    @Nested
    @DisplayName("구간 갱신")
    class RefreshFrom {
        @Test
        @DisplayName("이력이 없으면 지정 시작일부터 받아 전부 저장한다")
        void shouldBackfillFromFallbackWhenEmpty() {
            when(holdingRepository.findAll()).thenReturn(List.of(holding(PortfolioMarket.NASDAQ, "TSLA")));
            when(quoteRepository.findTopByIdMarketAndIdTickerOrderByIdAsOfDesc(PortfolioMarket.NASDAQ, "TSLA"))
                    .thenReturn(Optional.empty());
            when(overseasPricePort.fetchDailyCloses(PortfolioMarket.NASDAQ, "TSLA", FALLBACK)).thenReturn(List.of(
                    new OverseasClose(LocalDate.of(2024, 3, 1), new BigDecimal("180")),
                    new OverseasClose(LocalDate.of(2024, 3, 4), new BigDecimal("182"))));

            int saved = service.refreshFrom(FALLBACK);

            assertThat(saved).isEqualTo(2);
        }

        @Test
        @DisplayName("이력이 있으면 그 다음날부터 받는다")
        void shouldResumeFromDayAfterLastSaved() {
            PortfolioQuote last = PortfolioQuote.create(
                    PortfolioMarket.NASDAQ, "TSLA", new BigDecimal("180"), LocalDate.of(2026, 11, 10));
            when(holdingRepository.findAll()).thenReturn(List.of(holding(PortfolioMarket.NASDAQ, "TSLA")));
            when(quoteRepository.findTopByIdMarketAndIdTickerOrderByIdAsOfDesc(PortfolioMarket.NASDAQ, "TSLA"))
                    .thenReturn(Optional.of(last));
            when(overseasPricePort.fetchDailyCloses(PortfolioMarket.NASDAQ, "TSLA", LocalDate.of(2026, 11, 11)))
                    .thenReturn(List.of(new OverseasClose(LocalDate.of(2026, 11, 11), new BigDecimal("190"))));

            int saved = service.refreshFrom(FALLBACK);

            assertThat(saved).isEqualTo(1);
            ArgumentCaptor<PortfolioQuote> captor = ArgumentCaptor.forClass(PortfolioQuote.class);
            verify(quoteRepository).save(captor.capture());
            assertThat(captor.getValue().getAsOf()).isEqualTo(LocalDate.of(2026, 11, 11));
            assertThat(captor.getValue().getClosePrice()).isEqualByComparingTo("190");
        }

        @Test
        @DisplayName("국내 종목은 받지 않는다")
        void shouldSkipDomestic() {
            when(holdingRepository.findAll()).thenReturn(List.of(holding(PortfolioMarket.KOSPI, "005930")));

            int saved = service.refreshFrom(FALLBACK);

            assertThat(saved).isZero();
            verify(overseasPricePort, never()).fetchDailyCloses(any(), any(), any());
        }

        @Test
        @DisplayName("여러 계좌의 같은 종목은 한 번만 받는다")
        void shouldDedupSameTicker() {
            when(holdingRepository.findAll()).thenReturn(List.of(
                    holding(PortfolioMarket.NASDAQ, "TSLA"),
                    holding(PortfolioMarket.NASDAQ, "TSLA")));
            when(quoteRepository.findTopByIdMarketAndIdTickerOrderByIdAsOfDesc(PortfolioMarket.NASDAQ, "TSLA"))
                    .thenReturn(Optional.empty());
            when(overseasPricePort.fetchDailyCloses(PortfolioMarket.NASDAQ, "TSLA", FALLBACK))
                    .thenReturn(List.of(new OverseasClose(LocalDate.of(2024, 3, 1), new BigDecimal("180"))));

            int saved = service.refreshFrom(FALLBACK);

            assertThat(saved).isEqualTo(1);
            verify(overseasPricePort).fetchDailyCloses(eq(PortfolioMarket.NASDAQ), eq("TSLA"), any());
        }

        @Test
        @DisplayName("받아온 게 없으면 저장하지 않는다")
        void shouldSaveNothingWhenEmpty() {
            when(holdingRepository.findAll()).thenReturn(List.of(holding(PortfolioMarket.NASDAQ, "TSLA")));
            when(quoteRepository.findTopByIdMarketAndIdTickerOrderByIdAsOfDesc(PortfolioMarket.NASDAQ, "TSLA"))
                    .thenReturn(Optional.empty());
            when(overseasPricePort.fetchDailyCloses(PortfolioMarket.NASDAQ, "TSLA", FALLBACK)).thenReturn(List.of());

            int saved = service.refreshFrom(FALLBACK);

            assertThat(saved).isZero();
            verify(quoteRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("단건 갱신")
    class RefreshOne {
        @Test
        @DisplayName("조회된 종가를 오늘 자로 저장한다")
        void shouldSaveWhenFetched() {
            when(overseasPricePort.fetchClose(PortfolioMarket.NASDAQ, "TSLA"))
                    .thenReturn(Optional.of(new BigDecimal("189.98")));
            when(quoteRepository.findById(any())).thenReturn(Optional.empty());

            boolean updated = service.refreshOne(PortfolioMarket.NASDAQ, "TSLA");

            assertThat(updated).isTrue();
            ArgumentCaptor<PortfolioQuote> captor = ArgumentCaptor.forClass(PortfolioQuote.class);
            verify(quoteRepository).save(captor.capture());
            assertThat(captor.getValue().getMarket()).isEqualTo(PortfolioMarket.NASDAQ);
            assertThat(captor.getValue().getClosePrice()).isEqualByComparingTo("189.98");
            assertThat(captor.getValue().getAsOf()).isEqualTo(LocalDate.now());
        }

        @Test
        @DisplayName("국내 종목이면 아무것도 하지 않는다")
        void shouldSkipDomestic() {
            boolean updated = service.refreshOne(PortfolioMarket.KOSPI, "005930");

            assertThat(updated).isFalse();
            verify(quoteRepository, never()).save(any());
        }

        @Test
        @DisplayName("조회 실패하면 저장하지 않는다")
        void shouldSkipWhenFetchFails() {
            when(overseasPricePort.fetchClose(PortfolioMarket.NASDAQ, "TSLA")).thenReturn(Optional.empty());

            boolean updated = service.refreshOne(PortfolioMarket.NASDAQ, "TSLA");

            assertThat(updated).isFalse();
            verify(quoteRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("과거 종가 조회")
    class CloseOn {
        @Test
        @DisplayName("지정일 이하 가장 최근 종가를 반환한다")
        void shouldReturnCloseOnOrBefore() {
            PortfolioQuote quote = PortfolioQuote.create(
                    PortfolioMarket.NASDAQ, "TSLA", new BigDecimal("182"), LocalDate.of(2024, 3, 1));
            when(quoteRepository.findTopByIdMarketAndIdTickerAndIdAsOfLessThanEqualOrderByIdAsOfDesc(
                    PortfolioMarket.NASDAQ, "TSLA", LocalDate.of(2024, 3, 3))).thenReturn(Optional.of(quote));

            assertThat(service.closeOn(PortfolioMarket.NASDAQ, "TSLA", LocalDate.of(2024, 3, 3)))
                    .contains(new BigDecimal("182"));
        }
    }
}
