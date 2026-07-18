package com.dove.api.portfolio.controller;

import com.dove.api.TestApiApplication;
import com.dove.api.support.WithApiUser;
import com.dove.portfolio.application.service.PortfolioAccountService;
import com.dove.portfolio.domain.entity.PortfolioAccount;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 포트폴리오 계좌 API 통합 테스트.
 */
@SpringBootTest(classes = TestApiApplication.class)
@AutoConfigureMockMvc
@Transactional
class PortfolioAccountControllerTest {

    private static final long MEMBER_ID = 1L;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    PortfolioAccountService accountService;

    @Nested
    @DisplayName("계좌 목록 조회")
    class List {
        @Test
        @DisplayName("인증 없으면 401")
        void shouldReturn401WhenUnauthenticated() throws Exception {
            mockMvc.perform(get("/portfolio/accounts")).andExpect(status().isUnauthorized());
        }

        @Test
        @WithApiUser
        @DisplayName("PORTFOLIO_LEDGER 권한 없으면 403")
        void shouldReturn403WhenMissingCapability() throws Exception {
            mockMvc.perform(get("/portfolio/accounts")).andExpect(status().isForbidden());
        }

        @Test
        @WithApiUser(memberId = MEMBER_ID, capabilities = {"PORTFOLIO_LEDGER"})
        @DisplayName("본인 계좌만 반환")
        void shouldReturnOwnAccounts() throws Exception {
            accountService.create(MEMBER_ID, "장투", "미래에셋", "추세돌파", "tester");
            accountService.create(999L, "남의계좌", null, null, "other");

            mockMvc.perform(get("/portfolio/accounts"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].name").value("장투"));
        }
    }

    @Nested
    @DisplayName("계좌 생성")
    class Create {
        @Test
        @WithApiUser(memberId = MEMBER_ID, capabilities = {"PORTFOLIO_LEDGER"})
        @DisplayName("유효하면 201")
        void shouldCreateWhenValid() throws Exception {
            mockMvc.perform(post("/portfolio/accounts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"미국\",\"brokerName\":\"토스증권\"}"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").value("미국"))
                    .andExpect(jsonPath("$.brokerName").value("토스증권"));
        }

        @Test
        @WithApiUser(memberId = MEMBER_ID, capabilities = {"PORTFOLIO_LEDGER"})
        @DisplayName("이름 비면 400")
        void shouldReturn400WhenNameBlank() throws Exception {
            mockMvc.perform(post("/portfolio/accounts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithApiUser(memberId = MEMBER_ID, capabilities = {"PORTFOLIO_LEDGER"})
        @DisplayName("같은 이름 중복이면 409")
        void shouldReturn409WhenDuplicateName() throws Exception {
            accountService.create(MEMBER_ID, "단타", null, null, "tester");
            mockMvc.perform(post("/portfolio/accounts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"단타\"}"))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("계좌 수정/삭제")
    class UpdateDelete {
        @Test
        @WithApiUser(memberId = MEMBER_ID, capabilities = {"PORTFOLIO_LEDGER"})
        @DisplayName("수정 200")
        void shouldUpdate() throws Exception {
            PortfolioAccount a = accountService.create(MEMBER_ID, "장투", null, null, "tester");
            mockMvc.perform(put("/portfolio/accounts/" + a.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"장기투자\",\"description\":\"수정됨\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("장기투자"));
        }

        @Test
        @WithApiUser(memberId = MEMBER_ID, capabilities = {"PORTFOLIO_LEDGER"})
        @DisplayName("없는 계좌 수정 404")
        void shouldReturn404WhenUpdatingMissing() throws Exception {
            mockMvc.perform(put("/portfolio/accounts/999999")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"x\"}"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithApiUser(memberId = MEMBER_ID, capabilities = {"PORTFOLIO_LEDGER"})
        @DisplayName("삭제 204")
        void shouldDelete() throws Exception {
            PortfolioAccount a = accountService.create(MEMBER_ID, "삭제대상", null, null, "tester");
            mockMvc.perform(delete("/portfolio/accounts/" + a.getId())).andExpect(status().isNoContent());
        }

        @Test
        @WithApiUser(memberId = MEMBER_ID, capabilities = {"PORTFOLIO_LEDGER"})
        @DisplayName("남의 계좌는 404")
        void shouldReturn404WhenDeletingOthers() throws Exception {
            PortfolioAccount a = accountService.create(999L, "남의것", null, null, "other");
            mockMvc.perform(delete("/portfolio/accounts/" + a.getId())).andExpect(status().isNotFound());
        }
    }
}
