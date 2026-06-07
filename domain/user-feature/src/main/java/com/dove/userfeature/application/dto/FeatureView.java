package com.dove.userfeature.application.dto;

import com.dove.userfeature.domain.enums.FeatureCode;

import java.util.List;

/**
 * 기능 단위 메뉴 항목.
 *
 * @param featureCode  기능 코드
 * @param displayOrder 표시 순서
 * @param hidden       숨김 여부
 * @param subMenus     활성 하위 메뉴 목록
 */
public record FeatureView(FeatureCode featureCode, int displayOrder, boolean hidden, List<SubMenuView> subMenus) {}
