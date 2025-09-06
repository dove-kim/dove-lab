package com.dove.stockbatch.dto;

import com.dove.stockdata.enums.MarketType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class KrxDailyStockDataRequestTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void DTO생성_정상적으로생성된다() {
        // given
        MarketType marketType = MarketType.KOSPI;
        LocalDate baseDate = LocalDate.of(2024, 1, 15);

        // when
        KrxDailyStockDataRequest request = new KrxDailyStockDataRequest(marketType, baseDate);

        // then
        assertThat(request.getMarketType()).isEqualTo(MarketType.KOSPI);
        assertThat(request.getBaseDate()).isEqualTo(LocalDate.of(2024, 1, 15));
        assertThat(request.getEventVersion()).isEqualTo(1);
    }

    @Test
    void JSON직렬화_올바른형태로변환된다() throws Exception {
        // given
        KrxDailyStockDataRequest request = new KrxDailyStockDataRequest(
                MarketType.KOSDAQ,
                LocalDate.of(2024, 1, 15)
        );

        // when
        String json = objectMapper.writeValueAsString(request);

        // then
        assertThat(json).contains("\"eventVersion\":1");
        assertThat(json).contains("\"marketType\":\"KOSDAQ\"");
        assertThat(json).contains("\"baseDate\":\"20240115\""); // yyyyMMdd 형식
    }

    @Test
    void JSON역직렬화_올바르게변환된다() throws Exception {
        // given
        String json = "{\"eventVersion\":1,\"marketType\":\"KOSPI\",\"baseDate\":\"20240115\"}";

        // when
        KrxDailyStockDataRequest request = objectMapper.readValue(json, KrxDailyStockDataRequest.class);

        // then
        assertThat(request.getEventVersion()).isEqualTo(1);
        assertThat(request.getMarketType()).isEqualTo(MarketType.KOSPI);
        assertThat(request.getBaseDate()).isEqualTo(LocalDate.of(2024, 1, 15));
    }
}