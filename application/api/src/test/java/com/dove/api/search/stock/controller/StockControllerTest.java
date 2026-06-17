package com.dove.api.search.stock.controller;

import com.dove.api.TestApiApplication;
import com.dove.api.support.WithApiUser;
import com.dove.investorflow.application.service.InvestorDailyService;
import com.dove.investorflow.domain.entity.InvestorDaily;
import com.dove.market.domain.enums.MarketType;
import com.dove.stock.application.service.StockCommandService;
import com.dove.stock.domain.entity.Stock;

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
        @WithApiUser(capabilities = {"STOCK_VIEW"})
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
        @WithApiUser(capabilities = {"STOCK_VIEW"})
        @DisplayName("시드 종목 상세 반환")
        void shouldReturnDetailForExistingStock() throws Exception {
            mockMvc.perform(get("/stocks/" + TICKER + "/detail"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.ticker").value(TICKER));
        }

        @Test
        @WithApiUser(capabilities = {"STOCK_VIEW"})
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
        @WithApiUser(capabilities = {"STOCK_VIEW"})
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
        @WithApiUser(capabilities = {"STOCK_VIEW"})
        @DisplayName("주가 데이터 없으면 빈 배열")
        void shouldReturnEmptyPricesWhenNoData() throws Exception {
            mockMvc.perform(get("/stocks/" + TICKER + "/prices")
                            .param("source", "KRX"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").isEmpty());
        }

        @Test
        @WithApiUser(capabilities = {"STOCK_VIEW"})
        @DisplayName("없는 종목이면 404")
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
        @WithApiUser(capabilities = {"STOCK_VIEW"})
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
                            .param("from", "2024-01-01")
                            .param("to", "2024-01-31"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithApiUser(capabilities = {"STOCK_VIEW"})
        @DisplayName("데이터 없으면 빈 배열")
        void shouldReturnEmptyWhenNoData() throws Exception {
            mockMvc.perform(get("/stocks/" + TICKER + "/investor-flow")
                            .param("from", "2024-01-01")
                            .param("to", "2024-01-31"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").isEmpty());
        }

        @Test
        @WithApiUser(capabilities = {"STOCK_VIEW"})
        @DisplayName("날짜 범위 내 데이터를 거래일 오름차순으로 반환")
        void shouldReturnInvestorFlowAscendingByDate() throws Exception {
            investorDailyService.saveAll(List.of(
                    new InvestorDaily(TICKER, LocalDate.of(2024, 1, 1),
                            800L, 900L, 150L, 200L, 100L, 50L),
                    new InvestorDaily(TICKER, LocalDate.of(2024, 1, 2),
                            1000L, 500L, 200L, 100L, 300L, 150L)
            ));

            mockMvc.perform(get("/stocks/" + TICKER + "/investor-flow")
                            .param("from", "2024-01-01")
                            .param("to", "2024-01-31"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].date").value("2024-01-01"))
                    .andExpect(jsonPath("$[0].individualNet").value(-100))
                    .andExpect(jsonPath("$[1].date").value("2024-01-02"))
                    .andExpect(jsonPath("$[1].individualNet").value(500))
                    .andExpect(jsonPath("$[1].institutionNet").value(100))
                    .andExpect(jsonPath("$[1].foreignNet").value(150));
        }

        @Test
        @WithApiUser(capabilities = {"STOCK_VIEW"})
        @DisplayName("범위 밖 날짜 데이터는 포함하지 않음")
        void shouldExcludeDataOutsideDateRange() throws Exception {
            investorDailyService.saveAll(List.of(
                    new InvestorDaily(TICKER, LocalDate.of(2023, 12, 31),
                            100L, 50L, 50L, 50L, 50L, 50L),
                    new InvestorDaily(TICKER, LocalDate.of(2024, 1, 15),
                            1000L, 500L, 200L, 100L, 300L, 150L)
            ));

            mockMvc.perform(get("/stocks/" + TICKER + "/investor-flow")
                            .param("from", "2024-01-01")
                            .param("to", "2024-01-31"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].date").value("2024-01-15"));
        }
    }
}
