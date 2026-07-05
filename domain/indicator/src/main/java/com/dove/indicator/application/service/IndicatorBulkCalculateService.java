package com.dove.indicator.application.service;

import com.dove.indicator.application.exception.CursorRewoundException;
import com.dove.indicator.domain.calculator.CalculatorRunner;
import com.dove.indicator.domain.calculator.TechnicalIndicatorCalculator;
import com.dove.indicator.domain.entity.IndicatorCursor;
import com.dove.indicator.domain.entity.StockFeatureDaily;
import com.dove.indicator.domain.entity.StockFeatureDailyId;
import com.dove.indicator.domain.enums.IndicatorType;
import com.dove.stock.application.service.StockPriceQueryService;
import com.dove.stock.domain.entity.StockPrice;
import com.dove.stock.domain.enums.PriceType;
import com.dove.stock.domain.enums.StockExchange;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 한 그룹(종목·거래소·가격유형)의 기술적 지표를 청크 단위로 계산해 wide 피처 행으로 저장한다.
 */
@Service
@RequiredArgsConstructor
public class IndicatorBulkCalculateService {

    private int chunkSize = 100;

    private final StockPriceQueryService stockPriceQueryService;
    private final IndicatorCursorService cursorService;
    private final IndicatorChunkCommitService commitService;
    private final StockFeatureDailyService featureService;
    private final List<TechnicalIndicatorCalculator> calculators;

    private int maxRequiredCache = -1;

    /**
     * 등록된 모든 계산기 중 최대 요구 데이터 개수(lookback 길이 산정용). 최초 1회 계산 후 캐시한다.
     */
    private int maxRequiredDataSize() {
        if (maxRequiredCache < 0) {
            maxRequiredCache = calculators.stream()
                    .mapToInt(TechnicalIndicatorCalculator::requiredDataSize).max().orElse(0);
        }
        return maxRequiredCache;
    }

    /**
     * 그룹의 지표를 커서 다음 날부터 today까지 100행 청크 단위로 계산·저장한다.
     */
    public void calculateGroup(String ticker, StockExchange exchange, PriceType priceType, LocalDate today) {
        calculateGroup(ticker, exchange, priceType, today, null);
    }

    /**
     * 그룹의 지표를 커서 다음 날부터 today까지 계산·저장한다. {@code startFloor} 이전 날짜는 저장하지 않는다.
     */
    public void calculateGroup(String ticker, StockExchange exchange, PriceType priceType, LocalDate today,
                               LocalDate startFloor) {
        // expected/cursorExists = CAS 기준이 되는 커서 상태
        Optional<IndicatorCursor> cursorOpt = cursorService.findCursor(ticker, exchange, priceType);
        LocalDate expected = cursorOpt.map(IndicatorCursor::getCursorDate).orElse(null);
        boolean cursorExists = cursorOpt.isPresent();

        LocalDate from = IndicatorCursor.firstSaveDate(expected);
        if (startFloor != null && from.isBefore(startFloor)) from = startFloor;   // 최초 계산 시작일 하한

        while (!from.isAfter(today)) {
            List<StockPrice> chunk = stockPriceQueryService.findChunk(
                    ticker, exchange, priceType, from, today, chunkSize);
            if (chunk.isEmpty()) break;

            LocalDate chunkFirst = chunk.get(0).getTradeDate();

            // 청크 앞 lookback을 함께 읽어 윈도우·시드를 채운다.
            List<StockPrice> lookback = stockPriceQueryService.findBefore(
                    ticker, exchange, priceType, chunkFirst, maxRequiredDataSize());
            List<StockPrice> combined = new ArrayList<>(lookback);
            combined.addAll(chunk);

            // SEQ = 그룹 거래일 순번. 청크 첫 행 직전까지의 행 수가 기준.
            long seqBase = stockPriceQueryService.countBefore(ticker, exchange, priceType, chunkFirst);

            // 비감쇠 누적 지표(OBV 등)는 직전 거래일의 저장값을 시드로 이어받는다(lookback 재시드 불가).
            Map<IndicatorType, Double> persistedSeeds = lookback.isEmpty()
                    ? Map.of()
                    : featureService.findIndicatorsByDate(ticker, exchange, priceType,
                            lookback.get(lookback.size() - 1).getTradeDate());

            List<StockFeatureDaily> features = computeChunk(
                    ticker, exchange, priceType, combined, from, today, seqBase, persistedSeeds, LocalDateTime.now());

            LocalDate chunkEnd = chunk.get(chunk.size() - 1).getTradeDate();
            try {
                commitService.commit(ticker, exchange, priceType, features, expected, cursorExists, chunkEnd);
            } catch (CursorRewoundException e) {
                // 계산 중 rewind 발생 → 청크 롤백, 그룹 중단
                break;
            }

            expected = chunkEnd;
            cursorExists = true;
            from = chunkEnd.plusDays(1);
        }
    }

    /**
     * combined(=lookback + 청크)로 지표를 계산해, chunkFrom 이상 today 이하 날짜의 wide 행 목록을 만든다.
     */
    private List<StockFeatureDaily> computeChunk(String ticker, StockExchange exchange, PriceType priceType,
                                                 List<StockPrice> combined, LocalDate chunkFrom, LocalDate today,
                                                 long seqBase, Map<IndicatorType, Double> persistedSeeds,
                                                 LocalDateTime now) {
        // 계산기별 누적 시드를 캡슐화한 러너. 비감쇠 누적 지표는 직전 저장값으로 시드, 그 외는 lookback으로 재시드.
        // 직전 저장값이 있으면(재개·rewind) 그 값을 시드로, 없으면(최초 계산) null → lookback으로 워밍업(EMA는 SMA 시드).
        List<CalculatorRunner> runners = calculators.stream()
                .map(c -> c.requiresPersistedSeed()
                        ? new CalculatorRunner(c, persistedSeeds.get(c.indicatorType()))
                        : new CalculatorRunner(c))
                .toList();

        List<StockFeatureDaily> rows = new ArrayList<>();
        long seq = seqBase;
        for (int i = 0; i < combined.size(); i++) {
            StockPrice price = combined.get(i);
            LocalDate date = price.getTradeDate();
            if (date.isAfter(today)) break;
            boolean inChunk = !date.isBefore(chunkFrom); // lookback 행은 시드/윈도우용으로만 사용

            StockFeatureDaily row = null;
            if (inChunk) {
                seq++;
                StockFeatureDailyId id = new StockFeatureDailyId(ticker, exchange, priceType, date);
                row = new StockFeatureDaily(id, (int) seq, price.getOpenPrice(), price.getHighPrice(),
                        price.getLowPrice(), price.getClosePrice(), price.getVolume(), price.getTurnover(), now);
            }

            for (CalculatorRunner runner : runners) {
                // 직전 저장값을 시드로 받는 지표는 lookback 행에서 누적하지 않는다(이중 집계 방지).
                if (!inChunk && runner.requiresPersistedSeed()) continue;

                int required = runner.requiredDataSize();
                if (i + 1 < required) continue;
                List<StockPrice> pool = combined.subList(i + 1 - required, i + 1);

                // 감쇠 누적 지표의 시드 유지를 위해 lookback 행에서도 계산하되, 저장은 inChunk만
                Map<IndicatorType, Double> result = runner.compute(pool);
                if (!inChunk) continue;
                for (Map.Entry<IndicatorType, Double> entry : result.entrySet()) {
                    row.set(entry.getKey(), entry.getValue());
                }
            }
            if (inChunk) rows.add(row);
        }
        return rows;
    }
}
