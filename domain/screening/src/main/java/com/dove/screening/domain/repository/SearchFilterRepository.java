package com.dove.screening.domain.repository;

import com.dove.screening.domain.entity.SearchFilter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SearchFilterRepository extends JpaRepository<SearchFilter, Long> {

    List<SearchFilter> findAllByMemberIdOrderByDisplayOrderAscCreatedAtAsc(Long memberId);

    List<SearchFilter> findAllByMemberId(Long memberId);

    Optional<SearchFilter> findByIdAndMemberId(Long id, Long memberId);

    boolean existsByMemberIdAndName(Long memberId, String name);
}
