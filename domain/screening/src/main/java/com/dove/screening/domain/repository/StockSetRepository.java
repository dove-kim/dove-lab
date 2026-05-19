package com.dove.screening.domain.repository;

import com.dove.screening.domain.entity.StockSet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StockSetRepository extends JpaRepository<StockSet, Long> {
    List<StockSet> findAllByMemberIdOrderByCreatedAtDesc(Long memberId);
    Optional<StockSet> findByIdAndMemberId(Long id, Long memberId);
}
