package com.dove.userfeature.application.dto;

import com.dove.userfeature.domain.enums.ModuleCode;

import java.util.List;

/**
 * 모듈 단위 메뉴 항목.
 *
 * @param moduleCode   모듈 코드
 * @param displayOrder 표시 순서
 * @param hidden       숨김 여부
 * @param features     하위 기능 목록
 */
public record ModuleView(ModuleCode moduleCode, int displayOrder, boolean hidden, List<FeatureView> features) {}
