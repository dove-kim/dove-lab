package com.dove.modelserving.application.service;

import com.dove.modelserving.application.port.DryRunSampleSource;
import com.dove.modelserving.domain.entity.MlModel;
import com.dove.modelserving.domain.meta.ModelMetaParser;
import com.dove.modelserving.domain.zone.EntryZone;
import com.dove.modelserving.domain.zone.EntryZoneParser;
import com.dove.modelserving.infrastructure.repository.ScoreSourceRepositorySupport;
import com.dove.modelserving.infrastructure.scorer.PredictRow;
import com.dove.stock.domain.enums.PriceType;
import com.dove.stock.domain.enums.StockExchange;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 나이트 채점과 동일한 방식으로 최근 진입존 행을 모아 드라이런 표본을 공급하는 표본 소스.
 */
@Component
@RequiredArgsConstructor
public class EntryZoneSampleSource implements DryRunSampleSource {

    /** 표본을 모으며 거슬러 올라가는 최대 거래일 수(진입존이 드물어도 무한 스캔을 막는 상한). */
    static final int MAX_SCAN_TRADE_DATES = 60;

    private final ModelMetaParser metaParser;
    private final EntryZoneParser entryZoneParser;
    private final ScoreSourceRepositorySupport sourceSupport;
    private final EntryZoneRowAssembler rowAssembler;

    @Override
    @Transactional(readOnly = true)
    public List<PredictRow> sample(MlModel model, int limit) {
        Set<StockExchange> members = model.getScoreExchanges();
        if (members.isEmpty()) return List.of();
        EntryZone zone = entryZoneParser.parse(metaParser.parse(model.getMetaJson()).entryZone());
        PriceType priceType = model.getScorePriceType();

        List<PredictRow> rows = new ArrayList<>();
        for (LocalDate date : sourceSupport.findRecentTradeDates(members, priceType, MAX_SCAN_TRADE_DATES)) {
            for (StockExchange member : members) {
                for (PredictRow row : rowAssembler.assemble(zone, member, priceType, date)) {
                    rows.add(row);
                    if (rows.size() >= limit) return rows;
                }
            }
        }
        return rows;
    }
}
