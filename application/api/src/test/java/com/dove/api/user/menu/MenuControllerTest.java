package com.dove.api.user.menu;

import com.dove.api.TestApiApplication;
import com.dove.security.JwtProvider;
import com.dove.user.application.service.MemberProfileCommandService;
import com.dove.user.domain.entity.MemberProfile;
import com.dove.user.domain.entity.MemberRole;
import com.dove.userfeature.application.service.UserFeatureGrantCommandService;
import com.dove.userfeature.application.service.UserMenuDisplayCommandService;
import com.dove.userfeature.domain.enums.FeatureCode;
import com.dove.userfeature.domain.enums.ModuleCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = TestApiApplication.class)
@AutoConfigureMockMvc
@Transactional
class MenuControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired MemberProfileCommandService memberProfileCommandService;
    @Autowired UserFeatureGrantCommandService grantCommandService;
    @Autowired UserMenuDisplayCommandService menuCommandService;
    @Autowired JwtProvider jwtProvider;

    private String token;
    private Long memberId;

    @BeforeEach
    void setUp() {
        MemberProfile user = memberProfileCommandService.save(
                MemberProfile.create("menu@test.com", "메뉴유저", MemberRole.USER));
        memberId = user.getId();
        token = jwtProvider.generate(memberId, "menuuser", user.getName(), user.getRole().name(), false, false);
    }

    // ─── 인증 ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/account/menu — 인증 없으면 401")
    void shouldReturn401WhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/account/menu"))
                .andExpect(status().isUnauthorized());
    }

    // ─── 메뉴 조회 ───────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/account/menu — 활성 기능 없으면 빈 모듈 목록")
    void shouldReturnEmptyMenuWhenNoGrants() throws Exception {
        mockMvc.perform(get("/account/menu")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modules").isArray())
                .andExpect(jsonPath("$.modules").isEmpty());
    }

    @Test
    @DisplayName("GET /api/account/menu — 부여된 기능이 메뉴에 포함")
    void shouldReturnMenuWithGrantedFeature() throws Exception {
        grantCommandService.grant(memberId, FeatureCode.STOCK_SEARCH, null);

        mockMvc.perform(get("/account/menu")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modules[0].moduleCode").value("STOCK"))
                .andExpect(jsonPath("$.modules[0].features[0].featureCode").value("STOCK_SEARCH"))
                .andExpect(jsonPath("$.modules[0].features[0].hidden").value(false));
    }

    // ─── 모듈 순서 변경 ──────────────────────────────────────────────────

    @Test
    @DisplayName("PATCH /api/account/menu/modules/reorder — 204 반환")
    void shouldReorderModules() throws Exception {
        grantCommandService.grant(memberId, FeatureCode.STOCK_SEARCH, null);
        grantCommandService.grant(memberId, FeatureCode.BUDGET, null);

        String body = "{\"modules\":[\"BUDGET\",\"STOCK\"]}";

        mockMvc.perform(patch("/account/menu/modules/reorder")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("PATCH /api/account/menu/modules/reorder — modules 누락 시 400")
    void shouldReturn400WhenModulesMissing() throws Exception {
        mockMvc.perform(patch("/account/menu/modules/reorder")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // ─── 기능 순서 변경 ──────────────────────────────────────────────────

    @Test
    @DisplayName("PATCH /api/account/menu/modules/{module}/features/reorder — 204 반환")
    void shouldReorderFeatures() throws Exception {
        grantCommandService.grant(memberId, FeatureCode.STOCK_SEARCH, null);
        grantCommandService.grant(memberId, FeatureCode.STOCK_LEDGER, null);

        String body = "{\"features\":[\"STOCK_LEDGER\",\"STOCK_SEARCH\"]}";

        mockMvc.perform(patch("/account/menu/modules/STOCK/features/reorder")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNoContent());
    }

    // ─── 기능 숨김 ───────────────────────────────────────────────────────

    @Test
    @DisplayName("PATCH /api/account/menu/features/{feature}/hidden — 404 when display not found")
    void shouldReturn404WhenFeatureDisplayNotFound() throws Exception {
        String body = "{\"hidden\":true}";

        mockMvc.perform(patch("/account/menu/features/STOCK_SEARCH/hidden")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PATCH /api/account/menu/features/{feature}/hidden — 204 when display exists")
    void shouldHideFeature() throws Exception {
        grantCommandService.grant(memberId, FeatureCode.STOCK_SEARCH, null);

        String body = "{\"hidden\":true}";

        mockMvc.perform(patch("/account/menu/features/STOCK_SEARCH/hidden")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNoContent());
    }

    // ─── 모듈 숨김 ───────────────────────────────────────────────────────

    @Test
    @DisplayName("PATCH /api/account/menu/modules/{module}/hidden — 404 when display not found")
    void shouldReturn404WhenModuleDisplayNotFound() throws Exception {
        String body = "{\"hidden\":true}";

        mockMvc.perform(patch("/account/menu/modules/STOCK/hidden")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PATCH /api/account/menu/modules/{module}/hidden — 204 when display exists")
    void shouldHideModule() throws Exception {
        grantCommandService.grant(memberId, FeatureCode.STOCK_SEARCH, null);

        String body = "{\"hidden\":true}";

        mockMvc.perform(patch("/account/menu/modules/STOCK/hidden")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNoContent());
    }
}
