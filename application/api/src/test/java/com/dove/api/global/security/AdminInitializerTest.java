package com.dove.api.global.security;

import com.dove.auth.application.service.CredentialCommandService;
import com.dove.auth.application.service.CredentialQueryService;
import com.dove.auth.domain.entity.Credential;
import com.dove.user.application.service.MemberProfileCommandService;
import com.dove.user.application.service.MemberProfileQueryService;
import com.dove.user.domain.entity.MemberProfile;
import com.dove.user.domain.entity.MemberRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AdminInitializerTest {

    @Mock MemberProfileQueryService queryService;
    @Mock MemberProfileCommandService commandService;
    @Mock CredentialQueryService credentialQueryService;
    @Mock CredentialCommandService credentialCommandService;
    @Mock PasswordEncoder passwordEncoder;

    private AdminInitializer initializer() {
        AdminInitializer init = new AdminInitializer(
                queryService, commandService, credentialQueryService, credentialCommandService, passwordEncoder);
        ReflectionTestUtils.setField(init, "username", "admin");
        ReflectionTestUtils.setField(init, "password", "secret");
        return init;
    }

    @Test
    @DisplayName("username/password 비어 있으면 생성 건너뜀")
    void shouldSkipWhenCredentialsBlank() throws Exception {
        AdminInitializer init = new AdminInitializer(
                queryService, commandService, credentialQueryService, credentialCommandService, passwordEncoder);
        ReflectionTestUtils.setField(init, "username", "");
        ReflectionTestUtils.setField(init, "password", "");

        init.run(null);

        verify(queryService, never()).existsRoot();
        verify(commandService, never()).save(any());
    }

    @Test
    @DisplayName("루트 없으면 ROOT 역할로 생성")
    void shouldCreateSuperAdminWhenNotExists() throws Exception {
        given(queryService.existsRoot()).willReturn(false);
        MemberProfile profile = MemberProfile.create("admin@admin.local", "admin", MemberRole.ROOT);
        given(commandService.save(any())).willReturn(profile);
        given(passwordEncoder.encode("secret")).willReturn("encoded-secret");
        given(credentialCommandService.save(any(Credential.class))).willAnswer(i -> i.getArgument(0));

        initializer().run(null);

        verify(commandService).save(any());
        verify(credentialCommandService).save(any());
    }

    @Test
    @DisplayName("루트 존재 + 비밀번호 동일 → 갱신 없음")
    void shouldNotUpdatePasswordWhenUnchanged() throws Exception {
        given(queryService.existsRoot()).willReturn(true);
        Credential cred = Credential.create(1L, "admin", "hashed");
        given(credentialQueryService.findByUsername("admin")).willReturn(Optional.of(cred));
        given(passwordEncoder.matches("secret", "hashed")).willReturn(true);

        initializer().run(null);

        verify(credentialCommandService, never()).save(any());
    }

    @Test
    @DisplayName("루트 존재 + 비밀번호 변경 → 해시 갱신")
    void shouldUpdatePasswordWhenChanged() throws Exception {
        given(queryService.existsRoot()).willReturn(true);
        Credential cred = Credential.create(1L, "admin", "old-hash");
        given(credentialQueryService.findByUsername("admin")).willReturn(Optional.of(cred));
        given(passwordEncoder.matches("secret", "old-hash")).willReturn(false);
        given(passwordEncoder.encode("secret")).willReturn("new-hash");

        initializer().run(null);

        verify(credentialCommandService).save(cred);
    }
}
