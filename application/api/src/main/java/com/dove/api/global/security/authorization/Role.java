package com.dove.api.global.security.authorization;

/**
 * api 레이어 권한 등급 enum (USER &lt; ADMIN &lt; ROOT).
 */
public enum Role {
    USER,
    ADMIN,
    ROOT;

    /**
     * 이 등급이 요구 등급 이상인지 여부를 반환한다.
     */
    public boolean isAtLeast(Role required) {
        return this.ordinal() >= required.ordinal();
    }

    /**
     * 문자열을 권한 등급으로 변환한다.
     *
     * @throws IllegalArgumentException 값이 null이거나 알 수 없는 등급일 때
     */
    public static Role parse(String value) {
        if (value == null) {
            throw new IllegalArgumentException("ROLE_NULL");
        }
        return Role.valueOf(value);
    }
}
