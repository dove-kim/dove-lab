package com.dove.scheduler.service;

import com.dove.investorflow.application.service.InvestorDailyService;
import com.dove.investorflow.domain.entity.InvestorDaily;
import com.dove.jobstatus.JobStatusRegistry;
import com.dove.jobstatus.SchedulerJobName;
import com.dove.market.domain.enums.MarketType;
import com.dove.stock.application.service.StockQueryService;
import com.dove.stock.domain.entity.Stock;
import com.dove.stock.domain.enums.StockExchange;
import com.dove.stockcollection.application.port.InvestorDailyRow;
import com.dove.stockcollection.application.port.InvestorFetcher;
import com.dove.systemevent.application.service.SystemEventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * {@link InvestorCollectService} 단위 테스트.
 */
@ExtendWith(MockitoExtension.class)
class InvestorCollectServiceTest {

    @Mock InvestorFetcher fetcher;
    @Mock StockQueryService stockQueryService;
    @Mock InvestorDailyService investorDailyService;
    @Mock SystemEventService systemEventService;
    @Mock JobStatusRegistry jobStatusRegistry;

    @InjectMocks InvestorCollectService service;

    private static final LocalDate TODAY = LocalDate.of(2026, 6, 7);
    private static final String JOB = SchedulerJobName.INVESTOR_FLOW.name();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "concurrency", 4);
    }

    private Stock stock(String ticker, MarketType market) {
        return new Stock(ticker, null, market, null, null, null);
    }

    private InvestorDailyRow row(LocalDate tradeDate) {
        return new InvestorDailyRow(tradeDate, 10L, 5L, 20L, 8L, 30L, 12L);
    }

    @Nested
    @DisplayName("collectAll — 정상 수집")
    class CollectAllSuccess {

        @Test
        @DisplayName("종목별 fetch 결과를 InvestorDaily로 매핑해 저장하고 start·complete 호출")
        void shouldFetchMapSaveAndCompleteWhenStocksExist() {
            given(stockQueryService.findAll()).willReturn(List.of(stock("005930", MarketType.KOSPI)));
            given(fetcher.fetch(eq("005930"), any(), any())).willReturn(List.of(row(LocalDate.of(2026, 6, 7))));

            service.collectAll(TODAY);

            ArgumentCaptor<List<InvestorDaily>> captor = ArgumentCaptor.forClass(List.class);
            verify(investorDailyService).saveAll(captor.capture());
            List<InvestorDaily> rows = captor.getValue();
            assertThat(rows).hasSize(1);
            InvestorDaily saved = rows.get(0);
            assertThat(saved.getExchange()).isEqualTo(StockExchange.KOSPI);
            assertThat(saved.getStockCode()).isEqualTo("005930");
            assertThat(saved.getTradeDate()).isEqualTo(LocalDate.of(2026, 6, 7));
            assertThat(saved.getIndividualBuy()).isEqualTo(10L);
            assertThat(saved.getIndividualSell()).isEqualTo(5L);
            assertThat(saved.getInstitutionBuy()).isEqualTo(20L);
            assertThat(saved.getInstitutionSell()).isEqualTo(8L);
            assertThat(saved.getForeignBuy()).isEqualTo(30L);
            assertThat(saved.getForeignSell()).isEqualTo(12L);

            verify(jobStatusRegistry).start(JOB, 1);
            verify(jobStatusRegistry).complete(JOB);
        }

        @Test
        @DisplayName("fetch 조회는 today 단일일만 요청한다")
        void shouldFetchTodayOnlyWhenCollecting() {
            given(stockQueryService.findAll()).willReturn(List.of(stock("005930", MarketType.KOSPI)));
            given(fetcher.fetch(anyString(), any(), any())).willReturn(List.of(row(TODAY)));

            service.collectAll(TODAY);

            ArgumentCaptor<LocalDate> from = ArgumentCaptor.forClass(LocalDate.class);
            ArgumentCaptor<LocalDate> to = ArgumentCaptor.forClass(LocalDate.class);
            verify(fetcher).fetch(eq("005930"), from.capture(), to.capture());
            assertThat(from.getValue()).isEqualTo(TODAY);
            assertThat(to.getValue()).isEqualTo(TODAY);
        }
    }

    @Nested
    @DisplayName("collectAll — 빈 응답 스킵")
    class CollectAllEmpty {

        @Test
        @DisplayName("fetch가 빈 리스트면 saveAll을 호출하지 않는다")
        void shouldSkipSaveWhenFetchEmpty() {
            given(stockQueryService.findAll()).willReturn(List.of(stock("005930", MarketType.KOSPI)));
            given(fetcher.fetch(anyString(), any(), any())).willReturn(List.of());

            service.collectAll(TODAY);

            verify(investorDailyService, never()).saveAll(any());
            verify(jobStatusRegistry).complete(JOB);
        }
    }

    @Nested
    @DisplayName("collectAll — fetch 실패")
    class CollectAllFailure {

        @Test
        @DisplayName("fetch 예외 시 KIS 실패 기록·job fail 후 ParallelException 재던짐")
        void shouldRecordFailureAndRethrowWhenFetchThrows() {
            given(stockQueryService.findAll()).willReturn(List.of(stock("005930", MarketType.KOSPI)));
            given(fetcher.fetch(anyString(), any(), any()))
                    .willThrow(new RuntimeException("KIS 다운"));

            assertThatThrownBy(() -> service.collectAll(TODAY))
                    .isInstanceOf(com.dove.concurrent.ParallelException.class)
                    .hasCauseInstanceOf(RuntimeException.class);

            verify(systemEventService).recordKisApiFailure(eq("INVESTOR"), eq("KIS 다운"));
            verify(jobStatusRegistry).fail(eq(JOB), eq("KIS 다운"));
            verify(jobStatusRegistry, never()).complete(anyString());
            verify(investorDailyService, never()).saveAll(any());
            verify(jobStatusRegistry).start(JOB, 1);
        }
    }

    @Nested
    @DisplayName("collectAll — 거래소 매핑")
    class ExchangeMapping {

        @Test
        @DisplayName("KOSDAQ 종목은 StockExchange.KOSDAQ로 매핑된다")
        void shouldMapKosdaqMarketToKosdaqExchangeWhenSaving() {
            given(stockQueryService.findAll()).willReturn(List.of(stock("035720", MarketType.KOSDAQ)));
            given(fetcher.fetch(anyString(), any(), any())).willReturn(List.of(row(TODAY)));

            service.collectAll(TODAY);

            ArgumentCaptor<List<InvestorDaily>> captor = ArgumentCaptor.forClass(List.class);
            verify(investorDailyService).saveAll(captor.capture());
            assertThat(captor.getValue().get(0).getExchange()).isEqualTo(StockExchange.KOSDAQ);
        }
    }
}
