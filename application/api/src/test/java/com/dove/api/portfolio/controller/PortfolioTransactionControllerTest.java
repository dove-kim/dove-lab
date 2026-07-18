package com.dove.api.portfolio.controller;

import com.dove.api.TestApiApplication;
import com.dove.api.support.WithApiUser;
import com.dove.portfolio.application.service.PortfolioAccountService;
import com.dove.portfolio.application.service.PortfolioTransactionService;
import com.dove.portfolio.domain.entity.PortfolioAccount;
import com.dove.portfolio.domain.entity.PortfolioTransaction;
import com.dove.portfolio.domain.enums.TxType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 포트폴리오 거래 API 통합 테스트.
 */
@SpringBootTest(classes = TestApiApplication.class)
@AutoConfigureMockMvc
@Transactional
class PortfolioTransactionControllerTest {

    private static final long MEMBER_ID = 1L;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    PortfolioAccountService accountService;

    @Autowired
    PortfolioTransactionService transactionService;

    private long seedOwnAccount() {
        return accountService.create(MEMBER_ID, "국내", "미래에셋", null, "tester").getId();
    }

    private PortfolioTransaction seedBuy(long memberId, long accountId, String symbol) {
        return transactionService.create(memberId, accountId, TxType.BUY, LocalDate.of(2026, 7, 12), symbol, "KRW",
                new BigDecimal("10"), new BigDecimal("70000"), new BigDecimal("700000"), 0L, "추세돌파", null, "tester");
    }

    @Nested
    @DisplayName("거래 목록 조회")
    class List {
        @Test
        @DisplayName("인증 없으면 401")
        void shouldReturn401WhenUnauthenticated() throws Exception {
            mockMvc.perform(get("/portfolio/transactions")).andExpect(status().isUnauthorized());
        }

        @Test
        @WithApiUser
        @DisplayName("PORTFOLIO_LEDGER 권한 없으면 403")
        void shouldReturn403WhenMissingCapability() throws Exception {
            mockMvc.perform(get("/portfolio/transactions")).andExpect(status().isForbidden());
        }

        @Test
        @WithApiUser(memberId = MEMBER_ID, capabilities = {"PORTFOLIO_LEDGER"})
        @DisplayName("본인 거래만 계좌명과 함께 반환")
        void shouldReturnOwnTransactions() throws Exception {
            long accountId = seedOwnAccount();
            seedBuy(MEMBER_ID, accountId, "삼성전자");
            long othersAccount = accountService.create(999L, "남의계좌", null, null, "other").getId();
            seedBuy(999L, othersAccount, "SK하이닉스");

            mockMvc.perform(get("/portfolio/transactions"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].symbol").value("삼성전자"))
                    .andExpect(jsonPath("$[0].account").value("국내"));
        }
    }

    @Nested
    @DisplayName("거래 생성")
    class Create {
        @Test
        @WithApiUser(memberId = MEMBER_ID, capabilities = {"PORTFOLIO_LEDGER"})
        @DisplayName("유효하면 201")
        void shouldCreateWhenValid() throws Exception {
            long accountId = seedOwnAccount();
            mockMvc.perform(post("/portfolio/transactions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"accountId\":" + accountId + ",\"type\":\"BUY\",\"tradedAt\":\"2026-07-12\","
                                    + "\"symbol\":\"삼성전자\",\"currency\":\"KRW\",\"quantity\":10,\"price\":70000,"
                                    + "\"amount\":700000}"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.symbol").value("삼성전자"))
                    .andExpect(jsonPath("$.account").value("국내"));
        }

        @Test
        @WithApiUser(memberId = MEMBER_ID, capabilities = {"PORTFOLIO_LEDGER"})
        @DisplayName("통화 비면 400")
        void shouldReturn400WhenCurrencyBlank() throws Exception {
            long accountId = seedOwnAccount();
            mockMvc.perform(post("/portfolio/transactions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"accountId\":" + accountId + ",\"type\":\"DEPOSIT\",\"tradedAt\":\"2026-07-12\","
                                    + "\"currency\":\"\",\"amount\":1000000}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithApiUser(memberId = MEMBER_ID, capabilities = {"PORTFOLIO_LEDGER"})
        @DisplayName("본인 소유 아닌 계좌면 404")
        void shouldReturn404WhenAccountNotOwned() throws Exception {
            long othersAccount = accountService.create(999L, "남의계좌", null, null, "other").getId();
            mockMvc.perform(post("/portfolio/transactions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"accountId\":" + othersAccount + ",\"type\":\"BUY\",\"tradedAt\":\"2026-07-12\","
                                    + "\"symbol\":\"삼성전자\",\"currency\":\"KRW\",\"quantity\":10,\"price\":70000,"
                                    + "\"amount\":700000}"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("거래 수정/삭제")
    class UpdateDelete {
        @Test
        @WithApiUser(memberId = MEMBER_ID, capabilities = {"PORTFOLIO_LEDGER"})
        @DisplayName("수정 200")
        void shouldUpdate() throws Exception {
            long accountId = seedOwnAccount();
            PortfolioTransaction t = seedBuy(MEMBER_ID, accountId, "삼성전자");
            mockMvc.perform(put("/portfolio/transactions/" + t.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"type\":\"BUY\",\"tradedAt\":\"2026-07-13\",\"symbol\":\"삼성전자우\","
                                    + "\"currency\":\"KRW\",\"quantity\":5,\"price\":60000,\"amount\":300000}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.symbol").value("삼성전자우"));
        }

        @Test
        @WithApiUser(memberId = MEMBER_ID, capabilities = {"PORTFOLIO_LEDGER"})
        @DisplayName("없는 거래 수정 404")
        void shouldReturn404WhenUpdatingMissing() throws Exception {
            mockMvc.perform(put("/portfolio/transactions/999999")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"type\":\"BUY\",\"tradedAt\":\"2026-07-13\",\"currency\":\"KRW\","
                                    + "\"amount\":300000}"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithApiUser(memberId = MEMBER_ID, capabilities = {"PORTFOLIO_LEDGER"})
        @DisplayName("삭제 204")
        void shouldDelete() throws Exception {
            long accountId = seedOwnAccount();
            PortfolioTransaction t = seedBuy(MEMBER_ID, accountId, "삼성전자");
            mockMvc.perform(delete("/portfolio/transactions/" + t.getId())).andExpect(status().isNoContent());
        }

        @Test
        @WithApiUser(memberId = MEMBER_ID, capabilities = {"PORTFOLIO_LEDGER"})
        @DisplayName("남의 거래는 404")
        void shouldReturn404WhenDeletingOthers() throws Exception {
            long othersAccount = accountService.create(999L, "남의계좌", null, null, "other").getId();
            PortfolioTransaction t = seedBuy(999L, othersAccount, "SK하이닉스");
            mockMvc.perform(delete("/portfolio/transactions/" + t.getId())).andExpect(status().isNotFound());
        }
    }
}
