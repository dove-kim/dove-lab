package com.dove.api.search.market.controller;

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
class MarketControllerTest {

    @Autowired MockMvc mockMvc;

    @Nested
    @DisplayName("GET /market/trading-days")
    class GetTradingDays {

        @Test
        @DisplayName("인증 없으면 401")
        void shouldReturn401WhenUnauthenticated() throws Exception {
            mockMvc.perform(get("/market/trading-days"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithApiUser(capabilities = {"STOCK_VIEW"})
        @DisplayName("데이터 없어도 200 + latestDate·빈 거래일 배열")
        void shouldReturnTradingDaysWhenAuthenticated() throws Exception {
            mockMvc.perform(get("/market/trading-days"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.latestDate").isNotEmpty())
                    .andExpect(jsonPath("$.tradingDays").isArray());
        }

        @Test
        @WithApiUser(capabilities = {"STOCK_VIEW"})
        @DisplayName("limit 파라미터 지정 200")
        void shouldAcceptLimitParam() throws Exception {
            mockMvc.perform(get("/market/trading-days")
                            .param("limit", "5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tradingDays").isArray());
        }
    }
}
