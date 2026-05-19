package com.dove.userfeature.domain.repository;

import com.dove.userfeature.domain.entity.UserFeatureGrant;
import com.dove.userfeature.domain.enums.FeatureCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserFeatureGrantRepository extends JpaRepository<UserFeatureGrant, Long> {

    Optional<UserFeatureGrant> findByUserIdAndFeatureCode(Long userId, FeatureCode featureCode);

    List<UserFeatureGrant> findAllByUserIdAndActiveTrue(Long userId);

    List<UserFeatureGrant> findAllByUserId(Long userId);
}
