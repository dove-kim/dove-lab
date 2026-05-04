package com.dove.member.domain.repository;

import com.dove.member.domain.entity.Member;
import com.dove.member.domain.entity.MemberRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByUsername(String username);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByRole(MemberRole role);
}
