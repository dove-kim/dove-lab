package com.dove.fundamental.application;

import com.dove.fundamental.domain.entity.StockFundamental;
import com.dove.fundamental.domain.enums.FinancialStatementDiv;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * FundamentalTtmService 단위 테스트.
 */
@ExtendWith(MockitoExtension.class)
class FundamentalTtmServiceTest {

    @Mock
    private FundamentalQueryService queryService;

    @InjectMocks
    private FundamentalTtmService service;

    private static final LocalDate DATE = LocalDate.of(2024, 12, 1);

    private StockFundamental.StockFundamentalBuilder cfs(short year, String reportCode) {
        return cfsFor("005930", year, reportCode);
    }

    private StockFundamental.StockFundamentalBuilder cfsFor(String ticker, short year, String reportCode) {
        return StockFundamental.builder()
                .rceptNo(ticker + year + reportCode).fsDiv(FinancialStatementDiv.CFS)
                .ticker(ticker).corpCode("C" + ticker).fiscalYear(year)
                .reportCode(reportCode).rceptDt(DATE.minusDays(10)).amendment(false);
    }

    @Nested
    @DisplayName("resolve — TTM 스냅샷 조립")
    class Resolve {

        @Test
        @DisplayName("최신이 사업보고서(연간)면 연간값을 그대로 쓰고 지배주주값을 우선한다")
        void shouldUseAnnualDirectlyWithControllingValues() {
            StockFundamental annual = cfs((short) 2023, "11011")
                    .netIncome(999L).netIncomeControlling(800L)
                    .revenue(1000L).grossProfit(300L)
                    .totalEquity(999L).equityControlling(700L).totalAsset(6000L).build();
            when(queryService.findLatestOriginal("005930", FinancialStatementDiv.CFS, DATE))
                    .thenReturn(Optional.of(annual));

            TtmFundamental t = service.resolve("005930", DATE).orElseThrow();

            assertThat(t.netIncome()).isEqualTo(800L);   // 지배주주순이익 우선
            assertThat(t.equity()).isEqualTo(700L);      // 지배주주지분 우선
            assertThat(t.revenue()).isEqualTo(1000L);
            assertThat(t.grossProfit()).isEqualTo(300L);
            assertThat(t.totalAsset()).isEqualTo(6000L);
            assertThat(t.latestRceptNo()).isEqualTo("005930202311011");
        }

        @Test
        @DisplayName("최신이 3분기면 TTM(최신누적+전년연간−전년동기)로 flow를 계산한다")
        void shouldComputeTtmWhenLatestIsQuarter() {
            StockFundamental q3y = cfs((short) 2024, "11014")
                    .netIncomeControlling(90L).revenue(900L).grossProfit(270L)
                    .equityControlling(700L).totalAsset(6000L).build();
            StockFundamental annualPrev = cfs((short) 2023, "11011")
                    .netIncomeControlling(100L).revenue(1200L).grossProfit(360L).build();
            StockFundamental q3Prev = cfs((short) 2023, "11014")
                    .netIncomeControlling(60L).revenue(800L).grossProfit(240L).build();
            when(queryService.findLatestOriginal("005930", FinancialStatementDiv.CFS, DATE))
                    .thenReturn(Optional.of(q3y));
            when(queryService.findOriginal("005930", FinancialStatementDiv.CFS, (short) 2023, "11011", DATE))
                    .thenReturn(Optional.of(annualPrev));
            when(queryService.findOriginal("005930", FinancialStatementDiv.CFS, (short) 2023, "11014", DATE))
                    .thenReturn(Optional.of(q3Prev));

            TtmFundamental t = service.resolve("005930", DATE).orElseThrow();

            assertThat(t.netIncome()).isEqualTo(90L + 100L - 60L);       // 130
            assertThat(t.revenue()).isEqualTo(900L + 1200L - 800L);      // 1300
            assertThat(t.grossProfit()).isEqualTo(270L + 360L - 240L);   // 390
            assertThat(t.equity()).isEqualTo(700L);                      // 최신 시점
            assertThat(t.totalAsset()).isEqualTo(6000L);
        }

        @Test
        @DisplayName("분기 최신인데 전년 자료가 없으면 빈 값(저장 안 함)")
        void shouldReturnEmptyWhenPriorMissingForQuarter() {
            StockFundamental q3y = cfs((short) 2024, "11014")
                    .netIncomeControlling(90L).equityControlling(700L).totalAsset(6000L).build();
            when(queryService.findLatestOriginal("005930", FinancialStatementDiv.CFS, DATE))
                    .thenReturn(Optional.of(q3y));
            when(queryService.findOriginal("005930", FinancialStatementDiv.CFS, (short) 2023, "11011", DATE))
                    .thenReturn(Optional.empty());
            when(queryService.findOriginal("005930", FinancialStatementDiv.CFS, (short) 2023, "11014", DATE))
                    .thenReturn(Optional.empty());
            // CFS가 TTM 불가 → 별도(OFS)로 폴백 시도(여기선 없음)
            when(queryService.findLatestOriginal("005930", FinancialStatementDiv.OFS, DATE))
                    .thenReturn(Optional.empty());

            assertThat(service.resolve("005930", DATE)).isEmpty();
        }

        @Test
        @DisplayName("연결(CFS)이 없으면 별도(OFS)로 조회하고 지배주주값 없으면 전체를 쓴다")
        void shouldFallBackToOfsWhenNoCfs() {
            StockFundamental ofsAnnual = StockFundamental.builder()
                    .rceptNo("O2023").fsDiv(FinancialStatementDiv.OFS)
                    .ticker("005930").corpCode("00126380").fiscalYear((short) 2023)
                    .reportCode("11011").rceptDt(DATE.minusDays(10)).amendment(false)
                    .netIncome(500L).totalEquity(4000L).revenue(1000L).grossProfit(300L).totalAsset(6000L).build();
            when(queryService.findLatestOriginal("005930", FinancialStatementDiv.CFS, DATE))
                    .thenReturn(Optional.empty());
            when(queryService.findLatestOriginal("005930", FinancialStatementDiv.OFS, DATE))
                    .thenReturn(Optional.of(ofsAnnual));

            TtmFundamental t = service.resolve("005930", DATE).orElseThrow();

            assertThat(t.netIncome()).isEqualTo(500L);   // 지배주주 없음 → 전체
            assertThat(t.equity()).isEqualTo(4000L);
        }
    }

    @Nested
    @DisplayName("resolveAll — 윈도우 벌크 조립")
    class ResolveAll {

        @Test
        @DisplayName("윈도우 로드분을 종목별로 묶어 요청 종목만 TTM으로 조립한다")
        void shouldAssembleRequestedTickersFromWindow() {
            StockFundamental annualA = cfsFor("000660", (short) 2023, "11011")
                    .netIncomeControlling(50L).revenue(500L).grossProfit(150L)
                    .equityControlling(400L).totalAsset(3000L).build();
            StockFundamental q3B = cfsFor("005930", (short) 2024, "11014")
                    .netIncomeControlling(90L).revenue(900L).grossProfit(270L)
                    .equityControlling(700L).totalAsset(6000L).build();
            StockFundamental annualBprev = cfsFor("005930", (short) 2023, "11011")
                    .netIncomeControlling(100L).revenue(1200L).grossProfit(360L).build();
            StockFundamental q3Bprev = cfsFor("005930", (short) 2023, "11014")
                    .netIncomeControlling(60L).revenue(800L).grossProfit(240L).build();
            when(queryService.findOriginalsInWindow(DATE.minusYears(3), DATE))
                    .thenReturn(List.of(annualA, q3B, annualBprev, q3Bprev));

            Map<String, TtmFundamental> result = service.resolveAll(Set.of("000660", "005930"), DATE);

            assertThat(result).containsOnlyKeys("000660", "005930");
            assertThat(result.get("000660").netIncome()).isEqualTo(50L);          // 연간 직접
            assertThat(result.get("005930").netIncome()).isEqualTo(90L + 100L - 60L);  // TTM
            assertThat(result.get("005930").equity()).isEqualTo(700L);            // 최신 시점
        }
    }
}
