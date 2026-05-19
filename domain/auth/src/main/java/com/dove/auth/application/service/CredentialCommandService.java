package com.dove.auth.application.service;

import com.dove.auth.domain.entity.Credential;
import com.dove.auth.domain.repository.CredentialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CredentialCommandService {

    private final CredentialRepository credentialRepository;

    public Credential save(Credential credential) {
        return credentialRepository.save(credential);
    }
}
