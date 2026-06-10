package com.dove.investorflow.domain.repository;

import com.dove.investorflow.domain.entity.InvestorDaily;
import com.dove.investorflow.domain.entity.InvestorDailyId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * 일별 투자자별 매매동향 영속성 저장소.
 */
@Repository
public interface InvestorDailyRepository extends JpaRepository<InvestorDaily, InvestorDailyId> {

    /**
     * 종목코드·날짜 범위 기준 매매동향을 거래일 오름차순으로 조회한다.
     */
    List<InvestorDaily> findByIdStockCodeAndIdTradeDateBetweenOrderByIdTradeDate(
            String stockCode, LocalDate from, LocalDate to);
}
