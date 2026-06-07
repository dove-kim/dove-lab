package com.dove.auth.domain.repository;

import com.dove.auth.domain.entity.InviteCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 초대 코드 영속성 저장소.
 */
public interface InviteCodeRepository extends JpaRepository<InviteCode, Long> {

    Optional<InviteCode> findByCode(String code);
}
