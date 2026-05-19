package com.dove.api.admin.feature.dto;

import com.dove.userfeature.domain.enums.SubMenuCode;
import jakarta.validation.constraints.NotNull;

public record UpdateUserSubMenuRequest(
        @NotNull SubMenuCode subMenuCode,
        @NotNull GrantAction action
) {}
