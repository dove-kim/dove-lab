package com.dove.scheduler.fundamental;

import com.dove.fundamental.application.FundamentalCommandService;
import com.dove.fundamental.application.FundamentalTtmService;
import com.dove.fundamental.application.TtmFundamental;
import com.dove.fundamental.application.Valuation;
import com.dove.fundamental.application.ValuationCalculator;
import com.dove.fundamental.domain.entity.StockValuationDaily;
import com.dove.stock.application.service.StockPriceQueryService;
import com.dove.stock.application.service.StockShareCountService;
import com.dove.stock.domain.entity.StockPrice;
import com.dove.stock.domain.enums.PriceType;
import com.dove.stock.domain.enums.StockExchange;
import com.dove.stockcollection.application.service.CollectionProgress;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 일별 밸류에이션 계산 — 실제(RAW) 종가 × KRX 상장주식수(as-of)로 시총을, PIT 재무로 4비율을 산출해 STOCK_VALUATION_DAILY 에 저장.
 * DB만 읽어(가격·주식수·재무) 지표 계산과 독립 — 병행 실행 안전.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DailyValuationService {

    private static final List<StockExchange> UNIVERSE = List.of(StockExchange.KOSPI, StockExchange.KOSDAQ);

    private final StockPriceQueryService priceQueryService;
    private final FundamentalTtmService ttmService;
    private final StockShareCountService shareCountService;
    private final FundamentalCommandService fundamentalCommandService;
    /**
     * 자기 자신(프록시) — computeRange가 하루 단위 트랜잭션 compute를 프록시 경유로 호출(자기호출 우회).
     */
    private final ObjectProvider<DailyValuationService> self;

    /**
     * 특정 거래일의 전 종목 밸류에이션을 계산·저장한다.
     * 가격·재무(TTM)·상장주식수를 각각 벌크로 한 번씩 로드해 메모리에서 계산하고 배치 저장한다(종목별 개별 쿼리 제거).
     *
     * @return 저장 건수
     */
    @Transactional
    public int compute(LocalDate date) {
        Map<String, StockPrice> rows = priceQueryService.findByExchangesAndDate(UNIVERSE, PriceType.RAW, date);
        if (rows.isEmpty()) {
            return 0;       // 휴장일 등 — 벌크 로드 생략
        }
        Map<String, TtmFundamental> ttmByTicker = ttmService.resolveAll(rows.keySet(), date);
        Map<String, Long> sharesByTicker = shareCountService.findAllAsOf(date);

        List<StockValuationDaily> toSave = new ArrayList<>();
        for (Map.Entry<String, StockPrice> entry : rows.entrySet()) {
            String ticker = entry.getKey();
            Long close = entry.getValue().getClosePrice();
            TtmFundamental f = ttmByTicker.get(ticker);
            if (close == null || f == null) {
                continue;       // 가격/재무(TTM) 없으면 저장하지 않음
            }
            Long shares = sharesByTicker.get(ticker);
            Long marketCap = shares != null ? close * shares : null;    // 주식수 없으면 시총 null(PER·PBR·PSR만 null)
            Valuation v = ValuationCalculator.compute(marketCap, f);
            toSave.add(StockValuationDaily.builder()
                    .ticker(ticker)
                    .tradeDate(date)
                    .closePrice(close)
                    .marketCap(v.marketCap())
                    .per(v.per())
                    .pbr(v.pbr())
                    .psr(v.psr())
                    .gpa(v.gpa())
                    .fundRceptNo(f.latestRceptNo())
                    .build());
        }
        fundamentalCommandService.saveValuations(toSave);
        log.info("[일별 밸류에이션] {} — 저장 {}건", date, toSave.size());
        return toSave.size();
    }

    /**
     * [from..to] 각 거래일의 밸류에이션을 재계산·저장한다(멱등 upsert).
     * 재무 백필 후 영향받은 과거 구간을 다시 계산할 때 사용 — 커서 없이 날짜 독립.
     * 휴장일은 가격이 없어 자연히 0건 처리된다.
     *
     * @return 저장 건수 합계
     */
    public int computeRange(LocalDate from, LocalDate to, CollectionProgress progress) {
        int totalDays = (int) (to.toEpochDay() - from.toEpochDay() + 1);
        progress.onTotal(totalDays);
        int saved = 0;
        int done = 0;
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            saved += self.getObject().compute(date);
            progress.onProgress(++done);
        }
        log.info("[밸류에이션 재계산] {}~{} — 저장 {}건", from, to, saved);
        return saved;
    }

}
