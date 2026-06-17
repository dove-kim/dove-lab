package com.dove.api.search.stocktag.controller;

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
class StockTagControllerTest {

    @Autowired MockMvc mockMvc;

    @Nested
    @DisplayName("GET /stock-tags")
    class ListTags {

        @Test
        @DisplayName("인증 없으면 401")
        void shouldReturn401WhenUnauthenticated() throws Exception {
            mockMvc.perform(get("/stock-tags"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithApiUser()
        @DisplayName("STOCK_SEARCH 기능 없으면 403")
        void shouldReturn403WhenMissingFeature() throws Exception {
            mockMvc.perform(get("/stock-tags"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithApiUser(capabilities = {"STOCK_VIEW"})
        @DisplayName("분류 메타와 값 목록을 반환")
        void shouldReturnTagsWhenAuthorized() throws Exception {
            mockMvc.perform(get("/stock-tags"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tagFields").isArray())
                    .andExpect(jsonPath("$.numericFields").isArray());
        }
    }
}
