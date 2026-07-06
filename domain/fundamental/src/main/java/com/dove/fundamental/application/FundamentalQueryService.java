package com.dove.fundamental.application;

import com.dove.fundamental.domain.entity.StockFundamental;
import com.dove.fundamental.domain.entity.StockValuationDaily;
import com.dove.fundamental.domain.enums.FinancialStatementDiv;
import com.dove.fundamental.domain.repository.StockFundamentalRepository;
import com.dove.fundamental.domain.repository.StockValuationDailyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 재무제표·밸류에이션 조회(상세 화면용).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FundamentalQueryService {

    private final StockFundamentalRepository fundamentalRepository;
    private final StockValuationDailyRepository valuationRepository;

    /**
     * 종목의 재무제표를 최신 회계연도·공시일 순으로 반환한다.
     * 같은 (회계연도·보고서·재무구분)에 실공시가 있으면 백필(합성 F키) 행은 제외해 중복을 없앤다.
     */
    public List<StockFundamental> findStatements(String ticker) {
        List<StockFundamental> rows = fundamentalRepository.findByTickerOrderByFiscalYearDescRceptDtDesc(ticker);
        Set<String> realStatements = rows.stream()
                .filter(FundamentalQueryService::isReal)
                .map(FundamentalQueryService::statementKey)
                .collect(Collectors.toSet());
        return rows.stream()
                .filter(r -> isReal(r) || !realStatements.contains(statementKey(r)))
                .toList();
    }

    /** 합성 백필키(F…)가 아닌 실공시 행인지. */
    private static boolean isReal(StockFundamental f) {
        return !f.getRceptNo().startsWith("F");
    }

    private static String statementKey(StockFundamental f) {
        return f.getFiscalYear() + "|" + f.getReportCode() + "|" + f.getFsDiv();
    }

    /**
     * 종목의 최근 일별 밸류에이션(최대 250거래일)을 거래일 내림차순으로 반환한다.
     */
    public List<StockValuationDaily> findValuations(String ticker) {
        return valuationRepository.findTop250ByTickerOrderByTradeDateDesc(ticker);
    }

    /**
     * 종목의 최신 밸류에이션 1건을 반환한다.
     */
    public Optional<StockValuationDaily> findLatestValuation(String ticker) {
        return valuationRepository.findFirstByTickerOrderByTradeDateDesc(ticker);
    }

    /**
     * 공시일 ≤ 기준일인 원본(정정 아닌) 최신 재무 1건을 반환한다(PIT).
     */
    public Optional<StockFundamental> findLatestOriginal(String ticker, FinancialStatementDiv fsDiv, LocalDate date) {
        // 실공시 우선: 실공시 원본이 있으면 그것을, 없을 때만 백필(F) 행으로 폴백.
        Optional<StockFundamental> real = fundamentalRepository
                .findFirstByTickerAndFsDivAndAmendmentFalseAndRceptNoNotLikeAndRceptDtLessThanEqualOrderByRceptDtDesc(
                        ticker, fsDiv, "F%", date);
        return real.isPresent() ? real : fundamentalRepository
                .findFirstByTickerAndFsDivAndAmendmentFalseAndRceptDtLessThanEqualOrderByRceptDtDesc(ticker, fsDiv, date);
    }

    /**
     * 특정 (종목, 재무구분, 회계연도, 보고서)의 원본 재무를 공시일 ≤ 기준일 범위에서 반환한다(TTM 전년 자료).
     */
    public Optional<StockFundamental> findOriginal(String ticker, FinancialStatementDiv fsDiv,
                                                   short fiscalYear, String reportCode, LocalDate date) {
        // 실공시 우선: 해당 (연도·보고서)에 실공시 원본이 있으면 그것을, 없을 때만 백필(F) 행으로 폴백.
        Optional<StockFundamental> real = fundamentalRepository
                .findFirstByTickerAndFsDivAndFiscalYearAndReportCodeAndAmendmentFalseAndRceptNoNotLikeAndRceptDtLessThanEqualOrderByRceptDtDesc(
                        ticker, fsDiv, fiscalYear, reportCode, "F%", date);
        return real.isPresent() ? real : fundamentalRepository
                .findFirstByTickerAndFsDivAndFiscalYearAndReportCodeAndAmendmentFalseAndRceptDtLessThanEqualOrderByRceptDtDesc(
                        ticker, fsDiv, fiscalYear, reportCode, date);
    }

    /**
     * 공시일이 [from, to] 구간인 전 종목 원본 재무를 반환한다(날짜별 배치의 벌크 로드용).
     */
    public List<StockFundamental> findOriginalsInWindow(LocalDate from, LocalDate to) {
        return fundamentalRepository.findByAmendmentFalseAndRceptDtBetween(from, to);
    }

    /**
     * 해당 접수번호·재무구분의 재무가 이미 저장돼 있는지 여부.
     */
    public boolean existsStatement(String rceptNo, FinancialStatementDiv fsDiv) {
        return fundamentalRepository.existsByRceptNoAndFsDiv(rceptNo, fsDiv);
    }

    /**
     * 해당 종목·회계연도·보고서의 재무가 이미 저장돼 있는지 여부.
     */
    public boolean existsStatement(String ticker, short fiscalYear, String reportCode) {
        return fundamentalRepository.existsByTickerAndFiscalYearAndReportCode(ticker, fiscalYear, reportCode);
    }
}
