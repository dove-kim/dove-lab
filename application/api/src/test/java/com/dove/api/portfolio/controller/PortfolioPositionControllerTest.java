package com.dove.api.portfolio.controller;

import com.dove.api.TestApiApplication;
import com.dove.api.support.WithApiUser;
import com.dove.portfolio.application.port.FxRatePort;
import com.dove.portfolio.application.port.OverseasPricePort;
import com.dove.portfolio.application.service.PortfolioAccountService;
import com.dove.portfolio.application.service.PortfolioHoldingService;
import com.dove.portfolio.application.service.PortfolioTransactionService;
import com.dove.portfolio.domain.entity.PortfolioFxRate;
import com.dove.portfolio.domain.entity.PortfolioQuote;
import com.dove.portfolio.domain.enums.PortfolioMarket;
import com.dove.portfolio.domain.enums.TxType;
import com.dove.portfolio.domain.repository.PortfolioFxRateRepository;
import com.dove.portfolio.domain.repository.PortfolioQuoteRepository;
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
 * 포트폴리오 보유 포지션 조회 API 통합 테스트.
 */
@SpringBootTest(classes = TestApiApplication.class)
@AutoConfigureMockMvc
@Transactional
class PortfolioPositionControllerTest {

    private static final long MEMBER_ID = 1L;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    PortfolioAccountService accountService;

    @Autowired
    PortfolioTransactionService transactionService;

    @Autowired
    PortfolioHoldingService holdingService;

    @Autowired
    PortfolioQuoteRepository quoteRepository;

    @Autowired
    PortfolioFxRateRepository fxRateRepository;

    @MockitoBean
    OverseasPricePort overseasPricePort;

    @MockitoBean
    FxRatePort fxRatePort;

    private long buy(long accountId, String symbol, String currency, String qty, String price, long amount) {
        transactionService.create(MEMBER_ID, accountId, TxType.BUY, LocalDate.of(2026, 7, 1), symbol, currency,
                new BigDecimal(qty), new BigDecimal(price), BigDecimal.valueOf(amount), 0L, null, null, "tester");
        return accountId;
    }

    @Nested
    @DisplayName("포지션 계산")
    class Compute {
        @Test
        @DisplayName("인증 없으면 401")
        void shouldReturn401WhenUnauthenticated() throws Exception {
            mockMvc.perform(get("/portfolio/positions")).andExpect(status().isUnauthorized());
        }

        @Test
        @WithApiUser(memberId = MEMBER_ID, capabilities = {"PORTFOLIO_LEDGER"})
        @DisplayName("해외 종목은 종가·환율로 원화 평가액과 손익을 낸다")
        void shouldValueOverseasWithQuoteAndFx() throws Exception {
            long accountId = accountService.create(MEMBER_ID, "미국", null, null, "tester").getId();
            buy(accountId, "TSLA", "USD", "10", "180", 1_800L); // 금액도 원통화: 10×180=1,800 USD
            holdingService.attach(MEMBER_ID, accountId, "TSLA", PortfolioMarket.NASDAQ, "TSLA", "tester");
            quoteRepository.save(PortfolioQuote.create(PortfolioMarket.NASDAQ, "TSLA", new BigDecimal("200")));
            fxRateRepository.save(PortfolioFxRate.create("USD", new BigDecimal("1400"), LocalDate.of(2026, 7, 13)));

            // 원가 1,800×1,400=2,520,000 / 평가 200×10×1,400=2,800,000 → 손익 280,000
            mockMvc.perform(get("/portfolio/positions"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].symbol").value("TSLA"))
                    .andExpect(jsonPath("$[0].account").value("미국"))
                    .andExpect(jsonPath("$[0].evalKrw").value(2_800_000))
                    .andExpect(jsonPath("$[0].pnlKrw").value(280_000))
                    .andExpect(jsonPath("$[0].weightPct").value(100.0));
        }

        @Test
        @WithApiUser(memberId = MEMBER_ID, capabilities = {"PORTFOLIO_LEDGER"})
        @DisplayName("시장·티커 미연동 종목은 원가로 표시(손익 0)")
        void shouldFallbackToCostWhenUnidentified() throws Exception {
            long accountId = accountService.create(MEMBER_ID, "국내", null, null, "tester").getId();
            buy(accountId, "삼성전자", "KRW", "10", "70000", 700_000L);

            mockMvc.perform(get("/portfolio/positions"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].symbol").value("삼성전자"))
                    .andExpect(jsonPath("$[0].evalKrw").value(700_000))
                    .andExpect(jsonPath("$[0].pnlKrw").value(0));
        }
    }
}
