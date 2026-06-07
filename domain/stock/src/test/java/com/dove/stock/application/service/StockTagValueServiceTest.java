package com.dove.stock.application.service;

import com.dove.stock.domain.entity.StockTagValue;
import com.dove.stock.domain.repository.StockTagValueRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * StockTagValueService 통합 테스트.
 */
@DataJpaTest
@Import(StockTagValueService.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class StockTagValueServiceTest {

    @Autowired StockTagValueService service;
    @Autowired StockTagValueRepository repository;

    @AfterEach
    void cleanUp() {
        repository.deleteAll();
    }

    @Nested
    @DisplayName("registerIfAbsent — 멱등 등록")
    class RegisterIfAbsent {

        @Test
        @DisplayName("존재하지 않으면 새로 등록한다")
        void shouldRegisterWhenAbsent() {
            service.registerIfAbsent("SECUGRP", "주권");

            assertThat(repository.count()).isEqualTo(1);
            assertThat(repository.findAll().get(0).getValue()).isEqualTo("주권");
        }

        @Test
        @DisplayName("동일 field·value가 이미 있으면 등록하지 않는다")
        void shouldSkipWhenAlreadyExists() {
            service.registerIfAbsent("SECUGRP", "주권");

            service.registerIfAbsent("SECUGRP", "주권");

            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        @DisplayName("value가 null이면 등록하지 않는다")
        void shouldSkipWhenValueIsNull() {
            service.registerIfAbsent("SECUGRP", null);

            assertThat(repository.count()).isZero();
        }

        @Test
        @DisplayName("value가 빈 문자열이면 등록하지 않는다")
        void shouldSkipWhenValueIsBlank() {
            service.registerIfAbsent("SECUGRP", "  ");

            assertThat(repository.count()).isZero();
        }

        @Test
        @DisplayName("등록 시 표시명(label)은 원문값과 동일하게 초기화된다")
        void shouldInitializeLabelToValueWhenRegistered() {
            service.registerIfAbsent("INDUSTRY_LCLS", "전기전자");

            assertThat(repository.findAll().get(0).getLabel()).isEqualTo("전기전자");
        }
    }

    @Nested
    @DisplayName("updateLabel — 표시명 변경")
    class UpdateLabel {

        @Test
        @DisplayName("존재하면 표시명을 변경한다")
        void shouldUpdateLabelWhenExists() {
            service.registerIfAbsent("SECUGRP", "주권");
            Long id = repository.findAll().get(0).getId();

            service.updateLabel(id, "보통주식");

            assertThat(repository.findById(id).orElseThrow().getLabel()).isEqualTo("보통주식");
        }

        @Test
        @DisplayName("존재하지 않으면 NoSuchElementException을 던진다")
        void shouldThrowWhenNotFound() {
            assertThatThrownBy(() -> service.updateLabel(999L, "라벨"))
                    .isInstanceOf(NoSuchElementException.class);
        }

        @Test
        @DisplayName("label이 null이면 원문값으로 초기화된다")
        void shouldResetToValueWhenLabelIsNull() {
            service.registerIfAbsent("SECUGRP", "주권");
            Long id = repository.findAll().get(0).getId();
            service.updateLabel(id, "임시라벨");

            service.updateLabel(id, null);

            assertThat(repository.findById(id).orElseThrow().getLabel()).isEqualTo("주권");
        }

        @Test
        @DisplayName("label이 빈 문자열이면 원문값으로 초기화된다")
        void shouldResetToValueWhenLabelIsBlank() {
            service.registerIfAbsent("SECUGRP", "주권");
            Long id = repository.findAll().get(0).getId();

            service.updateLabel(id, "  ");

            assertThat(repository.findById(id).orElseThrow().getLabel()).isEqualTo("주권");
        }
    }

    @Nested
    @DisplayName("findAll — field·value 오름차순 전체 조회")
    class FindAll {

        @Test
        @DisplayName("field 오름차순 → value 오름차순으로 반환한다")
        void shouldReturnOrderedByFieldThenValueWhenFindAll() {
            service.registerIfAbsent("SECUGRP", "주권");
            service.registerIfAbsent("INDUSTRY_LCLS", "화학");
            service.registerIfAbsent("INDUSTRY_LCLS", "전기전자");

            List<StockTagValue> result = service.findAll();

            assertThat(result).extracting(StockTagValue::getField)
                    .containsExactly("INDUSTRY_LCLS", "INDUSTRY_LCLS", "SECUGRP");
            assertThat(result).extracting(StockTagValue::getValue)
                    .containsExactly("전기전자", "화학", "주권");
        }

        @Test
        @DisplayName("등록된 값이 없으면 빈 목록을 반환한다")
        void shouldReturnEmptyWhenNoneRegistered() {
            assertThat(service.findAll()).isEmpty();
        }
    }
}
