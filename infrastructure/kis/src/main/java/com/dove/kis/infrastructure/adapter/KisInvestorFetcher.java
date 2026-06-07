package com.dove.kis.infrastructure.adapter;

import com.dove.kis.infrastructure.client.KisStockClient;
import com.dove.kis.infrastructure.client.dto.KisInvestorResponse;
import com.dove.kis.quota.KisGate;
import com.dove.stockcollection.application.port.InvestorDailyRow;
import com.dove.stockcollection.application.port.InvestorFetcher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * KIS 종목별 투자자매매동향 어댑터 (FHKST01010900).
 */
@Component
@RequiredArgsConstructor
public class KisInvestorFetcher implements InvestorFetcher {

    private static final String TR_ID = "FHKST01010900";
    // FHKST01010900은 마켓 코드(J/NX/UN)에 무관하게 동일한 통합 데이터를 반환한다.
    private static final String MARKET_CODE = "J";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.BASIC_ISO_DATE;

    private final KisStockClient kisStockClient;
    private final KisGate kisGate;

    /**
     * 기간 내 투자자매매동향을 조회한다. 실패·데이터 없으면 빈 리스트.
     */
    @Override
    public List<InvestorDailyRow> fetch(String ticker, LocalDate from, LocalDate to) {
        KisInvestorResponse resp = kisGate.call(
                () -> kisStockClient.getInvestorTrend(
                        TR_ID, MARKET_CODE, ticker,
                        from.format(DATE_FMT), to.format(DATE_FMT)));
        if (resp == null || !resp.isSuccess() || resp.getDataList() == null) return List.of();
        return resp.getDataList().stream()
                .map(o -> new InvestorDailyRow(
                        LocalDate.parse(o.getTradingDate(), DATE_FMT),
                        o.getIndividualBuyVolLong(), o.getIndividualSellVolLong(),
                        o.getInstitutionBuyVolLong(), o.getInstitutionSellVolLong(),
                        o.getForeignBuyVolLong(), o.getForeignSellVolLong()))
                .toList();
    }
}
