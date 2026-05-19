package com.dove.indicator.infrastructure.repository;

import com.dove.market.domain.enums.MarketType;
import com.dove.indicator.TestConfiguration;
import com.dove.indicator.domain.entity.TechnicalIndicator;
import com.dove.indicator.domain.enums.IndicatorType;
import com.dove.indicator.domain.repository.TechnicalIndicatorRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
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

    private void save(String code, LocalDate tradeDate, IndicatorType type, double value) {
        technicalIndicatorRepository.save(new TechnicalIndicator(MARKET, code, tradeDate, type, value));
    }

    private void saveObv(LocalDate tradeDate, double value) {
        save(CODE, tradeDate, IndicatorType.OBV, value);
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

    // ─── findAllByMarketsAndDate ──────────────────────────────────────────

    @Test
    @DisplayName("findAllByMarketsAndDate — 해당 날짜 지표 코드별로 집계")
    void shouldReturnIndicatorsByCodeForDate() {
        LocalDate date = LocalDate.of(2024, 1, 3);
        save("005930", date, IndicatorType.SMA_5, 100.0);
        save("005930", date, IndicatorType.EMA_5, 101.0);
        save("000660", date, IndicatorType.SMA_5, 200.0);

        Map<String, Map<IndicatorType, Double>> result =
                queryRepository.findAllByMarketsAndDate(List.of(MARKET), date);

        assertThat(result).containsKey("005930");
        assertThat(result.get("005930")).containsEntry(IndicatorType.SMA_5, 100.0)
                .containsEntry(IndicatorType.EMA_5, 101.0);
        assertThat(result.get("000660")).containsEntry(IndicatorType.SMA_5, 200.0);
    }

    @Test
    @DisplayName("findAllByMarketsAndDate — 날짜 불일치 시 빈 맵")
    void shouldReturnEmptyMapWhenDateMismatch() {
        save(CODE, LocalDate.of(2024, 1, 2), IndicatorType.SMA_5, 100.0);

        Map<String, Map<IndicatorType, Double>> result =
                queryRepository.findAllByMarketsAndDate(List.of(MARKET), LocalDate.of(2024, 1, 3));

        assertThat(result).isEmpty();
    }

    // ─── findRecentByStock ────────────────────────────────────────────────

    @Test
    @DisplayName("findRecentByStock — limit 개수만큼 최신 날짜 반환")
    void shouldReturnRecentIndicatorsUpToLimit() {
        save(CODE, LocalDate.of(2024, 1, 1), IndicatorType.SMA_5, 100.0);
        save(CODE, LocalDate.of(2024, 1, 2), IndicatorType.SMA_5, 200.0);
        save(CODE, LocalDate.of(2024, 1, 3), IndicatorType.SMA_5, 300.0);

        Map<LocalDate, Map<IndicatorType, Double>> result =
                queryRepository.findRecentByStock(MARKET, CODE, List.of(IndicatorType.SMA_5), 2);

        assertThat(result).hasSize(2);
        assertThat(result).containsKey(LocalDate.of(2024, 1, 3));
        assertThat(result).containsKey(LocalDate.of(2024, 1, 2));
        assertThat(result).doesNotContainKey(LocalDate.of(2024, 1, 1));
    }

    @Test
    @DisplayName("findRecentByStock — 데이터 없으면 빈 맵")
    void shouldReturnEmptyMapWhenNoIndicators() {
        Map<LocalDate, Map<IndicatorType, Double>> result =
                queryRepository.findRecentByStock(MARKET, CODE, List.of(IndicatorType.SMA_5), 5);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findRecentByStock — types 필터: 요청한 지표만 포함")
    void shouldFilterByRequestedIndicatorTypes() {
        LocalDate date = LocalDate.of(2024, 1, 3);
        save(CODE, date, IndicatorType.SMA_5, 100.0);
        save(CODE, date, IndicatorType.EMA_5, 200.0);

        Map<LocalDate, Map<IndicatorType, Double>> result =
                queryRepository.findRecentByStock(MARKET, CODE, List.of(IndicatorType.SMA_5), 5);

        assertThat(result.get(date)).containsOnlyKeys(IndicatorType.SMA_5);
    }
}
