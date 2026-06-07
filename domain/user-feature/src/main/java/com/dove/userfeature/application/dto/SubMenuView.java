package com.dove.userfeature.application.dto;

import com.dove.userfeature.domain.enums.SubMenuCode;

/**
 * 하위 메뉴 항목.
 *
 * @param subMenuCode 하위 메뉴 코드
 */
public record SubMenuView(SubMenuCode subMenuCode) {}
