package com.dove.screening.application.service;

import com.dove.screening.domain.entity.IndicatorPreset;
import com.dove.screening.domain.repository.IndicatorPresetRepository;
import com.dove.screening.domain.value.IndicatorPresetItem;
import com.dove.screening.domain.value.PresetOverlay;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
@DisplayName("IndicatorPresetCommandService")
class IndicatorPresetCommandServiceTest {

    @Mock IndicatorPresetRepository indicatorPresetRepository;
    @InjectMocks IndicatorPresetCommandService service;

    private static final Long MEMBER_ID = 1L;
    private static final Long PRESET_ID = 10L;

    private IndicatorPreset makePreset(String name) {
        return IndicatorPreset.create(MEMBER_ID, name, List.of(), List.of());
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("프리셋을 저장하고 반환한다")
        void shouldSaveAndReturnPreset() {
            IndicatorPreset preset = makePreset("기본");
            given(indicatorPresetRepository.save(any())).willReturn(preset);

            IndicatorPreset result = service.create(MEMBER_ID, "기본", List.of(), List.of());

            assertThat(result).isSameAs(preset);
            verify(indicatorPresetRepository).save(any(IndicatorPreset.class));
        }

        @Test
        @DisplayName("오버레이를 함께 저장한다")
        void shouldSaveOverlayWhenProvided() {
            PresetOverlay overlay = new PresetOverlay(7L, 0.6, List.of(1L, 2L));
            given(indicatorPresetRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            IndicatorPreset result = service.create(MEMBER_ID, "기본", List.of(), List.of(), overlay);

            ArgumentCaptor<IndicatorPreset> captor = ArgumentCaptor.forClass(IndicatorPreset.class);
            verify(indicatorPresetRepository).save(captor.capture());
            assertThat(captor.getValue().getOverlay()).isEqualTo(overlay);
            assertThat(result.getOverlay()).isEqualTo(overlay);
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("프리셋을 찾으면 이름과 항목을 수정한다")
        void shouldUpdateWhenFound() {
            IndicatorPreset preset = makePreset("기본");
            given(indicatorPresetRepository.findByIdAndMemberId(PRESET_ID, MEMBER_ID))
                    .willReturn(Optional.of(preset));

            IndicatorPreset result = service.update(MEMBER_ID, PRESET_ID, "수정됨", List.of(), List.of());

            assertThat(result.getName()).isEqualTo("수정됨");
        }

        @Test
        @DisplayName("프리셋이 없으면 NoSuchElementException을 던진다")
        void shouldThrowWhenNotFound() {
            given(indicatorPresetRepository.findByIdAndMemberId(PRESET_ID, MEMBER_ID))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> service.update(MEMBER_ID, PRESET_ID, "이름", List.of(), List.of()))
                    .isInstanceOf(NoSuchElementException.class);
        }

        @Test
        @DisplayName("오버레이를 갱신한다")
        void shouldUpdateOverlayWhenFound() {
            IndicatorPreset preset = makePreset("기본");
            PresetOverlay overlay = new PresetOverlay(3L, 0.75, List.of(9L));
            given(indicatorPresetRepository.findByIdAndMemberId(PRESET_ID, MEMBER_ID))
                    .willReturn(Optional.of(preset));

            IndicatorPreset result = service.update(MEMBER_ID, PRESET_ID, "수정됨", List.of(), List.of(), overlay);

            assertThat(result.getOverlay()).isEqualTo(overlay);
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("프리셋을 찾으면 삭제한다")
        void shouldDeleteWhenFound() {
            IndicatorPreset preset = makePreset("기본");
            given(indicatorPresetRepository.findByIdAndMemberId(PRESET_ID, MEMBER_ID))
                    .willReturn(Optional.of(preset));

            service.delete(MEMBER_ID, PRESET_ID);

            verify(indicatorPresetRepository).delete(preset);
        }

        @Test
        @DisplayName("프리셋이 없으면 NoSuchElementException을 던진다")
        void shouldThrowWhenNotFound() {
            given(indicatorPresetRepository.findByIdAndMemberId(PRESET_ID, MEMBER_ID))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> service.delete(MEMBER_ID, PRESET_ID))
                    .isInstanceOf(NoSuchElementException.class);
        }
    }

    @Nested
    @DisplayName("reorder")
    class Reorder {

        @Test
        @DisplayName("전달 순서대로 displayOrder를 갱신한다")
        void shouldReorderByGivenIdOrder() {
            IndicatorPreset p1 = makePreset("A");
            IndicatorPreset p2 = makePreset("B");
            ReflectionTestUtils.setField(p1, "id", 1L);
            ReflectionTestUtils.setField(p2, "id", 2L);
            given(indicatorPresetRepository.findAllByMemberId(MEMBER_ID)).willReturn(List.of(p1, p2));

            service.reorder(MEMBER_ID, List.of(2L, 1L));

            assertThat(p2.getDisplayOrder()).isEqualTo(0);
            assertThat(p1.getDisplayOrder()).isEqualTo(1);
        }
    }
}
