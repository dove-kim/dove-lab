package com.dove.screening.application.service;

import com.dove.market.domain.enums.MarketType;
import com.dove.screening.domain.entity.SearchFilter;
import com.dove.screening.domain.enums.DateRule;
import com.dove.screening.domain.repository.SearchFilterRepository;
import com.dove.screening.domain.value.FilterExpression;
import com.dove.stock.domain.enums.PriceType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
@DisplayName("SearchFilterQueryService")
class SearchFilterQueryServiceTest {

    @Mock SearchFilterRepository repository;
    @InjectMocks SearchFilterQueryService service;

    private static final Long MEMBER_ID = 1L;
    private static final Long FILTER_ID = 10L;

    private SearchFilter makeFilter() {
        return SearchFilter.create(MEMBER_ID, "필터", DateRule.LATEST,
                List.of(MarketType.KOSPI), PriceType.RAW, FilterExpression.empty(), null);
    }

    @Nested
    @DisplayName("findAllByMemberId")
    class FindAllByMemberId {

        @Test
        @DisplayName("repository 위임 결과를 반환한다")
        void shouldReturnFiltersForMember() {
            SearchFilter filter = makeFilter();
            given(repository.findAllByMemberIdOrderByDisplayOrderAscCreatedAtAsc(MEMBER_ID))
                    .willReturn(List.of(filter));

            List<SearchFilter> result = service.findAllByMemberId(MEMBER_ID);

            assertThat(result).containsExactly(filter);
        }

        @Test
        @DisplayName("결과 없으면 빈 리스트를 반환한다")
        void shouldReturnEmptyWhenNoFilters() {
            given(repository.findAllByMemberIdOrderByDisplayOrderAscCreatedAtAsc(MEMBER_ID))
                    .willReturn(List.of());

            assertThat(service.findAllByMemberId(MEMBER_ID)).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByIdAndMemberId")
    class FindByIdAndMemberId {

        @Test
        @DisplayName("존재하면 Optional에 담아 반환한다")
        void shouldReturnFilterWhenFound() {
            SearchFilter filter = makeFilter();
            given(repository.findByIdAndMemberId(FILTER_ID, MEMBER_ID))
                    .willReturn(Optional.of(filter));

            assertThat(service.findByIdAndMemberId(FILTER_ID, MEMBER_ID)).contains(filter);
        }

        @Test
        @DisplayName("없으면 Optional.empty를 반환한다")
        void shouldReturnEmptyWhenNotFound() {
            given(repository.findByIdAndMemberId(FILTER_ID, MEMBER_ID))
                    .willReturn(Optional.empty());

            assertThat(service.findByIdAndMemberId(FILTER_ID, MEMBER_ID)).isEmpty();
        }
    }
}
