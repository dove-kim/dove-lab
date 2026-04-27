package com.dove.stockbatch.producer;

import com.dove.stockprice.application.dto.DailyStockPriceQuery;
import com.dove.market.domain.enums.MarketType;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest(properties = "krx.target-markets=KOSPI,KOSDAQ")
@DisplayName("DailyStockPriceProcessor 통합 테스트")
class DailyStockPriceProcessorIntegrationTest {

    private static final String TOPIC = DailyStockPriceQuery.TOPIC;
    private static final LocalDate PRETEND_TODAY = LocalDate.of(2026, 4, 21);
    private static final LocalDate EXPECTED_TARGET_DATE = LocalDate.of(2026, 4, 20);

    @TestConfiguration
    static class FixedClockConfig {
        @Bean
        @Primary
        public Clock clock() {
            return Clock.fixed(
                    PRETEND_TODAY.atStartOfDay(ZoneOffset.UTC).toInstant(),
                    ZoneId.of("UTC"));
        }
    }

    @Autowired
    private DailyStockPriceProcessor processor;

    @MockitoBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Test
    @DisplayName("KOSPI/KOSDAQ 설정 시 시장별로 어제 날짜의 PRICE 이벤트 발행")
    void shouldPublishPriceEventPerMarket() {
        processor.run();

        ArgumentCaptor<DailyStockPriceQuery> captor = ArgumentCaptor.forClass(DailyStockPriceQuery.class);
        verify(kafkaTemplate, times(2)).send(eq(TOPIC), any(String.class), captor.capture());

        Assertions.assertThat(captor.getAllValues())
                .extracting(DailyStockPriceQuery::getMarketType)
                .containsExactlyInAnyOrder(MarketType.KOSPI, MarketType.KOSDAQ);
        Assertions.assertThat(captor.getAllValues())
                .extracting(DailyStockPriceQuery::getBaseDate)
                .containsOnly(EXPECTED_TARGET_DATE);
    }
}
