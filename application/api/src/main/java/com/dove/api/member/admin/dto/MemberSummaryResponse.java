package com.dove.api.member.admin.dto;

import com.dove.api.member.admin.service.MemberSummary;

/**
 * 회원 요약 응답.
 *
 * @param id       회원 ID
 * @param name     표시 이름
 * @param email    이메일
 * @param username 아이디
 * @param role     권한 등급
 */
public record MemberSummaryResponse(Long id, String name, String email, String username, String role) {

    public static MemberSummaryResponse from(MemberSummary s) {
        return new MemberSummaryResponse(s.id(), s.name(), s.email(), s.username(), s.role());
    }
}
