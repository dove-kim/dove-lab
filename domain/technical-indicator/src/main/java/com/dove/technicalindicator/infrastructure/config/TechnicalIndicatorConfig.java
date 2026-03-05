package com.dove.technicalindicator.infrastructure.config;

import com.dove.technicalindicator.domain.calculator.*;
import com.dove.technicalindicator.domain.enums.IndicatorType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 기술적 지표 계산기 빈 등록 설정.
 * SMA(5/20/50/60/120/200), RSI, MACD, 볼린저밴드, 스토캐스틱, ADX, VR, OBV, ATR, MFI, CCI, Williams %R을 등록한다.
 */
@Configuration
public class TechnicalIndicatorConfig {

    @Bean
    public SmaCalculator sma5Calculator() {
        return new SmaCalculator(5, IndicatorType.SMA_5);
    }

    @Bean
    public SmaCalculator sma20Calculator() {
        return new SmaCalculator(20, IndicatorType.SMA_20);
    }

    @Bean
    public SmaCalculator sma50Calculator() {
        return new SmaCalculator(50, IndicatorType.SMA_50);
    }

    @Bean
    public SmaCalculator sma60Calculator() {
        return new SmaCalculator(60, IndicatorType.SMA_60);
    }

    @Bean
    public SmaCalculator sma120Calculator() {
        return new SmaCalculator(120, IndicatorType.SMA_120);
    }

    @Bean
    public SmaCalculator sma200Calculator() {
        return new SmaCalculator(200, IndicatorType.SMA_200);
    }

    @Bean
    public RsiCalculator rsiCalculator() {
        return new RsiCalculator();
    }

    @Bean
    public MacdCalculator macdCalculator() {
        return new MacdCalculator();
    }

    @Bean
    public BollingerBandsCalculator bollingerBandsCalculator() {
        return new BollingerBandsCalculator();
    }

    @Bean
    public StochasticCalculator stochasticCalculator() {
        return new StochasticCalculator();
    }

    @Bean
    public AdxCalculator adxCalculator() {
        return new AdxCalculator();
    }

    @Bean
    public VolumeRatioCalculator volumeRatioCalculator() {
        return new VolumeRatioCalculator();
    }

    @Bean
    public ObvCalculator obvCalculator() {
        return new ObvCalculator();
    }

    @Bean
    public AtrCalculator atrCalculator() {
        return new AtrCalculator();
    }

    @Bean
    public MfiCalculator mfiCalculator() {
        return new MfiCalculator();
    }

    @Bean
    public CciCalculator cciCalculator() {
        return new CciCalculator();
    }

    @Bean
    public WilliamsRCalculator williamsRCalculator() {
        return new WilliamsRCalculator();
    }

    @Bean
    public List<TechnicalIndicatorCalculator> technicalIndicatorCalculators(
            SmaCalculator sma5Calculator,
            SmaCalculator sma20Calculator,
            SmaCalculator sma50Calculator,
            SmaCalculator sma60Calculator,
            SmaCalculator sma120Calculator,
            SmaCalculator sma200Calculator,
            RsiCalculator rsiCalculator,
            MacdCalculator macdCalculator,
            BollingerBandsCalculator bollingerBandsCalculator,
            StochasticCalculator stochasticCalculator,
            AdxCalculator adxCalculator,
            VolumeRatioCalculator volumeRatioCalculator,
            ObvCalculator obvCalculator,
            AtrCalculator atrCalculator,
            MfiCalculator mfiCalculator,
            CciCalculator cciCalculator,
            WilliamsRCalculator williamsRCalculator) {
        return List.of(
                sma5Calculator, sma20Calculator, sma50Calculator,
                sma60Calculator, sma120Calculator, sma200Calculator,
                rsiCalculator, macdCalculator, bollingerBandsCalculator,
                stochasticCalculator, adxCalculator,
                volumeRatioCalculator, obvCalculator,
                atrCalculator, mfiCalculator, cciCalculator, williamsRCalculator
        );
    }
}
