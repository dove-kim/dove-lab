package com.dove.api.ops.systemevent.controller;

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
class SystemEventControllerTest {

    @Autowired MockMvc mockMvc;

    @Nested
    @DisplayName("GET /admin/ops/system-events")
    class GetSystemEvents {

        @Test
        @DisplayName("인증 없으면 401")
        void shouldReturn401WhenUnauthenticated() throws Exception {
            mockMvc.perform(get("/admin/ops/system-events"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithApiUser(role = "USER")
        @DisplayName("USER 권한이면 403")
        void shouldReturn403WhenUser() throws Exception {
            mockMvc.perform(get("/admin/ops/system-events"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithApiUser(role = "ROOT")
        @DisplayName("ROOT 권한이면 페이지 조회 200")
        void shouldReturnPageWhenRoot() throws Exception {
            mockMvc.perform(get("/admin/ops/system-events"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.size").value(50));
        }

        @Test
        @WithApiUser(role = "ROOT")
        @DisplayName("ROOT 권한이면 page/size 파라미터 반영")
        void shouldApplyPagingParamsWhenRoot() throws Exception {
            mockMvc.perform(get("/admin/ops/system-events")
                            .param("page", "1")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.number").value(1))
                    .andExpect(jsonPath("$.size").value(10));
        }
    }
}
