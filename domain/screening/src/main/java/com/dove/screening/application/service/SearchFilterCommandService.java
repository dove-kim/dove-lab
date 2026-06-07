package com.dove.screening.application.service;

import com.dove.market.domain.enums.MarketType;
import com.dove.screening.domain.entity.SearchFilter;
import com.dove.screening.domain.enums.DateRule;
import com.dove.screening.domain.repository.SearchFilterRepository;
import com.dove.screening.domain.value.FilterExpression;
import com.dove.stock.domain.enums.PriceType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

/**
 * 검색 필터 변경 서비스.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class SearchFilterCommandService {

    private final SearchFilterRepository searchFilterRepository;

    /**
     * 새 검색 필터를 생성한다.
     */
    public SearchFilter create(Long memberId, String name, DateRule dateRule,
                                List<MarketType> markets, PriceType priceType, FilterExpression expression,
                                Long stockFilterId) {
        return searchFilterRepository.save(
                SearchFilter.create(memberId, name, dateRule, markets, priceType, expression, stockFilterId));
    }

    /**
     * 회원이 소유한 검색 필터를 수정한다.
     *
     * @throws NoSuchElementException 해당 필터가 없을 때
     */
    public SearchFilter update(Long memberId, Long id, String name, DateRule dateRule,
                                List<MarketType> markets, PriceType priceType, FilterExpression expression,
                                Long stockFilterId) {
        SearchFilter filter = searchFilterRepository.findByIdAndMemberId(id, memberId)
                .orElseThrow(() -> new NoSuchElementException("FILTER_NOT_FOUND"));
        filter.update(name, dateRule, markets, priceType, expression, stockFilterId);
        return filter;
    }

    /**
     * 회원이 소유한 검색 필터를 삭제한다.
     *
     * @throws NoSuchElementException 해당 필터가 없을 때
     */
    public void delete(Long memberId, Long id) {
        SearchFilter filter = searchFilterRepository.findByIdAndMemberId(id, memberId)
                .orElseThrow(() -> new NoSuchElementException("FILTER_NOT_FOUND"));
        searchFilterRepository.delete(filter);
    }

    /**
     * 주어진 ID 순서대로 회원의 검색 필터 노출 순서를 재배치한다.
     */
    public void reorder(Long memberId, List<Long> orderedIds) {
        Map<Long, SearchFilter> filterMap = searchFilterRepository.findAllByMemberId(memberId)
                .stream().collect(Collectors.toMap(SearchFilter::getId, f -> f));
        for (int i = 0; i < orderedIds.size(); i++) {
            SearchFilter f = filterMap.get(orderedIds.get(i));
            if (f != null) f.updateDisplayOrder(i);
        }
    }
}
