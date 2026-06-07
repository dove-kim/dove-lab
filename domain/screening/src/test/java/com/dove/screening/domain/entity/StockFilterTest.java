package com.dove.screening.domain.entity;

import com.dove.screening.domain.value.StockCondition;
import com.dove.screening.domain.value.TagCondition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StockFilterTest {

    @Test
    @DisplayName("createSystem — memberId 는 null, enabled=true 로 설정")
    void shouldCreateSystemFilter() {
        StockFilter f = StockFilter.createSystem(
                "시스템필터", "설명",
                List.of(TagCondition.include("SECUGRP", "주권")),
                List.of(StockCondition.exclude("KOSPI", "005930")),
                List.of(),
                "admin");

        assertThat(f.getMemberId()).isNull();
        assertThat(f.isSystem()).isTrue();
        assertThat(f.getName()).isEqualTo("시스템필터");
        assertThat(f.isEnabled()).isTrue();
        assertThat(f.getCreatedBy()).isEqualTo("admin");
        assertThat(f.getTagConditions()).hasSize(1);
        assertThat(f.getStockConditions()).hasSize(1);
        assertThat(f.getCreatedAt()).isNotNull();
        assertThat(f.getUpdatedAt()).isNotNull();
        assertThat(f.getUpdatedBy()).isNull();
    }

    @Test
    @DisplayName("createPersonal — memberId 설정, enabled=true")
    void shouldCreatePersonalFilter() {
        StockFilter f = StockFilter.createPersonal(7L, "내필터", null, null, null, null, "user");

        assertThat(f.getMemberId()).isEqualTo(7L);
        assertThat(f.isSystem()).isFalse();
        assertThat(f.isEnabled()).isTrue();
        assertThat(f.getTagConditions()).isEmpty();
        assertThat(f.getStockConditions()).isEmpty();
    }

    @Test
    @DisplayName("createPersonal — memberId 가 null 이면 예외")
    void shouldThrowWhenMemberIdMissingOnCreatePersonal() {
        assertThatThrownBy(() -> StockFilter.createPersonal(null, "x", null, null, null, null, "u"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("isOwnedBy — memberId 일치 여부")
    void shouldCheckOwnership() {
        StockFilter personal = StockFilter.createPersonal(7L, "n", null, null, null, null, "u");
        StockFilter system = StockFilter.createSystem("s", null, null, null, null, "admin");

        assertThat(personal.isOwnedBy(7L)).isTrue();
        assertThat(personal.isOwnedBy(8L)).isFalse();
        assertThat(system.isOwnedBy(7L)).isFalse();
    }

    @Test
    @DisplayName("update — 필드 갱신 및 updatedBy 기록")
    void shouldUpdateFields() {
        StockFilter f = StockFilter.createSystem("이름", "설명", List.of(), List.of(), List.of(), "admin");

        f.update("새이름", "새설명",
                List.of(TagCondition.include("STOCK_TYPE", "보통주")),
                List.of(StockCondition.include("KOSPI", "005930")),
                List.of(),
                "editor");

        assertThat(f.getName()).isEqualTo("새이름");
        assertThat(f.getDescription()).isEqualTo("새설명");
        assertThat(f.getTagConditions()).hasSize(1);
        assertThat(f.getStockConditions()).hasSize(1);
        assertThat(f.getUpdatedBy()).isEqualTo("editor");
    }

    @Test
    @DisplayName("updateEnabled — enabled 토글")
    void shouldToggleEnabled() {
        StockFilter f = StockFilter.createSystem("s", null, List.of(), List.of(), List.of(), "admin");
        assertThat(f.isEnabled()).isTrue();

        f.updateEnabled(false, "editor");

        assertThat(f.isEnabled()).isFalse();
        assertThat(f.getUpdatedBy()).isEqualTo("editor");
    }

    @Test
    @DisplayName("updateDisplayOrder — displayOrder 갱신")
    void shouldUpdateDisplayOrder() {
        StockFilter f = StockFilter.createSystem("s", null, List.of(), List.of(), List.of(), "admin");

        f.updateDisplayOrder(5);

        assertThat(f.getDisplayOrder()).isEqualTo(5);
    }
}
