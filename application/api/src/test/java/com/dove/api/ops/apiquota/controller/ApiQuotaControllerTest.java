package com.dove.api.ops.apiquota.controller;

import com.dove.api.TestApiApplication;
import com.dove.api.support.WithApiUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = TestApiApplication.class)
@AutoConfigureMockMvc
@Transactional
class ApiQuotaControllerTest {

    @Autowired MockMvc mockMvc;

    @Nested
    @DisplayName("GET /admin/ops/api-quota")
    class GetQuotas {

        @Test
        @DisplayName("인증 없으면 401")
        void shouldReturn401WhenUnauthenticated() throws Exception {
            mockMvc.perform(get("/admin/ops/api-quota"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithApiUser(role = "USER")
        @DisplayName("USER 권한이면 403")
        void shouldReturn403WhenUser() throws Exception {
            mockMvc.perform(get("/admin/ops/api-quota"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithApiUser(role = "ROOT")
        @DisplayName("ROOT 권한이면 쿼터 목록 200")
        void shouldReturnQuotasWhenRoot() throws Exception {
            mockMvc.perform(get("/admin/ops/api-quota"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.quotas").isArray());
        }
    }
}
