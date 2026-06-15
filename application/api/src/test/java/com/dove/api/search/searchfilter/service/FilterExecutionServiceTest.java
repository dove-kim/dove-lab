package com.dove.api.search.searchfilter.service;

import com.dove.api.search.searchfilter.dto.FilterExecutionResult;
import com.dove.indicator.application.service.StockFeatureDailyService;
import com.dove.market.domain.enums.MarketType;
import com.dove.screening.application.service.StockFeatureFilterQueryService;
import com.dove.screening.application.service.StockFilterQueryService;
import com.dove.screening.domain.entity.SearchFilter;
import com.dove.screening.domain.enums.DateRule;
import com.dove.screening.domain.value.FeatureMatch;
import com.dove.screening.domain.value.FilterExpression;
import com.dove.stock.application.service.StockPriceQueryService;
import com.dove.stock.application.service.StockQueryService;
import com.dove.stock.domain.entity.Stock;
import com.dove.stock.domain.entity.StockPrice;
import com.dove.stock.domain.enums.PriceType;
import com.dove.stock.domain.enums.StockExchange;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

/**
 * 검색 필터 실행 서비스 단위 테스트.
 */
@ExtendWith(MockitoExtension.class)
class FilterExecutionServiceTest {

    private static final LocalDate EVAL_DATE = LocalDate.of(2026, 6, 5);
    private static final List<MarketType> KOSPI = List.of(MarketType.KOSPI);

    /** 거래량 > 1000 단일 조건 검색식. */
    private static final String VOLUME_GT_1000 =
            "{\"conditionType\":\"VOLUME_VALUE\",\"operator\":\"GT\",\"value\":1000}";

    @Mock StockFilterQueryService stockFilterQueryService;
    @Mock StockPriceQueryService priceQueryService;
    @Mock StockFeatureDailyService featureDailyService;
    @Mock StockFeatureFilterQueryService featureFilterQueryService;
    @Mock StockQueryService stockQueryService;

    @InjectMocks FilterExecutionService service;

    private SearchFilter filter(String json, Long stockFilterId) {
        return SearchFilter.create(1L, "필터", DateRule.SPECIFIC_DATE, KOSPI, PriceType.RAW,
                FilterExpression.parse(json), stockFilterId);
    }

    private StockPrice price(String ticker, long volume) {
        return new StockPrice(ticker, StockExchange.KOSPI, PriceType.RAW, EVAL_DATE,
                100L, 110L, 90L, 105L, volume, 0L);
    }

    private Stock stock(String ticker) {
        return new Stock(ticker, "KR" + ticker, MarketType.KOSPI, null, "주권", "보통주");
    }

    @Nested
    @DisplayName("DB 푸시다운")
    class DbPushDown {

        @Test
        @DisplayName("DB 푸시다운 성공 시 매칭 행을 결과로 변환한다")
        void shouldMapDbMatchesWhenPushedDown() {
            SearchFilter filter = filter(VOLUME_GT_1000, null);
            FeatureMatch row = new FeatureMatch("005930", StockExchange.KOSPI, 70000L, 5000L);
            given(featureFilterQueryService.findMatchingByExpression(anyCollection(), eq(PriceType.RAW), eq(EVAL_DATE), any()))
                    .willReturn(Optional.of(List.of(row)));
            given(stockQueryService.findByTickers(any())).willReturn(Map.of("005930", stock("005930")));
            given(stockQueryService.findNamesByTickers(any())).willReturn(Map.of("005930", "삼성전자"));
            given(featureFilterQueryService.countByExchangesAndDate(anyCollection(), eq(PriceType.RAW), eq(EVAL_DATE)))
                    .willReturn(900L);

            FilterExecutionResult result = service.execute(filter, EVAL_DATE);

            assertThat(result.evalDate()).isEqualTo(EVAL_DATE);
            assertThat(result.totalCandidates()).isEqualTo(900);
            assertThat(result.matches()).hasSize(1);
            assertThat(result.matches().get(0).ticker()).isEqualTo("005930");
            assertThat(result.matches().get(0).name()).isEqualTo("삼성전자");
            assertThat(result.matches().get(0).closePrice()).isEqualTo(70000L);
        }
    }

    @Nested
    @DisplayName("인메모리 폴백")
    class InMemoryFallback {

        @Test
        @DisplayName("DB 푸시다운 불가 시 전 종목 인메모리 평가로 폴백한다")
        void shouldFallBackToInMemoryWhenNotPushedDown() {
            SearchFilter filter = filter(VOLUME_GT_1000, null);
            given(featureFilterQueryService.findMatchingByExpression(anyCollection(), eq(PriceType.RAW), eq(EVAL_DATE), any()))
                    .willReturn(Optional.empty());
            given(priceQueryService.findByExchangesAndDate(anyCollection(), eq(PriceType.RAW), eq(EVAL_DATE)))
                    .willReturn(Map.of("AAA", price("AAA", 5000L), "BBB", price("BBB", 500L)));
            given(featureDailyService.findAllByExchangeAndDate(StockExchange.KOSPI, PriceType.RAW, EVAL_DATE))
                    .willReturn(Map.of());
            given(stockQueryService.findByTickers(any()))
                    .willReturn(Map.of("AAA", stock("AAA"), "BBB", stock("BBB")));
            given(stockQueryService.findNamesByTickers(any()))
                    .willReturn(Map.of("AAA", "에이종목", "BBB", "비종목"));

            FilterExecutionResult result = service.execute(filter, EVAL_DATE);

            assertThat(result.totalCandidates()).isEqualTo(2);
            assertThat(result.matches()).extracting("ticker").containsExactly("AAA");
            assertThat(result.matches().get(0).name()).isEqualTo("에이종목");
        }
    }

    @Nested
    @DisplayName("기준일 없음")
    class NoTradeDate {

        @Test
        @DisplayName("기준일 거래일 데이터가 없으면 422 NO_DATA_FOR_DATE")
        void shouldThrowUnprocessableWhenNoTradeDate() {
            SearchFilter filter = SearchFilter.create(1L, "필터", DateRule.LATEST, KOSPI, PriceType.RAW,
                    FilterExpression.parse(VOLUME_GT_1000), null);
            given(priceQueryService.findNthRecentTradeDateByExchanges(any(), eq(PriceType.RAW), any(), eq(0)))
                    .willReturn(null);

            assertThatThrownBy(() -> service.execute(filter, EVAL_DATE))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode().value())
                    .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY.value());
        }
    }

    @Nested
    @DisplayName("종목 필터")
    class StockFilterNarrowing {

        @Test
        @DisplayName("종목 필터 지정 시 통과 티커만 남긴다")
        void shouldNarrowByStockFilterWhenSet() {
            SearchFilter filter = filter(VOLUME_GT_1000, 99L);
            FeatureMatch a = new FeatureMatch("AAA", StockExchange.KOSPI, 100L, 5000L);
            FeatureMatch b = new FeatureMatch("BBB", StockExchange.KOSPI, 200L, 6000L);
            given(featureFilterQueryService.findMatchingByExpression(anyCollection(), eq(PriceType.RAW), eq(EVAL_DATE), any()))
                    .willReturn(Optional.of(List.of(a, b)));
            given(stockQueryService.findByTickers(any())).willReturn(Map.of("AAA", stock("AAA"), "BBB", stock("BBB")));
            given(stockQueryService.findNamesByTickers(any())).willReturn(Map.of("AAA", "에이", "BBB", "비"));
            given(featureFilterQueryService.countByExchangesAndDate(anyCollection(), eq(PriceType.RAW), eq(EVAL_DATE)))
                    .willReturn(2L);
            given(stockFilterQueryService.resolveTickers(eq(99L), any())).willReturn(Set.of("AAA"));

            FilterExecutionResult result = service.execute(filter, EVAL_DATE);

            assertThat(result.matches()).extracting("ticker").containsExactly("AAA");
        }
    }

    @Nested
    @DisplayName("주가유형")
    class PriceTypeHandling {

        @Test
        @DisplayName("RAW가 아닌 ADJUSTED 주가유형도 그대로 위임한다")
        void shouldUseAdjustedPriceTypeWhenFilterIsAdjusted() {
            SearchFilter filter = SearchFilter.create(1L, "필터", DateRule.SPECIFIC_DATE, KOSPI,
                    PriceType.ADJUSTED, FilterExpression.parse(VOLUME_GT_1000), null);
            FeatureMatch row = new FeatureMatch("AAA", StockExchange.KOSPI, 100L, 5000L);
            given(featureFilterQueryService.findMatchingByExpression(anyCollection(), eq(PriceType.ADJUSTED), eq(EVAL_DATE), any()))
                    .willReturn(Optional.of(List.of(row)));
            given(stockQueryService.findByTickers(any())).willReturn(Map.of("AAA", stock("AAA")));
            given(stockQueryService.findNamesByTickers(any())).willReturn(Map.of("AAA", "에이"));
            given(featureFilterQueryService.countByExchangesAndDate(anyCollection(), eq(PriceType.ADJUSTED), eq(EVAL_DATE)))
                    .willReturn(1L);

            FilterExecutionResult result = service.execute(filter, EVAL_DATE);

            assertThat(result.matches()).extracting("ticker").containsExactly("AAA");
        }
    }
}
