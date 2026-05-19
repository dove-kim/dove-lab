package com.dove.auth.domain.repository;

import com.dove.auth.domain.entity.Credential;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CredentialRepository extends JpaRepository<Credential, Long> {

    Optional<Credential> findByUsername(String username);

    boolean existsByUsername(String username);

    List<Credential> findAllByMemberIdIn(Collection<Long> memberIds);
}
