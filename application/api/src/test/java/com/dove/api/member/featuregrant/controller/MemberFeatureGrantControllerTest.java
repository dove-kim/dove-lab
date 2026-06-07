package com.dove.api.member.featuregrant.controller;

import com.dove.api.TestApiApplication;
import com.dove.api.support.WithApiUser;
import com.dove.user.application.service.MemberProfileCommandService;
import com.dove.user.domain.entity.MemberProfile;
import com.dove.auth.domain.enums.MemberRole;
import com.dove.userfeature.application.service.MemberFeatureGrantCommandService;
import com.dove.userfeature.domain.enums.FeatureCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
class MemberFeatureGrantControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired MemberProfileCommandService memberProfileCommandService;
    @Autowired MemberFeatureGrantCommandService grantCommandService;

    private Long targetMemberId;

    @BeforeEach
    void setUp() {
        MemberProfile member = memberProfileCommandService.save(
                MemberProfile.create("target@test.com", "대상유저", MemberRole.USER));
        targetMemberId = member.getId();
    }

    @Nested
    @DisplayName("PATCH /admin/users/{id}/features")
    class UpdateFeature {

        @Test
        @WithApiUser(role = "USER")
        @DisplayName("USER 권한이면 403")
        void shouldReturn403WhenUserAccessesAdminEndpoint() throws Exception {
            String body = "{\"featureCode\":\"STOCK_SEARCH\",\"action\":\"GRANT\"}";

            mockMvc.perform(patch("/admin/users/" + targetMemberId + "/features")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("인증 없으면 401")
        void shouldReturn401WhenUnauthenticated() throws Exception {
            String body = "{\"featureCode\":\"STOCK_SEARCH\",\"action\":\"GRANT\"}";

            mockMvc.perform(patch("/admin/users/" + targetMemberId + "/features")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithApiUser(role = "ADMIN")
        @DisplayName("featureCode 누락 시 400")
        void shouldReturn400WhenFeatureCodeMissing() throws Exception {
            String body = "{\"action\":\"GRANT\"}";

            mockMvc.perform(patch("/admin/users/" + targetMemberId + "/features")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithApiUser(role = "ADMIN")
        @DisplayName("action 누락 시 400")
        void shouldReturn400WhenActionMissing() throws Exception {
            String body = "{\"featureCode\":\"STOCK_SEARCH\"}";

            mockMvc.perform(patch("/admin/users/" + targetMemberId + "/features")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithApiUser(role = "ADMIN")
        @DisplayName("GRANT → 204")
        void shouldGrantFeature() throws Exception {
            String body = "{\"featureCode\":\"STOCK_SEARCH\",\"action\":\"GRANT\"}";

            mockMvc.perform(patch("/admin/users/" + targetMemberId + "/features")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isNoContent());
        }

        @Test
        @WithApiUser(role = "ADMIN")
        @DisplayName("중복 GRANT → 204 (멱등)")
        void shouldBeIdempotentForDuplicateGrant() throws Exception {
            grantCommandService.grant(targetMemberId, FeatureCode.STOCK_SEARCH, null);

            String body = "{\"featureCode\":\"STOCK_SEARCH\",\"action\":\"GRANT\"}";

            mockMvc.perform(patch("/admin/users/" + targetMemberId + "/features")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isNoContent());
        }

        @Test
        @WithApiUser(role = "ADMIN")
        @DisplayName("REVOKE → 204")
        void shouldRevokeFeature() throws Exception {
            grantCommandService.grant(targetMemberId, FeatureCode.STOCK_SEARCH, null);

            String body = "{\"featureCode\":\"STOCK_SEARCH\",\"action\":\"REVOKE\"}";

            mockMvc.perform(patch("/admin/users/" + targetMemberId + "/features")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isNoContent());
        }
    }

    @Nested
    @DisplayName("GET /admin/users/{id}/menu")
    class GetMemberMenu {

        @Test
        @WithApiUser(role = "ADMIN")
        @DisplayName("부여 전 빈 메뉴")
        void shouldReturnEmptyMenuForUserWithNoGrants() throws Exception {
            mockMvc.perform(get("/admin/users/" + targetMemberId + "/menu"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.modules").isEmpty());
        }

        @Test
        @WithApiUser(role = "ADMIN")
        @DisplayName("부여 후 메뉴 포함")
        void shouldReturnMenuAfterGrant() throws Exception {
            grantCommandService.grant(targetMemberId, FeatureCode.STOCK_SEARCH, null);

            mockMvc.perform(get("/admin/users/" + targetMemberId + "/menu"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.modules[0].moduleCode").value("STOCK"))
                    .andExpect(jsonPath("$.modules[0].features[0].featureCode").value("STOCK_SEARCH"));
        }

        @Test
        @WithApiUser(role = "USER")
        @DisplayName("USER 권한이면 403")
        void shouldReturn403WhenUserAccessesAdminMenuEndpoint() throws Exception {
            mockMvc.perform(get("/admin/users/" + targetMemberId + "/menu"))
                    .andExpect(status().isForbidden());
        }
    }
}
