package com.dove.api.stock.indicator;

import com.dove.api.TestApiApplication;
import com.dove.security.JwtProvider;
import com.dove.screening.application.service.IndicatorPresetCommandService;
import com.dove.user.application.service.MemberProfileCommandService;
import com.dove.user.domain.entity.MemberProfile;
import com.dove.user.domain.entity.MemberRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = TestApiApplication.class)
@AutoConfigureMockMvc
@Transactional
class IndicatorPresetControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired MemberProfileCommandService memberProfileCommandService;
    @Autowired IndicatorPresetCommandService presetCommandService;
    @Autowired JwtProvider jwtProvider;

    private String token;
    private Long memberId;

    private static final String ITEMS_JSON = "[{\"type\":\"SMA_5\",\"enabled\":true}]";

    @BeforeEach
    void setUp() {
        MemberProfile user = memberProfileCommandService.save(
                MemberProfile.create("preset@test.com", "프리셋유저", MemberRole.USER));
        memberId = user.getId();
        token = jwtProvider.generate(memberId, "presetuser", user.getName(), user.getRole().name(), false, false);
    }

    // ─── 인증 ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/indicator-presets — 인증 없으면 401")
    void shouldReturn401WhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/indicator-presets"))
                .andExpect(status().isUnauthorized());
    }

    // ─── 목록 ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/indicator-presets — 빈 목록 반환")
    void shouldReturnEmptyList() throws Exception {
        mockMvc.perform(get("/indicator-presets")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("GET /api/indicator-presets — 생성한 프리셋 목록에 포함")
    void shouldReturnCreatedPreset() throws Exception {
        presetCommandService.create(memberId, "기본프리셋", ITEMS_JSON, null);

        mockMvc.perform(get("/indicator-presets")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("기본프리셋"));
    }

    // ─── 생성 ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/indicator-presets — 프리셋 생성 201")
    void shouldCreatePreset() throws Exception {
        String body = """
                {"name":"신규프리셋","items":%s,"panelOrder":["volume","rsi"]}
                """.formatted(ITEMS_JSON);

        mockMvc.perform(post("/indicator-presets")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("신규프리셋"))
                .andExpect(jsonPath("$.panelOrder[0]").value("volume"));
    }

    @Test
    @DisplayName("POST /api/indicator-presets — name 누락 시 400")
    void shouldReturn400WhenNameMissing() throws Exception {
        String body = """
                {"items":%s}
                """.formatted(ITEMS_JSON);

        mockMvc.perform(post("/indicator-presets")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/indicator-presets — 이름 중복 시 409")
    void shouldReturn409WhenNameDuplicate() throws Exception {
        presetCommandService.create(memberId, "중복프리셋", ITEMS_JSON, null);

        String body = """
                {"name":"중복프리셋","items":%s}
                """.formatted(ITEMS_JSON);

        mockMvc.perform(post("/indicator-presets")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());
    }

    // ─── 수정 ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("PUT /api/indicator-presets/{id} — 없는 id면 404")
    void shouldReturn404WhenUpdatingNonExistentPreset() throws Exception {
        String body = """
                {"name":"없는프리셋","items":%s}
                """.formatted(ITEMS_JSON);

        mockMvc.perform(put("/indicator-presets/99999")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /api/indicator-presets/{id} — name 누락 시 400")
    void shouldReturn400WhenNameMissingOnUpdate() throws Exception {
        var preset = presetCommandService.create(memberId, "기존프리셋", ITEMS_JSON, null);

        String body = """
                {"items":%s}
                """.formatted(ITEMS_JSON);

        mockMvc.perform(put("/indicator-presets/" + preset.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /api/indicator-presets/{id} — 프리셋 수정")
    void shouldUpdatePreset() throws Exception {
        var preset = presetCommandService.create(memberId, "원래이름", ITEMS_JSON, null);

        String updatedItems = "[{\"type\":\"SMA_20\",\"enabled\":true}]";
        String body = """
                {"name":"바뀐이름","items":%s,"panelOrder":["macd"]}
                """.formatted(updatedItems);

        mockMvc.perform(put("/indicator-presets/" + preset.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("바뀐이름"))
                .andExpect(jsonPath("$.panelOrder[0]").value("macd"));
    }

    // ─── 순서 변경 ───────────────────────────────────────────────────────

    @Test
    @DisplayName("PATCH /api/indicator-presets/reorder — 204 반환")
    void shouldReorderPresets() throws Exception {
        var p1 = presetCommandService.create(memberId, "첫번째", ITEMS_JSON, null);
        var p2 = presetCommandService.create(memberId, "두번째", ITEMS_JSON, null);

        String body = "{\"ids\":[" + p2.getId() + "," + p1.getId() + "]}";

        mockMvc.perform(patch("/indicator-presets/reorder")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNoContent());
    }

    // ─── 삭제 ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("DELETE /api/indicator-presets/{id} — 없는 id면 404")
    void shouldReturn404WhenDeletingNonExistentPreset() throws Exception {
        mockMvc.perform(delete("/indicator-presets/99999")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/indicator-presets/{id} — 프리셋 삭제 204")
    void shouldDeletePreset() throws Exception {
        var preset = presetCommandService.create(memberId, "삭제프리셋", ITEMS_JSON, null);

        mockMvc.perform(delete("/indicator-presets/" + preset.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

}

