package com.dove.screening.application.service;

import com.dove.screening.domain.entity.IndicatorPreset;
import com.dove.screening.domain.repository.IndicatorPresetRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class IndicatorPresetQueryServiceTest {

    @Mock IndicatorPresetRepository repository;
    @InjectMocks IndicatorPresetQueryService service;

    private static final Long MEMBER_ID = 1L;

    @Test
    @DisplayName("findAllByMemberId — repository 위임 결과 반환")
    void shouldReturnAllPresetsForMember() {
        IndicatorPreset preset = IndicatorPreset.create(MEMBER_ID, "프리셋", "[]", null);
        given(repository.findAllByMemberIdOrderByDisplayOrderAscCreatedAtAsc(MEMBER_ID))
                .willReturn(List.of(preset));

        assertThat(service.findAllByMemberId(MEMBER_ID)).containsExactly(preset);
    }

    @Test
    @DisplayName("findAllByMemberId — 결과 없으면 빈 리스트")
    void shouldReturnEmptyWhenNoPresets() {
        given(repository.findAllByMemberIdOrderByDisplayOrderAscCreatedAtAsc(MEMBER_ID))
                .willReturn(List.of());

        assertThat(service.findAllByMemberId(MEMBER_ID)).isEmpty();
    }
}
