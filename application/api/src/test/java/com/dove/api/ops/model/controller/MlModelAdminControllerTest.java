package com.dove.api.ops.model.controller;

import com.dove.api.TestApiApplication;
import com.dove.api.support.WithApiUser;
import com.dove.modelserving.application.exception.ModelNotFoundException;
import com.dove.modelserving.application.service.ModelLifecycleService;
import com.dove.modelserving.application.service.ModelQueryService;
import com.dove.modelserving.application.service.ModelRegistrationService;
import com.dove.modelserving.application.service.ModelScoreCommandService;
import com.dove.modelserving.domain.entity.MlModel;
import com.dove.modelserving.domain.enums.ModelOutputType;
import com.dove.stock.domain.enums.PriceType;
import com.dove.stock.domain.enums.StockExchange;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = TestApiApplication.class)
@AutoConfigureMockMvc
@Transactional
class MlModelAdminControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ModelRegistrationService registrationService;
    @MockitoBean
    ModelLifecycleService lifecycleService;
    @MockitoBean
    ModelQueryService queryService;
    @MockitoBean
    ModelScoreCommandService scoreCommandService;

    private static MlModel stubModel() {
        return MlModel.register("swing_entry", "1.0.0", new byte[]{1, 2, 3}, "{}",
                ModelOutputType.PROBABILITY, Set.of(StockExchange.KOSPI, StockExchange.KOSDAQ),
                PriceType.ADJUSTED, "tester");
    }

    @Nested
    @DisplayName("GET /admin/ops/models")
    class ListModels {

        @Test
        @DisplayName("인증 없으면 401")
        void shouldReturn401WhenUnauthenticated() throws Exception {
            mockMvc.perform(get("/admin/ops/models"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithApiUser(role = "ADMIN")
        @DisplayName("ROOT 미만이면 403")
        void shouldReturn403WhenNotRoot() throws Exception {
            mockMvc.perform(get("/admin/ops/models"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithApiUser(role = "ROOT")
        @DisplayName("ROOT이면 모델 목록 200")
        void shouldReturnModelsWhenRoot() throws Exception {
            given(queryService.findAll()).willReturn(List.of(stubModel()));

            mockMvc.perform(get("/admin/ops/models"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].name").value("swing_entry"))
                    .andExpect(jsonPath("$[0].status").value("INACTIVE"));
        }
    }

    @Nested
    @DisplayName("POST /admin/ops/models")
    class RegisterModel {

        @Test
        @WithApiUser(role = "USER")
        @DisplayName("ROOT 미만이면 403")
        void shouldReturn403WhenNotRoot() throws Exception {
            MockMultipartFile artifact = new MockMultipartFile("artifact", "m.pkl",
                    "application/octet-stream", new byte[]{1});
            MockMultipartFile meta = new MockMultipartFile("meta", "meta.json",
                    "application/json", "{}".getBytes(StandardCharsets.UTF_8));

            mockMvc.perform(multipart("/admin/ops/models").file(artifact).file(meta))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithApiUser(role = "ROOT")
        @DisplayName("ROOT이면 등록 201")
        void shouldRegisterWhenRoot() throws Exception {
            given(registrationService.register(any(), any(), any(), any(), any(), eq("tester")))
                    .willReturn(stubModel());
            MockMultipartFile artifact = new MockMultipartFile("artifact", "m.pkl",
                    "application/octet-stream", new byte[]{1});
            MockMultipartFile meta = new MockMultipartFile("meta", "meta.json",
                    "application/json", "{}".getBytes(StandardCharsets.UTF_8));

            mockMvc.perform(multipart("/admin/ops/models").file(artifact).file(meta))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").value("swing_entry"));
        }
    }

    @Nested
    @DisplayName("POST /admin/ops/models/{id}/activate")
    class Activate {

        @Test
        @WithApiUser(role = "ROOT")
        @DisplayName("없는 모델이면 404 MODEL_NOT_FOUND")
        void shouldReturn404WhenMissing() throws Exception {
            given(lifecycleService.activate(99L)).willThrow(new ModelNotFoundException(99L));

            mockMvc.perform(post("/admin/ops/models/99/activate"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.detail").value("MODEL_NOT_FOUND"));
        }
    }

    @Nested
    @DisplayName("DELETE /admin/ops/models/{id}")
    class DeleteModel {

        @Test
        @WithApiUser(role = "ROOT")
        @DisplayName("ROOT이면 삭제 204")
        void shouldDeleteWhenRoot() throws Exception {
            mockMvc.perform(delete("/admin/ops/models/1"))
                    .andExpect(status().isNoContent());

            verify(lifecycleService).delete(1L);
        }
    }

    @Nested
    @DisplayName("POST /admin/ops/models/{id}/scores/delete")
    class DeleteScores {

        @Test
        @WithApiUser(role = "ROOT")
        @DisplayName("확인 플래그 없으면 400 DELETE_NOT_CONFIRMED")
        void shouldReturn400WhenNotConfirmed() throws Exception {
            mockMvc.perform(post("/admin/ops/models/1/scores/delete")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"confirm\":false}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").value("DELETE_NOT_CONFIRMED"));
        }

        @Test
        @WithApiUser(role = "ROOT")
        @DisplayName("확인 후 전체 삭제 시 삭제 행 수 반환 200")
        void shouldDeleteAllWhenConfirmed() throws Exception {
            given(scoreCommandService.deleteAll(1L)).willReturn(42L);

            mockMvc.perform(post("/admin/ops/models/1/scores/delete")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"confirm\":true}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.deleted").value(42));
        }
    }
}
