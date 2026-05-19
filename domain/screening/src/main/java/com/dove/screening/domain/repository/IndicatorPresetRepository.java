package com.dove.screening.domain.repository;

import com.dove.screening.domain.entity.IndicatorPreset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IndicatorPresetRepository extends JpaRepository<IndicatorPreset, Long> {
    List<IndicatorPreset> findAllByMemberIdOrderByDisplayOrderAscCreatedAtAsc(Long memberId);

    List<IndicatorPreset> findAllByMemberId(Long memberId);

    Optional<IndicatorPreset> findByIdAndMemberId(Long id, Long memberId);
}
