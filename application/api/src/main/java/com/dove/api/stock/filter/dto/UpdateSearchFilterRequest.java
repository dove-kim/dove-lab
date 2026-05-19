package com.dove.api.stock.filter.dto;

import com.dove.screening.domain.enums.DateRule;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateSearchFilterRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull DateRule dateRule,
        @NotEmpty List<String> markets,
        @NotBlank String expression,
        Long includeStockSetId,
        Long excludeStockSetId
) {}
