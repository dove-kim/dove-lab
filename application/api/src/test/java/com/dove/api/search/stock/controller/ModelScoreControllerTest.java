package com.dove.api.search.stock.controller;

import com.dove.api.TestApiApplication;
import com.dove.api.support.WithApiUser;
import com.dove.market.domain.enums.MarketType;
import com.dove.modelserving.domain.entity.MlModel;
import com.dove.modelserving.domain.entity.StockModelScore;
import com.dove.modelserving.domain.entity.StockModelScoreId;
import com.dove.modelserving.domain.enums.ModelOutputType;
import com.dove.modelserving.domain.repository.MlModelRepository;
import com.dove.modelserving.domain.repository.StockModelScoreRepository;
import com.dove.stock.application.service.StockCommandService;
import com.dove.stock.domain.entity.Stock;
import com.dove.stock.domain.enums.PriceType;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = TestApiApplication.class)
@AutoConfigureMockMvc
@Transactional
class ModelScoreControllerTest {

    private static final String TICKER = "005930";

    @Autowired MockMvc mockMvc;
    @Autowired StockCommandService stockCommandService;
    @Autowired MlModelRepository mlModelRepository;
    @Autowired StockModelScoreRepository scoreRepository;

    private Long modelId;

    @BeforeEach
    void setUp() {
        stockCommandService.insertIfAbsent(List.of(
                new Stock(TICKER, "KR7005930003", MarketType.KOSPI,
                        LocalDate.of(1975, 6, 11), "주권", "보통주")));
        MlModel model = mlModelRepository.save(MlModel.register(
                "swing_entry", "1.0.0", new byte[]{1}, "{}", ModelOutputType.PROBABILITY,
                Set.of(StockExchange.KOSPI, StockExchange.KOSDAQ), PriceType.ADJUSTED, "tester"));
        model.activate();
        modelId = mlModelRepository.save(model).getId();
    }

    private void seedScore(LocalDate date, float score) {
        scoreRepository.save(new StockModelScore(
                new StockModelScoreId(TICKER, StockExchange.INTEGRATED, PriceType.ADJUSTED, date, modelId),
                score, LocalDateTime.now()));
    }

    @Nested
    @DisplayName("GET /stocks/{ticker}/scores")
    class GetScores {

        @Test
        @DisplayName("인증 없으면 401")
        void shouldReturn401WhenUnauthenticated() throws Exception {
            mockMvc.perform(get("/stocks/" + TICKER + "/scores").param("modelId", modelId.toString()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithApiUser(capabilities = {"STOCK_VIEW"})
        @DisplayName("MODEL_SCORE 권한 없으면 403")
        void shouldReturn403WhenMissingCapability() throws Exception {
            mockMvc.perform(get("/stocks/" + TICKER + "/scores").param("modelId", modelId.toString()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithApiUser(capabilities = {"MODEL_SCORE"})
        @DisplayName("점수 데이터 없으면 빈 배열")
        void shouldReturnEmptyWhenNoData() throws Exception {
            mockMvc.perform(get("/stocks/" + TICKER + "/scores").param("modelId", modelId.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").isEmpty());
        }

        @Test
        @WithApiUser(capabilities = {"MODEL_SCORE"})
        @DisplayName("없는 종목이면 404")
        void shouldReturn404WhenUnknownStock() throws Exception {
            mockMvc.perform(get("/stocks/000000/scores").param("modelId", modelId.toString()))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithApiUser(capabilities = {"MODEL_SCORE"})
        @DisplayName("점수를 거래일 오름차순으로 반환")
        void shouldReturnScoresAscendingByDate() throws Exception {
            seedScore(LocalDate.of(2024, 1, 2), 0.7f);
            seedScore(LocalDate.of(2024, 1, 1), 0.3f);

            mockMvc.perform(get("/stocks/" + TICKER + "/scores").param("modelId", modelId.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].date").value("2024-01-01"))
                    .andExpect(jsonPath("$[0].score").value(0.3))
                    .andExpect(jsonPath("$[1].date").value("2024-01-02"))
                    .andExpect(jsonPath("$[1].score").value(0.7));
        }

        @Test
        @WithApiUser(capabilities = {"MODEL_SCORE"})
        @DisplayName("from·to 구간 밖 점수는 포함하지 않음")
        void shouldExcludeScoresOutsideDateRange() throws Exception {
            seedScore(LocalDate.of(2023, 12, 31), 0.1f);
            seedScore(LocalDate.of(2024, 1, 15), 0.5f);
            seedScore(LocalDate.of(2024, 2, 1), 0.9f);

            mockMvc.perform(get("/stocks/" + TICKER + "/scores")
                            .param("modelId", modelId.toString())
                            .param("from", "2024-01-01")
                            .param("to", "2024-01-31"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].date").value("2024-01-15"));
        }
    }

    @Nested
    @DisplayName("GET /stocks/models")
    class GetActiveModels {

        @Test
        @DisplayName("인증 없으면 401")
        void shouldReturn401WhenUnauthenticated() throws Exception {
            mockMvc.perform(get("/stocks/models"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithApiUser(capabilities = {"STOCK_VIEW"})
        @DisplayName("MODEL_SCORE 권한 없으면 403")
        void shouldReturn403WhenMissingCapability() throws Exception {
            mockMvc.perform(get("/stocks/models"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithApiUser(capabilities = {"MODEL_SCORE"})
        @DisplayName("활성 모델 목록에 시드 모델 포함")
        void shouldReturnActiveModels() throws Exception {
            mockMvc.perform(get("/stocks/models"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[?(@.id == " + modelId + ")]").exists())
                    .andExpect(jsonPath("$[?(@.id == " + modelId + ")].name").value("swing_entry"));
        }
    }
}
