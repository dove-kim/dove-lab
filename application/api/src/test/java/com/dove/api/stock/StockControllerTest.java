package com.dove.api.stock;

import com.dove.api.TestApiApplication;
import com.dove.market.application.service.MarketTradingDateService;
import com.dove.market.domain.enums.MarketType;
import com.dove.security.JwtProvider;
import com.dove.stock.application.service.StockCommandService;
import com.dove.stock.domain.entity.Stock;
import com.dove.stock.application.service.DailyStockPriceCommandService;
import com.dove.stock.domain.entity.DailyStockPrice;
import com.dove.indicator.domain.entity.TechnicalIndicator;
import com.dove.indicator.domain.enums.IndicatorType;
import com.dove.indicator.domain.repository.TechnicalIndicatorRepository;
import com.dove.user.application.service.MemberProfileCommandService;
import com.dove.user.domain.entity.MemberProfile;
import com.dove.user.domain.entity.MemberRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = TestApiApplication.class)
@AutoConfigureMockMvc
@Transactional
class StockControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired MemberProfileCommandService memberProfileCommandService;
    @Autowired StockCommandService stockCommandService;
    @Autowired DailyStockPriceCommandService priceCommandService;
    @Autowired MarketTradingDateService tradingDateService;
    @Autowired TechnicalIndicatorRepository indicatorRepository;
    @Autowired JwtProvider jwtProvider;

    private String userToken;

    @BeforeEach
    void setUp() {
        MemberProfile user = memberProfileCommandService.save(
                MemberProfile.create("u@u.com", "유저", MemberRole.USER));
        userToken = jwtProvider.generate(user.getId(), "user1", user.getName(), user.getRole().name(), false, false);
    }

    @Test
    void shouldReturn401WhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/stocks"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnEmptyListWhenNoStocks() throws Exception {
        mockMvc.perform(get("/stocks")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void shouldReturnAllStocks() throws Exception {
        stockCommandService.save(new Stock(MarketType.KOSPI, "005930", "삼성전자",
                LocalDate.of(2000, 1, 2), null));
        stockCommandService.save(new Stock(MarketType.KOSPI, "000660", "SK하이닉스",
                LocalDate.of(2000, 1, 2), null));

        mockMvc.perform(get("/stocks")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void shouldReturn401WhenNotAuthenticatedForPrices() throws Exception {
        mockMvc.perform(get("/stocks/005930/prices")
                        .param("market", "KOSPI"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn401WhenNotAuthenticatedForIndicators() throws Exception {
        mockMvc.perform(get("/stocks/005930/indicators")
                        .param("market", "KOSPI")
                        .param("types", "SMA_5"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn400WhenInvalidMarketForPrices() throws Exception {
        mockMvc.perform(get("/stocks/005930/prices")
                        .param("market", "INVALID_MARKET")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnPricesInAscendingDateOrder() throws Exception {
        // getPriceBars 는 LocalDate.now() 기준 limit*5일 범위로 조회하므로 최근 날짜 사용
        LocalDate d1 = LocalDate.now().minusDays(2);
        LocalDate d2 = LocalDate.now().minusDays(1);
        tradingDateService.upsert(MarketType.KOSPI, d1, true);
        tradingDateService.upsert(MarketType.KOSPI, d2, true);
        priceCommandService.save(new DailyStockPrice(MarketType.KOSPI, "005930", d2, 1000L, 51000L, 52000L, 50000L, 53000L));
        priceCommandService.save(new DailyStockPrice(MarketType.KOSPI, "005930", d1, 2000L, 50000L, 51000L, 49000L, 52000L));

        mockMvc.perform(get("/stocks/005930/prices")
                        .param("market", "KOSPI")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].date").value(d1.toString()))
                .andExpect(jsonPath("$[1].date").value(d2.toString()));
    }

    @Test
    void shouldReturn400WhenInvalidMarketForIndicators() throws Exception {
        mockMvc.perform(get("/stocks/005930/indicators")
                        .param("market", "GARBAGE")
                        .param("types", "MA5")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnEmptyIndicatorsForUnknownTypes() throws Exception {
        mockMvc.perform(get("/stocks/005930/indicators")
                        .param("market", "KOSPI")
                        .param("types", "UNKNOWN_TYPE")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void shouldReturnIndicatorsInAscendingDateOrder() throws Exception {
        LocalDate d1 = LocalDate.of(2024, 1, 2);
        LocalDate d2 = LocalDate.of(2024, 1, 3);
        indicatorRepository.save(new TechnicalIndicator(MarketType.KOSPI, "005930", d2, IndicatorType.SMA_5, 51000.0));
        indicatorRepository.save(new TechnicalIndicator(MarketType.KOSPI, "005930", d1, IndicatorType.SMA_5, 50000.0));

        mockMvc.perform(get("/stocks/005930/indicators")
                        .param("market", "KOSPI")
                        .param("types", "SMA_5")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].date").value(d1.toString()))
                .andExpect(jsonPath("$[0].values.SMA_5").value(50000.0))
                .andExpect(jsonPath("$[1].date").value(d2.toString()));
    }
}
