package com.dove.technicalindicator.domain.repository;

import com.dove.technicalindicator.domain.entity.TechnicalIndicator;
import com.dove.technicalindicator.domain.entity.TechnicalIndicatorId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** 기술적 지표 저장 리포지토리. */
@Repository
public interface TechnicalIndicatorRepository extends JpaRepository<TechnicalIndicator, TechnicalIndicatorId> {
}
