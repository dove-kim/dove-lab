package com.dove.screening.application.service;

import com.dove.market.domain.enums.MarketType;
import com.dove.screening.domain.entity.SearchFilter;
import com.dove.screening.domain.enums.DateRule;
import com.dove.screening.domain.repository.SearchFilterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class SearchFilterCommandService {

    private final SearchFilterRepository searchFilterRepository;

    public SearchFilter create(Long memberId, String name, DateRule dateRule,
                                List<MarketType> markets, String expression,
                                Long includeStockSetId, Long excludeStockSetId) {
        return searchFilterRepository.save(
                SearchFilter.create(memberId, name, dateRule, markets, expression,
                        includeStockSetId, excludeStockSetId));
    }

    public SearchFilter update(Long memberId, Long id, String name, DateRule dateRule,
                                List<MarketType> markets, String expression,
                                Long includeStockSetId, Long excludeStockSetId) {
        SearchFilter filter = searchFilterRepository.findByIdAndMemberId(id, memberId)
                .orElseThrow(() -> new NoSuchElementException("FILTER_NOT_FOUND"));
        filter.update(name, dateRule, markets, expression, includeStockSetId, excludeStockSetId);
        return filter;
    }

    public void delete(Long memberId, Long id) {
        SearchFilter filter = searchFilterRepository.findByIdAndMemberId(id, memberId)
                .orElseThrow(() -> new NoSuchElementException("FILTER_NOT_FOUND"));
        searchFilterRepository.delete(filter);
    }

    public void reorder(Long memberId, List<Long> orderedIds) {
        Map<Long, SearchFilter> filterMap = searchFilterRepository.findAllByMemberId(memberId)
                .stream().collect(Collectors.toMap(SearchFilter::getId, f -> f));
        for (int i = 0; i < orderedIds.size(); i++) {
            SearchFilter f = filterMap.get(orderedIds.get(i));
            if (f != null) f.updateDisplayOrder(i);
        }
    }
}
