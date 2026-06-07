package com.dove.stock.application.service;

import com.dove.stock.domain.entity.StockPrice;
import com.dove.stock.domain.entity.StockPriceId;
import com.dove.stock.domain.enums.PriceType;
import com.dove.stock.domain.enums.StockExchange;
import com.dove.stock.domain.repository.StockPriceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 종목 주가 저장.
 */
@Service
@RequiredArgsConstructor
public class StockPriceCommandService {

    private final StockPriceRepository stockPriceRepository;

    /** 멱등성 보장 upsert. 이미 존재하면 값을 갱신한다. */
    @Transactional
    public void upsert(String ticker, StockExchange exchange, PriceType priceType, LocalDate tradeDate,
                       Long openPrice, Long highPrice, Long lowPrice, Long closePrice,
                       Long volume, Long turnover) {
        StockPriceId id = new StockPriceId(ticker, exchange, priceType, tradeDate);
        stockPriceRepository.findById(id).ifPresentOrElse(
                p -> p.update(openPrice, highPrice, lowPrice, closePrice, volume, turnover),
                () -> stockPriceRepository.save(
                        new StockPrice(ticker, exchange, priceType, tradeDate,
                                openPrice, highPrice, lowPrice, closePrice, volume, turnover))
        );
    }

    /** 목록 일괄 upsert. */
    @Transactional
    public void upsertAll(List<StockPrice> prices) {
        if (prices.isEmpty()) return;
        Map<StockPriceId, StockPrice> existing = stockPriceRepository.findAllById(
                prices.stream().map(StockPrice::getId).collect(Collectors.toList())
        ).stream().collect(Collectors.toMap(StockPrice::getId, p -> p));

        List<StockPrice> toSave = prices.stream()
                .filter(p -> {
                    StockPrice ex = existing.get(p.getId());
                    if (ex != null) {
                        ex.update(p.getOpenPrice(), p.getHighPrice(), p.getLowPrice(),
                                p.getClosePrice(), p.getVolume(), p.getTurnover());
                        return false;
                    }
                    return true;
                }).collect(Collectors.toList());

        if (!toSave.isEmpty()) stockPriceRepository.saveAll(toSave);
    }
}
