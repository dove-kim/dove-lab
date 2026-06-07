package com.dove.kis.infrastructure.adapter;

import com.dove.kis.infrastructure.client.KisStockClient;
import com.dove.kis.infrastructure.client.dto.KisEstimateResponse;
import com.dove.kis.infrastructure.client.dto.KisInvestOpinionResponse;
import com.dove.kis.quota.KisGate;
import com.dove.stockcollection.application.port.AnalystFetcher;
import com.dove.stockcollection.domain.model.AnalystEstimate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * KIS 애널리스트 정보 어댑터 (투자의견·추정실적). on-demand 조회 전용.
 */
@Component
@RequiredArgsConstructor
public class KisAnalystFetcher implements AnalystFetcher {

    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String TR_INVEST_OPINION = "FHKST663300C0";
    private static final String TR_ESTIMATE = "HHKST668300C0";

    private final KisStockClient client;
    private final KisGate gate;

    @Override
    public List<Map<String, Object>> fetchInvestOpinion(String ticker, LocalDate from, LocalDate to) {
        KisInvestOpinionResponse resp = gate.call(() -> client.getInvestOpinion(
                TR_INVEST_OPINION, "J", "16633", ticker, from.format(YMD), to.format(YMD)));
        if (resp == null || !resp.isSuccess() || resp.getOutput() == null) return List.of();
        return resp.getOutput();
    }

    @Override
    public Optional<AnalystEstimate> fetchEstimate(String ticker) {
        KisEstimateResponse resp = gate.call(() -> client.getEstimatePerform(TR_ESTIMATE, ticker));
        if (resp == null || !resp.isSuccess()) return Optional.empty();
        return Optional.of(new AnalystEstimate(
                resp.getOutput1(), resp.getOutput2(), resp.getOutput3(), resp.getOutput4()));
    }
}
