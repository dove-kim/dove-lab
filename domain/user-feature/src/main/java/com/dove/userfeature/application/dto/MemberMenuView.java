package com.dove.userfeature.application.dto;

import java.util.List;

/**
 * 사용자 메뉴 조회 결과 — 모듈 트리.
 *
 * @param modules 표시 순서대로 정렬된 모듈 목록
 */
public record MemberMenuView(List<ModuleView> modules) {}
