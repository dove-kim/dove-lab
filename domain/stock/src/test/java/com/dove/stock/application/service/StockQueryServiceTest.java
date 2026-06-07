package com.dove.stock.application.service;

import com.dove.jpa.QuerydslConfiguration;
import com.dove.market.domain.enums.MarketType;
import com.dove.stock.domain.entity.Stock;
import com.dove.stock.domain.entity.StockDetail;
import com.dove.stock.domain.enums.StockExchange;
import com.dove.stock.domain.repository.StockDetailRepository;
import com.dove.stock.domain.repository.StockRepository;
import com.dove.stock.infrastructure.repository.StockRepositorySupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * StockQueryService 통합 테스트.
 */
@DataJpaTest
@Import({StockQueryService.class, StockRepositorySupport.class, QuerydslConfiguration.class})
class StockQueryServiceTest {

    @Autowired StockQueryService service;
    @Autowired StockRepository stockRepository;
    @Autowired StockDetailRepository stockDetailRepository;

    private Stock kospi(String ticker) {
        return new Stock(ticker, "ISIN-" + ticker, MarketType.KOSPI, LocalDate.of(2000, 1, 1), "주권", "보통주");
    }

    private Stock kosdaq(String ticker) {
        return new Stock(ticker, "ISIN-" + ticker, MarketType.KOSDAQ, LocalDate.of(2000, 1, 1), "주권", "보통주");
    }

    private StockDetail detailWith(String ticker, String abrvName, String prdtName) {
        StockDetail d = new StockDetail(ticker);
        d.applyProductInfo(prdtName, abrvName, null, null, null, null, null);
        return d;
    }

    @Nested
    @DisplayName("findNamesByTickers — 종목명 폴백 순서")
    class FindNamesByTickers {

        @Test
        @DisplayName("prdtAbrvName이 있으면 우선 반환한다")
        void shouldReturnAbrvNameWhenPresent() {
            stockDetailRepository.save(detailWith("005930", "삼성전자", "삼성전자(주)"));

            Map<String, String> result = service.findNamesByTickers(List.of("005930"));

            assertThat(result).containsEntry("005930", "삼성전자");
        }

        @Test
        @DisplayName("prdtAbrvName이 없으면 prdtName으로 폴백한다")
        void shouldFallbackToPrdtNameWhenAbrvNameBlank() {
            stockDetailRepository.save(detailWith("005930", "  ", "삼성전자(주)"));

            Map<String, String> result = service.findNamesByTickers(List.of("005930"));

            assertThat(result).containsEntry("005930", "삼성전자(주)");
        }

        @Test
        @DisplayName("prdtAbrvName·prdtName 모두 없으면 ticker로 폴백한다")
        void shouldFallbackToTickerWhenBothNamesBlank() {
            stockDetailRepository.save(detailWith("005930", null, null));

            Map<String, String> result = service.findNamesByTickers(List.of("005930"));

            assertThat(result).containsEntry("005930", "005930");
        }

        @Test
        @DisplayName("빈 티커 집합이면 빈 맵을 반환한다")
        void shouldReturnEmptyMapWhenTickersEmpty() {
            assertThat(service.findNamesByTickers(List.of())).isEmpty();
        }
    }

    @Nested
    @DisplayName("resolveExchange — 거래소 결정")
    class ResolveExchange {

        @Test
        @DisplayName("source가 NXT이면 NXT를 반환한다")
        void shouldReturnNxtWhenSourceIsNxt() {
            assertThat(service.resolveExchange("NXT", "005930")).isEqualTo(StockExchange.NXT);
        }

        @Test
        @DisplayName("source가 소문자여도 NXT를 반환한다")
        void shouldReturnNxtWhenSourceIsLowercase() {
            assertThat(service.resolveExchange("nxt", "005930")).isEqualTo(StockExchange.NXT);
        }

        @Test
        @DisplayName("source가 INTEGRATED이면 INTEGRATED를 반환한다")
        void shouldReturnIntegratedWhenSourceIsIntegrated() {
            assertThat(service.resolveExchange("INTEGRATED", "005930")).isEqualTo(StockExchange.INTEGRATED);
        }

        @Test
        @DisplayName("source가 KRX이면 종목의 실제 시장으로 매핑한다")
        void shouldMapToMarketExchangeWhenSourceIsKrx() {
            stockRepository.save(kospi("005930"));
            stockRepository.save(kosdaq("000660"));

            assertThat(service.resolveExchange("KRX", "005930")).isEqualTo(StockExchange.KOSPI);
            assertThat(service.resolveExchange("KRX", "000660")).isEqualTo(StockExchange.KOSDAQ);
        }

        @Test
        @DisplayName("KRX 경로에서 종목이 없으면 NoSuchElementException을 던진다")
        void shouldThrowWhenStockNotFoundOnKrxPath() {
            assertThatThrownBy(() -> service.resolveExchange("KRX", "999999"))
                    .isInstanceOf(NoSuchElementException.class);
        }
    }

    @Nested
    @DisplayName("findByMarket · findAll · findAllTickers — 시장별·전체 조회")
    class FindByMarketAndAll {

        @Test
        @DisplayName("시장별 종목을 반환한다")
        void shouldReturnStocksByMarket() {
            stockRepository.save(kospi("005930"));
            stockRepository.save(kosdaq("000660"));

            assertThat(service.findByMarket(MarketType.KOSPI))
                    .extracting(Stock::getTicker).containsExactly("005930");
        }

        @Test
        @DisplayName("전체 티커 목록을 반환한다")
        void shouldReturnAllTickers() {
            stockRepository.save(kospi("005930"));
            stockRepository.save(kosdaq("000660"));

            assertThat(service.findAllTickers()).containsExactlyInAnyOrder("005930", "000660");
        }
    }

    @Nested
    @DisplayName("findTickersByExchange — 거래소별 티커")
    class FindTickersByExchange {

        @Test
        @DisplayName("KOSPI 거래소는 KOSPI 종목 티커만 반환한다")
        void shouldReturnKospiTickersForKospiExchange() {
            stockRepository.save(kospi("005930"));
            stockRepository.save(kosdaq("000660"));

            assertThat(service.findTickersByExchange(StockExchange.KOSPI)).containsExactly("005930");
        }

        @Test
        @DisplayName("NXT 거래소는 KOSPI·KOSDAQ 종목 티커를 반환한다 (KONEX 제외)")
        void shouldReturnKospiAndKosdaqTickersForNxtExchange() {
            stockRepository.save(kospi("005930"));
            stockRepository.save(kosdaq("000660"));
            stockRepository.save(new Stock("024110", "ISIN-024110", MarketType.KONEX,
                    LocalDate.of(2000, 1, 1), "주권", "보통주"));

            assertThat(service.findTickersByExchange(StockExchange.NXT))
                    .containsExactlyInAnyOrder("005930", "000660");
        }
    }
}
