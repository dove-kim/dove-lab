package com.dove.api.member.admin.dto;

import com.dove.api.member.admin.service.MemberSummary;

import java.time.LocalDateTime;

/**
 * 회원 요약 응답.
 *
 * @param id        회원 ID
 * @param name      표시 이름
 * @param email     이메일
 * @param username  아이디
 * @param role      권한 등급
 * @param createdAt 가입일시
 * @param deletedAt 탈퇴일시 (null이면 활성)
 */
public record MemberSummaryResponse(Long id, String name, String email, String username, String role,
                                    LocalDateTime createdAt, LocalDateTime deletedAt) {

    public static MemberSummaryResponse from(MemberSummary s) {
        return new MemberSummaryResponse(s.id(), s.name(), s.email(), s.username(), s.role(),
                s.createdAt(), s.deletedAt());
    }
}
