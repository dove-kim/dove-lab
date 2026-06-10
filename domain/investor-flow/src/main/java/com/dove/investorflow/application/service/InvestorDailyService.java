package com.dove.investorflow.application.service;

import com.dove.investorflow.domain.entity.InvestorDaily;
import com.dove.investorflow.domain.entity.InvestorDailyId;
import com.dove.investorflow.domain.repository.InvestorDailyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 일별 투자자별 매매동향을 조회·저장하는 서비스.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InvestorDailyService {

    private final InvestorDailyRepository investorDailyRepository;

    /**
     * 종목코드·거래일로 단건 매매동향을 조회한다.
     */
    public Optional<InvestorDaily> findByCodeAndDate(String stockCode, LocalDate tradeDate) {
        return investorDailyRepository.findById(new InvestorDailyId(stockCode, tradeDate));
    }

    /**
     * 종목코드·날짜 범위 기준 매매동향을 거래일 오름차순으로 반환한다.
     */
    public List<InvestorDaily> findByCodeAndDateRange(String stockCode, LocalDate from, LocalDate to) {
        return investorDailyRepository.findByIdStockCodeAndIdTradeDateBetweenOrderByIdTradeDate(
                stockCode, from, to);
    }

    /**
     * 매매동향 목록을 저장(upsert)한다.
     */
    @Transactional
    public void saveAll(List<InvestorDaily> entities) {
        investorDailyRepository.saveAll(entities);
    }
}
