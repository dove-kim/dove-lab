package com.dove.api.portfolio.controller;

import com.dove.api.TestApiApplication;
import com.dove.api.support.WithApiUser;
import com.dove.portfolio.application.port.FxRatePort;
import com.dove.portfolio.application.port.OverseasPricePort;
import com.dove.portfolio.application.service.PortfolioAccountService;
import com.dove.portfolio.application.service.PortfolioHoldingService;
import com.dove.portfolio.domain.enums.PortfolioMarket;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 포트폴리오 종목 식별 API 통합 테스트.
 */
@SpringBootTest(classes = TestApiApplication.class)
@AutoConfigureMockMvc
@Transactional
class PortfolioHoldingControllerTest {

    private static final long MEMBER_ID = 1L;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    PortfolioAccountService accountService;

    @Autowired
    PortfolioHoldingService holdingService;

    // 종목 첫 진입 시 on-entry 시세·환율 조회가 외부(KIS/Frankfurter)를 때리지 않도록 격리.
    @MockitoBean
    OverseasPricePort overseasPricePort;

    @MockitoBean
    FxRatePort fxRatePort;

    private long seedOwnAccount() {
        return accountService.create(MEMBER_ID, "국내", "미래에셋", null, "tester").getId();
    }

    @Nested
    @DisplayName("식별 목록 조회")
    class List {
        @Test
        @DisplayName("인증 없으면 401")
        void shouldReturn401WhenUnauthenticated() throws Exception {
            mockMvc.perform(get("/portfolio/holdings")).andExpect(status().isUnauthorized());
        }

        @Test
        @WithApiUser
        @DisplayName("PORTFOLIO_LEDGER 권한 없으면 403")
        void shouldReturn403WhenMissingCapability() throws Exception {
            mockMvc.perform(get("/portfolio/holdings")).andExpect(status().isForbidden());
        }

        @Test
        @WithApiUser(memberId = MEMBER_ID, capabilities = {"PORTFOLIO_LEDGER"})
        @DisplayName("본인 식별만 통화와 함께 반환")
        void shouldReturnOwnHoldings() throws Exception {
            long accountId = seedOwnAccount();
            holdingService.attach(MEMBER_ID, accountId, "삼성전자", PortfolioMarket.KOSPI, "005930", "tester");
            long othersAccount = accountService.create(999L, "남", null, null, "other").getId();
            holdingService.attach(999L, othersAccount, "AAPL", PortfolioMarket.NASDAQ, "AAPL", "other");

            mockMvc.perform(get("/portfolio/holdings"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].symbol").value("삼성전자"))
                    .andExpect(jsonPath("$[0].currency").value("KRW"))
                    .andExpect(jsonPath("$[0].account").value("국내"));
        }
    }

    @Nested
    @DisplayName("식별 등록/갱신(upsert)")
    class Attach {
        @Test
        @WithApiUser(memberId = MEMBER_ID, capabilities = {"PORTFOLIO_LEDGER"})
        @DisplayName("신규 등록 시 시장 통화가 파생된다")
        void shouldAttachNew() throws Exception {
            long accountId = seedOwnAccount();
            mockMvc.perform(post("/portfolio/holdings")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"accountId\":" + accountId + ",\"symbol\":\"TSLA\",\"market\":\"NASDAQ\","
                                    + "\"ticker\":\"TSLA\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.ticker").value("TSLA"))
                    .andExpect(jsonPath("$.currency").value("USD"));
        }

        @Test
        @WithApiUser(memberId = MEMBER_ID, capabilities = {"PORTFOLIO_LEDGER"})
        @DisplayName("같은 계좌·종목 재등록은 갱신되어 중복 생성되지 않는다")
        void shouldUpdateOnDuplicate() throws Exception {
            long accountId = seedOwnAccount();
            holdingService.attach(MEMBER_ID, accountId, "삼성전자", PortfolioMarket.KOSPI, "005930", "tester");

            mockMvc.perform(post("/portfolio/holdings")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"accountId\":" + accountId + ",\"symbol\":\"삼성전자\",\"market\":\"KOSDAQ\","
                                    + "\"ticker\":\"999999\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.ticker").value("999999"));

            mockMvc.perform(get("/portfolio/holdings")).andExpect(jsonPath("$.length()").value(1));
        }

        @Test
        @WithApiUser(memberId = MEMBER_ID, capabilities = {"PORTFOLIO_LEDGER"})
        @DisplayName("본인 소유 아닌 계좌면 404")
        void shouldReturn404WhenAccountNotOwned() throws Exception {
            long othersAccount = accountService.create(999L, "남", null, null, "other").getId();
            mockMvc.perform(post("/portfolio/holdings")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"accountId\":" + othersAccount + ",\"symbol\":\"삼성전자\",\"market\":\"KOSPI\","
                                    + "\"ticker\":\"005930\"}"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithApiUser(memberId = MEMBER_ID, capabilities = {"PORTFOLIO_LEDGER"})
        @DisplayName("시장 값이 없으면 400")
        void shouldReturn400WhenMarketMissing() throws Exception {
            long accountId = seedOwnAccount();
            mockMvc.perform(post("/portfolio/holdings")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"accountId\":" + accountId + ",\"symbol\":\"삼성전자\",\"ticker\":\"005930\"}"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("배당 추적 설정")
    class Tracking {
        @Test
        @WithApiUser(memberId = MEMBER_ID, capabilities = {"PORTFOLIO_LEDGER"})
        @DisplayName("추적 대상으로 설정하면 플래그가 켜진다")
        void shouldSetTracked() throws Exception {
            long accountId = seedOwnAccount();
            var h = holdingService.attach(MEMBER_ID, accountId, "삼성전자", PortfolioMarket.KOSPI, "005930", "tester");

            mockMvc.perform(put("/portfolio/holdings/" + h.getId() + "/tracking")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"tracked\":true}"))
                    .andExpect(status().isOk());

            assertThat(holdingService.findByOwner(MEMBER_ID))
                    .singleElement()
                    .matches(x -> x.isDividendTracked());
        }

        @Test
        @WithApiUser(memberId = MEMBER_ID, capabilities = {"PORTFOLIO_LEDGER"})
        @DisplayName("tracked 값이 없으면 400")
        void shouldReturn400WhenTrackedMissing() throws Exception {
            long accountId = seedOwnAccount();
            var h = holdingService.attach(MEMBER_ID, accountId, "삼성전자", PortfolioMarket.KOSPI, "005930", "tester");
            mockMvc.perform(put("/portfolio/holdings/" + h.getId() + "/tracking")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithApiUser(memberId = MEMBER_ID, capabilities = {"PORTFOLIO_LEDGER"})
        @DisplayName("남의 보유는 404")
        void shouldReturn404WhenNotOwned() throws Exception {
            long othersAccount = accountService.create(999L, "남", null, null, "other").getId();
            var h = holdingService.attach(999L, othersAccount, "AAPL", PortfolioMarket.NASDAQ, "AAPL", "other");
            mockMvc.perform(put("/portfolio/holdings/" + h.getId() + "/tracking")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"tracked\":true}"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("식별 삭제")
    class Delete {
        @Test
        @WithApiUser(memberId = MEMBER_ID, capabilities = {"PORTFOLIO_LEDGER"})
        @DisplayName("삭제 204")
        void shouldDelete() throws Exception {
            long accountId = seedOwnAccount();
            var h = holdingService.attach(MEMBER_ID, accountId, "삼성전자", PortfolioMarket.KOSPI, "005930", "tester");
            mockMvc.perform(delete("/portfolio/holdings/" + h.getId())).andExpect(status().isNoContent());
        }

        @Test
        @WithApiUser(memberId = MEMBER_ID, capabilities = {"PORTFOLIO_LEDGER"})
        @DisplayName("남의 식별은 404")
        void shouldReturn404WhenDeletingOthers() throws Exception {
            long othersAccount = accountService.create(999L, "남", null, null, "other").getId();
            var h = holdingService.attach(999L, othersAccount, "AAPL", PortfolioMarket.NASDAQ, "AAPL", "other");
            mockMvc.perform(delete("/portfolio/holdings/" + h.getId())).andExpect(status().isNotFound());
        }
    }
}
