package com.dove.userfeature.domain.repository;

import com.dove.userfeature.domain.entity.UserFeatureDisplay;
import com.dove.userfeature.domain.enums.FeatureCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserFeatureDisplayRepository extends JpaRepository<UserFeatureDisplay, Long> {

    Optional<UserFeatureDisplay> findByUserIdAndFeatureCode(Long userId, FeatureCode featureCode);

    List<UserFeatureDisplay> findAllByUserId(Long userId);
}
