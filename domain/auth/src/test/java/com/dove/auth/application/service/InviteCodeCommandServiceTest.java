package com.dove.auth.application.service;

import com.dove.auth.domain.entity.InviteCode;
import com.dove.auth.domain.repository.InviteCodeRepository;
import com.dove.user.domain.entity.MemberRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class InviteCodeCommandServiceTest {

    @Mock
    private InviteCodeRepository inviteCodeRepository;

    @InjectMocks
    private InviteCodeCommandService commandService;

    @Test
    @DisplayName("create — InviteCode 생성·저장 호출")
    void shouldCreateAndSaveInviteCode() {
        LocalDateTime expiry = LocalDateTime.now().plusDays(3);
        InviteCode code = InviteCode.create(MemberRole.USER, expiry, "admin");
        given(inviteCodeRepository.save(any(InviteCode.class))).willReturn(code);

        InviteCode result = commandService.create(MemberRole.USER, expiry, "admin");

        assertThat(result.getRole()).isEqualTo(MemberRole.USER);
        verify(inviteCodeRepository).save(any(InviteCode.class));
    }

    @Test
    @DisplayName("use — usedAt 설정 후 save 호출")
    void shouldMarkCodeAsUsedAndSave() {
        InviteCode code = InviteCode.create(MemberRole.USER, LocalDateTime.now().plusDays(1), "admin");
        given(inviteCodeRepository.save(code)).willReturn(code);

        commandService.use(code);

        assertThat(code.getUsedAt()).isNotNull();
        verify(inviteCodeRepository).save(code);
    }
}
