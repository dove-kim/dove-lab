package com.dove.scheduler.fundamental;

import com.dove.fundamental.application.FundamentalCommandService;
import com.dove.fundamental.application.FundamentalQueryService;
import com.dove.fundamental.application.Valuation;
import com.dove.fundamental.application.ValuationCalculator;
import com.dove.fundamental.domain.entity.StockFundamental;
import com.dove.fundamental.domain.entity.StockValuationDaily;
import com.dove.fundamental.domain.enums.FinancialStatementDiv;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
    private final FundamentalQueryService fundamentalQueryService;
    private final StockShareCountService shareCountService;
    private final FundamentalCommandService fundamentalCommandService;
    /**
     * 자기 자신(프록시) — computeRange가 하루 단위 트랜잭션 compute를 프록시 경유로 호출(자기호출 우회).
     */
    private final ObjectProvider<DailyValuationService> self;

    /**
     * 특정 거래일의 전 종목 밸류에이션을 계산·저장한다.
     * 하루 단위 단일 트랜잭션으로 묶어 수천 건 저장을 배치 커밋한다(건별 autocommit 회피).
     *
     * @return 저장 건수
     */
    @Transactional
    public int compute(LocalDate date) {
        Map<String, StockPrice> rows = priceQueryService.findByExchangesAndDate(UNIVERSE, PriceType.RAW, date);
        int saved = 0;
        for (Map.Entry<String, StockPrice> entry : rows.entrySet()) {
            String ticker = entry.getKey();
            Long close = entry.getValue().getClosePrice();
            if (close == null) {
                continue;
            }
            Optional<StockFundamental> fundamental = latestPitFundamental(ticker, date);
            if (fundamental.isEmpty()) {
                continue;
            }
            StockFundamental f = fundamental.get();
            Long marketCap = marketCap(ticker, date, close);
            Valuation v = ValuationCalculator.compute(marketCap, f);
            fundamentalCommandService.saveValuation(StockValuationDaily.builder()
                    .ticker(ticker)
                    .tradeDate(date)
                    .closePrice(close)
                    .marketCap(v.marketCap())
                    .per(v.per())
                    .pbr(v.pbr())
                    .psr(v.psr())
                    .gpa(v.gpa())
                    .fundRceptNo(f.getRceptNo())
                    .build());
            saved++;
        }
        log.info("[일별 밸류에이션] {} — 저장 {}건", date, saved);
        return saved;
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

    /**
     * 시가총액 = 종가 × as-of 상장주식수(KRX). 주식수 미확보 시 null(시총 산출 불가 → 4비율 중 PER·PBR·PSR만 null).
     */
    private Long marketCap(String ticker, LocalDate date, long close) {
        return shareCountService.findAsOf(ticker, date)
                .map(sc -> close * sc.getListedShares())
                .orElse(null);
    }

    /**
     * 공시일 ≤ 기준일인 원본(정정 아닌) 최신 재무를 연결(CFS) 우선, 없으면 별도(OFS)로 반환한다.
     * 원본만 사용 = 정정 미반영(PIT 무결성). 원본은 회계기 순서대로 공시되어 공시일 최신 = 최신 회계기.
     */
    private Optional<StockFundamental> latestPitFundamental(String ticker, LocalDate date) {
        Optional<StockFundamental> cfs = fundamentalQueryService.findLatestOriginal(ticker, FinancialStatementDiv.CFS, date);
        if (cfs.isPresent()) {
            return cfs;
        }
        return fundamentalQueryService.findLatestOriginal(ticker, FinancialStatementDiv.OFS, date);
    }
}
