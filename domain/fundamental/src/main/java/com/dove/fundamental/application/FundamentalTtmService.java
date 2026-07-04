package com.dove.fundamental.application;

import com.dove.fundamental.domain.entity.StockFundamental;
import com.dove.fundamental.domain.enums.FinancialStatementDiv;
import com.dove.fundamental.domain.enums.ReportCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;

/**
 * PIT 기준 종목의 TTM 재무 스냅샷을 조립한다 — 연결(CFS) 우선, 없으면 별도(OFS).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FundamentalTtmService {

    /**
     * TTM 계산상 최신 보고서로부터 거슬러 필요한 최대 기간(전년 자료까지) — 벌크 로드 윈도우 하한.
     */
    private static final int WINDOW_YEARS = 3;

    private final FundamentalQueryService queryService;

    /**
     * 기준일까지 공시된 원본으로 종목의 TTM 스냅샷을 만든다(단건).
     * flow(순이익·매출·매출총이익)는 TTM, stock(자본·자산)은 최신 시점값. 연결 우선, 없으면 별도.
     * 최신이 분기/반기인데 전년 연간·동기 자료가 없으면 빈 값을 반환한다(근사·폴백 없음).
     */
    public Optional<TtmFundamental> resolve(String ticker, LocalDate date) {
        Optional<TtmFundamental> cfs = buildFromDb(ticker, FinancialStatementDiv.CFS, date);
        return cfs.isPresent() ? cfs : buildFromDb(ticker, FinancialStatementDiv.OFS, date);
    }

    /**
     * 여러 종목의 TTM 스냅샷을 날짜 기준으로 한 번에 만든다(배치용) — 재무 윈도우를 한 쿼리로 로드해 메모리에서 조립.
     * TTM 불가(전년 자료 부족)인 종목은 결과에서 제외된다.
     */
    public Map<String, TtmFundamental> resolveAll(Set<String> tickers, LocalDate date) {
        Map<String, List<StockFundamental>> byTicker = new HashMap<>();
        for (StockFundamental f : queryService.findOriginalsInWindow(date.minusYears(WINDOW_YEARS), date)) {
            if (tickers.contains(f.getTicker())) {
                byTicker.computeIfAbsent(f.getTicker(), k -> new ArrayList<>()).add(f);
            }
        }
        Map<String, TtmFundamental> result = new HashMap<>();
        byTicker.forEach((ticker, reports) -> buildFromReports(reports)
                .ifPresent(ttm -> result.put(ticker, ttm)));
        return result;
    }

    private Optional<TtmFundamental> buildFromDb(String ticker, FinancialStatementDiv fsDiv, LocalDate date) {
        return queryService.findLatestOriginal(ticker, fsDiv, date)
                .flatMap(latest -> assemble(latest,
                        (fiscalYear, reportCode) -> queryService.findOriginal(ticker, fsDiv, fiscalYear, reportCode, date)));
    }

    private Optional<TtmFundamental> buildFromReports(List<StockFundamental> reports) {
        Optional<TtmFundamental> cfs = buildFromReports(reports, FinancialStatementDiv.CFS);
        return cfs.isPresent() ? cfs : buildFromReports(reports, FinancialStatementDiv.OFS);
    }

    private Optional<TtmFundamental> buildFromReports(List<StockFundamental> reports, FinancialStatementDiv fsDiv) {
        List<StockFundamental> ofDiv = reports.stream().filter(r -> r.getFsDiv() == fsDiv).toList();
        return ofDiv.stream().max(Comparator.comparing(StockFundamental::getRceptDt))
                .flatMap(latest -> assemble(latest, (fiscalYear, reportCode) -> ofDiv.stream()
                        .filter(r -> r.getFiscalYear().shortValue() == fiscalYear && r.getReportCode().equals(reportCode))
                        .max(Comparator.comparing(StockFundamental::getRceptDt))));
    }

    /**
     * 최신 보고서 + (분기/반기면) 전년 자료 조회 함수로 TTM 스냅샷을 조립한다. 전년 자료 없으면 빈 값.
     */
    private Optional<TtmFundamental> assemble(StockFundamental latest,
                                              BiFunction<Short, String, Optional<StockFundamental>> priorLookup) {
        Long equity = controllingEquity(latest);
        Long asset = latest.getTotalAsset();

        Long netIncome, revenue, grossProfit;
        if (ReportCode.ANNUAL.code().equals(latest.getReportCode())) {
            netIncome = controllingNetIncome(latest);
            revenue = latest.getRevenue();
            grossProfit = latest.getGrossProfit();
        } else {
            short priorYear = (short) (latest.getFiscalYear() - 1);
            Optional<StockFundamental> priorAnnual = priorLookup.apply(priorYear, ReportCode.ANNUAL.code());
            Optional<StockFundamental> priorSame = priorLookup.apply(priorYear, latest.getReportCode());
            if (priorAnnual.isEmpty() || priorSame.isEmpty()) {
                return Optional.empty();     // TTM 불가 → 저장 안 함
            }
            netIncome = ttm(controllingNetIncome(latest), controllingNetIncome(priorAnnual.get()), controllingNetIncome(priorSame.get()));
            revenue = ttm(latest.getRevenue(), priorAnnual.get().getRevenue(), priorSame.get().getRevenue());
            grossProfit = ttm(latest.getGrossProfit(), priorAnnual.get().getGrossProfit(), priorSame.get().getGrossProfit());
        }
        return Optional.of(new TtmFundamental(revenue, grossProfit, netIncome, equity, asset, latest.getRceptNo()));
    }

    /**
     * 지배주주순이익 — 없으면(별도 등) 전체 당기순이익으로 대체.
     */
    private static Long controllingNetIncome(StockFundamental f) {
        return f.getNetIncomeControlling() != null ? f.getNetIncomeControlling() : f.getNetIncome();
    }

    /**
     * 지배주주지분 자본 — 없으면(별도 등) 전체 자본총계로 대체.
     */
    private static Long controllingEquity(StockFundamental f) {
        return f.getEquityControlling() != null ? f.getEquityControlling() : f.getTotalEquity();
    }

    /**
     * TTM 합산 — 세 값 중 하나라도 없으면 null.
     */
    private static Long ttm(Long latest, Long priorAnnual, Long priorSame) {
        if (latest == null || priorAnnual == null || priorSame == null) {
            return null;
        }
        return latest + priorAnnual - priorSame;
    }
}
