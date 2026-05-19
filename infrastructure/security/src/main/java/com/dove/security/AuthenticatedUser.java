package com.dove.security;

/**
 * 인증된 사용자 정보. SecurityContextHolder의 principal로 저장된다.
 *
 * <p>JwtFilter가 토큰에서 추출한 클레임을 이 record로 wrap하여 SecurityContext에 박는다.
 * 컨트롤러는 {@code @AuthenticationPrincipal AuthenticatedUser user}로 받아서
 * {@code user.memberId()}, {@code user.username()}, {@code user.role()}을 사용한다.
 *
 * <p>이 패턴의 이점은 매 요청마다 username으로 DB를 조회하지 않고 토큰 검증 한 번으로
 * 모든 인증 정보를 확보한다는 점이다 (cross-schema 조회 비용 0).
 */
public record AuthenticatedUser(Long memberId, String username, String role, boolean mustChangePassword) {}
