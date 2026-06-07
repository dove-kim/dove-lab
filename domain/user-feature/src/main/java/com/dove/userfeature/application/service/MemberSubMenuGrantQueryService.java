package com.dove.userfeature.application.service;

import com.dove.userfeature.domain.entity.MemberSubMenuGrant;
import com.dove.userfeature.domain.enums.SubMenuCode;
import com.dove.userfeature.domain.repository.MemberSubMenuGrantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * 사용자 하위 메뉴 부여 상태 조회 서비스.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberSubMenuGrantQueryService {

    private final MemberSubMenuGrantRepository repository;

    /**
     * 사용자에게 활성 부여된 하위 메뉴 코드 집합을 반환한다.
     */
    public Set<SubMenuCode> findActiveSubMenuCodes(Long memberId) {
        return repository.findAllByMemberIdAndActiveTrue(memberId).stream()
                .map(MemberSubMenuGrant::getSubMenuCode)
                .collect(Collectors.toSet());
    }
}
