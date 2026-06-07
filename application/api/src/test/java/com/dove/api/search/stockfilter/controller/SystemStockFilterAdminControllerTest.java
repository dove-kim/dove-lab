package com.dove.api.search.stockfilter.controller;

import com.dove.api.TestApiApplication;
import com.dove.api.support.WithApiUser;
import com.dove.screening.application.service.StockFilterCommandService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = TestApiApplication.class)
@AutoConfigureMockMvc
@Transactional
class SystemStockFilterAdminControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired StockFilterCommandService stockFilterCommandService;

    @Nested
    @DisplayName("POST /admin/stock-filters/system")
    class CreateSystemFilter {

        @Test
        @DisplayName("인증 없으면 401")
        void shouldReturn401WhenUnauthenticated() throws Exception {
            mockMvc.perform(post("/admin/stock-filters/system")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name":"시스템필터","tagConditions":[],"stockConditions":[],"numericConditions":[]}
                                    """))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithApiUser(role = "USER")
        @DisplayName("USER 권한이면 403")
        void shouldReturn403WhenUserCreates() throws Exception {
            mockMvc.perform(post("/admin/stock-filters/system")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name":"시스템필터","tagConditions":[],"stockConditions":[],"numericConditions":[]}
                                    """))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithApiUser(role = "ADMIN")
        @DisplayName("ADMIN 권한으로 201 생성")
        void shouldCreateSystemFilterAsAdmin() throws Exception {
            mockMvc.perform(post("/admin/stock-filters/system")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name":"시스템필터","tagConditions":[],"stockConditions":[],"numericConditions":[]}
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").value("시스템필터"))
                    .andExpect(jsonPath("$.scope").value("SYSTEM"));
        }

        @Test
        @WithApiUser(role = "ADMIN")
        @DisplayName("name 누락 시 400")
        void shouldReturn400WhenNameMissing() throws Exception {
            mockMvc.perform(post("/admin/stock-filters/system")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"tagConditions":[],"stockConditions":[],"numericConditions":[]}
                                    """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithApiUser(role = "ADMIN")
        @DisplayName("이름 중복 시 409")
        void shouldReturn409WhenNameDuplicate() throws Exception {
            stockFilterCommandService.createSystem("중복시스템", null, List.of(), List.of(), List.of(), "tester");

            mockMvc.perform(post("/admin/stock-filters/system")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name":"중복시스템","tagConditions":[],"stockConditions":[],"numericConditions":[]}
                                    """))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("PUT /admin/stock-filters/system/{id}")
    class UpdateSystemFilter {

        @Test
        @WithApiUser(role = "ADMIN")
        @DisplayName("수정 성공")
        void shouldUpdateSystemFilterAsAdmin() throws Exception {
            var filter = stockFilterCommandService.createSystem("원래시스템", null, List.of(), List.of(), List.of(), "tester");

            mockMvc.perform(put("/admin/stock-filters/system/" + filter.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name":"바뀐시스템","tagConditions":[],"stockConditions":[],"numericConditions":[]}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("바뀐시스템"));
        }

        @Test
        @WithApiUser(role = "ADMIN")
        @DisplayName("없는 id면 404")
        void shouldReturn404WhenUpdatingNonExistent() throws Exception {
            mockMvc.perform(put("/admin/stock-filters/system/99999")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name":"없는시스템","tagConditions":[],"stockConditions":[],"numericConditions":[]}
                                    """))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PATCH /admin/stock-filters/system/{id}/enabled")
    class SetEnabled {

        @Test
        @WithApiUser(role = "ADMIN")
        @DisplayName("비활성화 성공")
        void shouldSetEnabledAsAdmin() throws Exception {
            var filter = stockFilterCommandService.createSystem("토글시스템", null, List.of(), List.of(), List.of(), "tester");

            mockMvc.perform(patch("/admin/stock-filters/system/" + filter.getId() + "/enabled")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"enabled":false}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.enabled").value(false));
        }

        @Test
        @WithApiUser(role = "ADMIN")
        @DisplayName("없는 id면 404")
        void shouldReturn404WhenSettingEnabledForNonExistent() throws Exception {
            mockMvc.perform(patch("/admin/stock-filters/system/99999/enabled")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"enabled":false}
                                    """))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /admin/stock-filters/system/{id}")
    class DeleteSystemFilter {

        @Test
        @WithApiUser(role = "ADMIN")
        @DisplayName("삭제 204")
        void shouldDeleteSystemFilterAsAdmin() throws Exception {
            var filter = stockFilterCommandService.createSystem("삭제시스템", null, List.of(), List.of(), List.of(), "tester");

            mockMvc.perform(delete("/admin/stock-filters/system/" + filter.getId()))
                    .andExpect(status().isNoContent());
        }

        @Test
        @WithApiUser(role = "ADMIN")
        @DisplayName("없는 id면 404")
        void shouldReturn404WhenDeletingNonExistent() throws Exception {
            mockMvc.perform(delete("/admin/stock-filters/system/99999"))
                    .andExpect(status().isNotFound());
        }
    }
}
