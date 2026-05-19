package com.dove.api.stock.set.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateStockSetRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull List<String> codes
) {}
