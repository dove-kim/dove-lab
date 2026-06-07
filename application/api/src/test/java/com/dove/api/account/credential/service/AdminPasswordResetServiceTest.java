package com.dove.api.account.credential.service;

import com.dove.auth.application.service.CredentialService;
import com.dove.auth.infrastructure.security.TemporaryPasswordGenerator;
import com.dove.auth.domain.entity.Credential;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * 관리자 비밀번호 초기화 use case 단위 테스트.
 */
@ExtendWith(MockitoExtension.class)
class AdminPasswordResetServiceTest {

    @Mock CredentialService credentialService;
    @Mock TemporaryPasswordGenerator temporaryPasswordGenerator;
    @Mock PasswordEncoder passwordEncoder;

    @InjectMocks AdminPasswordResetService service;

    private static final Long USER_ID = 7L;

    @Nested
    @DisplayName("resetPassword")
    class ResetPassword {

        @Test
        @DisplayName("임시 비밀번호를 생성·저장하고 평문을 반환한다")
        void shouldReturnPlainTempPasswordWhenReset() {
            Credential credential = Credential.create(USER_ID, "user", "old-hash");
            given(credentialService.findByMemberId(USER_ID)).willReturn(Optional.of(credential));
            given(temporaryPasswordGenerator.generate()).willReturn("Temp123abc");
            given(passwordEncoder.encode("Temp123abc")).willReturn("new-hash");

            String result = service.resetPassword(USER_ID);

            assertThat(result).isEqualTo("Temp123abc");
            assertThat(credential.getPasswordHash()).isEqualTo("new-hash");
            assertThat(credential.isPasswordResetRequired()).isTrue();
            verify(credentialService).save(credential);
        }

        @Test
        @DisplayName("자격증명이 없으면 404 USER_NOT_FOUND")
        void shouldThrowNotFoundWhenCredentialMissing() {
            given(credentialService.findByMemberId(USER_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.resetPassword(USER_ID))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode().value())
                    .isEqualTo(HttpStatus.NOT_FOUND.value());
        }
    }
}
