package com.dove.api.account.menu.dto;

import com.dove.userfeature.domain.enums.ModuleCode;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ModuleReorderRequest(@NotNull List<ModuleCode> modules) {}
