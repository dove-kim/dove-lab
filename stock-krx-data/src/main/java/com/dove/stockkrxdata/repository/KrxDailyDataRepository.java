package com.dove.stockkrxdata.repository;

import com.dove.stockkrxdata.repository.entity.KrxDailyData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface KrxDailyDataRepository extends JpaRepository<KrxDailyData, Long> {
}
