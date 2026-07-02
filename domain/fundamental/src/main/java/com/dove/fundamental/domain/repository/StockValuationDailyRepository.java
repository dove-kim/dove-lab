package com.dove.fundamental.domain.repository;

import com.dove.fundamental.domain.entity.StockValuationDaily;
import com.dove.fundamental.domain.entity.StockValuationDailyId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 일별 밸류에이션 저장소.
 */
@Repository
public interface StockValuationDailyRepository extends JpaRepository<StockValuationDaily, StockValuationDailyId> {

    /**
     * 특정 거래일의 전 종목 밸류에이션을 반환한다(스크리닝·랭킹용).
     */
    List<StockValuationDaily> findByTradeDate(LocalDate tradeDate);

    /**
     * 종목의 최근 밸류에이션을 거래일 내림차순으로 조회한다(상세 탭 표시용).
     */
    List<StockValuationDaily> findTop250ByTickerOrderByTradeDateDesc(String ticker);

    /**
     * 종목의 최신 밸류에이션 1건(상세 화면 최신값 노출용).
     */
    Optional<StockValuationDaily> findFirstByTickerOrderByTradeDateDesc(String ticker);
}
