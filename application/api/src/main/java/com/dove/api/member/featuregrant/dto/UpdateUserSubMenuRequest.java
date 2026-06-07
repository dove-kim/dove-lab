package com.dove.api.member.featuregrant.dto;

import com.dove.api.member.featuregrant.enums.GrantAction;
import com.dove.userfeature.domain.enums.SubMenuCode;
import jakarta.validation.constraints.NotNull;

/**
 * 사용자 서브메뉴 권한 변경 요청.
 *
 * @param subMenuCode 서브메뉴 코드
 * @param action      부여/회수 동작
 */
public record UpdateUserSubMenuRequest(
        @NotNull SubMenuCode subMenuCode,
        @NotNull GrantAction action
) {
}
