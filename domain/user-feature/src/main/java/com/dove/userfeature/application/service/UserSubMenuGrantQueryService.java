package com.dove.userfeature.application.service;

import com.dove.userfeature.domain.enums.FeatureCode;
import com.dove.userfeature.domain.enums.SubMenuCode;
import com.dove.userfeature.domain.repository.UserSubMenuGrantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserSubMenuGrantQueryService {

    private final UserSubMenuGrantRepository repository;

    public Set<SubMenuCode> findActiveSubMenuCodes(Long userId) {
        return repository.findAllByUserIdAndActiveTrue(userId).stream()
                .map(g -> g.getSubMenuCode())
                .collect(Collectors.toSet());
    }

    public Set<SubMenuCode> findActiveByUserIdAndFeature(Long userId, FeatureCode featureCode) {
        return findActiveSubMenuCodes(userId).stream()
                .filter(s -> s.getFeature() == featureCode)
                .collect(Collectors.toSet());
    }
}
