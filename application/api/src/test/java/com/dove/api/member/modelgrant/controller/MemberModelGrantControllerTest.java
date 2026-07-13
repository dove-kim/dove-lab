package com.dove.api.member.modelgrant.controller;

import com.dove.api.TestApiApplication;
import com.dove.api.support.WithApiUser;
import com.dove.auth.domain.enums.MemberRole;
import com.dove.modelserving.domain.entity.MlModel;
import com.dove.modelserving.domain.enums.ModelOutputType;
import com.dove.modelserving.domain.repository.MlModelRepository;
import com.dove.stock.domain.enums.PriceType;
import com.dove.stock.domain.enums.StockExchange;
import com.dove.user.application.service.MemberProfileCommandService;
import com.dove.user.domain.entity.MemberProfile;
import com.dove.userfeature.application.service.MemberModelGrantCommandService;
import com.dove.userfeature.application.service.MemberModelGrantQueryService;
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

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = TestApiApplication.class)
@AutoConfigureMockMvc
@Transactional
class MemberModelGrantControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired MemberProfileCommandService memberProfileCommandService;
    @Autowired MlModelRepository mlModelRepository;
    @Autowired MemberModelGrantCommandService grantCommandService;
    @Autowired MemberModelGrantQueryService grantQueryService;

    private Long targetUserId;
    private Long modelId;

    @BeforeEach
    void setUp() {
        MemberProfile user = memberProfileCommandService.save(
                MemberProfile.create("user@test.com", "일반유저", MemberRole.USER));
        targetUserId = user.getId();
        MlModel model = mlModelRepository.save(MlModel.register(
                "swing_entry", "1.0.0", new byte[]{1}, "{}", ModelOutputType.PROBABILITY,
                Set.of(StockExchange.KOSPI, StockExchange.KOSDAQ), PriceType.ADJUSTED, "tester"));
        model.activate();
        modelId = mlModelRepository.save(model).getId();
    }

    @Nested
    @DisplayName("GET /admin/model-grants/models")
    class GrantableModels {

        @Test
        @DisplayName("인증 없으면 401")
        void shouldReturn401WhenUnauthenticated() throws Exception {
            mockMvc.perform(get("/admin/model-grants/models"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithApiUser(role = "USER")
        @DisplayName("USER 권한이면 403")
        void shouldReturn403WhenUser() throws Exception {
            mockMvc.perform(get("/admin/model-grants/models"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithApiUser(role = "ADMIN")
        @DisplayName("활성 모델 요약 목록에 시드 모델 포함")
        void shouldReturnActiveModels() throws Exception {
            mockMvc.perform(get("/admin/model-grants/models"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[?(@.id == " + modelId + ")]").exists())
                    .andExpect(jsonPath("$[?(@.id == " + modelId + ")].name").value("swing_entry"));
        }
    }

    @Nested
    @DisplayName("GET /admin/model-grants/users/{id}")
    class GetGrants {

        @Test
        @WithApiUser(role = "USER")
        @DisplayName("USER 권한이면 403")
        void shouldReturn403WhenUser() throws Exception {
            mockMvc.perform(get("/admin/model-grants/users/" + targetUserId))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithApiUser(role = "ADMIN")
        @DisplayName("부여 전 빈 목록")
        void shouldReturnEmptyWhenNoneGranted() throws Exception {
            mockMvc.perform(get("/admin/model-grants/users/" + targetUserId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").isEmpty());
        }

        @Test
        @WithApiUser(role = "ADMIN")
        @DisplayName("부여된 모델 ID 반환")
        void shouldReturnGrantedModelIds() throws Exception {
            grantCommandService.grant(targetUserId, modelId, 1L);

            mockMvc.perform(get("/admin/model-grants/users/" + targetUserId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0]").value(modelId));
        }
    }

    @Nested
    @DisplayName("PATCH /admin/model-grants/users/{id}")
    class UpdateGrant {

        @Test
        @WithApiUser(role = "USER")
        @DisplayName("USER 권한이면 403")
        void shouldReturn403WhenUser() throws Exception {
            mockMvc.perform(patch("/admin/model-grants/users/" + targetUserId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"modelId\":" + modelId + ",\"action\":\"GRANT\"}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithApiUser(role = "ADMIN")
        @DisplayName("ADMIN이 부여하면 204 + 보유 상태")
        void shouldGrantWhenAdmin() throws Exception {
            mockMvc.perform(patch("/admin/model-grants/users/" + targetUserId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"modelId\":" + modelId + ",\"action\":\"GRANT\"}"))
                    .andExpect(status().isNoContent());

            assertThat(grantQueryService.hasGrant(targetUserId, modelId)).isTrue();
        }

        @Test
        @WithApiUser(role = "ADMIN")
        @DisplayName("ADMIN이 회수하면 204 + 미보유 상태")
        void shouldRevokeWhenAdmin() throws Exception {
            grantCommandService.grant(targetUserId, modelId, 1L);

            mockMvc.perform(patch("/admin/model-grants/users/" + targetUserId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"modelId\":" + modelId + ",\"action\":\"REVOKE\"}"))
                    .andExpect(status().isNoContent());

            assertThat(grantQueryService.hasGrant(targetUserId, modelId)).isFalse();
        }

        @Test
        @WithApiUser(role = "ADMIN")
        @DisplayName("modelId 누락 시 400")
        void shouldReturn400WhenModelIdMissing() throws Exception {
            mockMvc.perform(patch("/admin/model-grants/users/" + targetUserId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"action\":\"GRANT\"}"))
                    .andExpect(status().isBadRequest());
        }
    }
}
