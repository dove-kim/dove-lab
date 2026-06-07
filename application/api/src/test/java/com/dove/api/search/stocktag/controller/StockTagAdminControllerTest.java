package com.dove.api.search.stocktag.controller;

import com.dove.api.TestApiApplication;
import com.dove.api.support.WithApiUser;
import com.dove.stock.domain.entity.StockTagValue;
import com.dove.stock.domain.repository.StockTagValueRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = TestApiApplication.class)
@AutoConfigureMockMvc
@Transactional
class StockTagAdminControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired StockTagValueRepository stockTagValueRepository;

    private Long tagValueId;

    @BeforeEach
    void setUp() {
        tagValueId = stockTagValueRepository.save(StockTagValue.of("SECUGRP", "주권")).getId();
    }

    @Nested
    @DisplayName("PATCH /admin/stock-tags/{id}/label")
    class UpdateLabel {

        @Test
        @DisplayName("인증 없으면 401")
        void shouldReturn401WhenUnauthenticated() throws Exception {
            mockMvc.perform(patch("/admin/stock-tags/" + tagValueId + "/label")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"label":"보통주"}
                                    """))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithApiUser(role = "ADMIN")
        @DisplayName("ADMIN 권한이면 403")
        void shouldReturn403WhenAdminUpdatesLabel() throws Exception {
            mockMvc.perform(patch("/admin/stock-tags/" + tagValueId + "/label")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"label":"보통주"}
                                    """))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithApiUser(role = "ROOT")
        @DisplayName("ROOT 권한으로 라벨 수정 204")
        void shouldUpdateLabelAsRoot() throws Exception {
            mockMvc.perform(patch("/admin/stock-tags/" + tagValueId + "/label")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"label":"보통주"}
                                    """))
                    .andExpect(status().isNoContent());
        }

        @Test
        @WithApiUser(role = "ROOT")
        @DisplayName("label 누락 시 400")
        void shouldReturn400WhenLabelMissing() throws Exception {
            mockMvc.perform(patch("/admin/stock-tags/" + tagValueId + "/label")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {}
                                    """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithApiUser(role = "ROOT")
        @DisplayName("없는 id면 404")
        void shouldReturn404WhenUpdatingNonExistent() throws Exception {
            mockMvc.perform(patch("/admin/stock-tags/99999/label")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"label":"보통주"}
                                    """))
                    .andExpect(status().isNotFound());
        }
    }
}
