package com.dove.api.stock.filter.dto;

import java.time.LocalDate;
import java.util.List;

public record ExecuteFilterResponse(
        Long filterId,
        String filterName,
        LocalDate evaluationDate,
        String dateRule,
        List<String> markets,
        int totalCandidates,
        int matchCount,
        List<StockMatchResult> results
) {}
