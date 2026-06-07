package com.dove.screening.application.service;

import com.dove.screening.domain.entity.IndicatorPreset;
import com.dove.screening.domain.repository.IndicatorPresetRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("IndicatorPresetQueryService")
class IndicatorPresetQueryServiceTest {

    @Mock IndicatorPresetRepository indicatorPresetRepository;
    @InjectMocks IndicatorPresetQueryService service;

    private static final Long MEMBER_ID = 1L;

    @Nested
    @DisplayName("findAllByMemberId")
    class FindAllByMemberId {

        @Test
        @DisplayName("회원의 프리셋 목록을 노출 순서대로 반환한다")
        void shouldReturnPresetsOrderedByDisplayOrder() {
            IndicatorPreset p1 = IndicatorPreset.create(MEMBER_ID, "A", List.of(), List.of());
            IndicatorPreset p2 = IndicatorPreset.create(MEMBER_ID, "B", List.of(), List.of());
            given(indicatorPresetRepository.findAllByMemberIdOrderByDisplayOrderAscCreatedAtAsc(MEMBER_ID))
                    .willReturn(List.of(p1, p2));

            List<IndicatorPreset> result = service.findAllByMemberId(MEMBER_ID);

            assertThat(result).containsExactly(p1, p2);
        }

        @Test
        @DisplayName("프리셋이 없으면 빈 리스트를 반환한다")
        void shouldReturnEmptyWhenNoPresets() {
            given(indicatorPresetRepository.findAllByMemberIdOrderByDisplayOrderAscCreatedAtAsc(MEMBER_ID))
                    .willReturn(List.of());

            assertThat(service.findAllByMemberId(MEMBER_ID)).isEmpty();
        }
    }
}
