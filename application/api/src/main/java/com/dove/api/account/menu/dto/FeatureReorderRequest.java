package com.dove.api.account.menu.dto;

import com.dove.userfeature.domain.enums.FeatureCode;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record FeatureReorderRequest(@NotNull List<FeatureCode> features) {}
