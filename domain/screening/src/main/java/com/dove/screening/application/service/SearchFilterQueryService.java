package com.dove.screening.application.service;

import com.dove.screening.domain.entity.SearchFilter;
import com.dove.screening.domain.repository.SearchFilterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 검색 필터 조회 서비스.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchFilterQueryService {

    private final SearchFilterRepository searchFilterRepository;

    /**
     * 회원의 검색 필터 목록을 노출 순서대로 조회한다.
     */
    public List<SearchFilter> findAllByMemberId(Long memberId) {
        return searchFilterRepository.findAllByMemberIdOrderByDisplayOrderAscCreatedAtAsc(memberId);
    }

    /**
     * 회원이 소유한 검색 필터 단건을 조회한다.
     */
    public Optional<SearchFilter> findByIdAndMemberId(Long id, Long memberId) {
        return searchFilterRepository.findByIdAndMemberId(id, memberId);
    }
}
