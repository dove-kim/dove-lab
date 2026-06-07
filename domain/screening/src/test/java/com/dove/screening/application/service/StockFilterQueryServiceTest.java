package com.dove.screening.application.service;

import com.dove.market.domain.enums.MarketType;
import com.dove.screening.domain.entity.StockFilter;
import com.dove.screening.domain.repository.StockFilterRepository;
import com.dove.screening.infrastructure.repository.StockTagFilterRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("StockFilterQueryService")
class StockFilterQueryServiceTest {

    @Mock StockFilterRepository repository;
    @Mock StockTagFilterRepository stockTagFilterRepository;
    @InjectMocks StockFilterQueryService service;

    @Nested
    @DisplayName("findSystemFilters")
    class FindSystemFilters {

        @Test
        @DisplayName("시스템 필터만 조회한다")
        void shouldFindSystemFilters() {
            StockFilter system = StockFilter.createSystem("s", null, List.of(), List.of(), List.of(), "admin");
            given(repository.findByMemberIdIsNullOrderByDisplayOrderAsc()).willReturn(List.of(system));

            List<StockFilter> result = service.findSystemFilters();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).isSystem()).isTrue();
        }
    }

    @Nested
    @DisplayName("findSystemFiltersEnabled")
    class FindSystemFiltersEnabled {

        @Test
        @DisplayName("활성 시스템 필터만 조회한다")
        void shouldFindEnabledSystemFilters() {
            StockFilter system = StockFilter.createSystem("s", null, List.of(), List.of(), List.of(), "admin");
            given(repository.findByMemberIdIsNullAndEnabledTrueOrderByDisplayOrderAsc())
                    .willReturn(List.of(system));

            List<StockFilter> result = service.findSystemFiltersEnabled();

            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("findPersonalFilters")
    class FindPersonalFilters {

        @Test
        @DisplayName("회원의 개인 필터만 조회한다")
        void shouldFindPersonalFilters() {
            StockFilter personal = StockFilter.createPersonal(7L, "n", null, List.of(), List.of(), List.of(), "u");
            given(repository.findByMemberIdOrderByDisplayOrderAsc(7L)).willReturn(List.of(personal));

            List<StockFilter> result = service.findPersonalFilters(7L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getMemberId()).isEqualTo(7L);
        }
    }

    @Nested
    @DisplayName("findAvailableForMember")
    class FindAvailableForMember {

        @Test
        @DisplayName("활성 시스템 필터와 개인 필터를 통합해 반환한다")
        void shouldReturnAvailableForMember() {
            StockFilter system = StockFilter.createSystem("s", null, List.of(), List.of(), List.of(), "admin");
            StockFilter personal = StockFilter.createPersonal(7L, "p", null, List.of(), List.of(), List.of(), "u");
            given(repository.findByMemberIdIsNullAndEnabledTrueOrderByDisplayOrderAsc())
                    .willReturn(List.of(system));
            given(repository.findByMemberIdOrderByDisplayOrderAsc(7L)).willReturn(List.of(personal));

            List<StockFilter> result = service.findAvailableForMember(7L);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).isSystem()).isTrue();
            assertThat(result.get(1).getMemberId()).isEqualTo(7L);
        }
    }

    @Nested
    @DisplayName("resolveTickers")
    class ResolveTickers {

        @Test
        @DisplayName("필터가 존재하면 태그 조건을 평가해 ticker 집합을 반환한다")
        void shouldReturnTickersWhenFilterExists() {
            StockFilter filter = StockFilter.createSystem("s", null, List.of(), List.of(), List.of(), "admin");
            given(repository.findById(1L)).willReturn(Optional.of(filter));
            given(stockTagFilterRepository.findTickers(any(), any(), any(), any()))
                    .willReturn(Set.of("005930", "000660"));

            Set<String> result = service.resolveTickers(1L, List.of(MarketType.KOSPI));

            assertThat(result).containsExactlyInAnyOrder("005930", "000660");
        }

        @Test
        @DisplayName("필터가 없으면 빈 집합을 반환한다")
        void shouldReturnEmptyWhenFilterNotFound() {
            given(repository.findById(999L)).willReturn(Optional.empty());

            Set<String> result = service.resolveTickers(999L, List.of(MarketType.KOSPI));

            assertThat(result).isEmpty();
        }
    }
}
