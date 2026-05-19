package com.dove.auth.application.service;

import com.dove.auth.domain.entity.InviteCode;
import com.dove.auth.domain.repository.InviteCodeRepository;
import com.dove.user.domain.entity.MemberRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class InviteCodeQueryServiceTest {

    @Mock
    private InviteCodeRepository inviteCodeRepository;

    @InjectMocks
    private InviteCodeQueryService queryService;

    @Test
    @DisplayName("findValidCode — 미사용·미만료 코드 반환")
    void shouldReturnValidCode() {
        InviteCode code = InviteCode.create(MemberRole.USER, LocalDateTime.now().plusDays(1), "admin");
        given(inviteCodeRepository.findByCode("abc")).willReturn(Optional.of(code));

        Optional<InviteCode> result = queryService.findValidCode("abc");

        assertThat(result).isPresent();
    }

    @Test
    @DisplayName("findValidCode — 만료된 코드 → filter에서 제외 → empty")
    void shouldReturnEmptyForExpiredCode() {
        InviteCode code = InviteCode.create(MemberRole.USER, LocalDateTime.now().minusSeconds(1), "admin");
        given(inviteCodeRepository.findByCode("old")).willReturn(Optional.of(code));

        Optional<InviteCode> result = queryService.findValidCode("old");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findValidCode — 사용된 코드 → empty")
    void shouldReturnEmptyForUsedCode() {
        InviteCode code = InviteCode.create(MemberRole.USER, LocalDateTime.now().plusDays(1), "admin");
        code.use();
        given(inviteCodeRepository.findByCode("used")).willReturn(Optional.of(code));

        Optional<InviteCode> result = queryService.findValidCode("used");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findValidCode — 존재하지 않는 코드 → empty")
    void shouldReturnEmptyWhenNotFound() {
        given(inviteCodeRepository.findByCode("none")).willReturn(Optional.empty());

        assertThat(queryService.findValidCode("none")).isEmpty();
    }

    @Test
    @DisplayName("findAll — 전체 코드 목록 반환")
    void shouldReturnAllCodes() {
        List<InviteCode> codes = List.of(
                InviteCode.create(MemberRole.USER, LocalDateTime.now().plusDays(1), "admin"),
                InviteCode.create(MemberRole.ADMIN, LocalDateTime.now().plusDays(7), "admin")
        );
        given(inviteCodeRepository.findAll()).willReturn(codes);

        assertThat(queryService.findAll()).hasSize(2);
    }
}
