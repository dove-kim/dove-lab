package com.dove.screening.domain.repository;

import com.dove.screening.domain.entity.StockFilter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 종목 필터 영속성 저장소.
 */
public interface StockFilterRepository extends JpaRepository<StockFilter, Long> {

    /** 개인 필터 단건 — 본인 소유 확인 포함. */
    Optional<StockFilter> findByIdAndMemberId(Long id, Long memberId);

    /** 본인 개인 필터 목록 (표시 순서 기준). */
    List<StockFilter> findByMemberIdOrderByDisplayOrderAsc(Long memberId);

    /** 시스템 필터 전체 (관리 화면용, enabled 무관). */
    List<StockFilter> findByMemberIdIsNullOrderByDisplayOrderAsc();

    /** picker 용 활성 시스템 필터. */
    List<StockFilter> findByMemberIdIsNullAndEnabledTrueOrderByDisplayOrderAsc();

    /** 시스템 필터 이름 중복 체크. */
    long countByMemberIdIsNullAndName(String name);

    /** 시스템 필터 이름 중복 체크 (수정 시 자기 자신 제외). */
    long countByMemberIdIsNullAndNameAndIdNot(String name, Long id);
}
