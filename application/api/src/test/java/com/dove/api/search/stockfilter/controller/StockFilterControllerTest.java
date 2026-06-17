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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = TestApiApplication.class)
@AutoConfigureMockMvc
@Transactional
class StockFilterControllerTest {

    private static final long MEMBER_ID = 1L;

    @Autowired MockMvc mockMvc;
    @Autowired StockFilterCommandService stockFilterCommandService;

    @Nested
    @DisplayName("GET /stock-filters/personal")
    class ListPersonalFilters {

        @Test
        @DisplayName("인증 없으면 401")
        void shouldReturn401WhenUnauthenticated() throws Exception {
            mockMvc.perform(get("/stock-filters/personal"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithApiUser()
        @DisplayName("STOCK_SEARCH 기능 없으면 403")
        void shouldReturn403WhenMissingFeature() throws Exception {
            mockMvc.perform(get("/stock-filters/personal"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithApiUser(memberId = MEMBER_ID, capabilities = {"STOCK_SEARCH"})
        @DisplayName("생성 전 빈 목록")
        void shouldReturnEmptyPersonalFiltersWhenNone() throws Exception {
            mockMvc.perform(get("/stock-filters/personal"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").isEmpty());
        }
    }

    @Nested
    @DisplayName("POST /stock-filters/personal")
    class CreatePersonalFilter {

        @Test
        @WithApiUser(memberId = MEMBER_ID, capabilities = {"STOCK_SEARCH"})
        @DisplayName("201 생성")
        void shouldCreatePersonalFilterWhenValid() throws Exception {
            mockMvc.perform(post("/stock-filters/personal")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name":"내종목필터","tagConditions":[],"stockConditions":[]}
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").value("내종목필터"));
        }

        @Test
        @WithApiUser(memberId = MEMBER_ID, capabilities = {"STOCK_SEARCH"})
        @DisplayName("name 누락 시 400")
        void shouldReturn400WhenNameMissing() throws Exception {
            mockMvc.perform(post("/stock-filters/personal")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"tagConditions":[],"stockConditions":[]}
                                    """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithApiUser(memberId = MEMBER_ID, capabilities = {"STOCK_SEARCH"})
        @DisplayName("이름 중복 시 409")
        void shouldReturn409WhenNameDuplicate() throws Exception {
            stockFilterCommandService.createPersonal(MEMBER_ID, "중복이름", null, List.of(), List.of(), List.of(), "tester");

            mockMvc.perform(post("/stock-filters/personal")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name":"중복이름","tagConditions":[],"stockConditions":[]}
                                    """))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("PUT /stock-filters/personal/{id}")
    class UpdatePersonalFilter {

        @Test
        @WithApiUser(memberId = MEMBER_ID, capabilities = {"STOCK_SEARCH"})
        @DisplayName("없는 id면 404")
        void shouldReturn404WhenUpdatingNonExistent() throws Exception {
            mockMvc.perform(put("/stock-filters/personal/99999")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name":"없는필터","tagConditions":[],"stockConditions":[]}
                                    """))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithApiUser(memberId = MEMBER_ID, capabilities = {"STOCK_SEARCH"})
        @DisplayName("수정 성공")
        void shouldUpdatePersonalFilterWhenExists() throws Exception {
            var filter = stockFilterCommandService.createPersonal(MEMBER_ID, "원래이름", null, List.of(), List.of(), List.of(), "tester");

            mockMvc.perform(put("/stock-filters/personal/" + filter.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name":"바뀐이름","tagConditions":[],"stockConditions":[]}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("바뀐이름"));
        }
    }

    @Nested
    @DisplayName("DELETE /stock-filters/personal/{id}")
    class DeletePersonalFilter {

        @Test
        @WithApiUser(memberId = MEMBER_ID, capabilities = {"STOCK_SEARCH"})
        @DisplayName("없는 id면 404")
        void shouldReturn404WhenDeletingNonExistent() throws Exception {
            mockMvc.perform(delete("/stock-filters/personal/99999"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithApiUser(memberId = MEMBER_ID, capabilities = {"STOCK_SEARCH"})
        @DisplayName("삭제 204")
        void shouldDeletePersonalFilterWhenExists() throws Exception {
            var filter = stockFilterCommandService.createPersonal(MEMBER_ID, "삭제할필터", null, List.of(), List.of(), List.of(), "tester");

            mockMvc.perform(delete("/stock-filters/personal/" + filter.getId()))
                    .andExpect(status().isNoContent());
        }
    }

    @Nested
    @DisplayName("GET /stock-filters/system")
    class ListSystemFilters {

        @Test
        @WithApiUser(memberId = MEMBER_ID, role = "USER", capabilities = {"STOCK_SEARCH"})
        @DisplayName("USER는 활성 필터만 조회")
        void shouldReturnOnlyEnabledSystemFiltersForUser() throws Exception {
            stockFilterCommandService.createSystem("활성필터", null, List.of(), List.of(), List.of(), "admin");
            var disabled = stockFilterCommandService.createSystem("비활성필터", null, List.of(), List.of(), List.of(), "admin");
            stockFilterCommandService.setEnabled(disabled.getId(), false, "admin");

            mockMvc.perform(get("/stock-filters/system"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].name").value("활성필터"));
        }

        @Test
        @WithApiUser(memberId = MEMBER_ID, role = "ADMIN", capabilities = {"STOCK_SEARCH"})
        @DisplayName("ADMIN은 비활성 필터도 조회")
        void shouldReturnAllSystemFiltersForAdmin() throws Exception {
            stockFilterCommandService.createSystem("활성필터", null, List.of(), List.of(), List.of(), "admin");
            var disabled = stockFilterCommandService.createSystem("비활성필터", null, List.of(), List.of(), List.of(), "admin");
            stockFilterCommandService.setEnabled(disabled.getId(), false, "admin");

            mockMvc.perform(get("/stock-filters/system"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2));
        }
    }

    @Nested
    @DisplayName("GET /stock-filters/available")
    class ListAvailableFilters {

        @Test
        @WithApiUser(memberId = MEMBER_ID, capabilities = {"STOCK_SEARCH"})
        @DisplayName("활성 시스템 필터 + 본인 개인 필터 반환")
        void shouldReturnAvailableFilters() throws Exception {
            stockFilterCommandService.createSystem("시스템필터", null, List.of(), List.of(), List.of(), "admin");
            stockFilterCommandService.createPersonal(MEMBER_ID, "내개인필터", null, List.of(), List.of(), List.of(), "tester");

            mockMvc.perform(get("/stock-filters/available"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2));
        }
    }
}
