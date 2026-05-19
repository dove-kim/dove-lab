package com.dove.screening.application.service;

import com.dove.screening.domain.entity.IndicatorPreset;
import com.dove.screening.domain.repository.IndicatorPresetRepository;
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
class IndicatorPresetCommandServiceTest {

    @Mock IndicatorPresetRepository repository;
    @InjectMocks IndicatorPresetCommandService service;

    private static final Long MEMBER_ID = 1L;
    private static final Long PRESET_ID = 10L;

    private IndicatorPreset makePreset() {
        return IndicatorPreset.create(MEMBER_ID, "프리셋", "[]", null);
    }

    @Test
    @DisplayName("create — repository.save 위임 후 반환")
    void shouldDelegateToRepositoryOnCreate() {
        IndicatorPreset preset = makePreset();
        given(repository.save(any())).willReturn(preset);

        IndicatorPreset result = service.create(MEMBER_ID, "프리셋", "[]", null);

        assertThat(result).isSameAs(preset);
        verify(repository).save(any(IndicatorPreset.class));
    }

    @Test
    @DisplayName("update — 프리셋 찾으면 update 호출 후 반환")
    void shouldUpdatePresetWhenFound() {
        IndicatorPreset preset = makePreset();
        given(repository.findByIdAndMemberId(PRESET_ID, MEMBER_ID)).willReturn(Optional.of(preset));

        IndicatorPreset result = service.update(MEMBER_ID, PRESET_ID, "새프리셋", "[{\"type\":\"EMA_5\"}]", "P1");

        assertThat(result.getName()).isEqualTo("새프리셋");
        assertThat(result.getItems()).isEqualTo("[{\"type\":\"EMA_5\"}]");
        assertThat(result.getPanelOrder()).isEqualTo("P1");
    }

    @Test
    @DisplayName("update — 프리셋 없으면 NoSuchElementException")
    void shouldThrowWhenPresetNotFoundOnUpdate() {
        given(repository.findByIdAndMemberId(PRESET_ID, MEMBER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(MEMBER_ID, PRESET_ID, "이름", "[]", null))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    @DisplayName("delete — 프리셋 찾으면 repository.delete 호출")
    void shouldDeleteWhenFound() {
        IndicatorPreset preset = makePreset();
        given(repository.findByIdAndMemberId(PRESET_ID, MEMBER_ID)).willReturn(Optional.of(preset));

        service.delete(MEMBER_ID, PRESET_ID);

        verify(repository).delete(preset);
    }

    @Test
    @DisplayName("delete — 프리셋 없으면 NoSuchElementException")
    void shouldThrowWhenPresetNotFoundOnDelete() {
        given(repository.findByIdAndMemberId(PRESET_ID, MEMBER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(MEMBER_ID, PRESET_ID))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    @DisplayName("reorder — 전달 순서대로 displayOrder 갱신")
    void shouldReorderPresets() {
        IndicatorPreset p1 = makePreset();
        IndicatorPreset p2 = IndicatorPreset.create(MEMBER_ID, "프리셋2", "[]", null);
        ReflectionTestUtils.setField(p1, "id", 1L);
        ReflectionTestUtils.setField(p2, "id", 2L);

        given(repository.findAllByMemberId(MEMBER_ID)).willReturn(List.of(p1, p2));

        service.reorder(MEMBER_ID, List.of(2L, 1L));   // p2 → 0번, p1 → 1번

        assertThat(p2.getDisplayOrder()).isEqualTo(0);
        assertThat(p1.getDisplayOrder()).isEqualTo(1);
    }
}
