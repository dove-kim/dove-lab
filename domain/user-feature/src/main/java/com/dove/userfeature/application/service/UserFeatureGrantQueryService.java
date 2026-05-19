package com.dove.userfeature.application.service;

import com.dove.userfeature.domain.enums.FeatureCode;
import com.dove.userfeature.domain.repository.UserFeatureGrantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserFeatureGrantQueryService {

    private final UserFeatureGrantRepository grantRepository;

    public Set<FeatureCode> findActiveFeatureCodes(Long userId) {
        return grantRepository.findAllByUserIdAndActiveTrue(userId).stream()
                .map(g -> g.getFeatureCode())
                .collect(Collectors.toSet());
    }

    public boolean hasActiveGrant(Long userId, FeatureCode featureCode) {
        return grantRepository.findByUserIdAndFeatureCode(userId, featureCode)
                .map(g -> g.isActive())
                .orElse(false);
    }
}
