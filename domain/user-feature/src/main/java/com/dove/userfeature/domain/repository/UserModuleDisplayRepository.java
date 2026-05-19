package com.dove.userfeature.domain.repository;

import com.dove.userfeature.domain.entity.UserModuleDisplay;
import com.dove.userfeature.domain.enums.ModuleCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserModuleDisplayRepository extends JpaRepository<UserModuleDisplay, Long> {

    Optional<UserModuleDisplay> findByUserIdAndModuleCode(Long userId, ModuleCode moduleCode);

    List<UserModuleDisplay> findAllByUserId(Long userId);
}
