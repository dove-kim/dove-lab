package com.dove.screening.domain.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IndicatorPresetTest {

    @Test
    @DisplayName("create — 필드 정상 설정")
    void shouldSetFieldsOnCreate() {
        IndicatorPreset p = IndicatorPreset.create(1L, "프리셋1", "[{\"type\":\"SMA_5\"}]", "P1,P2");

        assertThat(p.getMemberId()).isEqualTo(1L);
        assertThat(p.getName()).isEqualTo("프리셋1");
        assertThat(p.getItems()).isEqualTo("[{\"type\":\"SMA_5\"}]");
        assertThat(p.getPanelOrder()).isEqualTo("P1,P2");
        assertThat(p.getCreatedAt()).isNotNull();
        assertThat(p.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("update — name·items·panelOrder 갱신")
    void shouldUpdateFields() {
        IndicatorPreset p = IndicatorPreset.create(1L, "프리셋1", "[]", null);

        p.update("새프리셋", "[{\"type\":\"EMA_5\"}]", "P3");

        assertThat(p.getName()).isEqualTo("새프리셋");
        assertThat(p.getItems()).isEqualTo("[{\"type\":\"EMA_5\"}]");
        assertThat(p.getPanelOrder()).isEqualTo("P3");
    }

    @Test
    @DisplayName("updateDisplayOrder — displayOrder 갱신")
    void shouldUpdateDisplayOrder() {
        IndicatorPreset p = IndicatorPreset.create(1L, "프리셋", "[]", null);

        p.updateDisplayOrder(3);

        assertThat(p.getDisplayOrder()).isEqualTo(3);
    }
}
