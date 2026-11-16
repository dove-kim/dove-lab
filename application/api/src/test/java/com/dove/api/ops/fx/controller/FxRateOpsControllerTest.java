package com.dove.api.ops.fx.controller;

import com.dove.api.TestApiApplication;
import com.dove.api.support.WithApiUser;
import com.dove.portfolio.application.port.FxQuote;
import com.dove.portfolio.application.port.FxRatePort;
import com.dove.portfolio.application.port.OverseasPricePort;
import com.dove.portfolio.domain.entity.PortfolioFxRate;
import com.dove.portfolio.domain.repository.PortfolioFxRateRepository;
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
import java.util.List;

import static org.hamcrest.Matchers.contains;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = TestApiApplication.class)
@AutoConfigureMockMvc
@Transactional
class FxRateOpsControllerTest {

    @Autowired MockMvc mockMvc;

    @Autowired PortfolioFxRateRepository fxRateRepository;

    @MockitoBean FxRatePort fxRatePort;

    @MockitoBean OverseasPricePort overseasPricePort;

    @Nested
    @DisplayName("GET /admin/ops/fx")
    class GetOverview {

        @Test
        @DisplayName("인증 없으면 401")
        void shouldReturn401WhenUnauthenticated() throws Exception {
            mockMvc.perform(get("/admin/ops/fx"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithApiUser(role = "ADMIN")
        @DisplayName("ROOT가 아니면 403")
        void shouldReturn403WhenNotRoot() throws Exception {
            mockMvc.perform(get("/admin/ops/fx"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithApiUser(role = "ROOT")
        @DisplayName("ROOT면 통화별 현황과 최근 고시를 반환한다")
        void shouldReturnCoverageWhenRoot() throws Exception {
            fxRateRepository.save(PortfolioFxRate.create("USD", new BigDecimal("1335.62"), LocalDate.of(2024, 3, 1)));
            fxRateRepository.save(PortfolioFxRate.create("USD", new BigDecimal("1332.23"), LocalDate.of(2024, 3, 4)));

            mockMvc.perform(get("/admin/ops/fx"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.backfillStartDate").isNotEmpty())
                    .andExpect(jsonPath("$.coverages").isArray())
                    .andExpect(jsonPath("$.coverages[?(@.currency == 'USD')].rowCount").value(contains(2)))
                    .andExpect(jsonPath("$.coverages[?(@.currency == 'USD')].latestDate").value(contains("2024-03-04")))
                    .andExpect(jsonPath("$.recent[0].rateDate").value("2024-03-04"));
        }

        @Test
        @WithApiUser(role = "ROOT")
        @DisplayName("이력이 없는 통화도 행수 0으로 포함한다")
        void shouldIncludeCurrencyWithoutHistory() throws Exception {
            mockMvc.perform(get("/admin/ops/fx"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.coverages[?(@.currency == 'CNY')].rowCount").value(contains(0)));
        }
    }

    @Nested
    @DisplayName("POST /admin/ops/fx/refresh")
    class Refresh {

        @Test
        @WithApiUser(role = "USER")
        @DisplayName("ROOT가 아니면 403")
        void shouldReturn403WhenNotRoot() throws Exception {
            mockMvc.perform(post("/admin/ops/fx/refresh"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithApiUser(role = "ROOT")
        @DisplayName("받아온 환율과 종가 행수를 반환한다")
        void shouldReturnSavedCount() throws Exception {
            when(fxRatePort.fetchSeriesToKrw(eq("USD"), any(), any())).thenReturn(List.of(
                    new FxQuote("USD", new BigDecimal("1335.62"), LocalDate.of(2024, 3, 1)),
                    new FxQuote("USD", new BigDecimal("1332.23"), LocalDate.of(2024, 3, 4))));

            mockMvc.perform(post("/admin/ops/fx/refresh"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.savedRates").value(2))
                    .andExpect(jsonPath("$.savedQuotes").value(0));
        }
    }
}
