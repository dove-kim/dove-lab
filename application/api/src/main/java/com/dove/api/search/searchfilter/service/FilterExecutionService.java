package com.dove.api.search.searchfilter.service;

import com.dove.api.search.searchfilter.dto.FilterExecutionResult;
import com.dove.api.search.searchfilter.dto.MatchedStock;
import com.dove.indicator.application.service.StockFeatureDailyService;
import com.dove.indicator.domain.enums.IndicatorType;
import com.dove.market.domain.enums.MarketType;
import com.dove.screening.application.service.StockFeatureFilterQueryService;
import com.dove.screening.application.service.StockFilterQueryService;
import com.dove.screening.domain.entity.SearchFilter;
import com.dove.screening.domain.enums.DateRule;
import com.dove.screening.domain.value.EvalContext;
import com.dove.screening.domain.value.FeatureMatch;
import com.dove.screening.domain.value.FilterEvaluator;
import com.dove.screening.domain.value.FilterModel;
import com.dove.screening.domain.value.FilterNode;
import com.dove.stock.application.service.StockPriceQueryService;
import com.dove.stock.application.service.StockQueryService;
import com.dove.stock.domain.entity.Stock;
import com.dove.stock.domain.entity.StockPrice;
import com.dove.stock.domain.enums.PriceType;
import com.dove.stock.domain.enums.StockExchange;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.ArrayList;
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

    /**
     * 검색 필터를 실행해 통과 종목을 반환한다.
     *
     * @throws ResponseStatusException 기준일에 해당하는 거래일 데이터가 없는 경우 (422)
     */
    public FilterExecutionResult execute(SearchFilter filter, LocalDate referenceDate) {
        List<MarketType> markets = filter.getMarkets();
        PriceType priceType = filter.getPriceType();
        List<StockExchange> exchanges = filter.getExchange().resolveExchanges(markets);
        LocalDate evalDate = resolveDate(filter.getDateRule(), exchanges, priceType, referenceDate);
        if (evalDate == null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "NO_DATA_FOR_DATE");
        }

        JsonNode root = filter.getExpression() != null ? filter.getExpression().root() : null;
        FilterNode model = root != null ? FilterModel.parse(root) : null;
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
                        stock.getMarket().name(), r.closePrice(), r.volume()));
            }
            total = (int) featureFilterQueryService.countByExchangesAndDate(exchanges, priceType, evalDate);
        } else {
            // 폴백: SQL로 못 미는 조건(또는 식 없음) — 전 종목 로드 후 인메모리 평가
            Map<String, StockPrice> prices = priceQueryService.findByExchangesAndDate(exchanges, priceType, evalDate);
            Map<String, Map<IndicatorType, Double>> indicators = loadIndicators(exchanges, priceType, evalDate);
            Map<String, Stock> stockByTicker = stockQueryService.findByTickers(prices.keySet());
            Map<String, String> nameByTicker = stockQueryService.findNamesByTickers(prices.keySet());
            matches = new ArrayList<>();
            for (Map.Entry<String, StockPrice> entry : prices.entrySet()) {
                String ticker = entry.getKey();
                StockPrice price = entry.getValue();
                Stock stock = stockByTicker.get(ticker);
                if (stock == null || !markets.contains(stock.getMarket())) continue; // 시장 유니버스 제한
                EvalContext ctx = new EvalContext(stock.getMarket(), indicators.get(ticker), price);
                if (model != null && FilterEvaluator.evaluate(model, ctx)) {
                    matches.add(new MatchedStock(ticker, nameByTicker.getOrDefault(ticker, ticker),
                            stock.getMarket().name(), price.getClosePrice(), price.getVolume()));
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
        Map<String, Map<IndicatorType, Double>> merged = new java.util.HashMap<>();
        for (StockExchange exchange : exchanges) {
            merged.putAll(featureDailyService.findAllByExchangeAndDate(exchange, priceType, date));
        }
        return merged;
    }

    private LocalDate resolveDate(DateRule rule, List<StockExchange> exchanges, PriceType priceType, LocalDate reference) {
        LocalDate ref = reference != null ? reference : LocalDate.now();
        return switch (rule) {
            case LATEST -> priceQueryService.findNthRecentTradeDateByExchanges(exchanges, priceType, LocalDate.now(), 0);
            case SPECIFIC_DATE -> ref;
            case PREV_1D -> priceQueryService.findNthRecentTradeDateByExchanges(exchanges, priceType, ref, 0);
            case PREV_3D -> priceQueryService.findNthRecentTradeDateByExchanges(exchanges, priceType, ref, 2);
            case PREV_5D -> priceQueryService.findNthRecentTradeDateByExchanges(exchanges, priceType, ref, 4);
            case PREV_10D -> priceQueryService.findNthRecentTradeDateByExchanges(exchanges, priceType, ref, 9);
        };
    }

}
