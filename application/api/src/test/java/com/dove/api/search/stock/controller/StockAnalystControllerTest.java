package com.dove.api.search.stock.controller;

import com.dove.api.TestApiApplication;
import com.dove.api.support.WithApiUser;
import com.dove.stockcollection.application.port.AnalystFetcher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = TestApiApplication.class)
@AutoConfigureMockMvc
@Transactional
class StockAnalystControllerTest {

    private static final String TICKER = "005930";

    @Autowired MockMvc mockMvc;

    @MockitoBean AnalystFetcher analystFetcher;

    @Nested
    @DisplayName("GET /stocks/{ticker}/invest-opinion")
    class GetInvestOpinion {

        @Test
        @DisplayName("인증 없으면 401")
        void shouldReturn401WhenUnauthenticated() throws Exception {
            mockMvc.perform(get("/stocks/" + TICKER + "/invest-opinion"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithApiUser
        @DisplayName("STOCK_SEARCH 권한 없으면 403")
        void shouldReturn403WhenFeatureNotGranted() throws Exception {
            mockMvc.perform(get("/stocks/" + TICKER + "/invest-opinion"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithApiUser(features = {"STOCK_SEARCH"})
        @DisplayName("KIS 응답을 매핑해 반환")
        void shouldReturnInvestOpinionWhenGranted() throws Exception {
            given(analystFetcher.fetchInvestOpinion(eq(TICKER), any(LocalDate.class), any(LocalDate.class)))
                    .willReturn(List.<Map<String, Object>>of(Map.of(
                            "stck_bsop_date", "20260101",
                            "invt_opnn", "매수",
                            "rgbf_invt_opnn", "중립",
                            "mbcr_name", "도브증권",
                            "hts_goal_prc", "90000")));

            mockMvc.perform(get("/stocks/" + TICKER + "/invest-opinion"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].broker").value("도브증권"))
                    .andExpect(jsonPath("$[0].opinion").value("매수"));
        }

        @Test
        @WithApiUser(features = {"STOCK_SEARCH"})
        @DisplayName("KIS 데이터 없으면 빈 배열")
        void shouldReturnEmptyInvestOpinionWhenNoData() throws Exception {
            given(analystFetcher.fetchInvestOpinion(eq(TICKER), any(LocalDate.class), any(LocalDate.class)))
                    .willReturn(List.of());

            mockMvc.perform(get("/stocks/" + TICKER + "/invest-opinion"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").isEmpty());
        }
    }

    @Nested
    @DisplayName("GET /stocks/{ticker}/estimate")
    class GetEstimate {

        @Test
        @WithApiUser(features = {"STOCK_SEARCH"})
        @DisplayName("데이터 없으면 available=false")
        void shouldReturnEmptyEstimateWhenNoData() throws Exception {
            given(analystFetcher.fetchEstimate(TICKER)).willReturn(Optional.empty());

            mockMvc.perform(get("/stocks/" + TICKER + "/estimate"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.available").value(false));
        }

        @Test
        @WithApiUser
        @DisplayName("STOCK_SEARCH 권한 없으면 403")
        void shouldReturn403WhenEstimateFeatureNotGranted() throws Exception {
            mockMvc.perform(get("/stocks/" + TICKER + "/estimate"))
                    .andExpect(status().isForbidden());
        }
    }
}
