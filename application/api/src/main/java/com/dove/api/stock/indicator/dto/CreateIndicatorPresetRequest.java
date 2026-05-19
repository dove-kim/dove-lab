package com.dove.api.stock.indicator.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateIndicatorPresetRequest(
        @NotBlank String name,
        @NotNull JsonNode items,
        List<String> panelOrder
) {}
