package com.dove.api.search.stock.controller;

import com.dove.api.TestApiApplication;
import com.dove.api.support.WithApiUser;
import com.dove.investorflow.application.service.InvestorDailyService;
import com.dove.investorflow.domain.entity.InvestorDaily;
import com.dove.market.domain.enums.MarketType;
import com.dove.stock.application.service.StockCommandService;
import com.dove.stock.domain.entity.Stock;
import com.dove.stock.domain.enums.StockExchange;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = TestApiApplication.class)
@AutoConfigureMockMvc
@Transactional
class StockControllerTest {

    private static final String TICKER = "005930";

    @Autowired MockMvc mockMvc;
    @Autowired StockCommandService stockCommandService;
    @Autowired InvestorDailyService investorDailyService;

    @BeforeEach
    void setUp() {
        stockCommandService.insertIfAbsent(List.of(
                new Stock(TICKER, "KR7005930003", MarketType.KOSPI,
                        LocalDate.of(1975, 6, 11), "주권", "보통주")));
    }

    @Nested
    @DisplayName("GET /stocks")
    class GetStocks {

        @Test
        @DisplayName("인증 없으면 401")
        void shouldReturn401WhenUnauthenticated() throws Exception {
            mockMvc.perform(get("/stocks"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithApiUser
        @DisplayName("종목 목록에 시드 종목 포함")
        void shouldReturnStockListWhenAuthenticated() throws Exception {
            mockMvc.perform(get("/stocks"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[?(@.ticker == '" + TICKER + "')]").exists());
        }
    }

    @Nested
    @DisplayName("GET /stocks/{ticker}/detail")
    class GetDetail {

        @Test
        @WithApiUser
        @DisplayName("시드 종목 상세 반환")
        void shouldReturnDetailForExistingStock() throws Exception {
            mockMvc.perform(get("/stocks/" + TICKER + "/detail"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.ticker").value(TICKER));
        }

        @Test
        @WithApiUser
        @DisplayName("없는 종목이면 404")
        void shouldReturn404WhenDetailForUnknownStock() throws Exception {
            mockMvc.perform(get("/stocks/000000/detail"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /stocks/{ticker}/events")
    class GetEvents {

        @Test
        @WithApiUser
        @DisplayName("데이터 없으면 빈 배열")
        void shouldReturnEmptyEventsWhenNone() throws Exception {
            mockMvc.perform(get("/stocks/" + TICKER + "/events"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").isEmpty());
        }
    }

    @Nested
    @DisplayName("GET /stocks/{ticker}/prices")
    class GetPrices {

        @Test
        @WithApiUser
        @DisplayName("주가 데이터 없으면 빈 배열")
        void shouldReturnEmptyPricesWhenNoData() throws Exception {
            mockMvc.perform(get("/stocks/" + TICKER + "/prices")
                            .param("source", "KRX"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").isEmpty());
        }

        @Test
        @WithApiUser
        @DisplayName("KRX source로 없는 종목이면 404")
        void shouldReturn404WhenPricesForUnknownStock() throws Exception {
            mockMvc.perform(get("/stocks/000000/prices")
                            .param("source", "KRX"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /stocks/{ticker}/indicators")
    class GetIndicators {

        @Test
        @WithApiUser
        @DisplayName("지표 데이터 없으면 빈 배열")
        void shouldReturnEmptyIndicatorsWhenNoData() throws Exception {
            mockMvc.perform(get("/stocks/" + TICKER + "/indicators")
                            .param("source", "KRX")
                            .param("types", "SMA_5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").isEmpty());
        }
    }

    @Nested
    @DisplayName("GET /stocks/{ticker}/investor-flow")
    class GetInvestorFlow {

        @Test
        @DisplayName("인증 없으면 401")
        void shouldReturn401WhenUnauthenticated() throws Exception {
            mockMvc.perform(get("/stocks/" + TICKER + "/investor-flow")
                            .param("source", "KRX"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithApiUser
        @DisplayName("데이터 없으면 빈 배열")
        void shouldReturnEmptyWhenNoData() throws Exception {
            mockMvc.perform(get("/stocks/" + TICKER + "/investor-flow")
                            .param("source", "KRX"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").isEmpty());
        }

        @Test
        @WithApiUser
        @DisplayName("시드 데이터를 날짜 내림차순으로 반환")
        void shouldReturnInvestorFlowOrderedByDateDesc() throws Exception {
            // 005930은 KOSPI — source=KRX 파라미터가 내부적으로 KOSPI로 resolve됨
            investorDailyService.saveAll(List.of(
                    new InvestorDaily(StockExchange.KOSPI, TICKER, LocalDate.of(2024, 1, 1),
                            800L, 900L, 150L, 200L, 100L, 50L),
                    new InvestorDaily(StockExchange.KOSPI, TICKER, LocalDate.of(2024, 1, 2),
                            1000L, 500L, 200L, 100L, 300L, 150L)
            ));

            mockMvc.perform(get("/stocks/" + TICKER + "/investor-flow")
                            .param("source", "KRX"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].date").value("2024-01-02"))
                    .andExpect(jsonPath("$[0].individualNet").value(500))
                    .andExpect(jsonPath("$[0].institutionNet").value(100))
                    .andExpect(jsonPath("$[0].foreignNet").value(150))
                    .andExpect(jsonPath("$[1].date").value("2024-01-01"))
                    .andExpect(jsonPath("$[1].individualNet").value(-100));
        }
    }
}
