package com.dove.api.stock.filter.service;

import com.dove.market.domain.enums.MarketType;
import com.dove.stock.application.service.StockQueryService;
import com.dove.stock.domain.entity.Stock;
import com.dove.api.stock.filter.dto.ExecuteFilterResponse;
import com.dove.api.stock.filter.dto.StockMatchResult;
import com.dove.screening.application.service.SearchFilterQueryService;
import com.dove.screening.application.service.StockSetQueryService;
import com.dove.screening.domain.entity.SearchFilter;
import com.dove.screening.domain.entity.StockSet;
import com.dove.screening.domain.enums.DateRule;
import com.dove.stock.application.service.DailyStockPriceQueryService;
import com.dove.stock.domain.entity.DailyStockPrice;
import com.dove.indicator.application.service.TechnicalIndicatorQueryService;
import com.dove.indicator.domain.enums.IndicatorType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FilterExecutionService {

    private final SearchFilterQueryService searchFilterQueryService;
    private final StockSetQueryService stockSetQueryService;
    private final DailyStockPriceQueryService priceQueryService;
    private final TechnicalIndicatorQueryService indicatorQueryService;
    private final StockQueryService stockQueryService;
    private final ObjectMapper objectMapper;

    public ExecuteFilterResponse execute(Long memberId, Long filterId, LocalDate referenceDate) {
        SearchFilter filter = searchFilterQueryService.findByIdAndMemberId(filterId, memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "FILTER_NOT_FOUND"));

        List<MarketType> markets = filter.getMarketList();
        LocalDate evalDate = referenceDate != null
                ? referenceDate
                : resolveDate(filter.getDateRule(), markets, LocalDate.now());

        if (evalDate == null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "NO_DATA_FOR_DATE");
        }

        Map<String, DailyStockPrice> prices = priceQueryService.findAllByMarketsAndDate(markets, evalDate);
        Map<String, Map<IndicatorType, Double>> indicators = indicatorQueryService.findAllByMarketsAndDate(markets, evalDate);
        // 거래된 종목만 후보 — 거래정지 종목은 주가 없으므로 자연스럽게 제외
        Map<String, Stock> stockByCode = stockQueryService.findByCodesAsMap(prices.keySet());

        JsonNode root = parseExpression(filter.getExpression());

        List<StockMatchResult> results = new ArrayList<>();
        for (Map.Entry<String, DailyStockPrice> entry : prices.entrySet()) {
            String code = entry.getKey();
            DailyStockPrice price = entry.getValue();
            Stock stock = stockByCode.get(code);
            if (stock == null) continue;

            Map<IndicatorType, Double> stockIndicators = indicators.get(code);
            EvalContext ctx = new EvalContext(code, stock.getName(),
                    stock.getId().getMarketType(), stockIndicators, price);

            if (evaluate(root, ctx)) {
                results.add(new StockMatchResult(
                        code,
                        stock.getName(),
                        stock.getId().getMarketType().name(),
                        price.getClosePrice(),
                        price.getVolume()
                ));
            }
        }

        // 필터 레벨 포함/제외 적용 (포함 먼저, 제외 나중 → 겹치면 제외 우선)
        if (filter.getIncludeStockSetId() != null) {
            Set<String> includeCodes = stockSetQueryService
                    .findByIdAndMemberId(filter.getIncludeStockSetId(), memberId)
                    .map(StockSet::getCodeSet)
                    .orElse(Set.of());
            results = results.stream().filter(r -> includeCodes.contains(r.code())).toList();
        }
        if (filter.getExcludeStockSetId() != null) {
            Set<String> excludeCodes = stockSetQueryService
                    .findByIdAndMemberId(filter.getExcludeStockSetId(), memberId)
                    .map(StockSet::getCodeSet)
                    .orElse(Set.of());
            results = results.stream().filter(r -> !excludeCodes.contains(r.code())).toList();
        }

        return new ExecuteFilterResponse(
                filter.getId(),
                filter.getName(),
                evalDate,
                filter.getDateRule().name(),
                markets.stream().map(MarketType::name).toList(),
                prices.size(),
                results.size(),
                results
        );
    }

    // ─── 날짜 해소 ────────────────────────────────────────────────────────────

    private LocalDate resolveDate(DateRule rule, List<MarketType> markets, LocalDate reference) {
        return switch (rule) {
            case LATEST -> priceQueryService.findLatestTradeDate(markets).orElse(null);
            case SPECIFIC_DATE -> reference;
            case PREV_1D -> priceQueryService.findNthRecentTradeDate(markets, reference, false, 0).orElse(null);
            case PREV_3D -> priceQueryService.findNthRecentTradeDate(markets, reference, false, 2).orElse(null);
            case PREV_5D -> priceQueryService.findNthRecentTradeDate(markets, reference, false, 4).orElse(null);
            case PREV_10D -> priceQueryService.findNthRecentTradeDate(markets, reference, false, 9).orElse(null);
        };
    }

    // ─── 식 파싱 ──────────────────────────────────────────────────────────────

    private JsonNode parseExpression(String expression) {
        try {
            return objectMapper.readTree(expression);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_EXPRESSION");
        }
    }

    // ─── 식 평가 (재귀) ───────────────────────────────────────────────────────

    private boolean evaluate(JsonNode node, EvalContext ctx) {
        String nodeType = node.path("nodeType").asText();

        // 새 포맷: negated 필드 사용. 구 포맷 호환: logic = "NOT" 이면 negated=true 로 처리.
        boolean negated = node.has("negated")
                ? node.path("negated").asBoolean(false)
                : "NOT".equals(node.path("logic").asText("AND"));

        if ("GROUP".equals(nodeType)) {
            JsonNode children = node.path("children");
            JsonNode childOps = node.path("childOps");

            // 구 포맷에서 logic(AND/OR)을 기본 childOp으로 사용
            String legacyDefault = "AND";
            if (!node.has("negated")) {
                String oldLogic = node.path("logic").asText("AND");
                if (!"NOT".equals(oldLogic)) legacyDefault = oldLogic;
            }
            final String defaultOp = legacyDefault;

            if (children.isEmpty()) return !negated;

            boolean result = evaluate(children.get(0), ctx);
            for (int i = 1; i < children.size(); i++) {
                String op = (childOps.isArray() && childOps.size() > i - 1)
                        ? childOps.get(i - 1).asText(defaultOp)
                        : defaultOp;
                boolean childResult = evaluate(children.get(i), ctx);
                result = switch (op) {
                    case "OR"      -> result || childResult;
                    case "AND_NOT" -> result && !childResult;   // 구 포맷 호환
                    case "OR_NOT"  -> result || !childResult;   // 구 포맷 호환
                    default        -> result && childResult;    // AND
                };
            }
            return negated ? !result : result;
        }

        boolean condResult = evaluateCondition(node, ctx);
        return negated ? !condResult : condResult;
    }

    private boolean evaluateCondition(JsonNode node, EvalContext ctx) {
        String type = node.path("conditionType").asText();
        return switch (type) {
            case "INDICATOR_VALUE" -> evalIndicatorValue(node, ctx);
            case "INDICATOR_RANGE" -> evalIndicatorRange(node, ctx);
            case "INDICATOR_CROSS" -> evalIndicatorCross(node, ctx);
            case "PRICE_VALUE" -> evalPriceValue(node, ctx);
            case "PRICE_RANGE" -> evalPriceRange(node, ctx);
            case "VOLUME_VALUE" -> evalVolumeValue(node, ctx);
            case "VOLUME_RANGE" -> evalVolumeRange(node, ctx);
            case "PRICE_VS_INDICATOR" -> evalPriceVsIndicator(node, ctx);
            case "MARKET_FILTER" -> evalMarketFilter(node, ctx);
            default -> false;
        };
    }

    // ─── 조건별 평가 ──────────────────────────────────────────────────────────

    private boolean evalIndicatorValue(JsonNode n, EvalContext ctx) {
        Double val = getIndicator(ctx, n.path("indicator").asText());
        if (val == null) return false;
        return applyOp(val, n.path("operator").asText(), n.path("value").asDouble());
    }

    private boolean evalIndicatorRange(JsonNode n, EvalContext ctx) {
        Double val = getIndicator(ctx, n.path("indicator").asText());
        if (val == null) return false;
        double min = n.path("minValue").asDouble();
        double max = n.path("maxValue").asDouble();
        boolean minInc = n.path("minInclusive").asBoolean(true);
        boolean maxInc = n.path("maxInclusive").asBoolean(true);
        boolean lo = minInc ? val >= min : val > min;
        boolean hi = maxInc ? val <= max : val < max;
        return lo && hi;
    }

    private boolean evalIndicatorCross(JsonNode n, EvalContext ctx) {
        Double left = getIndicator(ctx, n.path("leftIndicator").asText());
        Double right = getIndicator(ctx, n.path("rightIndicator").asText());
        if (left == null || right == null) return false;
        return applyOp(left, n.path("operator").asText(), right);
    }

    private boolean evalPriceValue(JsonNode n, EvalContext ctx) {
        Long price = getPriceField(ctx, n.path("priceField").asText());
        if (price == null) return false;
        return applyOp(price.doubleValue(), n.path("operator").asText(), n.path("value").asDouble());
    }

    private boolean evalPriceRange(JsonNode n, EvalContext ctx) {
        Long price = getPriceField(ctx, n.path("priceField").asText());
        if (price == null) return false;
        double val = price.doubleValue();
        double min = n.path("minValue").asDouble();
        double max = n.path("maxValue").asDouble();
        boolean minInc = n.path("minInclusive").asBoolean(true);
        boolean maxInc = n.path("maxInclusive").asBoolean(true);
        return (minInc ? val >= min : val > min) && (maxInc ? val <= max : val < max);
    }

    private boolean evalVolumeValue(JsonNode n, EvalContext ctx) {
        if (ctx.price() == null) return false;
        double vol = ctx.price().getVolume().doubleValue();
        return applyOp(vol, n.path("operator").asText(), n.path("value").asDouble());
    }

    private boolean evalVolumeRange(JsonNode n, EvalContext ctx) {
        if (ctx.price() == null) return false;
        double vol = ctx.price().getVolume().doubleValue();
        double min = n.path("minValue").asDouble();
        double max = n.path("maxValue").asDouble();
        boolean minInc = n.path("minInclusive").asBoolean(true);
        boolean maxInc = n.path("maxInclusive").asBoolean(true);
        return (minInc ? vol >= min : vol > min) && (maxInc ? vol <= max : vol < max);
    }

    private boolean evalPriceVsIndicator(JsonNode n, EvalContext ctx) {
        Long price = getPriceField(ctx, n.path("priceField").asText());
        if (price == null) return false;
        Double indicator = getIndicator(ctx, n.path("indicator").asText());
        if (indicator == null) return false;
        return applyOp(price.doubleValue(), n.path("operator").asText(), indicator);
    }

    private boolean evalMarketFilter(JsonNode n, EvalContext ctx) {
        String market = ctx.marketType().name();
        for (JsonNode m : n.path("markets")) {
            if (market.equals(m.asText())) return true;
        }
        return false;
    }

    // ─── 헬퍼 ─────────────────────────────────────────────────────────────────

    private Double getIndicator(EvalContext ctx, String typeName) {
        if (ctx.indicators() == null) return null;
        try {
            return ctx.indicators().get(IndicatorType.valueOf(typeName));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private Long getPriceField(EvalContext ctx, String field) {
        if (ctx.price() == null) return null;
        return switch (field) {
            case "OPEN" -> ctx.price().getOpenPrice();
            case "HIGH" -> ctx.price().getHighPrice();
            case "LOW" -> ctx.price().getLowPrice();
            case "CLOSE" -> ctx.price().getClosePrice();
            default -> null;
        };
    }

    private boolean applyOp(double left, String op, double right) {
        return switch (op) {
            case "GT" -> left > right;
            case "GTE" -> left >= right;
            case "LT" -> left < right;
            case "LTE" -> left <= right;
            case "EQ" -> Double.compare(left, right) == 0;
            case "NEQ" -> Double.compare(left, right) != 0;
            default -> false;
        };
    }

    // ─── 평가 컨텍스트 ────────────────────────────────────────────────────────

    private record EvalContext(
            String code,
            String name,
            MarketType marketType,
            Map<IndicatorType, Double> indicators,
            DailyStockPrice price
    ) {}
}
