package com.dove.screening.application.service;

import com.dove.screening.domain.entity.SearchFilter;
import com.dove.screening.domain.repository.SearchFilterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchFilterQueryService {

    private final SearchFilterRepository searchFilterRepository;

    public List<SearchFilter> findAllByMemberId(Long memberId) {
        return searchFilterRepository.findAllByMemberIdOrderByDisplayOrderAscCreatedAtAsc(memberId);
    }

    public Optional<SearchFilter> findByIdAndMemberId(Long id, Long memberId) {
        return searchFilterRepository.findByIdAndMemberId(id, memberId);
    }
}
