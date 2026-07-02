package com.dove.fundamental.application;

import com.dove.fundamental.domain.entity.StockFundamental;
import com.dove.fundamental.domain.enums.FinancialStatementDiv;

import java.time.LocalDate;
import java.util.Map;

/**
 * 표준계정코드→금액 맵과 메타데이터로 STOCK_FUNDAMENTAL 엔티티를 조립한다.
 */
public final class FundamentalFactory {

    private FundamentalFactory() {
    }

    /**
     * 재무제표 계정 맵으로 엔티티를 만든다. 매출총이익이 없으면 매출−매출원가로 보완한다.
     */
    public static StockFundamental fromAccounts(
            String ticker, String corpCode, short fiscalYear, String reportCode,
            String rceptNo, LocalDate rceptDt, FinancialStatementDiv fsDiv, boolean amendment,
            Map<String, Long> accounts, Long commonShares) {

        Long revenue = accounts.get(StandardAccount.REVENUE);
        Long costOfSales = accounts.get(StandardAccount.COST_OF_SALES);
        Long grossProfit = accounts.get(StandardAccount.GROSS_PROFIT);
        if (grossProfit == null && revenue != null && costOfSales != null) {
            grossProfit = revenue - costOfSales;
        }

        return StockFundamental.builder()
                .rceptNo(rceptNo)
                .fsDiv(fsDiv)
                .ticker(ticker)
                .corpCode(corpCode)
                .fiscalYear(fiscalYear)
                .reportCode(reportCode)
                .rceptDt(rceptDt)
                .amendment(amendment)
                .revenue(revenue)
                .costOfSales(costOfSales)
                .grossProfit(grossProfit)
                .operatingIncome(accounts.get(StandardAccount.OPERATING_INCOME))
                .profitBeforeTax(accounts.get(StandardAccount.PROFIT_BEFORE_TAX))
                .netIncome(accounts.get(StandardAccount.NET_INCOME))
                .netIncomeControlling(accounts.get(StandardAccount.NET_INCOME_CONTROLLING))
                .totalAsset(accounts.get(StandardAccount.TOTAL_ASSET))
                .currentAsset(accounts.get(StandardAccount.CURRENT_ASSET))
                .noncurrentAsset(accounts.get(StandardAccount.NONCURRENT_ASSET))
                .cashAndEquivalents(accounts.get(StandardAccount.CASH_AND_EQUIVALENTS))
                .inventories(accounts.get(StandardAccount.INVENTORIES))
                .totalLiability(accounts.get(StandardAccount.TOTAL_LIABILITY))
                .currentLiability(accounts.get(StandardAccount.CURRENT_LIABILITY))
                .totalEquity(accounts.get(StandardAccount.TOTAL_EQUITY))
                .equityControlling(accounts.get(StandardAccount.EQUITY_CONTROLLING))
                .issuedCapital(accounts.get(StandardAccount.ISSUED_CAPITAL))
                .cashFlowOperating(accounts.get(StandardAccount.CF_OPERATING))
                .cashFlowInvesting(accounts.get(StandardAccount.CF_INVESTING))
                .cashFlowFinancing(accounts.get(StandardAccount.CF_FINANCING))
                .commonShares(commonShares)
                .build();
    }
}
