package com.dove.api.portfolio.controller;

import com.dove.api.TestApiApplication;
import com.dove.api.support.WithApiUser;
import com.dove.portfolio.application.port.FxRatePort;
import com.dove.portfolio.application.port.OverseasPricePort;
import com.dove.portfolio.application.service.PortfolioAccountService;
import com.dove.portfolio.application.service.PortfolioTransactionService;
import com.dove.portfolio.domain.enums.TxType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 포트폴리오 라운드트립(청산 성과) 조회 API 통합 테스트.
 */
@SpringBootTest(classes = TestApiApplication.class)
@AutoConfigureMockMvc
@Transactional
class PortfolioRoundTripControllerTest {

    private static final long MEMBER_ID = 1L;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    PortfolioAccountService accountService;

    @Autowired
    PortfolioTransactionService transactionService;

    @MockitoBean
    OverseasPricePort overseasPricePort;

    @MockitoBean
    FxRatePort fxRatePort;

    private void trade(TxType type, long accountId, String qty, String price, long amount, LocalDate date) {
        transactionService.create(MEMBER_ID, accountId, type, date, "삼성전자", "KRW",
                new BigDecimal(qty), new BigDecimal(price), BigDecimal.valueOf(amount), 0L, null, null, "tester");
    }

    @Nested
    @DisplayName("라운드트립 조회")
    class List {
        @Test
        @DisplayName("인증 없으면 401")
        void shouldReturn401WhenUnauthenticated() throws Exception {
            mockMvc.perform(get("/portfolio/roundtrips")).andExpect(status().isUnauthorized());
        }

        @Test
        @WithApiUser(memberId = MEMBER_ID, capabilities = {"PORTFOLIO_LEDGER"})
        @DisplayName("매수 후 전량 매도는 청산 라운드트립으로 집계된다")
        void shouldAggregateClosedTrip() throws Exception {
            long accountId = accountService.create(MEMBER_ID, "국내", null, null, "tester").getId();
            trade(TxType.BUY, accountId, "10", "70000", 700_000L, LocalDate.of(2026, 7, 1));
            trade(TxType.SELL, accountId, "10", "80000", 800_000L, LocalDate.of(2026, 7, 5));

            mockMvc.perform(get("/portfolio/roundtrips"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].symbol").value("삼성전자"))
                    .andExpect(jsonPath("$[0].group").value("국내"))
                    .andExpect(jsonPath("$[0].open").value(false))
                    .andExpect(jsonPath("$[0].holdingDays").value(4))
                    .andExpect(jsonPath("$[0].pnlKrw").value(100_000))
                    .andExpect(jsonPath("$[0].pnlPct").value(14.29));
        }
    }
}
