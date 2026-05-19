package com.dove.user.domain.repository;

import com.dove.user.domain.entity.MemberProfile;
import com.dove.user.domain.entity.MemberRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MemberProfileRepository extends JpaRepository<MemberProfile, Long> {

    boolean existsByEmail(String email);

    boolean existsByRole(MemberRole role);

    List<MemberProfile> findAllByOrderByCreatedAtDesc();
}
