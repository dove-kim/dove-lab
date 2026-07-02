package com.dove.fundamental.application;

import com.dove.fundamental.domain.entity.StockFundamental;
import com.dove.fundamental.domain.entity.StockValuationDaily;
import com.dove.fundamental.domain.repository.StockFundamentalRepository;
import com.dove.fundamental.domain.repository.StockValuationDailyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

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
     */
    public List<StockFundamental> findStatements(String ticker) {
        return fundamentalRepository.findByTickerOrderByFiscalYearDescRceptDtDesc(ticker);
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
}
