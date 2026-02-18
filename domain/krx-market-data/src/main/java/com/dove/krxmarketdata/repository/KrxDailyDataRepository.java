package com.dove.krxmarketdata.repository;

import com.dove.krxmarketdata.entity.KrxDailyData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface KrxDailyDataRepository extends JpaRepository<KrxDailyData, Long> {
}
