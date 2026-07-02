package com.dove.api.search.stock.controller;

import com.dove.api.global.security.authorization.RequireCapability;
import com.dove.api.search.stock.dto.FundamentalStatementResponse;
import com.dove.api.search.stock.dto.IndicatorBar;
import com.dove.api.search.stock.dto.InvestorFlowBar;
import com.dove.api.search.stock.dto.PriceBar;
import com.dove.api.search.stock.dto.StockDetailResponse;
import com.dove.api.search.stock.dto.StockEventResponse;
import com.dove.api.search.stock.dto.StockResponse;
import com.dove.api.search.stock.dto.ValuationResponse;
import com.dove.fundamental.application.FundamentalQueryService;
import com.dove.indicator.application.service.StockFeatureDailyService;
import com.dove.indicator.domain.enums.IndicatorType;
import com.dove.investorflow.application.service.InvestorDailyService;
import com.dove.stock.application.service.StockEventService;
import com.dove.stock.application.service.StockPriceQueryService;
import com.dove.stock.application.service.StockQueryService;
import com.dove.stock.domain.entity.Stock;
import com.dove.stock.domain.enums.PriceType;
import com.dove.stock.domain.enums.StockExchange;
import com.dove.userfeature.domain.capability.Capability;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 종목 목록·상세·주가·지표·권리이벤트 조회 API.
 */
@RestController
@RequestMapping("/stocks")
@RequiredArgsConstructor
@RequireCapability(Capability.STOCK_VIEW)
public class StockController {

    private final StockQueryService stockQueryService;
    private final StockPriceQueryService priceQueryService;
    private final StockFeatureDailyService featureDailyService;
    private final StockEventService stockEventQueryService;
    private final InvestorDailyService investorDailyService;
    private final FundamentalQueryService fundamentalQueryService;

    /**
     * 전체 종목 목록을 반환한다.
     */
    @GetMapping
    public List<StockResponse> getStocks() {
        List<Stock> stocks = stockQueryService.findAll();
        List<String> tickers = stocks.stream().map(Stock::getTicker).toList();
        Map<String, String> names = stockQueryService.findNamesByTickers(tickers);
        Map<String, com.dove.stock.domain.entity.StockDetail> details =
                stockQueryService.findDetailsByTickers(tickers);
        return stocks.stream()
                .map(s -> StockResponse.from(s,
                        names.getOrDefault(s.getTicker(), s.getTicker()),
                        details.get(s.getTicker())))
                .toList();
    }

    /**
     * 종목 상세 정보 조회 (기본 + STOCK_DETAIL).
     *
     * @throws ResponseStatusException 종목이 없으면 404 STOCK_NOT_FOUND
     */
    @GetMapping("/{ticker}/detail")
    public StockDetailResponse getDetail(@PathVariable String ticker) {
        Stock stock = stockQueryService.findByTicker(ticker)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "STOCK_NOT_FOUND"));
        return StockDetailResponse.from(stock, stockQueryService.findDetail(ticker).orElse(null));
    }

    /**
     * 종목 권리 이벤트(배당·증자·감자·합병/분할·액면) 조회 — 최신순.
     */
    @GetMapping("/{ticker}/events")
    public List<StockEventResponse> getEvents(@PathVariable String ticker) {
        return stockEventQueryService.findByTicker(ticker).stream()
                .map(StockEventResponse::from)
                .toList();
    }

    /**
     * 종목 주가 조회. before가 있으면 그 날짜 직전 과거 구간을, 없으면 최근 구간을 반환한다(둘 다 거래일 오름차순).
     *
     * @param source   KRX | NXT | INTEGRATED (대소문자 무관)
     * @param adjusted 수정주가 여부
     * @param limit    최대 봉 수 (기본 120)
     * @param before   이 거래일(exclusive) 직전 과거 봉을 조회 (과거 페이지네이션). 비어있으면 최근 봉.
     */
    @GetMapping("/{ticker}/prices")
    public List<PriceBar> getPrices(@PathVariable String ticker,
                                    @RequestParam String source,
                                    @RequestParam(defaultValue = "true") boolean adjusted,
                                    @RequestParam(defaultValue = "120") int limit,
                                    @RequestParam(required = false) String before) {
        StockExchange exchange = resolveExchange(source, ticker);
        PriceType priceType = adjusted ? PriceType.ADJUSTED : PriceType.RAW;
        var prices = (before != null && !before.isBlank())
                ? priceQueryService.findBefore(ticker, exchange, priceType, LocalDate.parse(before), limit)
                : priceQueryService.findRecent(ticker, exchange, priceType, limit);
        return prices.stream()
                .map(p -> PriceBar.of(p.getTradeDate(), p))
                .toList();
    }

    /**
     * 종목 지표 조회. before가 있으면 그 날짜 직전 과거 구간을, 없으면 최근 구간을 반환한다.
     *
     * @param source   KRX | NXT | INTEGRATED (대소문자 무관)
     * @param adjusted 수정주가 여부
     * @param before   이 거래일(exclusive) 직전 과거 지표를 조회 (과거 페이지네이션). 비어있으면 최근.
     */
    @GetMapping("/{ticker}/indicators")
    public List<IndicatorBar> getIndicators(@PathVariable String ticker,
                                            @RequestParam String source,
                                            @RequestParam(defaultValue = "true") boolean adjusted,
                                            @RequestParam(defaultValue = "120") int limit,
                                            @RequestParam List<String> types,
                                            @RequestParam(required = false) String before) {
        List<IndicatorType> indicatorTypes = types.stream()
                .map(IndicatorType::parseOrNull)
                .filter(Objects::nonNull)
                .toList();
        if (indicatorTypes.isEmpty()) return List.of();

        StockExchange exchange = resolveExchange(source, ticker);
        PriceType priceType = adjusted ? PriceType.ADJUSTED : PriceType.RAW;
        Map<LocalDate, Map<IndicatorType, Double>> bars =
                (before != null && !before.isBlank())
                        ? featureDailyService.findBeforeByStock(ticker, exchange, priceType, LocalDate.parse(before), limit)
                        : featureDailyService.findRecentByStock(ticker, exchange, priceType, limit);

        return bars.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> new IndicatorBar(
                        e.getKey().toString(),
                        e.getValue().entrySet().stream()
                                .filter(kv -> indicatorTypes.contains(kv.getKey()))
                                .collect(Collectors.toMap(kv -> kv.getKey().name(), Map.Entry::getValue))))
                .toList();
    }

    /**
     * 투자자별 일별 순매수 조회. 거래일 오름차순 반환.
     *
     * @param from 조회 시작일 (inclusive)
     * @param to   조회 종료일 (inclusive)
     */
    @GetMapping("/{ticker}/investor-flow")
    public List<InvestorFlowBar> getInvestorFlow(
            @PathVariable String ticker,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return investorDailyService.findByCodeAndDateRange(ticker, from, to).stream()
                .map(d -> new InvestorFlowBar(
                        d.getTradeDate().toString(),
                        d.individualNet(),
                        d.institutionNet(),
                        d.foreignNet()))
                .toList();
    }

    /**
     * 종목의 재무제표(공시 단위)를 최신순으로 반환한다.
     */
    @GetMapping("/{ticker}/fundamentals")
    public List<FundamentalStatementResponse> getFundamentals(@PathVariable String ticker) {
        return fundamentalQueryService.findStatements(ticker).stream()
                .map(FundamentalStatementResponse::from)
                .toList();
    }

    /**
     * 종목의 최근 일별 밸류에이션을 반환한다.
     */
    @GetMapping("/{ticker}/valuations")
    public List<ValuationResponse> getValuations(@PathVariable String ticker) {
        return fundamentalQueryService.findValuations(ticker).stream()
                .map(ValuationResponse::from)
                .toList();
    }

    /**
     * 종목의 최신 밸류에이션 1건을 반환한다(없으면 204).
     */
    @GetMapping("/{ticker}/valuation/latest")
    public ValuationResponse getLatestValuation(@PathVariable String ticker) {
        return fundamentalQueryService.findLatestValuation(ticker)
                .map(ValuationResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NO_CONTENT));
    }

    /**
     * source·종목으로 거래소를 결정한다. 종목을 찾지 못하면 404.
     *
     * @throws ResponseStatusException 종목이 없으면 404 STOCK_NOT_FOUND
     */
    private StockExchange resolveExchange(String source, String ticker) {
        try {
            return stockQueryService.resolveExchange(source, ticker);
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "STOCK_NOT_FOUND");
        }
    }
}
