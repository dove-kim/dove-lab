package com.dove.api.account.auth.dto;

import com.dove.auth.domain.enums.MemberRole;

/**
 * 로그인 처리 결과 (서비스 → 컨트롤러).
 *
 * @param accessToken        액세스 토큰
 * @param refreshToken       리프레시 토큰
 * @param memberId           회원 ID
 * @param username           아이디
 * @param name               표시 이름
 * @param role               권한 등급
 * @param rememberMe         로그인 상태 유지 여부
 * @param mustChangePassword 비밀번호 변경 강제 여부
 */
public record LoginResult(
        String accessToken,
        String refreshToken,
        Long memberId,
        String username,
        String name,
        MemberRole role,
        boolean rememberMe,
        boolean mustChangePassword
) {}
