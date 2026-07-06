package com.dove.indicator.infrastructure.repository;

import com.dove.indicator.domain.entity.StockFeatureDaily;
import com.dove.indicator.domain.entity.StockFeatureDailyId;
import com.dove.indicator.domain.repository.StockFeatureDailyRepository;
import com.dove.jpa.QuerydslConfiguration;
import com.dove.stock.domain.enums.PriceType;
import com.dove.stock.domain.enums.StockExchange;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * StockFeatureDailyRepositorySupport 통합 테스트(H2) — 순위 청크용 구간 조회 검증.
 */
@DataJpaTest
@Import({StockFeatureDailyRepositorySupport.class, QuerydslConfiguration.class})
class StockFeatureDailyRepositorySupportTest {

    private static final PriceType PT = PriceType.RAW;

    @Autowired StockFeatureDailyRepositorySupport support;
    @Autowired StockFeatureDailyRepository repository;

    private void save(String ticker, StockExchange ex, LocalDate date) {
        StockFeatureDailyId id = new StockFeatureDailyId(ticker, ex, PT, date);
        repository.save(new StockFeatureDaily(id, 1, 100L, 110L, 90L, 100L, 1000L, 5000L, LocalDateTime.now()));
    }

    @Test
    @DisplayName("구간 [from,to]의 universe 거래소 행만 거래일 오름차순으로 반환한다")
    void shouldReturnRowsInDateRangeAscendingWithinExchanges() {
        save("A", StockExchange.KOSPI, LocalDate.of(2024, 1, 3));
        save("B", StockExchange.KOSDAQ, LocalDate.of(2024, 1, 1));
        save("A", StockExchange.KOSPI, LocalDate.of(2024, 1, 5));   // to(1/4) 초과 → 제외
        save("C", StockExchange.NXT, LocalDate.of(2024, 1, 2));     // universe 밖 거래소 → 제외

        List<StockFeatureDaily> rows = support.findByExchangesAndPriceTypeAndDateBetween(
                List.of(StockExchange.KOSPI, StockExchange.KOSDAQ), PT,
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 4));

        assertThat(rows).extracting(r -> r.getId().getTradeDate())
                .containsExactly(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 3));
    }
}
