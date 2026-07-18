package com.dove.stock.application.service;

import com.dove.market.domain.enums.MarketType;
import com.dove.stock.application.dto.StockSearchHit;
import com.dove.stock.domain.entity.Stock;
import com.dove.stock.domain.entity.StockDetail;
import com.dove.stock.domain.enums.StockExchange;
import com.dove.stock.domain.repository.StockDetailRepository;
import com.dove.stock.domain.repository.StockRepository;
import com.dove.stock.infrastructure.repository.StockRepositorySupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 종목·종목 상세 조회.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockQueryService {

    private final StockRepository stockRepository;
    private final StockDetailRepository stockDetailRepository;
    private final StockRepositorySupport stockRepositorySupport;

    /**
     * 전체 종목을 조회한다.
     */
    public List<Stock> findAll() {
        return stockRepository.findAll();
    }

    /**
     * 이름·티커로 종목을 검색한다(자동완성용, 최대 limit건).
     */
    public List<StockSearchHit> search(String query, int limit) {
        String q = query == null ? "" : query.trim();
        if (q.isEmpty()) return List.of();
        List<StockDetail> details = stockDetailRepository
                .findTop20ByPrdtAbrvNameContainingIgnoreCaseOrPrdtNameContainingIgnoreCaseOrTickerStartingWith(q, q, q);
        List<String> tickers = details.stream().map(StockDetail::getTicker).toList();
        Map<String, Stock> stocks = findByTickers(tickers);
        Map<String, String> names = findNamesByTickers(tickers);
        return details.stream()
                .map(d -> stocks.get(d.getTicker()))
                .filter(Objects::nonNull)
                .limit(limit)
                .map(s -> new StockSearchHit(s.getTicker(), names.getOrDefault(s.getTicker(), s.getTicker()), s.getMarket().name()))
                .toList();
    }

    /**
     * 시장별 종목을 조회한다.
     */
    public List<Stock> findByMarket(MarketType market) {
        return stockRepository.findByMarket(market);
    }

    /**
     * DART 고유번호(corp_code)가 매핑된 종목만 조회한다.
     */
    public List<Stock> findWithCorpCode() {
        return stockRepository.findByCorpCodeIsNotNull();
    }

    /** 단건 Stock 조회. */
    public Optional<Stock> findByTicker(String ticker) {
        return stockRepository.findById(ticker);
    }

    /** 단건 StockDetail 조회 (상세 미수집이면 비어 있음). */
    public Optional<StockDetail> findDetail(String ticker) {
        return stockDetailRepository.findById(ticker);
    }

    /** 티커 집합 → StockDetail 맵. 상태(거래정지·관리종목 등) 일괄 표시용. */
    public Map<String, StockDetail> findDetailsByTickers(Collection<String> tickers) {
        if (tickers.isEmpty()) return Map.of();
        return stockDetailRepository.findAllById(tickers).stream()
                .collect(Collectors.toMap(StockDetail::getTicker, Function.identity()));
    }

    /** 티커 집합 → Stock 맵. 검색 필터 결과 매핑용. */
    public Map<String, Stock> findByTickers(Collection<String> tickers) {
        if (tickers.isEmpty()) return Map.of();
        return stockRepository.findAllById(tickers).stream()
                .collect(Collectors.toMap(Stock::getTicker, Function.identity()));
    }

    /**
     * 티커 집합 → 종목 약명 맵. StockDetail.prdtAbrvName 우선, 없으면 prdtName.
     * 검색 결과 종목명 표시용.
     */
    public Map<String, String> findNamesByTickers(Collection<String> tickers) {
        if (tickers.isEmpty()) return Map.of();
        return stockDetailRepository.findAllById(tickers).stream()
                .collect(Collectors.toMap(
                        StockDetail::getTicker,
                        d -> {
                            String name = d.getPrdtAbrvName();
                            if (name != null && !name.isBlank()) return name;
                            name = d.getPrdtName();
                            return (name != null && !name.isBlank()) ? name : d.getTicker();
                        }));
    }

    /**
     * 전체 종목 티커를 조회한다.
     */
    public List<String> findAllTickers() {
        return stockRepositorySupport.findAllTickers();
    }

    /**
     * 시장 목록에 해당하는 티커를 조회한다.
     */
    public List<String> findTickersByMarkets(List<MarketType> markets) {
        return stockRepositorySupport.findTickersByMarkets(markets);
    }

    /**
     * 거래소별 조회 대상 티커 목록.
     * NXT·INTEGRATED는 넥스트레이드 미취급인 KONEX를 제외하고 KOSPI·KOSDAQ만 반환.
     */
    public List<String> findTickersByExchange(StockExchange exchange) {
        return stockRepositorySupport.findTickersByMarkets(exchange.toMarkets());
    }

    /**
     * source 힌트(KRX/NXT/INTEGRATED)와 종목으로 거래소를 결정한다. KRX(기본)는 종목의 실제 시장으로 매핑.
     *
     * @throws NoSuchElementException KRX 기본 경로에서 종목을 찾지 못한 경우
     */
    public StockExchange resolveExchange(String source, String ticker) {
        return switch (source.toUpperCase()) {
            case "NXT" -> StockExchange.NXT;
            case "INTEGRATED" -> StockExchange.INTEGRATED;
            default -> StockExchange.fromMarket(
                    findByTicker(ticker)
                            .orElseThrow(() -> new NoSuchElementException("STOCK_NOT_FOUND"))
                            .getMarket());
        };
    }
}
