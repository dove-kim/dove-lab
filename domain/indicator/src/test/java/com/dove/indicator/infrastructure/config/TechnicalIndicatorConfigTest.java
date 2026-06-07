package com.dove.indicator.infrastructure.config;

import com.dove.indicator.domain.calculator.TechnicalIndicatorCalculator;
import com.dove.indicator.domain.enums.IndicatorType;
import com.dove.indicator.infrastructure.config.TechnicalIndicatorConfig;
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
    @DisplayName("33개 계산기가 모두 등록된다 (SMA 7 + EMA 6 + RSI 3 + 나머지 17)")
    void shouldRegisterAllCalculators() {
        assertThat(calculators).hasSize(33);
    }

    @Test
    @DisplayName("모든 계산기의 대표 지표 종류가 고유하다")
    void shouldHaveUniqueIndicatorTypes() {
        Set<IndicatorType> types = calculators.stream()
                .map(TechnicalIndicatorCalculator::indicatorType)
                .collect(Collectors.toSet());
        assertThat(types).hasSize(calculators.size());
    }
}
