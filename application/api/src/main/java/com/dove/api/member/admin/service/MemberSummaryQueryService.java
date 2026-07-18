package com.dove.api.member.admin.service;

import com.dove.auth.application.service.CredentialService;
import com.dove.user.application.service.MemberProfileQueryService;
import com.dove.user.domain.entity.MemberProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 관리 화면용 회원 목록 조합 조회 서비스.
 */
@Service
@RequiredArgsConstructor
public class MemberSummaryQueryService {

    private final MemberProfileQueryService memberProfileQueryService;
    private final CredentialService credentialService;

    /**
     * 전체 회원의 요약 정보를 반환한다.
     */
    public List<MemberSummary> findAllSummaries() {
        List<MemberProfile> profiles = memberProfileQueryService.findAll();
        Map<Long, String> usernameMap = credentialService.findUsernamesByMemberIds(
                profiles.stream().map(MemberProfile::getId).toList());
        return profiles.stream()
                .map(p -> new MemberSummary(
                        p.getId(), p.getName(), p.getEmail(),
                        usernameMap.getOrDefault(p.getId(), ""), p.getRole().name(),
                        p.getCreatedAt(), p.getDeletedAt()))
                .toList();
    }
}
