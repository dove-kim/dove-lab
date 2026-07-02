package com.dove.stock.application.service;

import com.dove.stock.domain.entity.StockDetail;
import com.dove.stock.domain.repository.StockDetailRepository;
import com.dove.stock.domain.value.StockStatusFlags;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 종목 상세에서 상태(거래정지·관리종목)를 조회한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockDetailQueryService {

    private final StockDetailRepository repository;

    /**
     * 티커 집합 → 상태 플래그 맵. 상세 미수집 티커는 결과에서 빠진다.
     */
    public Map<String, StockStatusFlags> findStatusByTickers(Collection<String> tickers) {
        if (tickers.isEmpty()) return Map.of();
        return repository.findAllById(tickers).stream()
                .collect(Collectors.toMap(
                        StockDetail::getTicker,
                        d -> new StockStatusFlags(
                                "Y".equalsIgnoreCase(d.getTrStopYn()),
                                "Y".equalsIgnoreCase(d.getAdmnItemYn()))));
    }
}
