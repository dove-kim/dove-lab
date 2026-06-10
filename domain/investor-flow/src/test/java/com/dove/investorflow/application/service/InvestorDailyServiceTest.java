package com.dove.investorflow.application.service;

import com.dove.investorflow.domain.entity.InvestorDaily;
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

    private InvestorDaily daily(String code, LocalDate date,
                                long indBuy, long indSell,
                                long instBuy, long instSell,
                                long forBuy, long forSell) {
        return new InvestorDaily(code, date,
                indBuy, indSell, instBuy, instSell, forBuy, forSell);
    }

    @Nested
    @DisplayName("findByCodeAndDate")
    class FindByCodeAndDate {

        @Test
        @DisplayName("저장한 데이터를 종목코드·날짜로 조회한다")
        void shouldReturnDataWhenExists() {
            service.saveAll(List.of(
                    daily("005930", LocalDate.of(2024, 1, 1),
                            100L, 50L, 200L, 100L, 300L, 150L)));

            Optional<InvestorDaily> result =
                    service.findByCodeAndDate("005930", LocalDate.of(2024, 1, 1));

            assertThat(result).isPresent();
            assertThat(result.get().individualNet()).isEqualTo(50L);
            assertThat(result.get().institutionNet()).isEqualTo(100L);
            assertThat(result.get().foreignNet()).isEqualTo(150L);
        }

        @Test
        @DisplayName("없는 날짜는 empty를 반환한다")
        void shouldReturnEmptyWhenNotExists() {
            Optional<InvestorDaily> result =
                    service.findByCodeAndDate("005930", LocalDate.of(2024, 1, 1));

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByCodeAndDateRange")
    class FindByCodeAndDateRange {

        @Test
        @DisplayName("날짜 범위 내 데이터를 거래일 오름차순으로 반환한다")
        void shouldReturnDataInAscOrderWithinRange() {
            service.saveAll(List.of(
                    daily("005930", LocalDate.of(2024, 1, 1), 1, 0, 1, 0, 1, 0),
                    daily("005930", LocalDate.of(2024, 1, 3), 1, 0, 1, 0, 1, 0),
                    daily("005930", LocalDate.of(2024, 1, 2), 1, 0, 1, 0, 1, 0)));

            List<InvestorDaily> result = service.findByCodeAndDateRange(
                    "005930", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 3));

            assertThat(result).hasSize(3);
            assertThat(result.get(0).getTradeDate()).isEqualTo(LocalDate.of(2024, 1, 1));
            assertThat(result.get(2).getTradeDate()).isEqualTo(LocalDate.of(2024, 1, 3));
        }

        @Test
        @DisplayName("범위 밖 날짜는 포함하지 않는다")
        void shouldExcludeDatesOutsideRange() {
            service.saveAll(List.of(
                    daily("005930", LocalDate.of(2023, 12, 31), 1, 0, 1, 0, 1, 0),
                    daily("005930", LocalDate.of(2024, 1, 1), 1, 0, 1, 0, 1, 0),
                    daily("005930", LocalDate.of(2024, 1, 5), 1, 0, 1, 0, 1, 0)));

            List<InvestorDaily> result = service.findByCodeAndDateRange(
                    "005930", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 3));

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getTradeDate()).isEqualTo(LocalDate.of(2024, 1, 1));
        }

        @Test
        @DisplayName("다른 종목 데이터는 포함하지 않는다")
        void shouldNotIncludeOtherStock() {
            service.saveAll(List.of(
                    daily("005930", LocalDate.of(2024, 1, 1), 1, 0, 1, 0, 1, 0),
                    daily("000660", LocalDate.of(2024, 1, 1), 1, 0, 1, 0, 1, 0)));

            List<InvestorDaily> result = service.findByCodeAndDateRange(
                    "005930", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 1));

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getStockCode()).isEqualTo("005930");
        }

        @Test
        @DisplayName("데이터가 없으면 빈 리스트를 반환한다")
        void shouldReturnEmptyWhenNoData() {
            List<InvestorDaily> result = service.findByCodeAndDateRange(
                    "005930", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31));

            assertThat(result).isEmpty();
        }
    }
}
