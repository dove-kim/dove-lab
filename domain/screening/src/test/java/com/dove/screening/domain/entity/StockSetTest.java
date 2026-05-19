package com.dove.screening.domain.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StockSetTest {

    @Test
    @DisplayName("create — 필드 정상 설정")
    void shouldSetFieldsOnCreate() {
        StockSet s = StockSet.create(1L, "세트1", List.of("005930", "000660"));

        assertThat(s.getMemberId()).isEqualTo(1L);
        assertThat(s.getName()).isEqualTo("세트1");
        assertThat(s.getCreatedAt()).isNotNull();
        assertThat(s.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("getCodeList — 쉼표 구분 문자열 → 코드 리스트 파싱")
    void shouldReturnCodeList() {
        StockSet s = StockSet.create(1L, "세트", List.of("005930", "000660", "035420"));

        assertThat(s.getCodeList()).containsExactly("005930", "000660", "035420");
    }

    @Test
    @DisplayName("getCodeList — 빈 리스트면 빈 결과")
    void shouldReturnEmptyListWhenNoCodes() {
        StockSet s = StockSet.create(1L, "빈세트", List.of());

        assertThat(s.getCodeList()).isEmpty();
    }

    @Test
    @DisplayName("getCodeSet — 중복 없는 Set 반환")
    void shouldReturnCodeSet() {
        StockSet s = StockSet.create(1L, "세트", List.of("005930", "000660"));

        assertThat(s.getCodeSet()).containsExactlyInAnyOrder("005930", "000660");
    }

    @Test
    @DisplayName("update — 이름과 코드 목록 갱신")
    void shouldUpdateNameAndCodes() {
        StockSet s = StockSet.create(1L, "세트1", List.of("005930"));

        s.update("세트2", List.of("000660", "035420"));

        assertThat(s.getName()).isEqualTo("세트2");
        assertThat(s.getCodeList()).containsExactly("000660", "035420");
    }
}
