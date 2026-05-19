package com.dove.auth.application.service;

import com.dove.auth.domain.entity.Credential;
import com.dove.auth.domain.repository.CredentialRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class CredentialCommandServiceTest {

    @Mock
    private CredentialRepository credentialRepository;

    @InjectMocks
    private CredentialCommandService commandService;

    @Test
    @DisplayName("save — 레포지토리 저장 결과 반환")
    void shouldDelegateSaveToRepository() {
        Credential c = Credential.create(1L, "alice", "hash");
        given(credentialRepository.save(c)).willReturn(c);

        Credential result = commandService.save(c);

        assertThat(result.getUsername()).isEqualTo("alice");
    }
}
