package com.dove.api.stock.filter;

import com.dove.api.TestApiApplication;
import com.dove.security.JwtProvider;
import com.dove.screening.application.service.SearchFilterCommandService;
import com.dove.screening.domain.enums.DateRule;
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
class SearchFilterControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired MemberProfileCommandService memberProfileCommandService;
    @Autowired SearchFilterCommandService searchFilterCommandService;
    @Autowired JwtProvider jwtProvider;

    private String token;
    private Long memberId;

    private static final String SIMPLE_EXPR =
            "{\"nodeType\":\"CONDITION\",\"conditionType\":\"PRICE_VALUE\",\"priceField\":\"CLOSE\",\"operator\":\"GT\",\"value\":0}";

    @BeforeEach
    void setUp() {
        MemberProfile user = memberProfileCommandService.save(
                MemberProfile.create("filter@test.com", "필터유저", MemberRole.USER));
        memberId = user.getId();
        token = jwtProvider.generate(memberId, "filteruser", user.getName(), user.getRole().name(), false, false);
    }

    // ─── 인증 ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/filters — 인증 없으면 401")
    void shouldReturn401WhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/filters"))
                .andExpect(status().isUnauthorized());
    }

    // ─── 목록 ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/filters — 빈 목록 반환")
    void shouldReturnEmptyList() throws Exception {
        mockMvc.perform(get("/filters")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("GET /api/filters — 생성한 필터 목록에 포함")
    void shouldReturnCreatedFilter() throws Exception {
        searchFilterCommandService.create(memberId, "내필터", DateRule.LATEST,
                List.of(com.dove.market.domain.enums.MarketType.KOSPI), SIMPLE_EXPR, null, null);

        mockMvc.perform(get("/filters")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("내필터"))
                .andExpect(jsonPath("$[0].dateRule").value("LATEST"));
    }

    // ─── 생성 ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/filters — 필터 생성 201")
    void shouldCreateFilter() throws Exception {
        String body = """
                {"name":"신규필터","dateRule":"LATEST","markets":["KOSPI"],
                 "expression":%s}
                """.formatted(quoted(SIMPLE_EXPR));

        mockMvc.perform(post("/filters")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("신규필터"))
                .andExpect(jsonPath("$.markets[0]").value("KOSPI"));
    }

    @Test
    @DisplayName("POST /api/filters — name 누락 시 400")
    void shouldReturn400WhenNameMissing() throws Exception {
        String body = """
                {"dateRule":"LATEST","markets":["KOSPI"],"expression":%s}
                """.formatted(quoted(SIMPLE_EXPR));

        mockMvc.perform(post("/filters")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/filters — 이름 중복 시 409")
    void shouldReturn409WhenNameDuplicate() throws Exception {
        searchFilterCommandService.create(memberId, "중복필터", DateRule.LATEST,
                List.of(com.dove.market.domain.enums.MarketType.KOSPI), SIMPLE_EXPR, null, null);

        String body = """
                {"name":"중복필터","dateRule":"LATEST","markets":["KOSPI"],
                 "expression":%s}
                """.formatted(quoted(SIMPLE_EXPR));

        mockMvc.perform(post("/filters")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());
    }

    // ─── 수정 ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("PUT /api/filters/{id} — 없는 id면 404")
    void shouldReturn404WhenUpdatingNonExistentFilter() throws Exception {
        String body = """
                {"name":"없는필터","dateRule":"LATEST","markets":["KOSPI"],
                 "expression":%s}
                """.formatted(quoted(SIMPLE_EXPR));

        mockMvc.perform(put("/filters/99999")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /api/filters/{id} — 필터 수정")
    void shouldUpdateFilter() throws Exception {
        var filter = searchFilterCommandService.create(memberId, "원래이름", DateRule.LATEST,
                List.of(com.dove.market.domain.enums.MarketType.KOSPI), SIMPLE_EXPR, null, null);

        String body = """
                {"name":"바뀐이름","dateRule":"PREV_1D","markets":["KOSDAQ"],
                 "expression":%s}
                """.formatted(quoted(SIMPLE_EXPR));

        mockMvc.perform(put("/filters/" + filter.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("바뀐이름"))
                .andExpect(jsonPath("$.dateRule").value("PREV_1D"));
    }

    // ─── 삭제 ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("DELETE /api/filters/{id} — 없는 id면 404")
    void shouldReturn404WhenDeletingNonExistentFilter() throws Exception {
        mockMvc.perform(delete("/filters/99999")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/filters/{id} — 필터 삭제 204")
    void shouldDeleteFilter() throws Exception {
        var filter = searchFilterCommandService.create(memberId, "삭제할필터", DateRule.LATEST,
                List.of(com.dove.market.domain.enums.MarketType.KOSPI), SIMPLE_EXPR, null, null);

        mockMvc.perform(delete("/filters/" + filter.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    // ─── 순서 변경 ───────────────────────────────────────────────────────

    @Test
    @DisplayName("PATCH /api/filters/reorder — 204 반환")
    void shouldReorderFilters() throws Exception {
        var f1 = searchFilterCommandService.create(memberId, "첫번째", DateRule.LATEST,
                List.of(com.dove.market.domain.enums.MarketType.KOSPI), SIMPLE_EXPR, null, null);
        var f2 = searchFilterCommandService.create(memberId, "두번째", DateRule.LATEST,
                List.of(com.dove.market.domain.enums.MarketType.KOSPI), SIMPLE_EXPR, null, null);

        String body = "{\"ids\":[" + f2.getId() + "," + f1.getId() + "]}";

        mockMvc.perform(patch("/filters/reorder")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNoContent());
    }

    // ─── 실행 ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/filters/{id}/execute — 인증 없으면 401")
    void shouldReturn401WhenExecuteUnauthenticated() throws Exception {
        mockMvc.perform(post("/filters/1/execute"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/filters/{id}/execute — 없는 id면 404")
    void shouldReturn404WhenExecutingNonExistentFilter() throws Exception {
        mockMvc.perform(post("/filters/99999/execute")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/filters/{id}/execute — 결과 반환 (빈 종목 → matchCount=0)")
    void shouldExecuteFilter() throws Exception {
        // SPECIFIC_DATE: referenceDate=null 이면 LocalDate.now()를 기준일로 사용 → 빈 결과 반환
        var filter = searchFilterCommandService.create(memberId, "실행필터", DateRule.SPECIFIC_DATE,
                List.of(com.dove.market.domain.enums.MarketType.KOSPI), SIMPLE_EXPR, null, null);

        mockMvc.perform(post("/filters/" + filter.getId() + "/execute")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchCount").value(0));
    }

    // ─── 헬퍼 ────────────────────────────────────────────────────────────

    /** JSON 문자열을 JSON 내 문자열 값으로 이스케이프 */
    private String quoted(String json) {
        return "\"" + json.replace("\"", "\\\"") + "\"";
    }
}
