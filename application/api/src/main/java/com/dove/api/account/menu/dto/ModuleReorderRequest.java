package com.dove.api.account.menu.dto;

import com.dove.userfeature.domain.enums.ModuleCode;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 모듈 표시 순서 변경 요청.
 *
 * @param modules 정렬된 모듈 코드 목록
 */
public record ModuleReorderRequest(@NotNull List<ModuleCode> modules) {
}
