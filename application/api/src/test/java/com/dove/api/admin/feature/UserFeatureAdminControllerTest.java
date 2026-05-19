package com.dove.api.admin.feature;

import com.dove.api.TestApiApplication;
import com.dove.security.JwtProvider;
import com.dove.user.application.service.MemberProfileCommandService;
import com.dove.user.domain.entity.MemberProfile;
import com.dove.user.domain.entity.MemberRole;
import com.dove.userfeature.application.service.UserFeatureGrantCommandService;
import com.dove.userfeature.domain.enums.FeatureCode;
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
class UserFeatureAdminControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired MemberProfileCommandService memberProfileCommandService;
    @Autowired UserFeatureGrantCommandService grantCommandService;
    @Autowired JwtProvider jwtProvider;

    private String adminToken;
    private String userToken;
    private Long targetUserId;

    @BeforeEach
    void setUp() {
        MemberProfile admin = memberProfileCommandService.save(
                MemberProfile.create("admin@test.com", "어드민", MemberRole.ADMIN));
        adminToken = jwtProvider.generate(admin.getId(), "adminuser", admin.getName(), admin.getRole().name(), false, false);

        MemberProfile user = memberProfileCommandService.save(
                MemberProfile.create("target@test.com", "대상유저", MemberRole.USER));
        targetUserId = user.getId();
        userToken = jwtProvider.generate(user.getId(), "targetuser", user.getName(), user.getRole().name(), false, false);
    }

    // ─── 권한 제어 ───────────────────────────────────────────────────────

    @Test
    @DisplayName("PATCH /api/admin/users/{id}/features — USER 토큰으로 접근 시 403")
    void shouldReturn403WhenUserAccessesAdminEndpoint() throws Exception {
        String body = "{\"featureCode\":\"STOCK_SEARCH\",\"action\":\"GRANT\"}";

        mockMvc.perform(patch("/admin/users/" + targetUserId + "/features")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PATCH /api/admin/users/{id}/features — 인증 없으면 401")
    void shouldReturn401WhenUnauthenticated() throws Exception {
        String body = "{\"featureCode\":\"STOCK_SEARCH\",\"action\":\"GRANT\"}";

        mockMvc.perform(patch("/admin/users/" + targetUserId + "/features")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    // ─── 유효성 검사 ─────────────────────────────────────────────────────

    @Test
    @DisplayName("PATCH /api/admin/users/{id}/features — featureCode 누락 시 400")
    void shouldReturn400WhenFeatureCodeMissing() throws Exception {
        String body = "{\"action\":\"GRANT\"}";

        mockMvc.perform(patch("/admin/users/" + targetUserId + "/features")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PATCH /api/admin/users/{id}/features — action 누락 시 400")
    void shouldReturn400WhenActionMissing() throws Exception {
        String body = "{\"featureCode\":\"STOCK_SEARCH\"}";

        mockMvc.perform(patch("/admin/users/" + targetUserId + "/features")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    // ─── 기능 부여 ───────────────────────────────────────────────────────

    @Test
    @DisplayName("PATCH /api/admin/users/{id}/features — GRANT → 204")
    void shouldGrantFeature() throws Exception {
        String body = "{\"featureCode\":\"STOCK_SEARCH\",\"action\":\"GRANT\"}";

        mockMvc.perform(patch("/admin/users/" + targetUserId + "/features")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("PATCH /api/admin/users/{id}/features — 중복 GRANT → 204 (멱등)")
    void shouldBeIdempotentForDuplicateGrant() throws Exception {
        grantCommandService.grant(targetUserId, FeatureCode.STOCK_SEARCH, null);

        String body = "{\"featureCode\":\"STOCK_SEARCH\",\"action\":\"GRANT\"}";

        mockMvc.perform(patch("/admin/users/" + targetUserId + "/features")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNoContent());
    }

    // ─── 기능 회수 ───────────────────────────────────────────────────────

    @Test
    @DisplayName("PATCH /api/admin/users/{id}/features — REVOKE → 204")
    void shouldRevokeFeature() throws Exception {
        grantCommandService.grant(targetUserId, FeatureCode.STOCK_SEARCH, null);

        String body = "{\"featureCode\":\"STOCK_SEARCH\",\"action\":\"REVOKE\"}";

        mockMvc.perform(patch("/admin/users/" + targetUserId + "/features")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNoContent());
    }

    // ─── 메뉴 조회 ───────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/admin/users/{id}/menu — 부여 전 빈 메뉴")
    void shouldReturnEmptyMenuForUserWithNoGrants() throws Exception {
        mockMvc.perform(get("/admin/users/" + targetUserId + "/menu")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modules").isEmpty());
    }

    @Test
    @DisplayName("GET /api/admin/users/{id}/menu — 부여 후 메뉴 포함")
    void shouldReturnMenuAfterGrant() throws Exception {
        grantCommandService.grant(targetUserId, FeatureCode.STOCK_SEARCH, null);

        mockMvc.perform(get("/admin/users/" + targetUserId + "/menu")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modules[0].moduleCode").value("STOCK"))
                .andExpect(jsonPath("$.modules[0].features[0].featureCode").value("STOCK_SEARCH"));
    }

    @Test
    @DisplayName("GET /api/admin/users/{id}/menu — USER 토큰으로 접근 시 403")
    void shouldReturn403WhenUserAccessesAdminMenuEndpoint() throws Exception {
        mockMvc.perform(get("/admin/users/" + targetUserId + "/menu")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }
}
