package com.dove.indicator.infrastructure.config;

import com.dove.indicator.domain.calculator.*;
import com.dove.indicator.domain.enums.IndicatorType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class TechnicalIndicatorConfig {

    @Bean
    public SmaCalculator sma5Calculator() {
        return new SmaCalculator(5, IndicatorType.SMA_5);
    }

    @Bean
    public SmaCalculator sma10Calculator() {
        return new SmaCalculator(10, IndicatorType.SMA_10);
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
    public EmaCalculator ema5Calculator() {
        return new EmaCalculator(5, IndicatorType.EMA_5);
    }

    @Bean
    public EmaCalculator ema10Calculator() {
        return new EmaCalculator(10, IndicatorType.EMA_10);
    }

    @Bean
    public EmaCalculator ema20Calculator() {
        return new EmaCalculator(20, IndicatorType.EMA_20);
    }

    @Bean
    public EmaCalculator ema60Calculator() {
        return new EmaCalculator(60, IndicatorType.EMA_60);
    }

    @Bean
    public EmaCalculator ema120Calculator() {
        return new EmaCalculator(120, IndicatorType.EMA_120);
    }

    @Bean
    public EmaCalculator ema200Calculator() {
        return new EmaCalculator(200, IndicatorType.EMA_200);
    }

    @Bean
    public RsiCalculator rsi9Calculator() {
        return new RsiCalculator(9, IndicatorType.RSI_9);
    }

    @Bean
    public RsiCalculator rsi14Calculator() {
        return new RsiCalculator(14, IndicatorType.RSI_14);
    }

    @Bean
    public RsiCalculator rsi21Calculator() {
        return new RsiCalculator(21, IndicatorType.RSI_21);
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
    public VolatilityCalculator volatilityCalculator() {
        return new VolatilityCalculator();
    }

    @Bean
    public NewHighLowFlagCalculator newHighLowFlagCalculator() {
        return new NewHighLowFlagCalculator();
    }

    @Bean
    public PriceRangeRatioCalculator priceRangeRatioCalculator() {
        return new PriceRangeRatioCalculator();
    }

    @Bean
    public VolumeMa20RatioCalculator volumeMa20RatioCalculator() {
        return new VolumeMa20RatioCalculator();
    }

    @Bean
    public GapOpenCalculator gapOpenCalculator() {
        return new GapOpenCalculator();
    }

    @Bean
    public List<TechnicalIndicatorCalculator> technicalIndicatorCalculators(
            SmaCalculator sma5Calculator,
            SmaCalculator sma10Calculator,
            SmaCalculator sma20Calculator,
            SmaCalculator sma50Calculator,
            SmaCalculator sma60Calculator,
            SmaCalculator sma120Calculator,
            SmaCalculator sma200Calculator,
            EmaCalculator ema5Calculator,
            EmaCalculator ema10Calculator,
            EmaCalculator ema20Calculator,
            EmaCalculator ema60Calculator,
            EmaCalculator ema120Calculator,
            EmaCalculator ema200Calculator,
            RsiCalculator rsi9Calculator,
            RsiCalculator rsi14Calculator,
            RsiCalculator rsi21Calculator,
            MacdCalculator macdCalculator,
            BollingerBandsCalculator bollingerBandsCalculator,
            StochasticCalculator stochasticCalculator,
            AdxCalculator adxCalculator,
            VolumeRatioCalculator volumeRatioCalculator,
            ObvCalculator obvCalculator,
            AtrCalculator atrCalculator,
            MfiCalculator mfiCalculator,
            CciCalculator cciCalculator,
            WilliamsRCalculator williamsRCalculator,
            VolatilityCalculator volatilityCalculator,
            NewHighLowFlagCalculator newHighLowFlagCalculator,
            PriceRangeRatioCalculator priceRangeRatioCalculator,
            VolumeMa20RatioCalculator volumeMa20RatioCalculator,
            GapOpenCalculator gapOpenCalculator) {
        return List.of(
                sma5Calculator, sma10Calculator, sma20Calculator, sma50Calculator,
                sma60Calculator, sma120Calculator, sma200Calculator,
                ema5Calculator, ema10Calculator, ema20Calculator,
                ema60Calculator, ema120Calculator, ema200Calculator,
                rsi9Calculator, rsi14Calculator, rsi21Calculator,
                macdCalculator, bollingerBandsCalculator,
                stochasticCalculator, adxCalculator,
                volumeRatioCalculator, obvCalculator,
                atrCalculator, mfiCalculator, cciCalculator, williamsRCalculator,
                volatilityCalculator,
                newHighLowFlagCalculator,
                priceRangeRatioCalculator, volumeMa20RatioCalculator, gapOpenCalculator
        );
    }
}
