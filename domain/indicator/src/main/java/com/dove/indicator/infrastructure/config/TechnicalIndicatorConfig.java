package com.dove.indicator.infrastructure.config;

import com.dove.indicator.domain.calculator.EmaCalculator;
import com.dove.indicator.domain.calculator.RsiCalculator;
import com.dove.indicator.domain.calculator.SmaCalculator;
import com.dove.indicator.domain.calculator.TechnicalIndicatorCalculator;
import com.dove.indicator.domain.enums.IndicatorType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * 기술적 지표 계산기 등록 — 파라미터별 다중 인스턴스가 필요한 SMA·EMA·RSI만 명시 등록하고,
 * 인자 없는 계산기는 같은 패키지의 @Component 스캔으로 자동 수집한다.
 */
@Configuration
@ComponentScan(basePackageClasses = TechnicalIndicatorCalculator.class)
public class TechnicalIndicatorConfig {

    @Bean public SmaCalculator sma5Calculator()   { return new SmaCalculator(5, IndicatorType.SMA_5); }
    @Bean public SmaCalculator sma10Calculator()  { return new SmaCalculator(10, IndicatorType.SMA_10); }
    @Bean public SmaCalculator sma20Calculator()  { return new SmaCalculator(20, IndicatorType.SMA_20); }
    @Bean public SmaCalculator sma50Calculator()  { return new SmaCalculator(50, IndicatorType.SMA_50); }
    @Bean public SmaCalculator sma60Calculator()  { return new SmaCalculator(60, IndicatorType.SMA_60); }
    @Bean public SmaCalculator sma120Calculator() { return new SmaCalculator(120, IndicatorType.SMA_120); }
    @Bean public SmaCalculator sma200Calculator() { return new SmaCalculator(200, IndicatorType.SMA_200); }

    @Bean public EmaCalculator ema5Calculator()   { return new EmaCalculator(5, IndicatorType.EMA_5); }
    @Bean public EmaCalculator ema10Calculator()  { return new EmaCalculator(10, IndicatorType.EMA_10); }
    @Bean public EmaCalculator ema20Calculator()  { return new EmaCalculator(20, IndicatorType.EMA_20); }
    @Bean public EmaCalculator ema60Calculator()  { return new EmaCalculator(60, IndicatorType.EMA_60); }
    @Bean public EmaCalculator ema120Calculator() { return new EmaCalculator(120, IndicatorType.EMA_120); }
    @Bean public EmaCalculator ema200Calculator() { return new EmaCalculator(200, IndicatorType.EMA_200); }

    @Bean public RsiCalculator rsi9Calculator()   { return new RsiCalculator(9, IndicatorType.RSI_9); }
    @Bean public RsiCalculator rsi14Calculator()  { return new RsiCalculator(14, IndicatorType.RSI_14); }
    @Bean public RsiCalculator rsi21Calculator()  { return new RsiCalculator(21, IndicatorType.RSI_21); }
}
