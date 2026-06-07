package com.dove.api.global.security;

import com.dove.auth.application.service.CredentialService;
import com.dove.auth.domain.entity.Credential;
import com.dove.user.application.service.MemberProfileCommandService;
import com.dove.user.application.service.MemberProfileQueryService;
import com.dove.user.domain.entity.MemberProfile;
import com.dove.auth.domain.enums.MemberRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
    @Mock CredentialService credentialService;
    @Mock PasswordEncoder passwordEncoder;

    private AdminInitializer initializer() {
        AdminInitializer init = new AdminInitializer(
                queryService, commandService, credentialService, passwordEncoder);
        ReflectionTestUtils.setField(init, "username", "admin");
        ReflectionTestUtils.setField(init, "password", "secret");
        return init;
    }

    @Nested
    @DisplayName("자격증명 비어 있음")
    class BlankCredentials {

        @Test
        @DisplayName("username/password 비어 있으면 생성 건너뜀")
        void shouldSkipWhenCredentialsBlank() throws Exception {
            AdminInitializer init = new AdminInitializer(
                    queryService, commandService, credentialService, passwordEncoder);
            ReflectionTestUtils.setField(init, "username", "");
            ReflectionTestUtils.setField(init, "password", "");

            init.run(null);

            verify(queryService, never()).existsRoot();
            verify(commandService, never()).save(any());
        }
    }

    @Nested
    @DisplayName("루트 없음")
    class RootMissing {

        @Test
        @DisplayName("루트 없으면 ROOT 역할로 생성")
        void shouldCreateSuperAdminWhenNotExists() throws Exception {
            given(queryService.existsRoot()).willReturn(false);
            MemberProfile profile = MemberProfile.create("admin@admin.local", "admin", MemberRole.ROOT);
            given(commandService.save(any())).willReturn(profile);
            given(passwordEncoder.encode("secret")).willReturn("encoded-secret");
            given(credentialService.save(any(Credential.class))).willAnswer(i -> i.getArgument(0));

            initializer().run(null);

            verify(commandService).save(any());
            verify(credentialService).save(any());
        }
    }

    @Nested
    @DisplayName("루트 존재")
    class RootExists {

        @Test
        @DisplayName("비밀번호 동일 → 갱신 없음")
        void shouldNotUpdatePasswordWhenUnchanged() throws Exception {
            given(queryService.existsRoot()).willReturn(true);
            Credential cred = Credential.create(1L, "admin", "hashed");
            given(credentialService.findByUsername("admin")).willReturn(Optional.of(cred));
            given(passwordEncoder.matches("secret", "hashed")).willReturn(true);

            initializer().run(null);

            verify(credentialService, never()).save(any());
        }

        @Test
        @DisplayName("비밀번호 변경 → 해시 갱신")
        void shouldUpdatePasswordWhenChanged() throws Exception {
            given(queryService.existsRoot()).willReturn(true);
            Credential cred = Credential.create(1L, "admin", "old-hash");
            given(credentialService.findByUsername("admin")).willReturn(Optional.of(cred));
            given(passwordEncoder.matches("secret", "old-hash")).willReturn(false);
            given(passwordEncoder.encode("secret")).willReturn("new-hash");

            initializer().run(null);

            verify(credentialService).save(cred);
        }
    }
}
