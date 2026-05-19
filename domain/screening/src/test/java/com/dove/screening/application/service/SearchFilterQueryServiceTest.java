package com.dove.screening.application.service;

import com.dove.market.domain.enums.MarketType;
import com.dove.screening.domain.entity.SearchFilter;
import com.dove.screening.domain.enums.DateRule;
import com.dove.screening.domain.repository.SearchFilterRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class SearchFilterQueryServiceTest {

    @Mock SearchFilterRepository repository;
    @InjectMocks SearchFilterQueryService service;

    private static final Long MEMBER_ID = 1L;
    private static final Long FILTER_ID = 10L;

    private SearchFilter makeFilter() {
        return SearchFilter.create(MEMBER_ID, "필터", DateRule.LATEST,
                List.of(MarketType.KOSPI), "{}", null, null);
    }

    @Test
    @DisplayName("findAllByMemberId — repository 위임 결과 반환")
    void shouldReturnFiltersForMember() {
        SearchFilter filter = makeFilter();
        given(repository.findAllByMemberIdOrderByDisplayOrderAscCreatedAtAsc(MEMBER_ID))
                .willReturn(List.of(filter));

        List<SearchFilter> result = service.findAllByMemberId(MEMBER_ID);

        assertThat(result).containsExactly(filter);
    }

    @Test
    @DisplayName("findAllByMemberId — 결과 없으면 빈 리스트")
    void shouldReturnEmptyWhenNoFilters() {
        given(repository.findAllByMemberIdOrderByDisplayOrderAscCreatedAtAsc(MEMBER_ID))
                .willReturn(List.of());

        assertThat(service.findAllByMemberId(MEMBER_ID)).isEmpty();
    }

    @Test
    @DisplayName("findByIdAndMemberId — 존재하면 Optional 반환")
    void shouldReturnFilterWhenFound() {
        SearchFilter filter = makeFilter();
        given(repository.findByIdAndMemberId(FILTER_ID, MEMBER_ID))
                .willReturn(Optional.of(filter));

        assertThat(service.findByIdAndMemberId(FILTER_ID, MEMBER_ID)).contains(filter);
    }

    @Test
    @DisplayName("findByIdAndMemberId — 없으면 Optional.empty")
    void shouldReturnEmptyWhenNotFound() {
        given(repository.findByIdAndMemberId(FILTER_ID, MEMBER_ID))
                .willReturn(Optional.empty());

        assertThat(service.findByIdAndMemberId(FILTER_ID, MEMBER_ID)).isEmpty();
    }
}
