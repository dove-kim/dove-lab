package com.dove.stockprice.domain.repository;

import com.dove.stockprice.domain.entity.StockDataChange;
import com.dove.stockprice.domain.entity.StockDataChangeId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * StockDataChange 리포지토리.
 * 주가 변경 기록의 저장, 조회, 삭제를 담당한다.
 */
@Repository
public interface StockDataChangeRepository extends JpaRepository<StockDataChange, StockDataChangeId> {

    /** 지정 시각 이전에 생성된 변경 기록을 조회한다. */
    List<StockDataChange> findAllByCreatedAtBefore(LocalDateTime threshold);

    /** 처리 완료된 변경 기록을 일괄 삭제한다. */
    default void deleteChanges(List<StockDataChange> entities) {
        deleteAllInBatch(entities);
    }
}
