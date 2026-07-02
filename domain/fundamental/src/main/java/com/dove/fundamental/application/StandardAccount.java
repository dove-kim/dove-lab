package com.dove.fundamental.application;

/**
 * DART 전체재무제표의 표준계정코드(account_id) 상수 — 엔티티 컬럼 매핑 기준.
 */
public final class StandardAccount {

    private StandardAccount() {
    }

    public static final String REVENUE = "ifrs-full_Revenue";
    public static final String COST_OF_SALES = "ifrs-full_CostOfSales";
    public static final String GROSS_PROFIT = "ifrs-full_GrossProfit";
    public static final String OPERATING_INCOME = "dart_OperatingIncomeLoss";
    public static final String PROFIT_BEFORE_TAX = "ifrs-full_ProfitLossBeforeTax";
    public static final String NET_INCOME = "ifrs-full_ProfitLoss";
    public static final String NET_INCOME_CONTROLLING = "ifrs-full_ProfitLossAttributableToOwnersOfParent";

    public static final String TOTAL_ASSET = "ifrs-full_Assets";
    public static final String CURRENT_ASSET = "ifrs-full_CurrentAssets";
    public static final String NONCURRENT_ASSET = "ifrs-full_NoncurrentAssets";
    public static final String CASH_AND_EQUIVALENTS = "ifrs-full_CashAndCashEquivalents";
    public static final String INVENTORIES = "ifrs-full_Inventories";
    public static final String TOTAL_LIABILITY = "ifrs-full_Liabilities";
    public static final String CURRENT_LIABILITY = "ifrs-full_CurrentLiabilities";
    public static final String TOTAL_EQUITY = "ifrs-full_Equity";
    public static final String EQUITY_CONTROLLING = "ifrs-full_EquityAttributableToOwnersOfParent";
    public static final String ISSUED_CAPITAL = "ifrs-full_IssuedCapital";

    public static final String CF_OPERATING = "ifrs-full_CashFlowsFromUsedInOperatingActivities";
    public static final String CF_INVESTING = "ifrs-full_CashFlowsFromUsedInInvestingActivities";
    public static final String CF_FINANCING = "ifrs-full_CashFlowsFromUsedInFinancingActivities";
}
