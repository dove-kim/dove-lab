package com.dove.api.stock.market;

import com.dove.api.TestApiApplication;
import com.dove.market.domain.enums.MarketType;
import com.dove.security.JwtProvider;
import com.dove.stock.application.service.DailyStockPriceCommandService;
import com.dove.stock.domain.entity.DailyStockPrice;
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
class MarketControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired MemberProfileCommandService memberProfileCommandService;
    @Autowired DailyStockPriceCommandService priceCommandService;
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
        mockMvc.perform(get("/market/trading-days"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnEmptyTradingDaysWhenNoData() throws Exception {
        mockMvc.perform(get("/market/trading-days")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tradingDays").isArray())
                .andExpect(jsonPath("$.tradingDays").isEmpty());
    }

    @Test
    void shouldReturnTradingDaysDescending() throws Exception {
        LocalDate d1 = LocalDate.of(2024, 1, 2);
        LocalDate d2 = LocalDate.of(2024, 1, 3);
        LocalDate d3 = LocalDate.of(2024, 1, 4);
        priceCommandService.save(new DailyStockPrice(MarketType.KOSPI, "005930", d1, 100L, 50000L, 51000L, 49000L, 52000L));
        priceCommandService.save(new DailyStockPrice(MarketType.KOSPI, "005930", d2, 200L, 51000L, 52000L, 50000L, 53000L));
        priceCommandService.save(new DailyStockPrice(MarketType.KOSPI, "005930", d3, 300L, 52000L, 53000L, 51000L, 54000L));

        mockMvc.perform(get("/market/trading-days")
                        .param("limit", "3")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tradingDays.length()").value(3))
                .andExpect(jsonPath("$.latestDate").value(d3.toString()))
                .andExpect(jsonPath("$.tradingDays[0]").value(d3.toString()))
                .andExpect(jsonPath("$.tradingDays[2]").value(d1.toString()));
    }

    @Test
    void shouldRespectLimitParameter() throws Exception {
        for (int i = 1; i <= 5; i++) {
            LocalDate d = LocalDate.of(2024, 1, i + 1);
            priceCommandService.save(new DailyStockPrice(MarketType.KOSPI, "005930", d, 100L, 50000L, 51000L, 49000L, 52000L));
        }

        mockMvc.perform(get("/market/trading-days")
                        .param("limit", "2")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tradingDays.length()").value(2));
    }
}
