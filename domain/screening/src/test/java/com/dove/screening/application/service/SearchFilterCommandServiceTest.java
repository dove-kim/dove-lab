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

import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SearchFilterCommandServiceTest {

    @Mock SearchFilterRepository repository;
    @InjectMocks SearchFilterCommandService service;

    private static final Long MEMBER_ID = 1L;
    private static final Long FILTER_ID = 10L;

    private SearchFilter makeFilter() {
        return SearchFilter.create(MEMBER_ID, "필터", DateRule.LATEST,
                List.of(MarketType.KOSPI), "{}", null, null);
    }

    @Test
    @DisplayName("create — repository.save 위임 후 반환")
    void shouldDelegateToRepositoryOnCreate() {
        SearchFilter filter = makeFilter();
        given(repository.save(any())).willReturn(filter);

        SearchFilter result = service.create(MEMBER_ID, "필터", DateRule.LATEST,
                List.of(MarketType.KOSPI), "{}", null, null);

        assertThat(result).isSameAs(filter);
        verify(repository).save(any(SearchFilter.class));
    }

    @Test
    @DisplayName("update — 필터 찾으면 update 호출 후 반환")
    void shouldUpdateFilterWhenFound() {
        SearchFilter filter = makeFilter();
        given(repository.findByIdAndMemberId(FILTER_ID, MEMBER_ID)).willReturn(Optional.of(filter));

        SearchFilter result = service.update(MEMBER_ID, FILTER_ID, "새이름", DateRule.PREV_3D,
                List.of(MarketType.KOSDAQ), "{}", null, null);

        assertThat(result.getName()).isEqualTo("새이름");
    }

    @Test
    @DisplayName("update — 필터 없으면 NoSuchElementException")
    void shouldThrowWhenFilterNotFoundOnUpdate() {
        given(repository.findByIdAndMemberId(FILTER_ID, MEMBER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(MEMBER_ID, FILTER_ID, "이름", DateRule.LATEST,
                List.of(MarketType.KOSPI), "{}", null, null))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    @DisplayName("delete — 필터 찾으면 repository.delete 호출")
    void shouldDeleteWhenFound() {
        SearchFilter filter = makeFilter();
        given(repository.findByIdAndMemberId(FILTER_ID, MEMBER_ID)).willReturn(Optional.of(filter));

        service.delete(MEMBER_ID, FILTER_ID);

        verify(repository).delete(filter);
    }

    @Test
    @DisplayName("delete — 필터 없으면 NoSuchElementException")
    void shouldThrowWhenFilterNotFoundOnDelete() {
        given(repository.findByIdAndMemberId(FILTER_ID, MEMBER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(MEMBER_ID, FILTER_ID))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    @DisplayName("reorder — 전달 순서대로 displayOrder 갱신")
    void shouldReorderFilters() {
        SearchFilter f1 = makeFilter();
        SearchFilter f2 = SearchFilter.create(MEMBER_ID, "필터2", DateRule.LATEST,
                List.of(MarketType.KOSPI), "{}", null, null);
        ReflectionTestUtils.setField(f1, "id", 1L);
        ReflectionTestUtils.setField(f2, "id", 2L);

        given(repository.findAllByMemberId(MEMBER_ID)).willReturn(List.of(f1, f2));

        service.reorder(MEMBER_ID, List.of(2L, 1L));   // f2 → 0번, f1 → 1번

        assertThat(f2.getDisplayOrder()).isEqualTo(0);
        assertThat(f1.getDisplayOrder()).isEqualTo(1);
    }
}
