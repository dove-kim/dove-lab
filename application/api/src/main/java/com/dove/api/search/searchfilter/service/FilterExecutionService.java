package com.dove.api.search.searchfilter.service;

import com.dove.api.search.searchfilter.dto.FilterExecutionResult;
import com.dove.api.search.searchfilter.dto.MatchedStock;
import com.dove.indicator.application.service.StockFeatureDailyService;
import com.dove.custommetric.application.service.CustomMetricDailyService;
import com.dove.fundamental.application.StockValuationQueryService;
import com.dove.indicator.application.service.StockRankDailyService;
import com.dove.indicator.domain.enums.IndicatorType;
import com.dove.indicator.domain.rank.enums.RankType;
import com.dove.market.domain.enums.MarketType;
import com.dove.modelserving.application.service.ModelScoreQueryService;
import com.dove.screening.application.service.StockFeatureFilterQueryService;
import com.dove.screening.application.service.StockFilterQueryService;
import com.dove.screening.domain.entity.SearchFilter;
import com.dove.screening.domain.enums.DateRule;
import com.dove.screening.domain.pipeline.FilterStage;
import com.dove.screening.domain.pipeline.PipelineStage;
import com.dove.screening.domain.pipeline.RankStage;
import com.dove.screening.domain.pipeline.SearchPipeline;
import com.dove.screening.domain.pipeline.SortDirection;
import com.dove.screening.domain.pipeline.SortField;
import com.dove.screening.domain.pipeline.SortKey;
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
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

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
    private final CustomMetricDailyService customMetricDailyService;
    private final ModelScoreQueryService modelScoreQueryService;
    private final StockValuationQueryService valuationQueryService;

    /**
     * 검색 필터를 실행해 통과 종목을 반환한다(모델 점수 게이트 없음 = 전체 노출).
     *
     * @throws ResponseStatusException 기준일에 해당하는 거래일 데이터가 없는 경우 (422)
     */
    public FilterExecutionResult execute(SearchFilter filter, LocalDate referenceDate) {
        return execute(filter, referenceDate, null);
    }

    /**
     * 검색 필터를 실행해 통과 종목을 반환한다. 표시 대상 모델이 가시 집합에 없으면 그 종목의 모델 점수를 숨긴다(정렬은 유지).
     *
     * @param visibleModelIds 사용자가 볼 수 있는 모델 ID 집합. null = 전체 허용(ROOT), 빈 집합 = 아무 모델도 못 봄
     * @throws ResponseStatusException 기준일에 해당하는 거래일 데이터가 없는 경우 (422)
     */
    public FilterExecutionResult execute(SearchFilter filter, LocalDate referenceDate, Set<Long> visibleModelIds) {
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

        // 파이프라인(순서 단계)이 지정되면 전 단계를 단일 거래일 인메모리로 순차 실행한다.
        List<PipelineStage> pipelineStages = SearchPipeline.parse(filter.getPipeline());
        if (!pipelineStages.isEmpty()) {
            return executePipeline(filter, markets, priceType, exchanges, model, applyStatus, evalDate,
                    pipelineStages, visibleModelIds);
        }

        // 표시 대상 모델 = 필터가 참조하는 첫 모델(파이프라인 없는 경로라 RANK 정렬키는 없음). 미가시 모델이면 점수 숨김.
        Long displayModelId = visibleDisplayModelId(model != null ? firstReferencedModelId(model) : null, visibleModelIds);

        // 리스트 정렬(시총)용 티커별 시가총액 — 단일 거래일 슬라이스라 항상 로드해도 저렴
        Map<String, Long> marketCaps = valuationQueryService.findMarketCapByDate(evalDate);

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
            // 이 경로는 모델점수를 안 불러오므로 표시 대상 모델이 있으면 그 점수만 1회 로드
            Map<String, Map<Long, Double>> modelScores = displayModelId != null
                    ? loadModelScores(Set.of(displayModelId), evalDate) : Map.of();
            matches = new ArrayList<>();
            for (FeatureMatch r : rows) {
                Stock stock = stockByTicker.get(r.ticker());
                if (stock == null || !markets.contains(stock.getMarket())) continue; // 시장 유니버스 제한
                matches.add(new MatchedStock(r.ticker(), names.getOrDefault(r.ticker(), r.ticker()),
                        stock.getMarket().name(), r.openPrice(), r.highPrice(), r.lowPrice(),
                        r.closePrice(), r.volume(), r.prevClose(), marketCaps.get(r.ticker()),
                        scoreOf(modelScores, r.ticker(), displayModelId)));
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
            Map<Long, Double> customMetrics = model != null
                    ? loadCustomMetrics(FilterOperands.referencedCustomMetricIds(model), evalDate) : Map.of();
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
                StockStatusFlags f = statusByTicker.getOrDefault(ticker, new StockStatusFlags(false, false));
                EvalContext ctx = new EvalContext(stock.getMarket(), indicators.get(ticker), price,
                        ranks.get(ticker), modelScores.get(ticker), customMetrics,
                        f.tradingHalted(), f.adminItem());
                if (model != null && FilterEvaluator.evaluate(model, ctx)) {
                    StockPrice prev = prevPrices.get(ticker);
                    matches.add(new MatchedStock(ticker, nameByTicker.getOrDefault(ticker, ticker),
                            stock.getMarket().name(), price.getOpenPrice(), price.getHighPrice(),
                            price.getLowPrice(), price.getClosePrice(), price.getVolume(),
                            prev != null ? prev.getClosePrice() : null, marketCaps.get(ticker),
                            scoreOf(modelScores, ticker, displayModelId)));
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
     * EXPRESSION(첫 단계) + PIPELINE 단계를 단일 거래일 인메모리로 순차 실행한다.
     */
    private FilterExecutionResult executePipeline(SearchFilter filter, List<MarketType> markets, PriceType priceType,
                                                  List<StockExchange> exchanges, FilterNode expressionModel,
                                                  boolean applyStatus, LocalDate evalDate,
                                                  List<PipelineStage> pipelineStages, Set<Long> visibleModelIds) {
        // 실제 실행 단계 = [FILTER(EXPRESSION)] + PIPELINE (EXPRESSION이 있으면 항상 첫 단계)
        List<PipelineStage> stages = new ArrayList<>();
        if (expressionModel != null) stages.add(new FilterStage(expressionModel));
        stages.addAll(pipelineStages);

        // 유니버스: 평가일 전 종목 + 전일 종가(등락률용)
        Map<String, StockPrice> prices = priceQueryService.findByExchangesAndDate(exchanges, priceType, evalDate);
        LocalDate prevDate = priceQueryService.findNthRecentTradeDateByExchanges(exchanges, priceType, evalDate, 1);
        Map<String, StockPrice> prevPrices = prevDate == null
                ? Map.of() : priceQueryService.findByExchangesAndDate(exchanges, priceType, prevDate);
        Map<String, Map<IndicatorType, Double>> indicators = loadIndicators(exchanges, priceType, evalDate);

        // 전 FILTER 단계 트리 합집합으로 필요한 부가 데이터만 로드
        List<FilterNode> filterNodes = stages.stream()
                .filter(s -> s instanceof FilterStage)
                .map(s -> ((FilterStage) s).filter())
                .toList();
        boolean usesRank = filterNodes.stream().anyMatch(FilterOperands::usesRank);
        Map<String, Map<RankType, Double>> ranks = usesRank ? loadRanks(exchanges, priceType, evalDate) : Map.of();
        Set<Long> customMetricIds = new LinkedHashSet<>();
        Set<Long> modelIds = new LinkedHashSet<>();
        for (FilterNode n : filterNodes) {
            customMetricIds.addAll(FilterOperands.referencedCustomMetricIds(n));
            modelIds.addAll(FilterOperands.referencedModelIds(n));
        }
        // RANK 단계 MODEL_SCORE 정렬키가 참조하는 모델점수도 로드 대상에 합친다
        modelIds.addAll(rankModelIds(pipelineStages));
        Map<Long, Double> customMetrics = loadCustomMetrics(customMetricIds, evalDate);
        Map<String, Map<Long, Double>> modelScores = loadModelScores(modelIds, evalDate);
        // 표시 대상 모델 = RANK MODEL_SCORE 정렬키의 첫 모델, 없으면 필터 참조 첫 모델. 미가시 모델이면 점수 숨김(정렬은 유지).
        Long displayModelId = visibleDisplayModelId(resolveDisplayModelId(filterNodes, pipelineStages), visibleModelIds);
        boolean needStatus = applyStatus && filterNodes.stream().anyMatch(FilterOperands::usesStockStatus);
        Map<String, StockStatusFlags> statusByTicker = needStatus
                ? stockDetailQueryService.findStatusByTickers(prices.keySet()) : Map.of();

        // RANK 단계에서 시가총액을 쓰면 시총 로드
        boolean usesMarketCap = pipelineStages.stream().anyMatch(s -> s instanceof RankStage r
                && r.sortKeys().stream().anyMatch(k -> k.field() == SortField.MARKET_CAP));
        Map<String, Long> marketCaps = usesMarketCap ? valuationQueryService.findMarketCapByDate(evalDate) : Map.of();

        Map<String, Stock> stockByTicker = stockQueryService.findByTickers(prices.keySet());
        Map<String, String> nameByTicker = stockQueryService.findNamesByTickers(prices.keySet());
        Set<String> allowedByStockFilter = filter.getStockFilterId() != null
                ? stockFilterQueryService.resolveTickers(filter.getStockFilterId(), markets) : null;

        // 후보 + 컨텍스트 조립 (시장 유니버스 + 종목필터 교집합)
        Map<String, EvalContext> contexts = new HashMap<>();
        List<String> candidates = new ArrayList<>();
        for (Map.Entry<String, StockPrice> entry : prices.entrySet()) {
            String ticker = entry.getKey();
            Stock stock = stockByTicker.get(ticker);
            if (stock == null || !markets.contains(stock.getMarket())) continue;
            if (allowedByStockFilter != null && !allowedByStockFilter.contains(ticker)) continue;
            StockStatusFlags f = statusByTicker.getOrDefault(ticker, new StockStatusFlags(false, false));
            contexts.put(ticker, new EvalContext(stock.getMarket(), indicators.get(ticker), entry.getValue(),
                    ranks.get(ticker), modelScores.get(ticker), customMetrics, f.tradingHalted(), f.adminItem()));
            candidates.add(ticker);
        }

        // 단계 순차 실행: FILTER=축소, RANK=정렬 후 상위 N
        for (PipelineStage stage : stages) {
            if (stage instanceof FilterStage fs) {
                candidates = candidates.stream()
                        .filter(t -> FilterEvaluator.evaluate(fs.filter(), contexts.get(t)))
                        .collect(Collectors.toCollection(ArrayList::new));
            } else if (stage instanceof RankStage rs) {
                candidates = applyRank(candidates, rs, prices, prevPrices, marketCaps, modelScores);
            }
        }

        int total = prices.size();
        List<MatchedStock> matches = new ArrayList<>();
        for (String ticker : candidates) {
            StockPrice price = prices.get(ticker);
            StockPrice prev = prevPrices.get(ticker);
            matches.add(new MatchedStock(ticker, nameByTicker.getOrDefault(ticker, ticker),
                    stockByTicker.get(ticker).getMarket().name(), price.getOpenPrice(), price.getHighPrice(),
                    price.getLowPrice(), price.getClosePrice(), price.getVolume(),
                    prev != null ? prev.getClosePrice() : null, marketCaps.get(ticker),
                    scoreOf(modelScores, ticker, displayModelId)));
        }
        return new FilterExecutionResult(evalDate, total, matches);
    }

    /**
     * RANK 단계를 적용한다 — 정렬 키로 정렬 후 limit이 있으면 상위 N개만 남긴다.
     */
    private List<String> applyRank(List<String> candidates, RankStage stage, Map<String, StockPrice> prices,
                                   Map<String, StockPrice> prevPrices, Map<String, Long> marketCaps,
                                   Map<String, Map<Long, Double>> modelScores) {
        List<String> sorted = new ArrayList<>(candidates);
        Comparator<String> comparator = null;
        for (SortKey key : stage.sortKeys()) {
            Comparator<String> next = comparatorFor(key, prices, prevPrices, marketCaps, modelScores);
            comparator = comparator == null ? next : comparator.thenComparing(next);
        }
        if (comparator != null) sorted.sort(comparator);
        if (stage.limit() != null) {
            int n = Math.max(0, Math.min(sorted.size(), stage.limit()));
            return new ArrayList<>(sorted.subList(0, n));
        }
        return sorted;
    }

    /**
     * 정렬 키 하나에 대한 비교자를 만든다 — 방향 반영, null 값은 방향 무관 항상 마지막.
     */
    private Comparator<String> comparatorFor(SortKey key, Map<String, StockPrice> prices,
                                             Map<String, StockPrice> prevPrices, Map<String, Long> marketCaps,
                                             Map<String, Map<Long, Double>> modelScores) {
        Function<String, Double> value = t -> sortValue(key, t, prices, prevPrices, marketCaps, modelScores);
        Comparator<Double> order = key.direction() == SortDirection.DESC
                ? Comparator.reverseOrder() : Comparator.naturalOrder();
        return Comparator.comparing(value, Comparator.nullsLast(order));
    }

    /**
     * 정렬 키의 종목 값을 반환한다(없으면 null).
     */
    private Double sortValue(SortKey key, String ticker, Map<String, StockPrice> prices,
                            Map<String, StockPrice> prevPrices, Map<String, Long> marketCaps,
                            Map<String, Map<Long, Double>> modelScores) {
        return switch (key.field()) {
            case CHANGE_RATE -> {
                StockPrice p = prices.get(ticker);
                StockPrice prev = prevPrices.get(ticker);
                Long close = p != null ? p.getClosePrice() : null;
                Long prevClose = prev != null ? prev.getClosePrice() : null;
                yield close != null && prevClose != null && prevClose > 0
                        ? (close - prevClose) / (double) prevClose : null;
            }
            case MARKET_CAP -> {
                Long cap = marketCaps.get(ticker);
                yield cap != null ? cap.doubleValue() : null;
            }
            case VOLUME -> {
                StockPrice p = prices.get(ticker);
                yield p != null && p.getVolume() != null ? p.getVolume().doubleValue() : null;
            }
            case MODEL_SCORE -> scoreOf(modelScores, ticker, key.modelId());
        };
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
     * 참조 커스텀 지표별 계산값을 모아 metricId→값 맵으로 반환한다(거래일당 시장 단일 스칼라).
     */
    private Map<Long, Double> loadCustomMetrics(Set<Long> metricIds, LocalDate date) {
        Map<Long, Double> byId = new HashMap<>();
        for (Long metricId : metricIds) {
            customMetricDailyService.findValue(metricId, date).ifPresent(v -> byId.put(metricId, v));
        }
        return byId;
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

    /**
     * 결과에 표시할 모델 ID를 정한다 — RANK 단계 MODEL_SCORE 정렬키의 첫 모델, 없으면 필터 참조 첫 모델, 그것도 없으면 null.
     */
    private Long resolveDisplayModelId(List<FilterNode> filterNodes, List<PipelineStage> pipelineStages) {
        Set<Long> rankIds = rankModelIds(pipelineStages);
        if (!rankIds.isEmpty()) return rankIds.iterator().next();
        for (FilterNode n : filterNodes) {
            Long id = firstReferencedModelId(n);
            if (id != null) return id;
        }
        return null;
    }

    /**
     * 필터 트리가 참조하는 첫 모델 ID를 반환한다(없으면 null).
     */
    private Long firstReferencedModelId(FilterNode node) {
        Set<Long> ids = FilterOperands.referencedModelIds(node);
        return ids.isEmpty() ? null : ids.iterator().next();
    }

    /**
     * RANK 단계의 MODEL_SCORE 정렬키가 참조하는 모델 ID 집합을 순서대로 반환한다.
     */
    private Set<Long> rankModelIds(List<PipelineStage> pipelineStages) {
        Set<Long> ids = new LinkedHashSet<>();
        for (PipelineStage stage : pipelineStages) {
            if (stage instanceof RankStage rs) {
                for (SortKey key : rs.sortKeys()) {
                    if (key.field() == SortField.MODEL_SCORE && key.modelId() != null) ids.add(key.modelId());
                }
            }
        }
        return ids;
    }

    /**
     * 표시 대상 모델 ID를 가시성으로 게이트한다 — 가시 집합이 있고 그 안에 없으면 null(점수 투영 안 함).
     */
    private Long visibleDisplayModelId(Long displayModelId, Set<Long> visibleModelIds) {
        boolean canSee = visibleModelIds == null || (displayModelId != null && visibleModelIds.contains(displayModelId));
        return canSee ? displayModelId : null;
    }

    /**
     * 티커의 표시 대상 모델 점수를 반환한다 — 대상 모델이 없거나 점수 없는 종목이면 null.
     */
    private Double scoreOf(Map<String, Map<Long, Double>> modelScores, String ticker, Long modelId) {
        if (modelId == null) return null;
        Map<Long, Double> byModel = modelScores.get(ticker);
        return byModel != null ? byModel.get(modelId) : null;
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
