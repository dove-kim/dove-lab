package com.dove.api.account.menu.controller;

import com.dove.api.TestApiApplication;
import com.dove.api.support.WithApiUser;
import com.dove.userfeature.application.service.MemberFeatureGrantCommandService;
import com.dove.userfeature.domain.enums.FeatureCode;
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
class MenuControllerTest {

    private static final long MEMBER_ID = 1L;

    @Autowired MockMvc mockMvc;
    @Autowired MemberFeatureGrantCommandService grantCommandService;

    @Nested
    @DisplayName("GET /account/menu")
    class GetMenu {

        @Test
        @DisplayName("인증 없으면 401")
        void shouldReturn401WhenUnauthenticated() throws Exception {
            mockMvc.perform(get("/account/menu"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithApiUser
        @DisplayName("활성 기능 없으면 빈 모듈 목록")
        void shouldReturnEmptyMenuWhenNoGrants() throws Exception {
            mockMvc.perform(get("/account/menu"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.modules").isArray())
                    .andExpect(jsonPath("$.modules").isEmpty());
        }

        @Test
        @WithApiUser
        @DisplayName("부여된 기능이 메뉴에 포함")
        void shouldReturnMenuWithGrantedFeature() throws Exception {
            grantCommandService.grant(MEMBER_ID, FeatureCode.STOCK_SEARCH, null);

            mockMvc.perform(get("/account/menu"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.modules[0].moduleCode").value("STOCK"))
                    .andExpect(jsonPath("$.modules[0].features[0].featureCode").value("STOCK_SEARCH"))
                    .andExpect(jsonPath("$.modules[0].features[0].hidden").value(false));
        }
    }

    @Nested
    @DisplayName("PATCH /account/menu/modules/reorder")
    class ReorderModules {

        @Test
        @WithApiUser
        @DisplayName("204 반환")
        void shouldReorderModules() throws Exception {
            grantCommandService.grant(MEMBER_ID, FeatureCode.STOCK_SEARCH, null);
            grantCommandService.grant(MEMBER_ID, FeatureCode.BUDGET, null);

            String body = "{\"modules\":[\"BUDGET\",\"STOCK\"]}";

            mockMvc.perform(patch("/account/menu/modules/reorder")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isNoContent());
        }

        @Test
        @WithApiUser
        @DisplayName("modules 누락 시 400")
        void shouldReturn400WhenModulesMissing() throws Exception {
            mockMvc.perform(patch("/account/menu/modules/reorder")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PATCH /account/menu/modules/{module}/features/reorder")
    class ReorderFeatures {

        @Test
        @WithApiUser
        @DisplayName("204 반환")
        void shouldReorderFeatures() throws Exception {
            grantCommandService.grant(MEMBER_ID, FeatureCode.STOCK_SEARCH, null);
            grantCommandService.grant(MEMBER_ID, FeatureCode.STOCK_LEDGER, null);

            String body = "{\"features\":[\"STOCK_LEDGER\",\"STOCK_SEARCH\"]}";

            mockMvc.perform(patch("/account/menu/modules/STOCK/features/reorder")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isNoContent());
        }
    }

    @Nested
    @DisplayName("PATCH /account/menu/features/{feature}/hidden")
    class SetFeatureHidden {

        @Test
        @WithApiUser
        @DisplayName("표시 설정이 없으면 404")
        void shouldReturn404WhenFeatureDisplayNotFound() throws Exception {
            String body = "{\"hidden\":true}";

            mockMvc.perform(patch("/account/menu/features/STOCK_SEARCH/hidden")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithApiUser
        @DisplayName("표시 설정이 있으면 204")
        void shouldHideFeature() throws Exception {
            grantCommandService.grant(MEMBER_ID, FeatureCode.STOCK_SEARCH, null);

            String body = "{\"hidden\":true}";

            mockMvc.perform(patch("/account/menu/features/STOCK_SEARCH/hidden")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isNoContent());
        }
    }

    @Nested
    @DisplayName("PATCH /account/menu/modules/{module}/hidden")
    class SetModuleHidden {

        @Test
        @WithApiUser
        @DisplayName("표시 설정이 없으면 404")
        void shouldReturn404WhenModuleDisplayNotFound() throws Exception {
            String body = "{\"hidden\":true}";

            mockMvc.perform(patch("/account/menu/modules/STOCK/hidden")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithApiUser
        @DisplayName("표시 설정이 있으면 204")
        void shouldHideModule() throws Exception {
            grantCommandService.grant(MEMBER_ID, FeatureCode.STOCK_SEARCH, null);

            String body = "{\"hidden\":true}";

            mockMvc.perform(patch("/account/menu/modules/STOCK/hidden")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isNoContent());
        }
    }
}
