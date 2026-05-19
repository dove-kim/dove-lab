package com.dove.user.application.service;

import com.dove.user.domain.entity.MemberProfile;
import com.dove.user.domain.entity.MemberRole;
import com.dove.user.domain.repository.MemberProfileRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class MemberProfileCommandServiceTest {

    @Mock
    private MemberProfileRepository memberProfileRepository;

    @InjectMocks
    private MemberProfileCommandService commandService;

    @Test
    @DisplayName("save — 레포지토리 저장 결과 반환")
    void shouldDelegateSaveToRepository() {
        MemberProfile profile = MemberProfile.create("a@a.com", "Alice", MemberRole.USER);
        given(memberProfileRepository.save(profile)).willReturn(profile);

        MemberProfile result = commandService.save(profile);

        assertThat(result.getEmail()).isEqualTo("a@a.com");
    }
}
