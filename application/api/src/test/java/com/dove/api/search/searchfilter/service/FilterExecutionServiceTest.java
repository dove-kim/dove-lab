package com.dove.api.search.searchfilter.service;

import com.dove.api.search.searchfilter.dto.FilterExecutionResult;
import com.dove.custommetric.application.service.CustomMetricDailyService;
import com.dove.fundamental.application.StockValuationQueryService;
import com.dove.indicator.application.service.StockFeatureDailyService;
import com.dove.indicator.application.service.StockRankDailyService;
import com.dove.market.domain.enums.MarketType;
import com.dove.modelserving.application.service.ModelScoreQueryService;
import com.dove.screening.application.service.StockFeatureFilterQueryService;
import com.dove.screening.application.service.StockFilterQueryService;
import com.dove.screening.domain.entity.SearchFilter;
import com.dove.screening.domain.enums.DateRule;
import com.dove.screening.domain.enums.FilterVenue;
import com.dove.screening.domain.value.FeatureMatch;
import com.dove.screening.domain.value.FilterExpression;
import com.dove.stock.application.service.StockDetailQueryService;
import com.dove.stock.application.service.StockPriceQueryService;
import com.dove.stock.application.service.StockQueryService;
import com.dove.stock.domain.entity.Stock;
import com.dove.stock.domain.entity.StockPrice;
import com.dove.stock.domain.enums.PriceType;
import com.dove.stock.domain.enums.StockExchange;
import com.dove.stock.domain.value.StockStatusFlags;
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

    /** 거래정지·관리종목 제외 조건 검색식. */
    private static final String STOCK_STATUS_EXCLUDE_BOTH =
            "{\"conditionType\":\"STOCK_STATUS\",\"exclude\":[\"TRADING_HALT\",\"ADMIN_ITEM\"]}";

    @Mock StockFilterQueryService stockFilterQueryService;
    @Mock StockPriceQueryService priceQueryService;
    @Mock StockFeatureDailyService featureDailyService;
    @Mock StockFeatureFilterQueryService featureFilterQueryService;
    @Mock StockQueryService stockQueryService;
    @Mock StockDetailQueryService stockDetailQueryService;
    @Mock StockRankDailyService rankDailyService;
    @Mock CustomMetricDailyService customMetricDailyService;
    @Mock ModelScoreQueryService modelScoreQueryService;
    @Mock StockValuationQueryService valuationQueryService;

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
            FeatureMatch row = new FeatureMatch("005930", StockExchange.KOSPI, 68000L, 71000L, 67000L, 70000L, 5000L, 69000L);
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
            assertThat(result.matches().get(0).openPrice()).isEqualTo(68000L);
            assertThat(result.matches().get(0).closePrice()).isEqualTo(70000L);
            assertThat(result.matches().get(0).prevClose()).isEqualTo(69000L);
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
            FeatureMatch a = new FeatureMatch("AAA", StockExchange.KOSPI, null, null, null, 100L, 5000L, null);
            FeatureMatch b = new FeatureMatch("BBB", StockExchange.KOSPI, null, null, null, 200L, 6000L, null);
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
            FeatureMatch row = new FeatureMatch("AAA", StockExchange.KOSPI, null, null, null, 100L, 5000L, null);
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

    @Nested
    @DisplayName("종목 상태 필터")
    class StockStatusFilter {

        @Test
        @DisplayName("거래정지·관리종목 종목을 인메모리 평가로 제외한다")
        void shouldExcludeHaltedAndAdminInMemory() {
            SearchFilter filter = SearchFilter.create(1L, "필터", DateRule.LATEST, KOSPI, PriceType.RAW,
                    FilterExpression.parse(STOCK_STATUS_EXCLUDE_BOTH), null);
            given(priceQueryService.findNthRecentTradeDateByExchanges(any(), eq(PriceType.RAW), any(), eq(0)))
                    .willReturn(EVAL_DATE);
            given(featureFilterQueryService.findMatchingByExpression(anyCollection(), eq(PriceType.RAW), eq(EVAL_DATE), any()))
                    .willReturn(Optional.empty());
            given(priceQueryService.findByExchangesAndDate(anyCollection(), eq(PriceType.RAW), eq(EVAL_DATE)))
                    .willReturn(Map.of("AAA", price("AAA", 5000L), "BBB", price("BBB", 5000L), "CCC", price("CCC", 5000L)));
            given(featureDailyService.findAllByExchangeAndDate(StockExchange.KOSPI, PriceType.RAW, EVAL_DATE))
                    .willReturn(Map.of());
            given(stockDetailQueryService.findStatusByTickers(any())).willReturn(Map.of(
                    "AAA", new StockStatusFlags(false, false),
                    "BBB", new StockStatusFlags(true, false),
                    "CCC", new StockStatusFlags(false, true)));
            given(stockQueryService.findByTickers(any()))
                    .willReturn(Map.of("AAA", stock("AAA"), "BBB", stock("BBB"), "CCC", stock("CCC")));
            given(stockQueryService.findNamesByTickers(any()))
                    .willReturn(Map.of("AAA", "에이", "BBB", "비", "CCC", "씨"));

            FilterExecutionResult result = service.execute(filter, EVAL_DATE);

            assertThat(result.matches()).extracting("ticker").containsExactly("AAA");
        }

        @Test
        @DisplayName("DateRule이 LATEST가 아니면 종목상태 조건을 무시(no-op)하고 전 종목을 평가한다")
        void shouldIgnoreStatusWhenNotLatest() {
            SearchFilter filter = SearchFilter.create(1L, "필터", DateRule.SPECIFIC_DATE, KOSPI, PriceType.RAW,
                    FilterExpression.parse(STOCK_STATUS_EXCLUDE_BOTH), null);
            given(featureFilterQueryService.findMatchingByExpression(anyCollection(), eq(PriceType.RAW), eq(EVAL_DATE), any()))
                    .willReturn(Optional.empty());
            given(priceQueryService.findByExchangesAndDate(anyCollection(), eq(PriceType.RAW), eq(EVAL_DATE)))
                    .willReturn(Map.of("AAA", price("AAA", 5000L), "BBB", price("BBB", 5000L)));
            given(featureDailyService.findAllByExchangeAndDate(StockExchange.KOSPI, PriceType.RAW, EVAL_DATE))
                    .willReturn(Map.of());
            given(stockQueryService.findByTickers(any()))
                    .willReturn(Map.of("AAA", stock("AAA"), "BBB", stock("BBB")));
            given(stockQueryService.findNamesByTickers(any()))
                    .willReturn(Map.of("AAA", "에이", "BBB", "비"));

            // 과거일자라 상태 조회 자체를 하지 않고(거래정지였을 BBB도 제외 안 됨) 나머지 조건만 적용
            FilterExecutionResult result = service.execute(filter, EVAL_DATE);

            assertThat(result.matches()).extracting("ticker").containsExactlyInAnyOrder("AAA", "BBB");
        }
    }

    @Nested
    @DisplayName("파이프라인")
    class Pipeline {

        /** 모든 종목 통과 검색식(등락률 정렬 대상 확보용). */
        private static final String PASS_ALL =
                "{\"conditionType\":\"VOLUME_VALUE\",\"operator\":\"GT\",\"value\":0}";

        private static final LocalDate PREV_DATE = EVAL_DATE.minusDays(1);

        private SearchFilter pipelineFilter(String expression, String pipeline) {
            return SearchFilter.create(1L, "필터", DateRule.SPECIFIC_DATE, KOSPI, PriceType.RAW,
                    FilterVenue.KRX, FilterExpression.parse(expression), null, pipeline);
        }

        private StockPrice priceClose(String ticker, LocalDate date, long close) {
            return new StockPrice(ticker, StockExchange.KOSPI, PriceType.RAW, date, 100L, 110L, 90L, close, 5000L, 0L);
        }

        @Test
        @DisplayName("RANK 단계로 등락률 내림차순 정렬 후 상위 N만 남긴다")
        void shouldRankByChangeRateDescAndLimit() {
            String pipeline = "[{\"type\":\"RANK\",\"sort\":[{\"field\":\"CHANGE_RATE\",\"direction\":\"DESC\"}],\"limit\":2}]";
            SearchFilter filter = pipelineFilter(PASS_ALL, pipeline);
            given(priceQueryService.findByExchangesAndDate(anyCollection(), eq(PriceType.RAW), eq(EVAL_DATE)))
                    .willReturn(Map.of(
                            "AAA", priceClose("AAA", EVAL_DATE, 105L),
                            "BBB", priceClose("BBB", EVAL_DATE, 105L),
                            "CCC", priceClose("CCC", EVAL_DATE, 105L)));
            given(priceQueryService.findNthRecentTradeDateByExchanges(anyCollection(), eq(PriceType.RAW), eq(EVAL_DATE), eq(1)))
                    .willReturn(PREV_DATE);
            given(priceQueryService.findByExchangesAndDate(anyCollection(), eq(PriceType.RAW), eq(PREV_DATE)))
                    .willReturn(Map.of(
                            "AAA", priceClose("AAA", PREV_DATE, 100L),   // +5%
                            "BBB", priceClose("BBB", PREV_DATE, 105L),   // 0%
                            "CCC", priceClose("CCC", PREV_DATE, 210L)));  // -50%
            given(featureDailyService.findAllByExchangeAndDate(StockExchange.KOSPI, PriceType.RAW, EVAL_DATE))
                    .willReturn(Map.of());
            given(stockQueryService.findByTickers(any()))
                    .willReturn(Map.of("AAA", stock("AAA"), "BBB", stock("BBB"), "CCC", stock("CCC")));
            given(stockQueryService.findNamesByTickers(any()))
                    .willReturn(Map.of("AAA", "에이", "BBB", "비", "CCC", "씨"));

            FilterExecutionResult result = service.execute(filter, EVAL_DATE);

            assertThat(result.totalCandidates()).isEqualTo(3);
            assertThat(result.matches()).extracting("ticker").containsExactly("AAA", "BBB");
        }

        @Test
        @DisplayName("RANK 시총 정렬 시 시총 없는 종목은 마지막, 결과에 시총을 포함한다")
        void shouldRankByMarketCapWithNullsLastAndExposeMarketCap() {
            String pipeline = "[{\"type\":\"RANK\",\"sort\":[{\"field\":\"MARKET_CAP\",\"direction\":\"DESC\"}]}]";
            SearchFilter filter = pipelineFilter(PASS_ALL, pipeline);
            given(priceQueryService.findByExchangesAndDate(anyCollection(), eq(PriceType.RAW), eq(EVAL_DATE)))
                    .willReturn(Map.of(
                            "AAA", priceClose("AAA", EVAL_DATE, 105L),
                            "BBB", priceClose("BBB", EVAL_DATE, 105L)));
            given(priceQueryService.findNthRecentTradeDateByExchanges(anyCollection(), eq(PriceType.RAW), eq(EVAL_DATE), eq(1)))
                    .willReturn(null);
            given(featureDailyService.findAllByExchangeAndDate(StockExchange.KOSPI, PriceType.RAW, EVAL_DATE))
                    .willReturn(Map.of());
            given(valuationQueryService.findMarketCapByDate(EVAL_DATE))
                    .willReturn(Map.of("BBB", 500L)); // AAA는 시총 없음 → 마지막
            given(stockQueryService.findByTickers(any()))
                    .willReturn(Map.of("AAA", stock("AAA"), "BBB", stock("BBB")));
            given(stockQueryService.findNamesByTickers(any()))
                    .willReturn(Map.of("AAA", "에이", "BBB", "비"));

            FilterExecutionResult result = service.execute(filter, EVAL_DATE);

            assertThat(result.matches()).extracting("ticker").containsExactly("BBB", "AAA");
            assertThat(result.matches().get(0).marketCap()).isEqualTo(500L);
            assertThat(result.matches().get(1).marketCap()).isNull();
        }

        @Test
        @DisplayName("RANK MODEL_SCORE 내림차순으로 상위 N만 남기고 점수 없는 종목은 제외하며 결과에 점수를 투영한다")
        void shouldRankByModelScoreDescLimitAndProjectScore() {
            String pipeline = "[{\"type\":\"RANK\",\"sort\":["
                    + "{\"field\":\"MODEL_SCORE\",\"direction\":\"DESC\",\"modelId\":7}],\"limit\":2}]";
            SearchFilter filter = pipelineFilter(PASS_ALL, pipeline);
            given(priceQueryService.findByExchangesAndDate(anyCollection(), eq(PriceType.RAW), eq(EVAL_DATE)))
                    .willReturn(Map.of(
                            "AAA", priceClose("AAA", EVAL_DATE, 105L),
                            "BBB", priceClose("BBB", EVAL_DATE, 105L),
                            "CCC", priceClose("CCC", EVAL_DATE, 105L),
                            "DDD", priceClose("DDD", EVAL_DATE, 105L)));
            given(priceQueryService.findNthRecentTradeDateByExchanges(anyCollection(), eq(PriceType.RAW), eq(EVAL_DATE), eq(1)))
                    .willReturn(null);
            given(featureDailyService.findAllByExchangeAndDate(StockExchange.KOSPI, PriceType.RAW, EVAL_DATE))
                    .willReturn(Map.of());
            given(modelScoreQueryService.findScoresByModelAndDate(7L, EVAL_DATE))
                    .willReturn(Map.of("AAA", 0.9, "BBB", 0.5, "CCC", 0.1)); // DDD는 점수 없음(sparse) → 마지막/제외
            given(stockQueryService.findByTickers(any()))
                    .willReturn(Map.of("AAA", stock("AAA"), "BBB", stock("BBB"),
                            "CCC", stock("CCC"), "DDD", stock("DDD")));
            given(stockQueryService.findNamesByTickers(any()))
                    .willReturn(Map.of("AAA", "에이", "BBB", "비", "CCC", "씨", "DDD", "디"));

            FilterExecutionResult result = service.execute(filter, EVAL_DATE);

            assertThat(result.matches()).extracting("ticker").containsExactly("AAA", "BBB");
            assertThat(result.matches().get(0).modelScore()).isEqualTo(0.9);
            assertThat(result.matches().get(1).modelScore()).isEqualTo(0.5);
        }
    }

    @Nested
    @DisplayName("모델 점수 가시성 게이트")
    class ModelScoreVisibilityGate {

        /** 모든 종목 통과 검색식. */
        private static final String PASS_ALL =
                "{\"conditionType\":\"VOLUME_VALUE\",\"operator\":\"GT\",\"value\":0}";

        /** MODEL_SCORE(modelId=7) 내림차순 RANK 파이프라인. */
        private static final String RANK_BY_MODEL_7 = "[{\"type\":\"RANK\",\"sort\":["
                + "{\"field\":\"MODEL_SCORE\",\"direction\":\"DESC\",\"modelId\":7}]}]";

        private SearchFilter modelPipelineFilter() {
            return SearchFilter.create(1L, "필터", DateRule.SPECIFIC_DATE, KOSPI, PriceType.RAW,
                    FilterVenue.KRX, FilterExpression.parse(PASS_ALL), null, RANK_BY_MODEL_7);
        }

        private StockPrice priceClose(String ticker, long close) {
            return new StockPrice(ticker, StockExchange.KOSPI, PriceType.RAW, EVAL_DATE, 100L, 110L, 90L, close, 5000L, 0L);
        }

        private void givenTwoStocksWithModelScores() {
            given(priceQueryService.findByExchangesAndDate(anyCollection(), eq(PriceType.RAW), eq(EVAL_DATE)))
                    .willReturn(Map.of("AAA", priceClose("AAA", 105L), "BBB", priceClose("BBB", 105L)));
            given(priceQueryService.findNthRecentTradeDateByExchanges(anyCollection(), eq(PriceType.RAW), eq(EVAL_DATE), eq(1)))
                    .willReturn(null);
            given(featureDailyService.findAllByExchangeAndDate(StockExchange.KOSPI, PriceType.RAW, EVAL_DATE))
                    .willReturn(Map.of());
            given(modelScoreQueryService.findScoresByModelAndDate(7L, EVAL_DATE))
                    .willReturn(Map.of("AAA", 0.9, "BBB", 0.5));
            given(stockQueryService.findByTickers(any()))
                    .willReturn(Map.of("AAA", stock("AAA"), "BBB", stock("BBB")));
            given(stockQueryService.findNamesByTickers(any()))
                    .willReturn(Map.of("AAA", "에이", "BBB", "비"));
        }

        @Test
        @DisplayName("가시 집합이 비어 미부여면 점수를 숨기되 정렬은 유지한다")
        void shouldHideScoreButKeepOrderWhenModelNotVisible() {
            givenTwoStocksWithModelScores();

            FilterExecutionResult result = service.execute(modelPipelineFilter(), EVAL_DATE, Set.of());

            // 정렬(점수 내림차순)은 유지되어 순서는 그대로, 값만 숨김
            assertThat(result.matches()).extracting("ticker").containsExactly("AAA", "BBB");
            assertThat(result.matches().get(0).modelScore()).isNull();
            assertThat(result.matches().get(1).modelScore()).isNull();
        }

        @Test
        @DisplayName("가시 집합에 모델이 있으면 점수를 노출한다")
        void shouldExposeScoreWhenModelVisible() {
            givenTwoStocksWithModelScores();

            FilterExecutionResult result = service.execute(modelPipelineFilter(), EVAL_DATE, Set.of(7L));

            assertThat(result.matches()).extracting("ticker").containsExactly("AAA", "BBB");
            assertThat(result.matches().get(0).modelScore()).isEqualTo(0.9);
            assertThat(result.matches().get(1).modelScore()).isEqualTo(0.5);
        }
    }
}
