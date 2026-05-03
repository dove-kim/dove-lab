package com.dove.member.domain.repository;

import com.dove.member.domain.entity.InviteCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface InviteCodeRepository extends JpaRepository<InviteCode, Long> {
    Optional<InviteCode> findByCodeAndUsedAtIsNullAndExpiresAtAfter(String code, LocalDateTime now);
    List<InviteCode> findAllByOrderByCreatedAtDesc();
}
