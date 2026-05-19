package com.dove.api.stock.filter.service;

import com.dove.api.stock.filter.dto.ExecuteFilterResponse;
import com.dove.market.domain.enums.MarketType;
import com.dove.stock.application.service.StockQueryService;
import com.dove.stock.domain.entity.Stock;
import com.dove.screening.application.service.SearchFilterQueryService;
import com.dove.screening.application.service.StockSetQueryService;
import com.dove.screening.domain.entity.SearchFilter;
import com.dove.screening.domain.entity.StockSet;
import com.dove.screening.domain.enums.DateRule;
import com.dove.stock.application.service.DailyStockPriceQueryService;
import com.dove.stock.domain.entity.DailyStockPrice;
import com.dove.indicator.application.service.TechnicalIndicatorQueryService;
import com.dove.indicator.domain.enums.IndicatorType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class FilterExecutionServiceTest {

    @Mock SearchFilterQueryService searchFilterQueryService;
    @Mock StockSetQueryService stockSetQueryService;
    @Mock DailyStockPriceQueryService priceQueryService;
    @Mock TechnicalIndicatorQueryService indicatorQueryService;
    @Mock StockQueryService stockQueryService;

    FilterExecutionService service;

    private static final Long MEMBER_ID = 1L;
    private static final Long FILTER_ID = 10L;
    private static final LocalDate DATE = LocalDate.of(2024, 1, 3);

    @BeforeEach
    void setUp() {
        service = new FilterExecutionService(
                searchFilterQueryService, stockSetQueryService,
                priceQueryService, indicatorQueryService,
                stockQueryService, new ObjectMapper());
    }

    // ─── PRICE_VALUE 조건 ───────────────────────────────────────────────────

    @Test
    @DisplayName("PRICE_VALUE GT — 종가 > 임계값이면 매칭")
    void shouldMatchWhenClosePriceGtThreshold() {
        setupFilter("""
                {"nodeType":"CONDITION","conditionType":"PRICE_VALUE","priceField":"CLOSE","operator":"GT","value":50000}
                """);
        setupStock("005930");
        setupPrice("005930", 51000L);
        setupEmptyIndicators();

        ExecuteFilterResponse result = service.execute(MEMBER_ID, FILTER_ID, DATE);

        assertThat(result.matchCount()).isEqualTo(1);
        assertThat(result.results().get(0).code()).isEqualTo("005930");
    }

    @Test
    @DisplayName("PRICE_VALUE GT — 종가 <= 임계값이면 미매칭")
    void shouldNotMatchWhenClosePriceNotGt() {
        setupFilter("""
                {"nodeType":"CONDITION","conditionType":"PRICE_VALUE","priceField":"CLOSE","operator":"GT","value":51000}
                """);
        setupStock("005930");
        setupPrice("005930", 51000L);
        setupEmptyIndicators();

        assertThat(service.execute(MEMBER_ID, FILTER_ID, DATE).matchCount()).isZero();
    }

    @Test
    @DisplayName("PRICE_VALUE LTE — 종가 <= 임계값이면 매칭")
    void shouldMatchWhenClosePriceLte() {
        setupFilter("""
                {"nodeType":"CONDITION","conditionType":"PRICE_VALUE","priceField":"CLOSE","operator":"LTE","value":51000}
                """);
        setupStock("005930");
        setupPrice("005930", 51000L);
        setupEmptyIndicators();

        assertThat(service.execute(MEMBER_ID, FILTER_ID, DATE).matchCount()).isEqualTo(1);
    }

    // ─── INDICATOR_VALUE 조건 ───────────────────────────────────────────────

    @Test
    @DisplayName("INDICATOR_VALUE GTE — 지표 >= 임계값이면 매칭")
    void shouldMatchWhenIndicatorGte() {
        setupFilter("""
                {"nodeType":"CONDITION","conditionType":"INDICATOR_VALUE","indicator":"SMA_5","operator":"GTE","value":50000}
                """);
        setupStock("005930");
        setupNullPrice("005930");
        setupIndicator("005930", IndicatorType.SMA_5, 50000.0);

        assertThat(service.execute(MEMBER_ID, FILTER_ID, DATE).matchCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("INDICATOR_VALUE — 지표 없으면 미매칭")
    void shouldNotMatchWhenIndicatorMissing() {
        setupFilter("""
                {"nodeType":"CONDITION","conditionType":"INDICATOR_VALUE","indicator":"SMA_5","operator":"GT","value":0}
                """);
        setupStock("005930");
        setupNullPrice("005930");
        setupEmptyIndicators();

        assertThat(service.execute(MEMBER_ID, FILTER_ID, DATE).matchCount()).isZero();
    }

    // ─── INDICATOR_RANGE 조건 ──────────────────────────────────────────────

    @Test
    @DisplayName("INDICATOR_RANGE — 범위 내 지표 매칭 (inclusive)")
    void shouldMatchWhenIndicatorInRange() {
        setupFilter("""
                {"nodeType":"CONDITION","conditionType":"INDICATOR_RANGE","indicator":"SMA_5",
                 "minValue":40000,"maxValue":60000,"minInclusive":true,"maxInclusive":true}
                """);
        setupStock("005930");
        setupNullPrice("005930");
        setupIndicator("005930", IndicatorType.SMA_5, 50000.0);

        assertThat(service.execute(MEMBER_ID, FILTER_ID, DATE).matchCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("INDICATOR_RANGE — 경계 초과 미매칭")
    void shouldNotMatchWhenIndicatorOutOfRange() {
        setupFilter("""
                {"nodeType":"CONDITION","conditionType":"INDICATOR_RANGE","indicator":"SMA_5",
                 "minValue":40000,"maxValue":49999,"minInclusive":true,"maxInclusive":true}
                """);
        setupStock("005930");
        setupNullPrice("005930");
        setupIndicator("005930", IndicatorType.SMA_5, 50000.0);

        assertThat(service.execute(MEMBER_ID, FILTER_ID, DATE).matchCount()).isZero();
    }

    // ─── INDICATOR_CROSS 조건 ─────────────────────────────────────────────

    @Test
    @DisplayName("INDICATOR_CROSS GT — 왼쪽 > 오른쪽 지표이면 매칭")
    void shouldMatchWhenLeftIndicatorGtRight() {
        setupFilter("""
                {"nodeType":"CONDITION","conditionType":"INDICATOR_CROSS","leftIndicator":"SMA_5","rightIndicator":"SMA_20","operator":"GT"}
                """);
        setupStock("005930");
        setupNullPrice("005930");
        given(indicatorQueryService.findAllByMarketsAndDate(any(), eq(DATE)))
                .willReturn(Map.of("005930", Map.of(IndicatorType.SMA_5, 55000.0, IndicatorType.SMA_20, 50000.0)));

        assertThat(service.execute(MEMBER_ID, FILTER_ID, DATE).matchCount()).isEqualTo(1);
    }

    // ─── VOLUME 조건 ──────────────────────────────────────────────────────

    @Test
    @DisplayName("VOLUME_VALUE GT — 거래량 > 임계값이면 매칭")
    void shouldMatchWhenVolumeGt() {
        setupFilter("""
                {"nodeType":"CONDITION","conditionType":"VOLUME_VALUE","operator":"GT","value":500}
                """);
        setupStock("005930");
        setupPrice("005930", 50000L, 1000L);
        setupEmptyIndicators();

        assertThat(service.execute(MEMBER_ID, FILTER_ID, DATE).matchCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("VOLUME_RANGE — 거래량 범위 매칭")
    void shouldMatchWhenVolumeInRange() {
        setupFilter("""
                {"nodeType":"CONDITION","conditionType":"VOLUME_RANGE","minValue":500,"maxValue":2000,"minInclusive":true,"maxInclusive":true}
                """);
        setupStock("005930");
        setupPrice("005930", 50000L, 1000L);
        setupEmptyIndicators();

        assertThat(service.execute(MEMBER_ID, FILTER_ID, DATE).matchCount()).isEqualTo(1);
    }

    // ─── PRICE_RANGE 조건 ─────────────────────────────────────────────────

    @Test
    @DisplayName("PRICE_RANGE — 시가 범위 매칭 exclusive")
    void shouldMatchWhenOpenPriceInRangeExclusive() {
        // setupPrice(code, closePrice=50000) → openPrice = closePrice - 1000 = 49000
        setupFilter("""
                {"nodeType":"CONDITION","conditionType":"PRICE_RANGE","priceField":"OPEN",
                 "minValue":48000,"maxValue":52000,"minInclusive":false,"maxInclusive":false}
                """);
        setupStock("005930");
        setupPrice("005930", 50000L);
        setupEmptyIndicators();

        assertThat(service.execute(MEMBER_ID, FILTER_ID, DATE).matchCount()).isEqualTo(1);
    }

    // ─── PRICE_VS_INDICATOR 조건 ──────────────────────────────────────────

    @Test
    @DisplayName("PRICE_VS_INDICATOR — 종가 > SMA_5이면 매칭")
    void shouldMatchWhenCloseGtMA5() {
        setupFilter("""
                {"nodeType":"CONDITION","conditionType":"PRICE_VS_INDICATOR","priceField":"CLOSE","indicator":"SMA_5","operator":"GT"}
                """);
        setupStock("005930");
        setupPrice("005930", 55000L);
        setupIndicator("005930", IndicatorType.SMA_5, 50000.0);

        assertThat(service.execute(MEMBER_ID, FILTER_ID, DATE).matchCount()).isEqualTo(1);
    }

    // ─── MARKET_FILTER 조건 ───────────────────────────────────────────────

    @Test
    @DisplayName("MARKET_FILTER — 해당 시장이면 매칭")
    void shouldMatchWhenMarketMatches() {
        setupFilter("""
                {"nodeType":"CONDITION","conditionType":"MARKET_FILTER","markets":["KOSPI"]}
                """);
        setupStock("005930");
        setupPrice("005930", 50000L);
        setupEmptyIndicators();

        assertThat(service.execute(MEMBER_ID, FILTER_ID, DATE).matchCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("MARKET_FILTER — 다른 시장이면 미매칭")
    void shouldNotMatchWhenMarketDiffers() {
        setupFilter("""
                {"nodeType":"CONDITION","conditionType":"MARKET_FILTER","markets":["KOSDAQ"]}
                """);
        setupStock("005930");
        setupPrice("005930", 50000L);
        setupEmptyIndicators();

        assertThat(service.execute(MEMBER_ID, FILTER_ID, DATE).matchCount()).isZero();
    }

    // ─── GROUP 로직 ───────────────────────────────────────────────────────

    @Test
    @DisplayName("GROUP AND — 두 조건 모두 만족하면 매칭")
    void shouldMatchWhenBothAndConditionsMet() {
        setupFilter("""
                {"nodeType":"GROUP","children":[
                  {"nodeType":"CONDITION","conditionType":"PRICE_VALUE","priceField":"CLOSE","operator":"GT","value":40000},
                  {"nodeType":"CONDITION","conditionType":"PRICE_VALUE","priceField":"CLOSE","operator":"LT","value":60000}
                ],"childOps":["AND"]}
                """);
        setupStock("005930");
        setupPrice("005930", 50000L);
        setupEmptyIndicators();

        assertThat(service.execute(MEMBER_ID, FILTER_ID, DATE).matchCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("GROUP OR — 하나만 만족해도 매칭")
    void shouldMatchWhenOneOrConditionMet() {
        setupFilter("""
                {"nodeType":"GROUP","children":[
                  {"nodeType":"CONDITION","conditionType":"PRICE_VALUE","priceField":"CLOSE","operator":"GT","value":99000},
                  {"nodeType":"CONDITION","conditionType":"PRICE_VALUE","priceField":"CLOSE","operator":"LT","value":60000}
                ],"childOps":["OR"]}
                """);
        setupStock("005930");
        setupPrice("005930", 50000L);
        setupEmptyIndicators();

        assertThat(service.execute(MEMBER_ID, FILTER_ID, DATE).matchCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("negated 조건 — 결과 반전")
    void shouldInvertResultWhenNegated() {
        setupFilter("""
                {"nodeType":"CONDITION","conditionType":"PRICE_VALUE","priceField":"CLOSE","operator":"GT","value":99000,"negated":true}
                """);
        setupStock("005930");
        setupPrice("005930", 50000L);
        setupEmptyIndicators();

        // 50000 > 99000은 false이지만 negated=true이므로 매칭
        assertThat(service.execute(MEMBER_ID, FILTER_ID, DATE).matchCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("가격·지표 모두 없는 종목 제외")
    void shouldSkipStockWithNoPriceAndNoIndicator() {
        setupFilter("""
                {"nodeType":"CONDITION","conditionType":"PRICE_VALUE","priceField":"CLOSE","operator":"GT","value":0}
                """);
        setupStock("005930");
        given(priceQueryService.findAllByMarketsAndDate(any(), eq(DATE))).willReturn(Map.of());
        given(indicatorQueryService.findAllByMarketsAndDate(any(), eq(DATE))).willReturn(Map.of());

        assertThat(service.execute(MEMBER_ID, FILTER_ID, DATE).matchCount()).isZero();
    }

    @Test
    @DisplayName("evaluationDate — 지정 날짜 그대로 반환")
    void shouldUseProvidedReferenceDate() {
        setupFilter("""
                {"nodeType":"CONDITION","conditionType":"PRICE_VALUE","priceField":"CLOSE","operator":"GT","value":0}
                """);
        setupStock("005930");
        setupPrice("005930", 50000L);
        setupEmptyIndicators();

        ExecuteFilterResponse result = service.execute(MEMBER_ID, FILTER_ID, DATE);

        assertThat(result.evaluationDate()).isEqualTo(DATE);
    }

    // ─── include / exclude StockSet ──────────────────────────────────────

    @Test
    @DisplayName("includeStockSetId 있으면 해당 코드만 결과에 포함")
    void shouldFilterByIncludeStockSet() {
        Long includeSetId = 99L;
        SearchFilter filter = SearchFilter.create(MEMBER_ID, "필터", DateRule.SPECIFIC_DATE,
                List.of(MarketType.KOSPI),
                """
                {"nodeType":"CONDITION","conditionType":"PRICE_VALUE","priceField":"CLOSE","operator":"GT","value":0}
                """, includeSetId, null);
        given(searchFilterQueryService.findByIdAndMemberId(FILTER_ID, MEMBER_ID))
                .willReturn(Optional.of(filter));
        setupStock("005930");
        setupPrice("005930", 50000L);
        setupEmptyIndicators();
        StockSet includeSet = StockSet.create(MEMBER_ID, "포함세트", List.of("000660")); // 005930 미포함
        given(stockSetQueryService.findByIdAndMemberId(includeSetId, MEMBER_ID))
                .willReturn(Optional.of(includeSet));

        assertThat(service.execute(MEMBER_ID, FILTER_ID, DATE).matchCount()).isZero();
    }

    @Test
    @DisplayName("excludeStockSetId 있으면 해당 코드가 결과에서 제외")
    void shouldFilterByExcludeStockSet() {
        Long excludeSetId = 98L;
        SearchFilter filter = SearchFilter.create(MEMBER_ID, "필터", DateRule.SPECIFIC_DATE,
                List.of(MarketType.KOSPI),
                """
                {"nodeType":"CONDITION","conditionType":"PRICE_VALUE","priceField":"CLOSE","operator":"GT","value":0}
                """, null, excludeSetId);
        given(searchFilterQueryService.findByIdAndMemberId(FILTER_ID, MEMBER_ID))
                .willReturn(Optional.of(filter));
        setupStock("005930");
        setupPrice("005930", 50000L);
        setupEmptyIndicators();
        StockSet excludeSet = StockSet.create(MEMBER_ID, "제외세트", List.of("005930")); // 005930 제외
        given(stockSetQueryService.findByIdAndMemberId(excludeSetId, MEMBER_ID))
                .willReturn(Optional.of(excludeSet));

        assertThat(service.execute(MEMBER_ID, FILTER_ID, DATE).matchCount()).isZero();
    }

    // ─── resolveDate / 에러 경로 ─────────────────────────────────────────

    @Test
    @DisplayName("execute — 필터 없으면 404 NOT_FOUND")
    void shouldThrow404WhenFilterNotFound() {
        given(searchFilterQueryService.findByIdAndMemberId(FILTER_ID, MEMBER_ID))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(MEMBER_ID, FILTER_ID, DATE))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("FILTER_NOT_FOUND");
    }

    @Test
    @DisplayName("resolveDate — LATEST + 데이터 있으면 findLatestTradeDate 결과 사용")
    void shouldUseLatestTradeDateForLatestRule() {
        setupFilterWithRule(DateRule.LATEST);
        given(priceQueryService.findLatestTradeDate(any())).willReturn(Optional.of(DATE));
        given(priceQueryService.findAllByMarketsAndDate(any(), eq(DATE))).willReturn(Map.of());
        given(indicatorQueryService.findAllByMarketsAndDate(any(), eq(DATE))).willReturn(Map.of());

        ExecuteFilterResponse result = service.execute(MEMBER_ID, FILTER_ID, null);

        assertThat(result.evaluationDate()).isEqualTo(DATE);
    }

    @Test
    @DisplayName("resolveDate — LATEST + 데이터 없으면 422 NO_DATA_FOR_DATE")
    void shouldThrow422WhenLatestHasNoData() {
        setupFilterWithRule(DateRule.LATEST);
        given(priceQueryService.findLatestTradeDate(any())).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(MEMBER_ID, FILTER_ID, null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("NO_DATA_FOR_DATE");
    }

    @Test
    @DisplayName("resolveDate — PREV_1D: findNthRecentTradeDate(n=0) 호출")
    void shouldCallFindNthRecentTradeDateWithN0ForPrev1D() {
        setupFilterWithRule(DateRule.PREV_1D);
        given(priceQueryService.findNthRecentTradeDate(any(), any(LocalDate.class), eq(false), eq(0L)))
                .willReturn(Optional.of(DATE));
        given(priceQueryService.findAllByMarketsAndDate(any(), eq(DATE))).willReturn(Map.of());
        given(indicatorQueryService.findAllByMarketsAndDate(any(), eq(DATE))).willReturn(Map.of());

        assertThat(service.execute(MEMBER_ID, FILTER_ID, null).evaluationDate()).isEqualTo(DATE);
    }

    @Test
    @DisplayName("resolveDate — PREV_3D: findNthRecentTradeDate(n=2) 호출")
    void shouldCallFindNthRecentTradeDateWithN2ForPrev3D() {
        setupFilterWithRule(DateRule.PREV_3D);
        given(priceQueryService.findNthRecentTradeDate(any(), any(LocalDate.class), eq(false), eq(2L)))
                .willReturn(Optional.of(DATE));
        given(priceQueryService.findAllByMarketsAndDate(any(), eq(DATE))).willReturn(Map.of());
        given(indicatorQueryService.findAllByMarketsAndDate(any(), eq(DATE))).willReturn(Map.of());

        assertThat(service.execute(MEMBER_ID, FILTER_ID, null).evaluationDate()).isEqualTo(DATE);
    }

    @Test
    @DisplayName("resolveDate — PREV_5D: findNthRecentTradeDate(n=4) 호출")
    void shouldCallFindNthRecentTradeDateWithN4ForPrev5D() {
        setupFilterWithRule(DateRule.PREV_5D);
        given(priceQueryService.findNthRecentTradeDate(any(), any(LocalDate.class), eq(false), eq(4L)))
                .willReturn(Optional.of(DATE));
        given(priceQueryService.findAllByMarketsAndDate(any(), eq(DATE))).willReturn(Map.of());
        given(indicatorQueryService.findAllByMarketsAndDate(any(), eq(DATE))).willReturn(Map.of());

        assertThat(service.execute(MEMBER_ID, FILTER_ID, null).evaluationDate()).isEqualTo(DATE);
    }

    @Test
    @DisplayName("resolveDate — PREV_10D: findNthRecentTradeDate(n=9) 호출")
    void shouldCallFindNthRecentTradeDateWithN9ForPrev10D() {
        setupFilterWithRule(DateRule.PREV_10D);
        given(priceQueryService.findNthRecentTradeDate(any(), any(LocalDate.class), eq(false), eq(9L)))
                .willReturn(Optional.of(DATE));
        given(priceQueryService.findAllByMarketsAndDate(any(), eq(DATE))).willReturn(Map.of());
        given(indicatorQueryService.findAllByMarketsAndDate(any(), eq(DATE))).willReturn(Map.of());

        assertThat(service.execute(MEMBER_ID, FILTER_ID, null).evaluationDate()).isEqualTo(DATE);
    }

    // ─── 헬퍼 ────────────────────────────────────────────────────────────

    private void setupFilter(String expression) {
        SearchFilter filter = SearchFilter.create(MEMBER_ID, "테스트필터", DateRule.SPECIFIC_DATE,
                List.of(MarketType.KOSPI), expression, null, null);
        given(searchFilterQueryService.findByIdAndMemberId(FILTER_ID, MEMBER_ID))
                .willReturn(Optional.of(filter));
    }

    private void setupFilterWithRule(DateRule rule) {
        SearchFilter filter = SearchFilter.create(MEMBER_ID, "테스트필터", rule,
                List.of(MarketType.KOSPI),
                """
                {"nodeType":"CONDITION","conditionType":"PRICE_VALUE","priceField":"CLOSE","operator":"GT","value":0}
                """, null, null);
        given(searchFilterQueryService.findByIdAndMemberId(FILTER_ID, MEMBER_ID))
                .willReturn(Optional.of(filter));
    }

    private void setupStock(String code) {
        Stock stock = new Stock(MarketType.KOSPI, code, "테스트주식", LocalDate.of(2000, 1, 2), null);
        given(stockQueryService.findByCodesAsMap(any()))
                .willReturn(Map.of(code, stock));
    }

    private void setupPrice(String code, long closePrice) {
        setupPrice(code, closePrice, 1000L);
    }

    private void setupPrice(String code, long closePrice, long volume) {
        DailyStockPrice price = new DailyStockPrice(MarketType.KOSPI, code, DATE,
                volume, closePrice - 1000L, closePrice, closePrice - 2000L, closePrice + 1000L);
        given(priceQueryService.findAllByMarketsAndDate(any(), eq(DATE)))
                .willReturn(Map.of(code, price));
    }

    private void setupNullPrice(String code) {
        // 주가 없는 종목도 평가 루프에 포함되도록 더미 가격(0) 제공 — 가격 조건은 0으로 평가됨
        DailyStockPrice dummy = new DailyStockPrice(MarketType.KOSPI, code, DATE, 0L, 0L, 0L, 0L, 0L);
        given(priceQueryService.findAllByMarketsAndDate(any(), eq(DATE)))
                .willReturn(Map.of(code, dummy));
    }

    private void setupIndicator(String code, IndicatorType type, double value) {
        given(indicatorQueryService.findAllByMarketsAndDate(any(), eq(DATE)))
                .willReturn(Map.of(code, Map.of(type, value)));
    }

    private void setupEmptyIndicators() {
        given(indicatorQueryService.findAllByMarketsAndDate(any(), eq(DATE))).willReturn(Map.of());
    }
}
