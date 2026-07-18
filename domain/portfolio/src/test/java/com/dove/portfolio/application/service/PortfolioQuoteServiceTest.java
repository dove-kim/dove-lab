package com.dove.portfolio.application.service;

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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 해외 종가 갱신 서비스를 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class PortfolioQuoteServiceTest {

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
    @DisplayName("해외 종가 갱신")
    class RefreshOverseas {
        @Test
        @DisplayName("해외 종목만 갱신하고 국내는 건너뛴다")
        void shouldRefreshOverseasAndSkipDomestic() {
            when(holdingRepository.findAll()).thenReturn(List.of(
                    holding(PortfolioMarket.NASDAQ, "TSLA"),
                    holding(PortfolioMarket.KOSPI, "005930")));
            when(overseasPricePort.fetchClose(PortfolioMarket.NASDAQ, "TSLA"))
                    .thenReturn(Optional.of(new BigDecimal("189.98")));
            when(quoteRepository.findByMarketAndTicker(PortfolioMarket.NASDAQ, "TSLA")).thenReturn(Optional.empty());

            int updated = service.refreshOverseas();

            assertThat(updated).isEqualTo(1);
            ArgumentCaptor<PortfolioQuote> captor = ArgumentCaptor.forClass(PortfolioQuote.class);
            verify(quoteRepository).save(captor.capture());
            assertThat(captor.getValue().getMarket()).isEqualTo(PortfolioMarket.NASDAQ);
            assertThat(captor.getValue().getClosePrice()).isEqualByComparingTo("189.98");
        }

        @Test
        @DisplayName("여러 계좌의 같은 종목은 한 번만 조회한다")
        void shouldDedupSameTicker() {
            when(holdingRepository.findAll()).thenReturn(List.of(
                    holding(PortfolioMarket.NASDAQ, "TSLA"),
                    holding(PortfolioMarket.NASDAQ, "TSLA")));
            when(overseasPricePort.fetchClose(PortfolioMarket.NASDAQ, "TSLA"))
                    .thenReturn(Optional.of(new BigDecimal("189.98")));
            when(quoteRepository.findByMarketAndTicker(PortfolioMarket.NASDAQ, "TSLA")).thenReturn(Optional.empty());

            int updated = service.refreshOverseas();

            assertThat(updated).isEqualTo(1);
            verify(overseasPricePort).fetchClose(PortfolioMarket.NASDAQ, "TSLA");
        }

        @Test
        @DisplayName("기존 종가는 갱신되고 새로 저장하지 않는다")
        void shouldUpdateExisting() {
            PortfolioQuote existing = PortfolioQuote.create(PortfolioMarket.NASDAQ, "TSLA", new BigDecimal("180"));
            when(holdingRepository.findAll()).thenReturn(List.of(holding(PortfolioMarket.NASDAQ, "TSLA")));
            when(overseasPricePort.fetchClose(PortfolioMarket.NASDAQ, "TSLA"))
                    .thenReturn(Optional.of(new BigDecimal("190")));
            when(quoteRepository.findByMarketAndTicker(PortfolioMarket.NASDAQ, "TSLA")).thenReturn(Optional.of(existing));

            int updated = service.refreshOverseas();

            assertThat(updated).isEqualTo(1);
            assertThat(existing.getClosePrice()).isEqualByComparingTo("190");
            verify(quoteRepository, never()).save(any());
        }

        @Test
        @DisplayName("조회 실패한 종목은 건너뛰어 저장하지 않는다")
        void shouldSkipWhenFetchFails() {
            when(holdingRepository.findAll()).thenReturn(List.of(holding(PortfolioMarket.NASDAQ, "TSLA")));
            when(overseasPricePort.fetchClose(PortfolioMarket.NASDAQ, "TSLA")).thenReturn(Optional.empty());

            int updated = service.refreshOverseas();

            assertThat(updated).isZero();
            verify(quoteRepository, never()).save(any());
        }
    }
}
