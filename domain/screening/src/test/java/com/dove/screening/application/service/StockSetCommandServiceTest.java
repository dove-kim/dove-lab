package com.dove.screening.application.service;

import com.dove.screening.domain.entity.StockSet;
import com.dove.screening.domain.repository.StockSetRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class StockSetCommandServiceTest {

    @Mock StockSetRepository repository;
    @InjectMocks StockSetCommandService service;

    private static final Long MEMBER_ID = 1L;
    private static final Long SET_ID = 10L;

    private StockSet makeSet() {
        return StockSet.create(MEMBER_ID, "세트", List.of("005930"));
    }

    @Test
    @DisplayName("create — repository.save 위임 후 반환")
    void shouldDelegateToRepositoryOnCreate() {
        StockSet stockSet = makeSet();
        given(repository.save(any())).willReturn(stockSet);

        StockSet result = service.create(MEMBER_ID, "세트", List.of("005930"));

        assertThat(result).isSameAs(stockSet);
        verify(repository).save(any(StockSet.class));
    }

    @Test
    @DisplayName("update — 세트 찾으면 update 호출 후 반환")
    void shouldUpdateStockSetWhenFound() {
        StockSet stockSet = makeSet();
        given(repository.findByIdAndMemberId(SET_ID, MEMBER_ID)).willReturn(Optional.of(stockSet));

        StockSet result = service.update(MEMBER_ID, SET_ID, "새세트", List.of("000660"));

        assertThat(result.getName()).isEqualTo("새세트");
        assertThat(result.getCodeList()).containsExactly("000660");
    }

    @Test
    @DisplayName("update — 세트 없으면 NoSuchElementException")
    void shouldThrowWhenStockSetNotFoundOnUpdate() {
        given(repository.findByIdAndMemberId(SET_ID, MEMBER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(MEMBER_ID, SET_ID, "세트", List.of()))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    @DisplayName("delete — 세트 찾으면 repository.delete 호출")
    void shouldDeleteWhenFound() {
        StockSet stockSet = makeSet();
        given(repository.findByIdAndMemberId(SET_ID, MEMBER_ID)).willReturn(Optional.of(stockSet));

        service.delete(MEMBER_ID, SET_ID);

        verify(repository).delete(stockSet);
    }

    @Test
    @DisplayName("delete — 세트 없으면 NoSuchElementException")
    void shouldThrowWhenStockSetNotFoundOnDelete() {
        given(repository.findByIdAndMemberId(SET_ID, MEMBER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(MEMBER_ID, SET_ID))
                .isInstanceOf(NoSuchElementException.class);
    }
}
