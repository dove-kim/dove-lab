package com.dove.technicalindicator.infrastructure.repository;

import com.dove.market.domain.enums.MarketType;
import com.dove.technicalindicator.TestConfiguration;
import com.dove.technicalindicator.domain.entity.TechnicalIndicator;
import com.dove.technicalindicator.domain.enums.IndicatorType;
import com.dove.technicalindicator.domain.repository.TechnicalIndicatorRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ContextConfiguration(classes = TestConfiguration.class)
@Import({TechnicalIndicatorQueryRepository.class, com.dove.jpa.QuerydslConfiguration.class})
class TechnicalIndicatorQueryRepositoryTest {

    @Autowired
    private TechnicalIndicatorRepository technicalIndicatorRepository;

    @Autowired
    private TechnicalIndicatorQueryRepository queryRepository;

    private static final MarketType MARKET = MarketType.KOSPI;
    private static final String CODE = "005930";

    private void saveObv(LocalDate tradeDate, double value) {
        technicalIndicatorRepository.save(
                new TechnicalIndicator(MARKET, CODE, tradeDate, IndicatorType.OBV, value));
    }

    @Test
    @DisplayName("범위 내 여러 OBV 행 중 최신 날짜 것을 반환한다")
    void shouldReturnLatestObvValueInRange() {
        LocalDate jan1 = LocalDate.of(2024, 1, 1);
        LocalDate jan3 = LocalDate.of(2024, 1, 3);
        LocalDate jan5 = LocalDate.of(2024, 1, 5);

        saveObv(jan1, 1000.0);
        saveObv(jan3, 3000.0);
        saveObv(jan5, 5000.0);

        Optional<Double> result = queryRepository.findLatestObvValue(
                MARKET, CODE,
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 1, 5));

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(3000.0);
    }

    @Test
    @DisplayName("범위 밖이면 empty를 반환한다")
    void shouldReturnEmptyWhenNoObvInRange() {
        saveObv(LocalDate.of(2024, 1, 10), 9000.0);

        Optional<Double> result = queryRepository.findLatestObvValue(
                MARKET, CODE,
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 1, 5));

        assertThat(result).isEmpty();
    }
}
