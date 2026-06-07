package com.dove.auth.domain.enums;

/**
 * 회원 인가 권한 (USER &lt; ADMIN &lt; ROOT 계층, ordinal 비교).
 */
public enum MemberRole {
    USER,
    ADMIN,
    ROOT
}
