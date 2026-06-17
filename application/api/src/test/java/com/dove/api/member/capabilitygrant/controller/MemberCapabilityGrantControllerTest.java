package com.dove.api.member.capabilitygrant.controller;

import com.dove.api.TestApiApplication;
import com.dove.api.support.WithApiUser;
import com.dove.auth.domain.enums.MemberRole;
import com.dove.user.application.service.MemberProfileCommandService;
import com.dove.user.domain.entity.MemberProfile;
import com.dove.userfeature.application.service.MemberCapabilityGrantCommandService;
import com.dove.userfeature.application.service.MemberCapabilityGrantQueryService;
import com.dove.userfeature.domain.capability.Capability;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = TestApiApplication.class)
@AutoConfigureMockMvc
@Transactional
class MemberCapabilityGrantControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired MemberProfileCommandService memberProfileCommandService;
    @Autowired MemberCapabilityGrantCommandService grantCommandService;
    @Autowired MemberCapabilityGrantQueryService grantQueryService;

    private Long targetUserId;

    @BeforeEach
    void setUp() {
        MemberProfile user = memberProfileCommandService.save(
                MemberProfile.create("user@test.com", "일반유저", MemberRole.USER));
        targetUserId = user.getId();
    }

    @Nested
    @DisplayName("GET /admin/users/{id}/capabilities")
    class GetCapabilities {

        @Test
        @DisplayName("인증 없으면 401")
        void shouldReturn401WhenUnauthenticated() throws Exception {
            mockMvc.perform(get("/admin/users/" + targetUserId + "/capabilities"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithApiUser(role = "USER")
        @DisplayName("USER 권한이면 403")
        void shouldReturn403WhenUser() throws Exception {
            mockMvc.perform(get("/admin/users/" + targetUserId + "/capabilities"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithApiUser(role = "ADMIN")
        @DisplayName("부여 전 빈 목록")
        void shouldReturnEmptyWhenNoneGranted() throws Exception {
            mockMvc.perform(get("/admin/users/" + targetUserId + "/capabilities"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").isEmpty());
        }

        @Test
        @WithApiUser(role = "ADMIN")
        @DisplayName("부여된 capability 반환")
        void shouldReturnGrantedCapabilities() throws Exception {
            grantCommandService.grant(targetUserId, Capability.STOCK_VIEW, 1L);

            mockMvc.perform(get("/admin/users/" + targetUserId + "/capabilities"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0]").value("STOCK_VIEW"));
        }
    }

    @Nested
    @DisplayName("PATCH /admin/users/{id}/capabilities")
    class UpdateCapability {

        @Test
        @WithApiUser(role = "USER")
        @DisplayName("USER 권한이면 403")
        void shouldReturn403WhenUser() throws Exception {
            mockMvc.perform(patch("/admin/users/" + targetUserId + "/capabilities")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"capability":"STOCK_VIEW","action":"GRANT"}
                                    """))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithApiUser(role = "ADMIN")
        @DisplayName("ADMIN이 부여하면 204 + 보유 상태")
        void shouldGrantWhenAdmin() throws Exception {
            mockMvc.perform(patch("/admin/users/" + targetUserId + "/capabilities")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"capability":"STOCK_SEARCH","action":"GRANT"}
                                    """))
                    .andExpect(status().isNoContent());

            assertThat(grantQueryService.hasGrant(targetUserId, Capability.STOCK_SEARCH)).isTrue();
        }

        @Test
        @WithApiUser(role = "ADMIN")
        @DisplayName("ADMIN이 회수하면 204 + 미보유 상태")
        void shouldRevokeWhenAdmin() throws Exception {
            grantCommandService.grant(targetUserId, Capability.STOCK_SEARCH, 1L);

            mockMvc.perform(patch("/admin/users/" + targetUserId + "/capabilities")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"capability":"STOCK_SEARCH","action":"REVOKE"}
                                    """))
                    .andExpect(status().isNoContent());

            assertThat(grantQueryService.hasGrant(targetUserId, Capability.STOCK_SEARCH)).isFalse();
        }

        @Test
        @WithApiUser(role = "ADMIN")
        @DisplayName("capability 누락 시 400")
        void shouldReturn400WhenCapabilityMissing() throws Exception {
            mockMvc.perform(patch("/admin/users/" + targetUserId + "/capabilities")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"action":"GRANT"}
                                    """))
                    .andExpect(status().isBadRequest());
        }
    }
}
