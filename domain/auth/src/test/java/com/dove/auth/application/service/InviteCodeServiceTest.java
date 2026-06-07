package com.dove.auth.application.service;

import com.dove.auth.domain.entity.InviteCode;
import com.dove.auth.domain.repository.InviteCodeRepository;
import com.dove.auth.domain.enums.MemberRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class InviteCodeServiceTest {

    @Mock
    private InviteCodeRepository inviteCodeRepository;

    @InjectMocks
    private InviteCodeService inviteCodeService;

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("InviteCode 생성·저장 호출")
        void shouldCreateAndSaveInviteCode() {
            LocalDateTime expiry = LocalDateTime.now().plusDays(3);
            InviteCode code = InviteCode.create(MemberRole.USER, expiry, "admin");
            given(inviteCodeRepository.save(any(InviteCode.class))).willReturn(code);

            InviteCode result = inviteCodeService.create(MemberRole.USER, expiry, "admin");

            assertThat(result.getRole()).isEqualTo(MemberRole.USER);
            verify(inviteCodeRepository).save(any(InviteCode.class));
        }
    }

    @Nested
    @DisplayName("use")
    class Use {

        @Test
        @DisplayName("usedAt 설정 후 save 호출")
        void shouldMarkCodeAsUsedAndSave() {
            InviteCode code = InviteCode.create(MemberRole.USER, LocalDateTime.now().plusDays(1), "admin");
            given(inviteCodeRepository.save(code)).willReturn(code);

            inviteCodeService.use(code);

            assertThat(code.getUsedAt()).isNotNull();
            verify(inviteCodeRepository).save(code);
        }
    }

    @Nested
    @DisplayName("findValidCode")
    class FindValidCode {

        @Test
        @DisplayName("미사용·미만료 코드 반환")
        void shouldReturnValidCode() {
            InviteCode code = InviteCode.create(MemberRole.USER, LocalDateTime.now().plusDays(1), "admin");
            given(inviteCodeRepository.findByCode("abc")).willReturn(Optional.of(code));

            Optional<InviteCode> result = inviteCodeService.findValidCode("abc");

            assertThat(result).isPresent();
        }

        @Test
        @DisplayName("만료된 코드 → empty")
        void shouldReturnEmptyForExpiredCode() {
            InviteCode code = InviteCode.create(MemberRole.USER, LocalDateTime.now().minusSeconds(1), "admin");
            given(inviteCodeRepository.findByCode("old")).willReturn(Optional.of(code));

            Optional<InviteCode> result = inviteCodeService.findValidCode("old");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("사용된 코드 → empty")
        void shouldReturnEmptyForUsedCode() {
            InviteCode code = InviteCode.create(MemberRole.USER, LocalDateTime.now().plusDays(1), "admin");
            code.use();
            given(inviteCodeRepository.findByCode("used")).willReturn(Optional.of(code));

            Optional<InviteCode> result = inviteCodeService.findValidCode("used");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("존재하지 않는 코드 → empty")
        void shouldReturnEmptyWhenNotFound() {
            given(inviteCodeRepository.findByCode("none")).willReturn(Optional.empty());

            assertThat(inviteCodeService.findValidCode("none")).isEmpty();
        }
    }

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("전체 코드 목록 반환")
        void shouldReturnAllCodes() {
            List<InviteCode> codes = List.of(
                    InviteCode.create(MemberRole.USER, LocalDateTime.now().plusDays(1), "admin"),
                    InviteCode.create(MemberRole.ADMIN, LocalDateTime.now().plusDays(7), "admin")
            );
            given(inviteCodeRepository.findAll()).willReturn(codes);

            assertThat(inviteCodeService.findAll()).hasSize(2);
        }
    }
}
