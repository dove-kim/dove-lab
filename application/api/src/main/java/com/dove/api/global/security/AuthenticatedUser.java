package com.dove.api.global.security;

import java.util.Set;

/**
 * 인증된 사용자 정보. SecurityContext 의 principal 로 저장된다.
 *
 * @param memberId 회원 식별자
 * @param username 로그인 아이디
 * @param role 권한 등급
 * @param mustChangePassword 비밀번호 변경 강제 여부
 * @param grantedFeatures 부여된 기능 코드(FeatureCode.name()) 집합
 */
public record AuthenticatedUser(
        Long memberId,
        String username,
        String role,
        boolean mustChangePassword,
        Set<String> grantedFeatures
) {
    public AuthenticatedUser {
        grantedFeatures = grantedFeatures == null ? Set.of() : Set.copyOf(grantedFeatures);
    }
}
