package com.dove.technicalindicator.infrastructure.repository;

import com.dove.market.domain.enums.MarketType;
import com.dove.stock.domain.entity.Stock;
import com.dove.stock.domain.entity.StockId;
import com.dove.stock.domain.enums.TradingStatus;
import com.dove.technicalindicator.TestConfiguration;
import com.dove.technicalindicator.domain.entity.IndicatorCursor;
import com.dove.technicalindicator.domain.entity.IndicatorCursorId;
import com.dove.technicalindicator.domain.enums.IndicatorType;
import com.dove.technicalindicator.domain.repository.IndicatorCursorRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ContextConfiguration(classes = TestConfiguration.class)
@Import({IndicatorCursorQueryRepository.class, com.dove.jpa.QuerydslConfiguration.class})
class IndicatorCursorQueryRepositoryTest {

    @Autowired private TestEntityManager em;
    @Autowired private IndicatorCursorRepository cursorRepository;
    @Autowired private IndicatorCursorQueryRepository queryRepository;

    private static final MarketType MARKET = MarketType.KOSPI;
    private static final LocalDate LATEST = LocalDate.of(2026, 4, 17);

    private void saveStock(String code, TradingStatus status) {
        em.persistAndFlush(new Stock(new StockId(MARKET, code), code + "종목", status));
    }

    private void saveCursor(String code, IndicatorType type, LocalDate date) {
        cursorRepository.save(new IndicatorCursor(new IndicatorCursorId(MARKET, code, type), date));
    }

    @Test
    @DisplayName("ACTIVE 종목의 stale cursor → 반환")
    void shouldReturnActiveStockWithStaleCursor() {
        saveStock("005930", TradingStatus.ACTIVE);
        saveCursor("005930", IndicatorType.SMA_5, LATEST.minusDays(1));

        List<String> result = queryRepository.findEligibleStockCodes(MARKET);

        assertThat(result).containsExactly("005930");
    }

    @Test
    @DisplayName("SUSPENDED 종목의 stale cursor → 반환")
    void shouldReturnSuspendedStockWithStaleCursor() {
        saveStock("000660", TradingStatus.SUSPENDED);
        saveCursor("000660", IndicatorType.SMA_5, LATEST.minusDays(2));

        List<String> result = queryRepository.findEligibleStockCodes(MARKET);

        assertThat(result).containsExactly("000660");
    }

    @Test
    @DisplayName("DELISTED 종목 → 반환 안 함")
    void shouldNotReturnDelistedStock() {
        saveStock("999999", TradingStatus.DELISTED);
        saveCursor("999999", IndicatorType.SMA_5, LATEST.minusDays(1));

        List<String> result = queryRepository.findEligibleStockCodes(MARKET);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("cursor up-to-date인 ACTIVE 종목도 반환 (신규 지표 대비)")
    void shouldReturnActiveStockEvenWhenCursorIsUpToDate() {
        saveStock("005930", TradingStatus.ACTIVE);
        saveCursor("005930", IndicatorType.SMA_5, LATEST);

        List<String> result = queryRepository.findEligibleStockCodes(MARKET);

        assertThat(result).containsExactly("005930");
    }

    @Test
    @DisplayName("cursor 없는 ACTIVE 종목 → 반환 (신규 지표 추가 시 자동 처리)")
    void shouldReturnActiveStockWithNoCursor() {
        saveStock("005930", TradingStatus.ACTIVE);

        List<String> result = queryRepository.findEligibleStockCodes(MARKET);

        assertThat(result).containsExactly("005930");
    }

    @Test
    @DisplayName("같은 종목에 cursor 여러 개여도 distinct로 1건만 반환")
    void shouldReturnDistinctCodeForMultipleCursors() {
        saveStock("005930", TradingStatus.ACTIVE);
        saveCursor("005930", IndicatorType.SMA_5, LATEST.minusDays(1));
        saveCursor("005930", IndicatorType.RSI_14, LATEST.minusDays(2));

        List<String> result = queryRepository.findEligibleStockCodes(MARKET);

        assertThat(result).hasSize(1).containsExactly("005930");
    }
}
