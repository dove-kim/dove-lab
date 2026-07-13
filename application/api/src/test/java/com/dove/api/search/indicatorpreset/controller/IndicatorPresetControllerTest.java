package com.dove.api.search.indicatorpreset.controller;

import com.dove.api.TestApiApplication;
import com.dove.api.support.WithApiUser;
import com.dove.screening.application.service.IndicatorPresetCommandService;
import com.dove.screening.domain.value.IndicatorPresetItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = TestApiApplication.class)
@AutoConfigureMockMvc
@Transactional
class IndicatorPresetControllerTest {

    private static final long MEMBER_ID = 1L;

    @Autowired MockMvc mockMvc;
    @Autowired IndicatorPresetCommandService indicatorPresetCommandService;

    private IndicatorPresetItem item() {
        return new IndicatorPresetItem("SMA", true, "#ff0000", 1.0);
    }

    @Nested
    @DisplayName("GET /indicator-presets")
    class ListPresets {

        @Test
        @DisplayName("인증 없으면 401")
        void shouldReturn401WhenUnauthenticated() throws Exception {
            mockMvc.perform(get("/indicator-presets"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithApiUser()
        @DisplayName("STOCK_SEARCH 기능 없으면 403")
        void shouldReturn403WhenMissingFeature() throws Exception {
            mockMvc.perform(get("/indicator-presets"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithApiUser(memberId = MEMBER_ID, capabilities = {"STOCK_VIEW"})
        @DisplayName("생성 전 빈 목록")
        void shouldReturnEmptyListWhenNoPresets() throws Exception {
            mockMvc.perform(get("/indicator-presets"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").isEmpty());
        }
    }

    @Nested
    @DisplayName("POST /indicator-presets")
    class CreatePreset {

        @Test
        @WithApiUser(memberId = MEMBER_ID, capabilities = {"STOCK_VIEW"})
        @DisplayName("201 생성")
        void shouldCreatePresetWhenValid() throws Exception {
            mockMvc.perform(post("/indicator-presets")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name":"내프리셋","items":[],"panelOrder":[]}
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").value("내프리셋"));
        }

        @Test
        @WithApiUser(memberId = MEMBER_ID, capabilities = {"STOCK_VIEW"})
        @DisplayName("오버레이를 포함해 생성하면 응답에 오버레이가 담긴다")
        void shouldCreatePresetWithOverlay() throws Exception {
            mockMvc.perform(post("/indicator-presets")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name":"오버레이프리셋","items":[],"panelOrder":[],
                                     "overlay":{"signalModelId":7,"signalThreshold":0.6,"seriesMetricIds":[1,2]}}
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.overlay.signalModelId").value(7))
                    .andExpect(jsonPath("$.overlay.signalThreshold").value(0.6))
                    .andExpect(jsonPath("$.overlay.seriesMetricIds[0]").value(1))
                    .andExpect(jsonPath("$.overlay.seriesMetricIds[1]").value(2));
        }

        @Test
        @WithApiUser(memberId = MEMBER_ID, capabilities = {"STOCK_VIEW"})
        @DisplayName("오버레이 없이 생성하면 응답 오버레이는 null")
        void shouldCreatePresetWithNullOverlay() throws Exception {
            mockMvc.perform(post("/indicator-presets")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name":"노오버레이","items":[],"panelOrder":[]}
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.overlay").isEmpty());
        }

        @Test
        @WithApiUser(memberId = MEMBER_ID, capabilities = {"STOCK_VIEW"})
        @DisplayName("name 누락 시 400")
        void shouldReturn400WhenNameMissing() throws Exception {
            mockMvc.perform(post("/indicator-presets")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"items":[],"panelOrder":[]}
                                    """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithApiUser(memberId = MEMBER_ID, capabilities = {"STOCK_VIEW"})
        @DisplayName("이름 중복 시 409")
        void shouldReturn409WhenNameDuplicate() throws Exception {
            indicatorPresetCommandService.create(MEMBER_ID, "중복프리셋", List.of(item()), List.of());

            mockMvc.perform(post("/indicator-presets")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name":"중복프리셋","items":[],"panelOrder":[]}
                                    """))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("PUT /indicator-presets/{id}")
    class UpdatePreset {

        @Test
        @WithApiUser(memberId = MEMBER_ID, capabilities = {"STOCK_VIEW"})
        @DisplayName("수정 성공")
        void shouldUpdatePresetWhenValid() throws Exception {
            var preset = indicatorPresetCommandService.create(MEMBER_ID, "원래프리셋", List.of(item()), List.of());

            mockMvc.perform(put("/indicator-presets/" + preset.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name":"바뀐프리셋","items":[],"panelOrder":[]}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("바뀐프리셋"));
        }

        @Test
        @WithApiUser(memberId = MEMBER_ID, capabilities = {"STOCK_VIEW"})
        @DisplayName("없는 id면 404")
        void shouldReturn404WhenUpdatingNonExistent() throws Exception {
            mockMvc.perform(put("/indicator-presets/99999")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name":"없는프리셋","items":[],"panelOrder":[]}
                                    """))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PATCH /indicator-presets/reorder")
    class ReorderPresets {

        @Test
        @WithApiUser(memberId = MEMBER_ID, capabilities = {"STOCK_VIEW"})
        @DisplayName("순서 변경 204")
        void shouldReorderPresetsWhenValid() throws Exception {
            var a = indicatorPresetCommandService.create(MEMBER_ID, "프리셋A", List.of(item()), List.of());
            var b = indicatorPresetCommandService.create(MEMBER_ID, "프리셋B", List.of(item()), List.of());

            mockMvc.perform(patch("/indicator-presets/reorder")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"ids":[%d,%d]}
                                    """.formatted(b.getId(), a.getId())))
                    .andExpect(status().isNoContent());
        }
    }

    @Nested
    @DisplayName("DELETE /indicator-presets/{id}")
    class DeletePreset {

        @Test
        @WithApiUser(memberId = MEMBER_ID, capabilities = {"STOCK_VIEW"})
        @DisplayName("삭제 204")
        void shouldDeletePresetWhenExists() throws Exception {
            var preset = indicatorPresetCommandService.create(MEMBER_ID, "삭제프리셋", List.of(item()), List.of());

            mockMvc.perform(delete("/indicator-presets/" + preset.getId()))
                    .andExpect(status().isNoContent());
        }

        @Test
        @WithApiUser(memberId = MEMBER_ID, capabilities = {"STOCK_VIEW"})
        @DisplayName("없는 id면 404")
        void shouldReturn404WhenDeletingNonExistent() throws Exception {
            mockMvc.perform(delete("/indicator-presets/99999"))
                    .andExpect(status().isNotFound());
        }
    }
}
