package com.dove.technicalindicator.domain.repository;

import com.dove.market.domain.enums.MarketType;
import com.dove.technicalindicator.domain.entity.IndicatorCursor;
import com.dove.technicalindicator.domain.entity.IndicatorCursorId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IndicatorCursorRepository extends JpaRepository<IndicatorCursor, IndicatorCursorId> {

    List<IndicatorCursor> findAllById_MarketTypeAndId_Code(MarketType marketType, String code);
}
