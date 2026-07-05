package com.dove.api.search.searchfilter.service;

import com.dove.api.search.searchfilter.dto.FilterExecutionResult;
import com.dove.api.search.searchfilter.dto.MatchedStock;
import com.dove.indicator.application.service.StockBreadthDailyService;
import com.dove.indicator.application.service.StockFeatureDailyService;
import com.dove.indicator.application.service.StockRankDailyService;
import com.dove.indicator.domain.enums.IndicatorType;
import com.dove.indicator.domain.rank.enums.RankType;
import com.dove.market.domain.enums.MarketType;
import com.dove.modelserving.application.service.ModelScoreQueryService;
import com.dove.screening.application.service.StockFeatureFilterQueryService;
import com.dove.screening.application.service.StockFilterQueryService;
import com.dove.screening.domain.entity.SearchFilter;
import com.dove.screening.domain.enums.DateRule;
import com.dove.screening.domain.value.EvalContext;
import com.dove.screening.domain.value.FeatureMatch;
import com.dove.screening.domain.value.FilterEvaluator;
import com.dove.screening.domain.value.FilterModel;
import com.dove.screening.domain.value.FilterNode;
import com.dove.screening.domain.value.FilterOperands;
import com.dove.stock.application.service.StockDetailQueryService;
import com.dove.stock.application.service.StockPriceQueryService;
import com.dove.stock.application.service.StockQueryService;
import com.dove.stock.domain.entity.Stock;
import com.dove.stock.domain.entity.StockPrice;
import com.dove.stock.domain.enums.PriceType;
import com.dove.stock.domain.enums.StockExchange;
import com.dove.stock.domain.value.StockStatusFlags;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 검색 필터를 실행해 조건에 맞는 종목을 찾는다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FilterExecutionService {

    private final StockFilterQueryService stockFilterQueryService;
    private final StockPriceQueryService priceQueryService;
    private final StockFeatureDailyService featureDailyService;
    private final StockFeatureFilterQueryService featureFilterQueryService;
    private final StockQueryService stockQueryService;
    private final StockDetailQueryService stockDetailQueryService;
    private final StockRankDailyService rankDailyService;
    private final StockBreadthDailyService breadthDailyService;
    private final ModelScoreQueryService modelScoreQueryService;

    /**
     * 검색 필터를 실행해 통과 종목을 반환한다.
     *
     * @throws ResponseStatusException 기준일에 해당하는 거래일 데이터가 없는 경우 (422)
     */
    public FilterExecutionResult execute(SearchFilter filter, LocalDate referenceDate) {
        List<MarketType> markets = filter.getMarkets();
        PriceType priceType = filter.getPriceType();
        List<StockExchange> exchanges = filter.getExchange().resolveExchanges(markets);

        JsonNode root = filter.getExpression() != null ? filter.getExpression().root() : null;
        FilterNode model = root != null ? FilterModel.parse(root) : null;

        // 종목 상태(거래정지·관리종목)는 최신 상세 단건만 보유 → 최신일자에서만 적용하고, 과거일자에선 조건을 무시한다.
        boolean applyStatus = model != null && FilterOperands.usesStockStatus(model)
                && filter.getDateRule() == DateRule.LATEST;

        LocalDate evalDate = resolveDate(filter.getDateRule(), exchanges, priceType, referenceDate);
        if (evalDate == null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "NO_DATA_FOR_DATE");
        }

        Optional<List<FeatureMatch>> dbMatched = model != null
                ? featureFilterQueryService.findMatchingByExpression(exchanges, priceType, evalDate, model)
                : Optional.empty();

        List<MatchedStock> matches;
        int total;
        if (dbMatched.isPresent()) {
            // 검색식을 wide 테이블 WHERE로 밀어넣어 DB가 매칭 종목만 반환 (전 종목 인메모리 로드 없음)
            List<FeatureMatch> rows = dbMatched.get();
            List<String> tickers = rows.stream().map(FeatureMatch::ticker).toList();
            Map<String, Stock> stockByTicker = stockQueryService.findByTickers(tickers);
            Map<String, String> names = stockQueryService.findNamesByTickers(tickers);
            matches = new ArrayList<>();
            for (FeatureMatch r : rows) {
                Stock stock = stockByTicker.get(r.ticker());
                if (stock == null || !markets.contains(stock.getMarket())) continue; // 시장 유니버스 제한
                matches.add(new MatchedStock(r.ticker(), names.getOrDefault(r.ticker(), r.ticker()),
                        stock.getMarket().name(), r.openPrice(), r.highPrice(), r.lowPrice(),
                        r.closePrice(), r.volume(), r.prevClose()));
            }
            total = (int) featureFilterQueryService.countByExchangesAndDate(exchanges, priceType, evalDate);
        } else {
            // 폴백: SQL로 못 미는 조건(또는 식 없음) — 전 종목 로드 후 인메모리 평가
            Map<String, StockPrice> prices = priceQueryService.findByExchangesAndDate(exchanges, priceType, evalDate);
            // 전일 종가(등락률용) — 평가일 직전 거래일 슬라이스
            LocalDate prevDate = priceQueryService.findNthRecentTradeDateByExchanges(exchanges, priceType, evalDate, 1);
            Map<String, StockPrice> prevPrices = prevDate == null
                    ? Map.of() : priceQueryService.findByExchangesAndDate(exchanges, priceType, prevDate);
            Map<String, Map<IndicatorType, Double>> indicators = loadIndicators(exchanges, priceType, evalDate);
            Map<String, Map<RankType, Double>> ranks = model != null && FilterOperands.usesRank(model)
                    ? loadRanks(exchanges, priceType, evalDate) : Map.of();
            Map<StockExchange, Double> breadthByExchange = model != null && FilterOperands.usesBreadth(model)
                    ? loadBreadth(exchanges, priceType, evalDate) : Map.of();
            Map<String, Map<Long, Double>> modelScores = model != null
                    ? loadModelScores(FilterOperands.referencedModelIds(model), evalDate) : Map.of();
            Map<String, StockStatusFlags> statusByTicker = applyStatus
                    ? stockDetailQueryService.findStatusByTickers(prices.keySet()) : Map.of();
            Map<String, Stock> stockByTicker = stockQueryService.findByTickers(prices.keySet());
            Map<String, String> nameByTicker = stockQueryService.findNamesByTickers(prices.keySet());
            matches = new ArrayList<>();
            for (Map.Entry<String, StockPrice> entry : prices.entrySet()) {
                String ticker = entry.getKey();
                StockPrice price = entry.getValue();
                Stock stock = stockByTicker.get(ticker);
                if (stock == null || !markets.contains(stock.getMarket())) continue; // 시장 유니버스 제한
                Double breadth = breadthByExchange.get(StockExchange.fromMarket(stock.getMarket()));
                StockStatusFlags f = statusByTicker.getOrDefault(ticker, new StockStatusFlags(false, false));
                EvalContext ctx = new EvalContext(stock.getMarket(), indicators.get(ticker), price,
                        ranks.get(ticker), modelScores.get(ticker), breadth, f.tradingHalted(), f.adminItem());
                if (model != null && FilterEvaluator.evaluate(model, ctx)) {
                    StockPrice prev = prevPrices.get(ticker);
                    matches.add(new MatchedStock(ticker, nameByTicker.getOrDefault(ticker, ticker),
                            stock.getMarket().name(), price.getOpenPrice(), price.getHighPrice(),
                            price.getLowPrice(), price.getClosePrice(), price.getVolume(),
                            prev != null ? prev.getClosePrice() : null));
                }
            }
            total = prices.size();
        }

        // 종목 필터(stockFilter) 적용 — 지정 시 통과 티커만 유지
        if (filter.getStockFilterId() != null) {
            Set<String> allowed = stockFilterQueryService.resolveTickers(filter.getStockFilterId(), markets);
            matches = matches.stream().filter(m -> allowed.contains(m.ticker())).toList();
        }

        return new FilterExecutionResult(evalDate, total, matches);
    }

    /**
     * 거래소별 지표를 병합해 ticker→지표맵으로 반환한다.
     */
    private Map<String, Map<IndicatorType, Double>> loadIndicators(List<StockExchange> exchanges, PriceType priceType, LocalDate date) {
        Map<String, Map<IndicatorType, Double>> merged = new HashMap<>();
        for (StockExchange exchange : exchanges) {
            merged.putAll(featureDailyService.findAllByExchangeAndDate(exchange, priceType, date));
        }
        return merged;
    }

    /**
     * 거래소별 순위를 병합해 ticker→순위맵으로 반환한다.
     */
    private Map<String, Map<RankType, Double>> loadRanks(List<StockExchange> exchanges, PriceType priceType, LocalDate date) {
        Map<String, Map<RankType, Double>> merged = new HashMap<>();
        for (StockExchange exchange : exchanges) {
            merged.putAll(rankDailyService.findAllByExchangeAndDate(exchange, priceType, date));
        }
        return merged;
    }

    /**
     * 거래소별 당일 상승비율을 모아 거래소→상승비율 맵으로 반환한다.
     */
    private Map<StockExchange, Double> loadBreadth(List<StockExchange> exchanges, PriceType priceType, LocalDate date) {
        Map<StockExchange, Double> byExchange = new HashMap<>();
        for (StockExchange exchange : exchanges) {
            breadthDailyService.findAdvanceRatio(exchange, priceType, date)
                    .ifPresent(ratio -> byExchange.put(exchange, ratio));
        }
        return byExchange;
    }

    /**
     * 참조 모델별 점수를 모아 ticker→(modelId→점수)맵으로 반환한다.
     */
    private Map<String, Map<Long, Double>> loadModelScores(Set<Long> modelIds, LocalDate date) {
        Map<String, Map<Long, Double>> byTicker = new HashMap<>();
        for (Long modelId : modelIds) {
            Map<String, Double> scores = modelScoreQueryService.findScoresByModelAndDate(modelId, date);
            for (Map.Entry<String, Double> e : scores.entrySet()) {
                byTicker.computeIfAbsent(e.getKey(), t -> new HashMap<>()).put(modelId, e.getValue());
            }
        }
        return byTicker;
    }

    private LocalDate resolveDate(DateRule rule, List<StockExchange> exchanges, PriceType priceType, LocalDate reference) {
        LocalDate ref = reference != null ? reference : LocalDate.now();
        return switch (rule) {
            // 선택한 기준일(ref) 이하의 최신 거래일 — ref가 최신일이면 최신, 과거로 이동하면 그 날짜 기준(날짜 네비게이션 반영)
            case LATEST -> priceQueryService.findNthRecentTradeDateByExchanges(exchanges, priceType, ref, 0);
            case SPECIFIC_DATE -> ref;
            case PREV_1D -> priceQueryService.findNthRecentTradeDateByExchanges(exchanges, priceType, ref, 0);
            case PREV_3D -> priceQueryService.findNthRecentTradeDateByExchanges(exchanges, priceType, ref, 2);
            case PREV_5D -> priceQueryService.findNthRecentTradeDateByExchanges(exchanges, priceType, ref, 4);
            case PREV_10D -> priceQueryService.findNthRecentTradeDateByExchanges(exchanges, priceType, ref, 9);
        };
    }

}
