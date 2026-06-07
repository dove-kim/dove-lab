package com.dove.api.account.menu.dto;

import com.dove.userfeature.domain.enums.FeatureCode;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 기능 표시 순서 변경 요청.
 *
 * @param features 정렬된 기능 코드 목록
 */
public record FeatureReorderRequest(@NotNull List<FeatureCode> features) {
}
