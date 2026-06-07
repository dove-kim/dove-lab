package com.dove.indicator.domain.calculator;

import com.dove.indicator.domain.enums.IndicatorType;
import com.dove.stock.domain.entity.StockPrice;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CalculatorRunnerTest {

    @Mock
    private TechnicalIndicatorCalculator calculator;

    private final List<StockPrice> pool = List.of();

    @Nested
    @DisplayName("비누적 계산기")
    class NonCumulative {

        @Test
        @DisplayName("매 호출 calculate()만 사용하고 시드를 쓰지 않는다")
        void shouldAlwaysUseCalculate() {
            given(calculator.isCumulative()).willReturn(false);
            CalculatorRunner runner = new CalculatorRunner(calculator);

            runner.compute(pool);
            runner.compute(pool);

            verify(calculator, times(2)).calculate(any());
            verify(calculator, never()).calculateWithSeed(any(), anyDouble());
        }
    }

    @Nested
    @DisplayName("누적 계산기")
    class Cumulative {

        @Test
        @DisplayName("첫 완전 윈도우는 calculate(cold), 이후엔 직전 대표값을 시드로 calculateWithSeed")
        void shouldSeedFromPreviousRepresentativeValue() {
            given(calculator.isCumulative()).willReturn(true);
            given(calculator.indicatorType()).willReturn(IndicatorType.EMA_5);
            given(calculator.calculate(any())).willReturn(Map.of(IndicatorType.EMA_5, 10.0));
            given(calculator.calculateWithSeed(any(), anyDouble())).willReturn(Map.of(IndicatorType.EMA_5, 20.0));
            CalculatorRunner runner = new CalculatorRunner(calculator);

            runner.compute(pool); // cold → 시드 10.0
            runner.compute(pool); // warm → 시드 10.0 사용, 결과로 시드 20.0
            runner.compute(pool); // warm → 시드 20.0 사용

            verify(calculator, times(1)).calculate(any());
            ArgumentCaptor<Double> seeds = ArgumentCaptor.forClass(Double.class);
            verify(calculator, times(2)).calculateWithSeed(any(), seeds.capture());
            assertThat(seeds.getAllValues()).containsExactly(10.0, 20.0);
        }
    }
}
