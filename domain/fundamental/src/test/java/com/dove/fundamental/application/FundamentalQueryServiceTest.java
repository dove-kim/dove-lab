package com.dove.fundamental.application;

import com.dove.fundamental.domain.entity.StockFundamental;
import com.dove.fundamental.domain.enums.FinancialStatementDiv;
import com.dove.fundamental.domain.repository.StockFundamentalRepository;
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

/**
 * FundamentalQueryService 통합 테스트(H2) — 실공시(비-F) 우선 멱등성 검증.
 */
@DataJpaTest
@Import(FundamentalQueryService.class)
class FundamentalQueryServiceTest {

    private static final String TICKER = "012450";
    private static final FinancialStatementDiv CFS = FinancialStatementDiv.CFS;
    private static final LocalDate BASE = LocalDate.of(2026, 12, 31);

    @Autowired FundamentalQueryService service;
    @Autowired StockFundamentalRepository repository;

    private void save(String rceptNo, short year, String report, LocalDate rceptDt, Long revenue) {
        repository.save(StockFundamental.builder()
                .rceptNo(rceptNo).fsDiv(CFS).ticker(TICKER).corpCode("00126380")
                .fiscalYear(year).reportCode(report).rceptDt(rceptDt).amendment(false)
                .revenue(revenue).grossProfit(revenue).netIncome(revenue)
                .totalEquity(1000L).totalAsset(2000L)
                .build());
    }

    @Nested
    @DisplayName("실공시 우선(findOriginal / findLatestOriginal)")
    class RealPreferred {

        @Test
        @DisplayName("같은 (연도·보고서)에 실공시와 백필F가 함께면 findOriginal은 실공시를 반환한다")
        void shouldReturnRealNotBackfillWhenBothExist() {
            save("F001263802025A", (short) 2025, "11011", LocalDate.of(2026, 4, 1), null);   // 백필: 합성공시일 늦음·매출 null
            save("20260316001112", (short) 2025, "11011", LocalDate.of(2026, 3, 16), 9999L); // 실공시: 공시일 이름·매출 있음

            Optional<StockFundamental> r = service.findOriginal(TICKER, CFS, (short) 2025, "11011", BASE);

            assertThat(r).isPresent();
            assertThat(r.get().getRceptNo()).isEqualTo("20260316001112");
            assertThat(r.get().getRevenue()).isEqualTo(9999L);
        }

        @Test
        @DisplayName("findLatestOriginal도 실공시를 우선한다")
        void shouldPreferRealInLatestOriginal() {
            save("F001263802025A", (short) 2025, "11011", LocalDate.of(2026, 4, 1), null);
            save("20260316001112", (short) 2025, "11011", LocalDate.of(2026, 3, 16), 9999L);

            Optional<StockFundamental> r = service.findLatestOriginal(TICKER, CFS, BASE);

            assertThat(r).isPresent();
            assertThat(r.get().getRceptNo()).isEqualTo("20260316001112");
        }

        @Test
        @DisplayName("실공시가 없으면 백필F로 폴백한다")
        void shouldFallBackToBackfillWhenNoReal() {
            save("F001263802013A", (short) 2013, "11011", LocalDate.of(2014, 4, 1), 5000L);

            Optional<StockFundamental> r = service.findOriginal(TICKER, CFS, (short) 2013, "11011", BASE);

            assertThat(r).isPresent();
            assertThat(r.get().getRceptNo()).isEqualTo("F001263802013A");
            assertThat(r.get().getRevenue()).isEqualTo(5000L);
        }
    }

    @Nested
    @DisplayName("화면 목록 중복 제거(findStatements)")
    class DisplayDedup {

        @Test
        @DisplayName("실공시 있는 (연도·보고서)의 백필F는 목록에서 제외하고, 실공시 없는 백필F는 유지한다")
        void shouldDropBackfillWhenRealExistsButKeepBackfillOtherwise() {
            save("F001263802025A", (short) 2025, "11011", LocalDate.of(2026, 4, 1), null);   // 실공시 있음 → 제외
            save("20260316001112", (short) 2025, "11011", LocalDate.of(2026, 3, 16), 9999L); // 실공시
            save("F001263802013A", (short) 2013, "11011", LocalDate.of(2014, 4, 1), 5000L);  // 실공시 없음 → 유지

            List<StockFundamental> rows = service.findStatements(TICKER);

            assertThat(rows).extracting(StockFundamental::getRceptNo)
                    .containsExactlyInAnyOrder("20260316001112", "F001263802013A");
        }
    }
}
