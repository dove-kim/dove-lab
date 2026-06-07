package com.dove.investorflow.application.service;

import com.dove.investorflow.domain.entity.InvestorDaily;
import com.dove.stock.domain.enums.StockExchange;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(InvestorDailyService.class)
class InvestorDailyServiceTest {

    @Autowired
    InvestorDailyService service;

    private InvestorDaily daily(StockExchange exchange, String code, LocalDate date,
                                long indBuy, long indSell,
                                long instBuy, long instSell,
                                long forBuy, long forSell) {
        return new InvestorDaily(exchange, code, date,
                indBuy, indSell, instBuy, instSell, forBuy, forSell);
    }

    @Nested
    @DisplayName("findBySourceAndCodeAndDate")
    class FindBySourceAndCodeAndDate {

        @Test
        @DisplayName("저장한 데이터를 거래소·코드·날짜로 조회한다")
        void shouldReturnDataWhenExists() {
            service.saveAll(List.of(
                    daily(StockExchange.KOSPI, "005930", LocalDate.of(2024, 1, 1),
                            100L, 50L, 200L, 100L, 300L, 150L)));

            Optional<InvestorDaily> result = service.findBySourceAndCodeAndDate(
                    StockExchange.KOSPI, "005930", LocalDate.of(2024, 1, 1));

            assertThat(result).isPresent();
            assertThat(result.get().individualNet()).isEqualTo(50L);
            assertThat(result.get().institutionNet()).isEqualTo(100L);
            assertThat(result.get().foreignNet()).isEqualTo(150L);
        }

        @Test
        @DisplayName("없는 날짜는 empty를 반환한다")
        void shouldReturnEmptyWhenNotExists() {
            Optional<InvestorDaily> result = service.findBySourceAndCodeAndDate(
                    StockExchange.KOSPI, "005930", LocalDate.of(2024, 1, 1));

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("같은 종목이라도 다른 거래소면 조회되지 않는다")
        void shouldReturnEmptyWhenExchangeDiffers() {
            service.saveAll(List.of(
                    daily(StockExchange.KOSPI, "005930", LocalDate.of(2024, 1, 1),
                            100L, 50L, 200L, 100L, 300L, 150L)));

            Optional<InvestorDaily> result = service.findBySourceAndCodeAndDate(
                    StockExchange.KOSDAQ, "005930", LocalDate.of(2024, 1, 1));

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findRecent")
    class FindRecent {

        @Test
        @DisplayName("거래일 내림차순으로 최대 limit건 반환한다")
        void shouldReturnRecentInDescOrderUpToLimit() {
            service.saveAll(List.of(
                    daily(StockExchange.KOSPI, "005930", LocalDate.of(2024, 1, 1), 1, 0, 1, 0, 1, 0),
                    daily(StockExchange.KOSPI, "005930", LocalDate.of(2024, 1, 3), 1, 0, 1, 0, 1, 0),
                    daily(StockExchange.KOSPI, "005930", LocalDate.of(2024, 1, 2), 1, 0, 1, 0, 1, 0)));

            List<InvestorDaily> result = service.findRecent(StockExchange.KOSPI, "005930", 2);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getTradeDate()).isEqualTo(LocalDate.of(2024, 1, 3));
            assertThat(result.get(1).getTradeDate()).isEqualTo(LocalDate.of(2024, 1, 2));
        }

        @Test
        @DisplayName("다른 거래소 데이터는 포함하지 않는다")
        void shouldNotIncludeOtherExchange() {
            service.saveAll(List.of(
                    daily(StockExchange.KOSPI, "005930", LocalDate.of(2024, 1, 1), 1, 0, 1, 0, 1, 0),
                    daily(StockExchange.KOSDAQ, "005930", LocalDate.of(2024, 1, 1), 1, 0, 1, 0, 1, 0)));

            List<InvestorDaily> result = service.findRecent(StockExchange.KOSPI, "005930", 10);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getExchange()).isEqualTo(StockExchange.KOSPI);
        }

        @Test
        @DisplayName("다른 종목 데이터는 포함하지 않는다")
        void shouldNotIncludeOtherStock() {
            service.saveAll(List.of(
                    daily(StockExchange.KOSPI, "005930", LocalDate.of(2024, 1, 1), 1, 0, 1, 0, 1, 0),
                    daily(StockExchange.KOSPI, "000660", LocalDate.of(2024, 1, 1), 1, 0, 1, 0, 1, 0)));

            List<InvestorDaily> result = service.findRecent(StockExchange.KOSPI, "005930", 10);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getStockCode()).isEqualTo("005930");
        }

        @Test
        @DisplayName("데이터가 없으면 빈 리스트를 반환한다")
        void shouldReturnEmptyWhenNoData() {
            List<InvestorDaily> result = service.findRecent(StockExchange.KOSPI, "005930", 10);

            assertThat(result).isEmpty();
        }
    }
}
