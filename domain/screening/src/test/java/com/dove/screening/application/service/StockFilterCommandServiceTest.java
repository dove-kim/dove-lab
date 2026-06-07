package com.dove.screening.application.service;

import com.dove.screening.application.exception.DuplicateStockFilterNameException;
import com.dove.screening.domain.entity.StockFilter;
import com.dove.screening.domain.repository.StockFilterRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("StockFilterCommandService")
class StockFilterCommandServiceTest {

    @Mock StockFilterRepository repository;
    @InjectMocks StockFilterCommandService service;

    private static final Long MEMBER_ID = 1L;
    private static final Long FILTER_ID = 10L;

    @Nested
    @DisplayName("createSystem")
    class CreateSystem {

        @Test
        @DisplayName("이름 중복 없으면 저장한다")
        void shouldCreateSystemWhenNameUnique() {
            given(repository.countByMemberIdIsNullAndName("이름")).willReturn(0L);
            given(repository.save(any(StockFilter.class))).willAnswer(inv -> inv.getArgument(0));

            StockFilter result = service.createSystem("이름", null, List.of(), List.of(), List.of(), "admin");

            assertThat(result.isSystem()).isTrue();
            assertThat(result.getName()).isEqualTo("이름");
            verify(repository).save(any(StockFilter.class));
        }

        @Test
        @DisplayName("이름 중복이면 DuplicateStockFilterNameException을 던진다")
        void shouldThrowOnDuplicateSystemName() {
            given(repository.countByMemberIdIsNullAndName("이름")).willReturn(1L);

            assertThatThrownBy(() -> service.createSystem("이름", null, List.of(), List.of(), List.of(), "admin"))
                    .isInstanceOf(DuplicateStockFilterNameException.class);
        }
    }

    @Nested
    @DisplayName("updateSystem")
    class UpdateSystem {

        @Test
        @DisplayName("개인 필터에 시스템 수정을 시도하면 NOT_FOUND를 던진다")
        void shouldThrowWhenUpdatingNonSystemFilter() {
            StockFilter personal = StockFilter.createPersonal(MEMBER_ID, "n", null, List.of(), List.of(), List.of(), "u");
            given(repository.findById(FILTER_ID)).willReturn(Optional.of(personal));

            assertThatThrownBy(() -> service.updateSystem(FILTER_ID, "n2", null, List.of(), List.of(), List.of(), "u"))
                    .isInstanceOf(NoSuchElementException.class);
        }

        @Test
        @DisplayName("변경하려는 이름이 중복이면 DuplicateStockFilterNameException을 던진다")
        void shouldThrowWhenNewSystemNameDuplicated() {
            StockFilter system = StockFilter.createSystem("기존", null, List.of(), List.of(), List.of(), "admin");
            given(repository.findById(FILTER_ID)).willReturn(Optional.of(system));
            given(repository.countByMemberIdIsNullAndNameAndIdNot("새이름", FILTER_ID)).willReturn(1L);

            assertThatThrownBy(() ->
                    service.updateSystem(FILTER_ID, "새이름", null, List.of(), List.of(), List.of(), "admin"))
                    .isInstanceOf(DuplicateStockFilterNameException.class);
        }
    }

    @Nested
    @DisplayName("setEnabled")
    class SetEnabled {

        @Test
        @DisplayName("시스템 필터의 활성 여부를 변경한다")
        void shouldSetEnabled() {
            StockFilter system = StockFilter.createSystem("s", null, List.of(), List.of(), List.of(), "admin");
            given(repository.findById(FILTER_ID)).willReturn(Optional.of(system));

            StockFilter result = service.setEnabled(FILTER_ID, false, "editor");

            assertThat(result.isEnabled()).isFalse();
        }
    }

    @Nested
    @DisplayName("deleteSystem")
    class DeleteSystem {

        @Test
        @DisplayName("개인 필터에 시스템 삭제를 시도하면 NOT_FOUND를 던진다")
        void shouldRejectDeleteSystemOnPersonal() {
            StockFilter personal = StockFilter.createPersonal(MEMBER_ID, "n", null, List.of(), List.of(), List.of(), "u");
            given(repository.findById(FILTER_ID)).willReturn(Optional.of(personal));

            assertThatThrownBy(() -> service.deleteSystem(FILTER_ID))
                    .isInstanceOf(NoSuchElementException.class);
        }
    }

    @Nested
    @DisplayName("createPersonal")
    class CreatePersonal {

        @Test
        @DisplayName("saveAndFlush에 위임한다")
        void shouldCreatePersonal() {
            given(repository.saveAndFlush(any(StockFilter.class))).willAnswer(inv -> inv.getArgument(0));

            StockFilter result = service.createPersonal(MEMBER_ID, "이름", null, List.of(), List.of(), List.of(), "user");

            assertThat(result.getMemberId()).isEqualTo(MEMBER_ID);
            verify(repository).saveAndFlush(any(StockFilter.class));
        }

        @Test
        @DisplayName("DataIntegrityViolation 발생 시 DuplicateStockFilterNameException으로 변환한다")
        void shouldTranslateDuplicateOnCreatePersonal() {
            given(repository.saveAndFlush(any(StockFilter.class)))
                    .willThrow(new DataIntegrityViolationException("dup"));

            assertThatThrownBy(() ->
                    service.createPersonal(MEMBER_ID, "이름", null, List.of(), List.of(), List.of(), "u"))
                    .isInstanceOf(DuplicateStockFilterNameException.class);
        }
    }

    @Nested
    @DisplayName("updatePersonal")
    class UpdatePersonal {

        @Test
        @DisplayName("본인 필터를 갱신한다")
        void shouldUpdatePersonal() {
            StockFilter personal = StockFilter.createPersonal(MEMBER_ID, "이름", null, List.of(), List.of(), List.of(), "u");
            given(repository.findByIdAndMemberId(FILTER_ID, MEMBER_ID)).willReturn(Optional.of(personal));

            StockFilter result = service.updatePersonal(MEMBER_ID, FILTER_ID, "새이름", null,
                    List.of(), List.of(), List.of(), "u");

            assertThat(result.getName()).isEqualTo("새이름");
        }

        @Test
        @DisplayName("본인 것이 아니면 NOT_FOUND를 던진다")
        void shouldThrowWhenPersonalNotOwned() {
            given(repository.findByIdAndMemberId(FILTER_ID, MEMBER_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() ->
                    service.updatePersonal(MEMBER_ID, FILTER_ID, "n", null, List.of(), List.of(), List.of(), "u"))
                    .isInstanceOf(NoSuchElementException.class);
        }
    }

    @Nested
    @DisplayName("deletePersonal")
    class DeletePersonal {

        @Test
        @DisplayName("본인 필터를 삭제한다")
        void shouldDeletePersonal() {
            StockFilter personal = StockFilter.createPersonal(MEMBER_ID, "n", null, List.of(), List.of(), List.of(), "u");
            given(repository.findByIdAndMemberId(FILTER_ID, MEMBER_ID)).willReturn(Optional.of(personal));

            service.deletePersonal(MEMBER_ID, FILTER_ID);

            verify(repository).delete(personal);
        }
    }
}
