package com.dove.technicalindicator.infrastructure.config;

import com.dove.technicalindicator.domain.calculator.TechnicalIndicatorCalculator;
import com.dove.technicalindicator.infrastructure.config.TechnicalIndicatorConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringJUnitConfig(TechnicalIndicatorConfig.class)
class TechnicalIndicatorConfigTest {

    @Autowired
    private List<TechnicalIndicatorCalculator> calculators;

    @Test
    @DisplayName("17개 계산기가 모두 등록된다 (SMA 6 + 나머지 11)")
    void shouldRegisterAllCalculators() {
        assertThat(calculators).hasSize(17);
    }

    @Test
    @DisplayName("모든 계산기의 이름이 고유하다")
    void shouldHaveUniqueNames() {
        Set<String> names = calculators.stream()
                .map(TechnicalIndicatorCalculator::getName)
                .collect(Collectors.toSet());
        assertThat(names).hasSize(calculators.size());
    }
}
