package com.dove.api.member.admin.service;

import java.time.LocalDateTime;

/**
 * 회원 관리 화면용 조합 읽기 모델 (회원 신원 + 로그인 자격).
 */
public record MemberSummary(Long id, String name, String email, String username, String role,
                            LocalDateTime createdAt, LocalDateTime deletedAt) {
}
