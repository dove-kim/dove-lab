package com.dove.technicalindicator.application.service;

import com.dove.market.domain.enums.MarketType;
import com.dove.technicalindicator.TestConfiguration;
import com.dove.technicalindicator.domain.entity.IndicatorCursor;
import com.dove.technicalindicator.domain.entity.IndicatorCursorId;
import com.dove.technicalindicator.domain.enums.IndicatorType;
import com.dove.technicalindicator.domain.repository.IndicatorCursorRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ContextConfiguration(classes = TestConfiguration.class)
@Import({IndicatorCursorCommandService.class, IndicatorCursorQueryService.class,
        com.dove.technicalindicator.infrastructure.repository.IndicatorCursorQueryRepository.class,
        com.dove.jpa.QuerydslConfiguration.class})
class IndicatorCursorServiceTest {

    @Autowired
    private IndicatorCursorCommandService commandService;

    @Autowired
    private IndicatorCursorQueryService queryService;

    @Autowired
    private IndicatorCursorRepository repository;

    private static final MarketType MARKET = MarketType.KOSPI;
    private static final String CODE = "005930";
    private static final LocalDate INITIAL = LocalDate.of(2010, 1, 1);

    @Test
    @DisplayName("커서가 없으면 initialDate.minusDays(1)로 lazy 생성한다")
    void shouldCreateCursorWithInitialDateMinusOne() {
        IndicatorCursor cursor = commandService.getOrCreate(MARKET, CODE, IndicatorType.SMA_5, INITIAL);

        assertThat(cursor.getLastCalculatedDate()).isEqualTo(INITIAL.minusDays(1));
        assertThat(cursor.getId().getMarketType()).isEqualTo(MARKET);
        assertThat(cursor.getId().getCode()).isEqualTo(CODE);
        assertThat(cursor.getId().getIndicatorType()).isEqualTo(IndicatorType.SMA_5);
    }

    @Test
    @DisplayName("커서가 이미 있으면 기존 값을 반환한다")
    void shouldReturnExistingCursor() {
        commandService.getOrCreate(MARKET, CODE, IndicatorType.SMA_5, INITIAL);
        IndicatorCursor second = commandService.getOrCreate(MARKET, CODE, IndicatorType.SMA_5, INITIAL);

        assertThat(repository.count()).isEqualTo(1);
        assertThat(second.getLastCalculatedDate()).isEqualTo(INITIAL.minusDays(1));
    }

    @Test
    @DisplayName("(market, code)로 모든 calculator cursor를 조회한다")
    void shouldFindAllCursorsByMarketAndCode() {
        commandService.getOrCreate(MARKET, CODE, IndicatorType.SMA_5, INITIAL);
        commandService.getOrCreate(MARKET, CODE, IndicatorType.RSI_14, INITIAL);
        commandService.getOrCreate(MARKET, CODE, IndicatorType.MACD_LINE, INITIAL);

        List<IndicatorCursor> cursors = queryService.findAllByMarketAndCode(MARKET, CODE);

        assertThat(cursors).hasSize(3);
    }

    @Test
    @DisplayName("insertedDate <= lastCalculatedDate → insertedDate.minusDays(1)로 되감는다")
    void shouldRewindCursorWhenInsertedDateIsNotAfterLastCalculated() {
        LocalDate jan10 = LocalDate.of(2024, 1, 10);
        LocalDate jan5 = LocalDate.of(2024, 1, 5);

        repository.save(new IndicatorCursor(new IndicatorCursorId(MARKET, CODE, IndicatorType.SMA_5), jan10));

        commandService.rewindIfBefore(MARKET, CODE, IndicatorType.SMA_5, jan5);

        IndicatorCursor cursor = repository.findById(new IndicatorCursorId(MARKET, CODE, IndicatorType.SMA_5))
                .orElseThrow();
        assertThat(cursor.getLastCalculatedDate()).isEqualTo(jan5.minusDays(1));
    }

    @Test
    @DisplayName("insertedDate > lastCalculatedDate → 되감기 없음")
    void shouldNotRewindWhenInsertedDateIsAfterLastCalculated() {
        LocalDate jan5 = LocalDate.of(2024, 1, 5);
        LocalDate jan10 = LocalDate.of(2024, 1, 10);

        repository.save(new IndicatorCursor(new IndicatorCursorId(MARKET, CODE, IndicatorType.SMA_5), jan5));

        commandService.rewindIfBefore(MARKET, CODE, IndicatorType.SMA_5, jan10);

        IndicatorCursor cursor = repository.findById(new IndicatorCursorId(MARKET, CODE, IndicatorType.SMA_5))
                .orElseThrow();
        assertThat(cursor.getLastCalculatedDate()).isEqualTo(jan5);
    }
}
