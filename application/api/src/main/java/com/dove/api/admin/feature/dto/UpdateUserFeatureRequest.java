package com.dove.api.admin.feature.dto;

import com.dove.userfeature.domain.enums.FeatureCode;
import jakarta.validation.constraints.NotNull;

public record UpdateUserFeatureRequest(
        @NotNull FeatureCode featureCode,
        @NotNull GrantAction action
) {}
