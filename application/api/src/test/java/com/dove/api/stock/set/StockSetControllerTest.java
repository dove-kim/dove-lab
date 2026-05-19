package com.dove.api.stock.set;

import com.dove.api.TestApiApplication;
import com.dove.security.JwtProvider;
import com.dove.screening.application.service.StockSetCommandService;
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

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = TestApiApplication.class)
@AutoConfigureMockMvc
@Transactional
class StockSetControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired MemberProfileCommandService memberProfileCommandService;
    @Autowired StockSetCommandService stockSetCommandService;
    @Autowired JwtProvider jwtProvider;

    private String token;
    private Long memberId;

    @BeforeEach
    void setUp() {
        MemberProfile user = memberProfileCommandService.save(
                MemberProfile.create("stockset@test.com", "세트유저", MemberRole.USER));
        memberId = user.getId();
        token = jwtProvider.generate(memberId, "setuser", user.getName(), user.getRole().name(), false, false);
    }

    // ─── 인증 ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/stock-sets — 인증 없으면 401")
    void shouldReturn401WhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/stock-sets"))
                .andExpect(status().isUnauthorized());
    }

    // ─── 목록 ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/stock-sets — 빈 목록 반환")
    void shouldReturnEmptyList() throws Exception {
        mockMvc.perform(get("/stock-sets")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("GET /api/stock-sets — 생성한 세트 목록에 포함")
    void shouldReturnCreatedStockSet() throws Exception {
        stockSetCommandService.create(memberId, "관심종목", List.of("005930", "000660"));

        mockMvc.perform(get("/stock-sets")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("관심종목"))
                .andExpect(jsonPath("$[0].codes").isArray());
    }

    // ─── 단건 조회 ───────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/stock-sets/{id} — 세트 반환")
    void shouldReturnStockSetById() throws Exception {
        var set = stockSetCommandService.create(memberId, "단건조회", List.of("005930"));

        mockMvc.perform(get("/stock-sets/" + set.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("단건조회"))
                .andExpect(jsonPath("$.codes[0]").value("005930"));
    }

    // ─── 생성 ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/stock-sets — 세트 생성 201")
    void shouldCreateStockSet() throws Exception {
        String body = """
                {"name":"신규세트","codes":["005930","000660"]}
                """;

        mockMvc.perform(post("/stock-sets")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("신규세트"))
                .andExpect(jsonPath("$.codes.length()").value(2));
    }

    @Test
    @DisplayName("POST /api/stock-sets — name 누락 시 400")
    void shouldReturn400WhenNameMissing() throws Exception {
        String body = """
                {"codes":["005930"]}
                """;

        mockMvc.perform(post("/stock-sets")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/stock-sets — 이름 중복 시 409")
    void shouldReturn409WhenNameDuplicate() throws Exception {
        stockSetCommandService.create(memberId, "중복세트", List.of("005930"));

        String body = """
                {"name":"중복세트","codes":["000660"]}
                """;

        mockMvc.perform(post("/stock-sets")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());
    }

    // ─── 단건 조회 에러 ──────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/stock-sets/{id} — 없는 id면 404")
    void shouldReturn404WhenStockSetNotFound() throws Exception {
        mockMvc.perform(get("/stock-sets/99999")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    // ─── 수정 ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("PUT /api/stock-sets/{id} — 세트 수정")
    void shouldUpdateStockSet() throws Exception {
        var set = stockSetCommandService.create(memberId, "원래이름", List.of("005930"));

        String body = """
                {"name":"바뀐이름","codes":["000660","035420"]}
                """;

        mockMvc.perform(put("/stock-sets/" + set.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("바뀐이름"))
                .andExpect(jsonPath("$.codes.length()").value(2));
    }

    @Test
    @DisplayName("PUT /api/stock-sets/{id} — 없는 id면 404")
    void shouldReturn404WhenUpdatingNonExistentStockSet() throws Exception {
        String body = """
                {"name":"없는세트","codes":["005930"]}
                """;

        mockMvc.perform(put("/stock-sets/99999")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }

    // ─── 삭제 ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("DELETE /api/stock-sets/{id} — 세트 삭제 204")
    void shouldDeleteStockSet() throws Exception {
        var set = stockSetCommandService.create(memberId, "삭제세트", List.of("005930"));

        mockMvc.perform(delete("/stock-sets/" + set.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /api/stock-sets/{id} — 없는 id면 404")
    void shouldReturn404WhenDeletingNonExistentStockSet() throws Exception {
        mockMvc.perform(delete("/stock-sets/99999")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

}

