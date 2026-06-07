package com.dove.indicator.application.service;

import com.dove.indicator.application.exception.CursorRewoundException;
import com.dove.indicator.domain.entity.IndicatorCursor;
import com.dove.indicator.domain.repository.IndicatorCursorRepository;
import com.dove.indicator.infrastructure.repository.IndicatorCursorRepositorySupport;
import com.dove.jpa.QuerydslConfiguration;
import com.dove.stock.domain.enums.PriceType;
import com.dove.stock.domain.enums.StockExchange;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import({IndicatorCursorService.class,
        IndicatorCursorRepositorySupport.class, QuerydslConfiguration.class})
class IndicatorCursorServiceTest {

    private static final LocalDate EARLIEST = LocalDate.of(1985, 1, 1);
    private static final String TICKER = "005930";
    private static final StockExchange EX = StockExchange.KOSPI;
    private static final PriceType PT = PriceType.RAW;

    @Autowired IndicatorCursorService cursorService;
    @Autowired IndicatorCursorRepository repository;
    @Autowired TestEntityManager em;

    @Test
    @DisplayName("firstSaveDate — 커서 없으면(null) 1985-01-01, 있으면 cursor+1")
    void firstSaveDate() {
        assertThat(IndicatorCursor.firstSaveDate(null)).isEqualTo(EARLIEST);
        assertThat(IndicatorCursor.firstSaveDate(LocalDate.of(2024, 5, 30))).isEqualTo(LocalDate.of(2024, 5, 31));
    }

    @Test
    @DisplayName("advanceForwardCas — 커서 없으면(cursorExists=false) 생성 후 전진")
    void advanceForwardCasCreatesWhenAbsent() {
        cursorService.advanceForwardCas(TICKER, EX, PT, null, false, LocalDate.of(2024, 5, 30));

        assertThat(cursorDate()).isEqualTo(LocalDate.of(2024, 5, 30));
    }

    @Test
    @DisplayName("advanceForwardCas — expected와 일치하면 전진한다")
    void advanceForwardCasAdvancesWhenExpectedMatches() {
        seedCursor(TICKER, EX, PT, LocalDate.of(2024, 1, 1));

        cursorService.advanceForwardCas(TICKER, EX, PT, LocalDate.of(2024, 1, 1), true, LocalDate.of(2024, 6, 1));

        assertThat(cursorDate()).isEqualTo(LocalDate.of(2024, 6, 1));
    }

    @Test
    @DisplayName("advanceForwardCas — cursorDate가 null인 커서는 expected=null로 전진한다")
    void advanceForwardCasAdvancesWhenNullCursor() {
        repository.save(new IndicatorCursor(TICKER, EX, PT)); // cursorDate=null
        em.flush();
        em.clear();

        cursorService.advanceForwardCas(TICKER, EX, PT, null, true, LocalDate.of(2024, 6, 1));

        assertThat(cursorDate()).isEqualTo(LocalDate.of(2024, 6, 1));
    }

    @Test
    @DisplayName("advanceForwardCas — 그 사이 커서가 rewind되면(expected 불일치) 예외로 거부한다")
    void advanceForwardCasThrowsWhenRewound() {
        seedCursor(TICKER, EX, PT, LocalDate.of(2024, 12, 31));
        // 계산은 12/31 기준이었는데, 그 사이 수집이 3/15로 rewind
        cursorService.rewindExchangeBefore(EX, LocalDate.of(2024, 3, 15));

        assertThatThrownBy(() ->
                cursorService.advanceForwardCas(TICKER, EX, PT, LocalDate.of(2024, 12, 31), true,
                        LocalDate.of(2025, 1, 31)))
                .isInstanceOf(CursorRewoundException.class);

        assertThat(cursorDate()).isEqualTo(LocalDate.of(2024, 3, 14)); // rewound 유지
    }

    @Test
    @DisplayName("rewindExchangeBefore — 거래소 전체에서 변경일 이후 커서를 일괄로 직전까지 되돌린다")
    void rewindExchangeBeforeBulk() {
        seedCursor(TICKER, EX, PriceType.RAW, LocalDate.of(2024, 12, 31));
        seedCursor(TICKER, EX, PriceType.ADJUSTED, LocalDate.of(2024, 12, 31));
        seedCursor("000660", EX, PriceType.RAW, LocalDate.of(2024, 1, 10)); // 이미 과거 → 유지
        em.flush();
        em.clear();

        cursorService.rewindExchangeBefore(EX, LocalDate.of(2024, 3, 15));

        em.flush();
        em.clear();
        assertThat(repository.findByTickerAndExchangeAndPriceType(TICKER, EX, PriceType.RAW)
                .orElseThrow().getCursorDate()).isEqualTo(LocalDate.of(2024, 3, 14));
        assertThat(repository.findByTickerAndExchangeAndPriceType(TICKER, EX, PriceType.ADJUSTED)
                .orElseThrow().getCursorDate()).isEqualTo(LocalDate.of(2024, 3, 14));
        assertThat(repository.findByTickerAndExchangeAndPriceType("000660", EX, PriceType.RAW)
                .orElseThrow().getCursorDate()).isEqualTo(LocalDate.of(2024, 1, 10)); // 과거라 유지
    }

    @Test
    @DisplayName("clearAdjusted — ADJUSTED 커서만 삭제하고 RAW는 남긴다")
    void clearAdjustedDeletesOnlyAdjusted() {
        seedCursor(TICKER, EX, PriceType.RAW, LocalDate.of(2024, 1, 1));
        seedCursor(TICKER, EX, PriceType.ADJUSTED, LocalDate.of(2024, 1, 1));

        cursorService.clearAdjusted(TICKER, EX);

        em.flush();
        em.clear();
        assertThat(repository.findByTickerAndExchangeAndPriceType(TICKER, EX, PriceType.ADJUSTED)).isEmpty();
        assertThat(repository.findByTickerAndExchangeAndPriceType(TICKER, EX, PriceType.RAW)).isPresent();
    }

    /** 생성 경로(advanceForwardCas, cursorExists=false)로 커서를 시드한다. */
    private void seedCursor(String ticker, StockExchange exchange, PriceType priceType, LocalDate date) {
        cursorService.advanceForwardCas(ticker, exchange, priceType, null, false, date);
    }

    /** CAS는 QueryDSL 벌크 update라 영속성 컨텍스트를 우회 → DB 값을 보려면 flush 후 clear. */
    private LocalDate cursorDate() {
        em.flush();
        em.clear();
        return repository.findByTickerAndExchangeAndPriceType(TICKER, EX, PT).orElseThrow().getCursorDate();
    }
}
