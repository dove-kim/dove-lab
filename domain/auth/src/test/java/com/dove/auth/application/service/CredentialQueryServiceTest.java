package com.dove.auth.application.service;

import com.dove.auth.domain.entity.Credential;
import com.dove.auth.domain.repository.CredentialRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class CredentialQueryServiceTest {

    @Mock
    private CredentialRepository credentialRepository;

    @InjectMocks
    private CredentialQueryService queryService;

    @Test
    @DisplayName("findByUsername — 레포지토리 결과 그대로 반환")
    void shouldDelegateToRepository() {
        Credential c = Credential.create(1L, "alice", "hash");
        given(credentialRepository.findByUsername("alice")).willReturn(Optional.of(c));

        Optional<Credential> result = queryService.findByUsername("alice");

        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo("alice");
    }

    @Test
    @DisplayName("findByUsername — 없으면 empty 반환")
    void shouldReturnEmptyWhenNotFound() {
        given(credentialRepository.findByUsername("unknown")).willReturn(Optional.empty());

        assertThat(queryService.findByUsername("unknown")).isEmpty();
    }

    @Test
    @DisplayName("existsByUsername — true/false 전달")
    void shouldDelegateExistsCheck() {
        given(credentialRepository.existsByUsername("alice")).willReturn(true);
        given(credentialRepository.existsByUsername("ghost")).willReturn(false);

        assertThat(queryService.existsByUsername("alice")).isTrue();
        assertThat(queryService.existsByUsername("ghost")).isFalse();
    }
}
