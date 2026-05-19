package com.dove.user.domain.entity;

/**
 * 회원 권한 enum.
 *
 * <p>인증 도메인의 일부가 아닌 사용자 신원의 속성으로 보고 domain/user에 둔다.
 * domain/auth는 이를 단방향 import할 수 있다 (예: InviteCode가 발급 대상 ROLE 보유).
 */
public enum MemberRole {
    USER,
    ADMIN,        // 사용자 관리 권한 + 앱 기능 사용
    ROOT          // 최초 계정. 시스템 설정·어드민 관리 + 앱 기능 사용
}
