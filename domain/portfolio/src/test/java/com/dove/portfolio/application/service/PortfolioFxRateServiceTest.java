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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 환율 갱신 서비스를 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class PortfolioFxRateServiceTest {

    @Mock
    PortfolioFxRateRepository repository;

    @Mock
    FxRatePort fxRatePort;

    @InjectMocks
    PortfolioFxRateService service;

    @Nested
    @DisplayName("환율 갱신")
    class RefreshAll {
        @Test
        @DisplayName("조회된 통화는 신규 저장된다")
        void shouldSaveWhenNew() {
            when(fxRatePort.fetchRateToKrw("USD"))
                    .thenReturn(Optional.of(new FxQuote("USD", new BigDecimal("1494.09"), LocalDate.of(2026, 7, 13))));
            when(repository.findById("USD")).thenReturn(Optional.empty());

            int updated = service.refreshAll();

            assertThat(updated).isEqualTo(1);
            ArgumentCaptor<PortfolioFxRate> captor = ArgumentCaptor.forClass(PortfolioFxRate.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getCurrency()).isEqualTo("USD");
            assertThat(captor.getValue().getRate()).isEqualByComparingTo("1494.09");
        }

        @Test
        @DisplayName("기존 통화는 갱신되고 새로 저장하지 않는다")
        void shouldUpdateWhenExisting() {
            PortfolioFxRate existing = PortfolioFxRate.create("USD", new BigDecimal("1400"), LocalDate.of(2026, 7, 1));
            when(fxRatePort.fetchRateToKrw("USD"))
                    .thenReturn(Optional.of(new FxQuote("USD", new BigDecimal("1500"), LocalDate.of(2026, 7, 13))));
            when(repository.findById("USD")).thenReturn(Optional.of(existing));

            int updated = service.refreshAll();

            assertThat(updated).isEqualTo(1);
            assertThat(existing.getRate()).isEqualByComparingTo("1500");
            verify(repository, never()).save(existing);
        }

        @Test
        @DisplayName("조회 실패한 통화는 건너뛰어 저장하지 않는다")
        void shouldSkipWhenFetchFails() {
            int updated = service.refreshAll();

            assertThat(updated).isZero();
            verify(repository, never()).save(any());
        }
    }
}
