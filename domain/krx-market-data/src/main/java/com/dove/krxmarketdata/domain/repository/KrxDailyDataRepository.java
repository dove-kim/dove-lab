package com.dove.krxmarketdata.domain.repository;

import com.dove.krxmarketdata.domain.entity.KrxDailyData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** KRX API 호출 이력 저장 리포지토리. */
@Repository
public interface KrxDailyDataRepository extends JpaRepository<KrxDailyData, Long> {
}
