package com.dove.api.account.menu.dto;

/**
 * 메뉴 숨김 여부 설정 요청.
 *
 * @param hidden 숨김 여부
 */
public record SetHiddenRequest(boolean hidden) {
}
