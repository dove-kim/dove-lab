package com.dove.fundamental.domain.repository;

import com.dove.fundamental.domain.entity.StockFundamental;
import com.dove.fundamental.domain.entity.StockFundamentalId;
import com.dove.fundamental.domain.enums.FinancialStatementDiv;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 재무제표 원자료 저장소.
 */
@Repository
public interface StockFundamentalRepository extends JpaRepository<StockFundamental, StockFundamentalId> {

    /**
     * 공시일이 기준일 이하인 것 중 가장 최근 공시(정정 포함)를 반환한다 — PIT 정책2(발표된 최신).
     */
    Optional<StockFundamental> findFirstByTickerAndFsDivAndRceptDtLessThanEqualOrderByRceptDtDesc(
            String ticker, FinancialStatementDiv fsDiv, LocalDate rceptDt);

    /**
     * 공시일이 기준일 이하인 것 중 정정 아닌 최신 공시를 반환한다 — PIT 정책1(원본만).
     */
    Optional<StockFundamental> findFirstByTickerAndFsDivAndAmendmentFalseAndRceptDtLessThanEqualOrderByRceptDtDesc(
            String ticker, FinancialStatementDiv fsDiv, LocalDate rceptDt);

    /**
     * 이미 수집된 공시인지 여부(신규 폴링 중복 방지용).
     */
    boolean existsByRceptNoAndFsDiv(String rceptNo, FinancialStatementDiv fsDiv);

    /**
     * 해당 (종목, 회계연도, 보고서)가 이미 수집됐는지 여부(백필 재개용).
     */
    boolean existsByTickerAndFiscalYearAndReportCode(String ticker, Short fiscalYear, String reportCode);

    /**
     * 종목의 재무제표를 최신 회계연도·공시일 순으로 조회한다(상세 화면 표시용).
     */
    List<StockFundamental> findByTickerOrderByFiscalYearDescRceptDtDesc(String ticker);
}
