package com.dove.portfolio.domain.converter;

import com.dove.portfolio.domain.value.RebalancePlanCash;
import com.dove.portfolio.domain.value.RebalancePlanConfig;
import com.dove.portfolio.domain.value.RebalancePlanEntry;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.ArrayList;
import java.util.List;

/**
 * PORTFOLIO_REBALANCE_PLAN.ENTRIES JSON ↔ RebalancePlanConfig 변환기 — 구형 배열(센티넬) 저장분은 읽을 때 흡수한다.
 */
@Converter
public class RebalancePlanConfigConverter implements AttributeConverter<RebalancePlanConfig, String> {

    private static final ObjectMapper MAPPER = com.dove.jpa.JsonSupport.MAPPER;
    private static final TypeReference<List<RebalancePlanEntry>> LEGACY_TYPE = new TypeReference<>() {};
    private static final String LEGACY_SLOTS = "__SLOTS__";
    private static final String LEGACY_PARTRATE = "__PARTRATE__";
    private static final String LEGACY_CASH = "__CASH__";
    private static final int DEFAULT_SLOTS = 8;
    private static final double DEFAULT_PARTRATE = 10;

    @Override
    public String convertToDatabaseColumn(RebalancePlanConfig attribute) {
        RebalancePlanConfig config = attribute == null
                ? new RebalancePlanConfig(DEFAULT_SLOTS, DEFAULT_PARTRATE, List.of(), List.of())
                : attribute;
        try {
            return MAPPER.writeValueAsString(config);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("RebalancePlanConfig 직렬화 실패", e);
        }
    }

    @Override
    public RebalancePlanConfig convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return new RebalancePlanConfig(DEFAULT_SLOTS, DEFAULT_PARTRATE, List.of(), List.of());
        }
        try {
            // 구형 배열([...])은 센티넬을 분리해 설정으로 흡수, 신형은 객체({...})로 직접 역직렬화.
            return dbData.trim().startsWith("[") ? fromLegacyArray(dbData) : MAPPER.readValue(dbData, RebalancePlanConfig.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("RebalancePlanConfig 역직렬화 실패: " + dbData, e);
        }
    }

    /** 구형 배열(센티넬 __SLOTS__/__PARTRATE__/__CASH__ 혼합)을 설정 객체로 변환한다. */
    private RebalancePlanConfig fromLegacyArray(String dbData) throws JsonProcessingException {
        List<RebalancePlanEntry> raw = MAPPER.readValue(dbData, LEGACY_TYPE);
        int slots = DEFAULT_SLOTS;
        double partRate = DEFAULT_PARTRATE;
        List<RebalancePlanEntry> positions = new ArrayList<>();
        List<RebalancePlanCash> cash = new ArrayList<>();
        for (RebalancePlanEntry e : raw) {
            switch (e.symbol()) {
                case LEGACY_SLOTS -> slots = Math.max(1, (int) Math.round(e.targetPct()));
                case LEGACY_PARTRATE -> partRate = e.targetPct();
                case LEGACY_CASH -> cash.add(new RebalancePlanCash(e.account(), e.currency(), e.targetPct()));
                default -> positions.add(e);
            }
        }
        return new RebalancePlanConfig(slots, partRate, positions, cash);
    }
}
