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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class StockSetQueryServiceTest {

    @Mock StockSetRepository repository;
    @InjectMocks StockSetQueryService service;

    private static final Long MEMBER_ID = 1L;
    private static final Long SET_ID = 10L;

    @Test
    @DisplayName("findAllByMemberId — repository 위임 결과 반환")
    void shouldReturnAllStockSetsForMember() {
        StockSet s = StockSet.create(MEMBER_ID, "세트", List.of("005930"));
        given(repository.findAllByMemberIdOrderByCreatedAtDesc(MEMBER_ID)).willReturn(List.of(s));

        assertThat(service.findAllByMemberId(MEMBER_ID)).containsExactly(s);
    }

    @Test
    @DisplayName("findAllByMemberId — 결과 없으면 빈 리스트")
    void shouldReturnEmptyWhenNoStockSets() {
        given(repository.findAllByMemberIdOrderByCreatedAtDesc(MEMBER_ID)).willReturn(List.of());

        assertThat(service.findAllByMemberId(MEMBER_ID)).isEmpty();
    }

    @Test
    @DisplayName("findByIdAndMemberId — 존재하면 Optional 반환")
    void shouldReturnStockSetWhenFound() {
        StockSet s = StockSet.create(MEMBER_ID, "세트", List.of());
        given(repository.findByIdAndMemberId(SET_ID, MEMBER_ID)).willReturn(Optional.of(s));

        assertThat(service.findByIdAndMemberId(SET_ID, MEMBER_ID)).contains(s);
    }

    @Test
    @DisplayName("findByIdAndMemberId — 없으면 Optional.empty")
    void shouldReturnEmptyWhenNotFound() {
        given(repository.findByIdAndMemberId(SET_ID, MEMBER_ID)).willReturn(Optional.empty());

        assertThat(service.findByIdAndMemberId(SET_ID, MEMBER_ID)).isEmpty();
    }
}
