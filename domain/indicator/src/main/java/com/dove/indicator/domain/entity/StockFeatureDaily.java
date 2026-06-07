package com.dove.indicator.domain.entity;

import com.dove.indicator.domain.enums.IndicatorType;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.Map;

/**
 * 한 (종목, 거래소, 가격유형, 거래일)의 모든 기술적 지표를 한 행에 담는 wide 피처 행.
 */
@Getter
@Entity
@Table(name = "STOCK_FEATURE_DAILY",
        indexes = {
                @Index(name = "IDX_SFD_EXCHANGE_DATE", columnList = "EXCHANGE, PRICE_TYPE, TRADE_DATE"),
                @Index(name = "IDX_SFD_SEQ", columnList = "TICKER, EXCHANGE, PRICE_TYPE, SEQ")
        })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockFeatureDaily {

    @EmbeddedId
    private StockFeatureDailyId id;

    @Column(name = "SEQ", nullable = false)
    @Comment("그룹 내 거래일 순번(전일·미래수익 비교용)")
    private Integer seq;

    @Column(name = "OPEN_PRICE")
    private Long openPrice;
    @Column(name = "HIGH_PRICE")
    private Long highPrice;
    @Column(name = "LOW_PRICE")
    private Long lowPrice;
    @Column(name = "CLOSE_PRICE")
    private Long closePrice;
    @Column(name = "VOLUME")
    private Long volume;
    @Column(name = "TURNOVER")
    private Long turnover;

    // 이동평균
    @Column(name = "SMA_5") private Float sma5;
    @Column(name = "SMA_10") private Float sma10;
    @Column(name = "SMA_20") private Float sma20;
    @Column(name = "SMA_50") private Float sma50;
    @Column(name = "SMA_60") private Float sma60;
    @Column(name = "SMA_120") private Float sma120;
    @Column(name = "SMA_200") private Float sma200;
    // 지수이평
    @Column(name = "EMA_5") private Float ema5;
    @Column(name = "EMA_10") private Float ema10;
    @Column(name = "EMA_20") private Float ema20;
    @Column(name = "EMA_60") private Float ema60;
    @Column(name = "EMA_120") private Float ema120;
    @Column(name = "EMA_200") private Float ema200;
    // 모멘텀
    @Column(name = "RSI_9") private Float rsi9;
    @Column(name = "RSI_14") private Float rsi14;
    @Column(name = "RSI_21") private Float rsi21;
    // MACD
    @Column(name = "MACD_LINE") private Float macdLine;
    @Column(name = "MACD_SIGNAL") private Float macdSignal;
    @Column(name = "MACD_HISTOGRAM") private Float macdHistogram;
    // 스토캐스틱
    @Column(name = "STOCHASTIC_K_14_7") private Float stochasticK147;
    @Column(name = "STOCHASTIC_D_14_7") private Float stochasticD147;
    // 추세
    @Column(name = "ADX_14") private Float adx14;
    @Column(name = "PLUS_DI_14") private Float plusDi14;
    @Column(name = "MINUS_DI_14") private Float minusDi14;
    // 거래량
    @Column(name = "VOLUME_RATIO_20") private Float volumeRatio20;
    @Column(name = "OBV") private Float obv;
    // 볼린저
    @Column(name = "BB_UPPER_20") private Float bbUpper20;
    @Column(name = "BB_MIDDLE_20") private Float bbMiddle20;
    @Column(name = "BB_LOWER_20") private Float bbLower20;
    @Column(name = "BB_PERCENT_B_20") private Float bbPercentB20;
    @Column(name = "BB_WIDTH_20") private Float bbWidth20;
    // 변동성·기타
    @Column(name = "ATR") private Float atr;
    @Column(name = "MFI") private Float mfi;
    @Column(name = "CCI") private Float cci;
    @Column(name = "WILLIAMS_R") private Float williamsR;
    @Column(name = "VOLATILITY_5D") private Float volatility5d;
    @Column(name = "VOLATILITY_20D") private Float volatility20d;
    // 위치비율
    @Column(name = "HIGH_20D_RATIO") private Float high20dRatio;
    @Column(name = "HIGH_52W_RATIO") private Float high52wRatio;
    @Column(name = "LOW_20D_RATIO") private Float low20dRatio;
    @Column(name = "VOLUME_MA20_RATIO") private Float volumeMa20Ratio;
    @Column(name = "GAP_OPEN") private Float gapOpen;
    // 수익률
    @Column(name = "RET_1D") private Float ret1d;
    @Column(name = "RET_5D") private Float ret5d;
    @Column(name = "RET_10D") private Float ret10d;
    // 캔들
    @Column(name = "BODY_RATIO") private Float bodyRatio;
    @Column(name = "LOWER_WICK") private Float lowerWick;
    // 신고저 플래그
    @Column(name = "IS_52W_HIGH") private Boolean is52wHigh;
    @Column(name = "IS_52W_LOW") private Boolean is52wLow;
    @Column(name = "IS_20D_HIGH") private Boolean is20dHigh;
    @Column(name = "IS_20D_LOW") private Boolean is20dLow;

    @Column(name = "CALCULATED_AT", nullable = false)
    @Comment("계산 일시")
    private LocalDateTime calculatedAt;

    public StockFeatureDaily(StockFeatureDailyId id, Integer seq, Long openPrice, Long highPrice, Long lowPrice,
                             Long closePrice, Long volume, Long turnover, LocalDateTime calculatedAt) {
        this.id = id;
        this.seq = seq;
        this.openPrice = openPrice;
        this.highPrice = highPrice;
        this.lowPrice = lowPrice;
        this.closePrice = closePrice;
        this.volume = volume;
        this.turnover = turnover;
        this.calculatedAt = calculatedAt;
    }

    /**
     * 계산기가 산출한 지표 값을 해당 컬럼에 채운다. IS_* 플래그는 0/1을 boolean으로 저장한다.
     *
     * @throws IllegalArgumentException 매핑되지 않은 지표 종류
     */
    public void set(IndicatorType type, Double value) {
        if (value == null) return;
        Float v = value.floatValue();
        switch (type) {
            case SMA_5 -> sma5 = v;
            case SMA_10 -> sma10 = v;
            case SMA_20 -> sma20 = v;
            case SMA_50 -> sma50 = v;
            case SMA_60 -> sma60 = v;
            case SMA_120 -> sma120 = v;
            case SMA_200 -> sma200 = v;
            case EMA_5 -> ema5 = v;
            case EMA_10 -> ema10 = v;
            case EMA_20 -> ema20 = v;
            case EMA_60 -> ema60 = v;
            case EMA_120 -> ema120 = v;
            case EMA_200 -> ema200 = v;
            case RSI_9 -> rsi9 = v;
            case RSI_14 -> rsi14 = v;
            case RSI_21 -> rsi21 = v;
            case MACD_LINE -> macdLine = v;
            case MACD_SIGNAL -> macdSignal = v;
            case MACD_HISTOGRAM -> macdHistogram = v;
            case STOCHASTIC_K_14_7 -> stochasticK147 = v;
            case STOCHASTIC_D_14_7 -> stochasticD147 = v;
            case ADX_14 -> adx14 = v;
            case PLUS_DI_14 -> plusDi14 = v;
            case MINUS_DI_14 -> minusDi14 = v;
            case VOLUME_RATIO_20 -> volumeRatio20 = v;
            case OBV -> obv = v;
            case BB_UPPER_20 -> bbUpper20 = v;
            case BB_MIDDLE_20 -> bbMiddle20 = v;
            case BB_LOWER_20 -> bbLower20 = v;
            case BB_PERCENT_B_20 -> bbPercentB20 = v;
            case BB_WIDTH_20 -> bbWidth20 = v;
            case ATR -> atr = v;
            case MFI -> mfi = v;
            case CCI -> cci = v;
            case WILLIAMS_R -> williamsR = v;
            case VOLATILITY_5D -> volatility5d = v;
            case VOLATILITY_20D -> volatility20d = v;
            case HIGH_20D_RATIO -> high20dRatio = v;
            case HIGH_52W_RATIO -> high52wRatio = v;
            case LOW_20D_RATIO -> low20dRatio = v;
            case VOLUME_MA20_RATIO -> volumeMa20Ratio = v;
            case GAP_OPEN -> gapOpen = v;
            case RET_1D -> ret1d = v;
            case RET_5D -> ret5d = v;
            case RET_10D -> ret10d = v;
            case BODY_RATIO -> bodyRatio = v;
            case LOWER_WICK -> lowerWick = v;
            case IS_52W_HIGH -> is52wHigh = value != 0.0;
            case IS_52W_LOW -> is52wLow = value != 0.0;
            case IS_20D_HIGH -> is20dHigh = value != 0.0;
            case IS_20D_LOW -> is20dLow = value != 0.0;
        }
    }

    /**
     * 값이 채워진(NULL 아닌) 지표만 담은 맵을 반환한다. IS_* 플래그는 0.0/1.0으로 변환한다.
     */
    public Map<IndicatorType, Double> toIndicatorMap() {
        Map<IndicatorType, Double> m = new EnumMap<>(IndicatorType.class);
        putIfNotNull(m, IndicatorType.SMA_5, sma5);
        putIfNotNull(m, IndicatorType.SMA_10, sma10);
        putIfNotNull(m, IndicatorType.SMA_20, sma20);
        putIfNotNull(m, IndicatorType.SMA_50, sma50);
        putIfNotNull(m, IndicatorType.SMA_60, sma60);
        putIfNotNull(m, IndicatorType.SMA_120, sma120);
        putIfNotNull(m, IndicatorType.SMA_200, sma200);
        putIfNotNull(m, IndicatorType.EMA_5, ema5);
        putIfNotNull(m, IndicatorType.EMA_10, ema10);
        putIfNotNull(m, IndicatorType.EMA_20, ema20);
        putIfNotNull(m, IndicatorType.EMA_60, ema60);
        putIfNotNull(m, IndicatorType.EMA_120, ema120);
        putIfNotNull(m, IndicatorType.EMA_200, ema200);
        putIfNotNull(m, IndicatorType.RSI_9, rsi9);
        putIfNotNull(m, IndicatorType.RSI_14, rsi14);
        putIfNotNull(m, IndicatorType.RSI_21, rsi21);
        putIfNotNull(m, IndicatorType.MACD_LINE, macdLine);
        putIfNotNull(m, IndicatorType.MACD_SIGNAL, macdSignal);
        putIfNotNull(m, IndicatorType.MACD_HISTOGRAM, macdHistogram);
        putIfNotNull(m, IndicatorType.STOCHASTIC_K_14_7, stochasticK147);
        putIfNotNull(m, IndicatorType.STOCHASTIC_D_14_7, stochasticD147);
        putIfNotNull(m, IndicatorType.ADX_14, adx14);
        putIfNotNull(m, IndicatorType.PLUS_DI_14, plusDi14);
        putIfNotNull(m, IndicatorType.MINUS_DI_14, minusDi14);
        putIfNotNull(m, IndicatorType.VOLUME_RATIO_20, volumeRatio20);
        putIfNotNull(m, IndicatorType.OBV, obv);
        putIfNotNull(m, IndicatorType.BB_UPPER_20, bbUpper20);
        putIfNotNull(m, IndicatorType.BB_MIDDLE_20, bbMiddle20);
        putIfNotNull(m, IndicatorType.BB_LOWER_20, bbLower20);
        putIfNotNull(m, IndicatorType.BB_PERCENT_B_20, bbPercentB20);
        putIfNotNull(m, IndicatorType.BB_WIDTH_20, bbWidth20);
        putIfNotNull(m, IndicatorType.ATR, atr);
        putIfNotNull(m, IndicatorType.MFI, mfi);
        putIfNotNull(m, IndicatorType.CCI, cci);
        putIfNotNull(m, IndicatorType.WILLIAMS_R, williamsR);
        putIfNotNull(m, IndicatorType.VOLATILITY_5D, volatility5d);
        putIfNotNull(m, IndicatorType.VOLATILITY_20D, volatility20d);
        putIfNotNull(m, IndicatorType.HIGH_20D_RATIO, high20dRatio);
        putIfNotNull(m, IndicatorType.HIGH_52W_RATIO, high52wRatio);
        putIfNotNull(m, IndicatorType.LOW_20D_RATIO, low20dRatio);
        putIfNotNull(m, IndicatorType.VOLUME_MA20_RATIO, volumeMa20Ratio);
        putIfNotNull(m, IndicatorType.GAP_OPEN, gapOpen);
        putIfNotNull(m, IndicatorType.RET_1D, ret1d);
        putIfNotNull(m, IndicatorType.RET_5D, ret5d);
        putIfNotNull(m, IndicatorType.RET_10D, ret10d);
        putIfNotNull(m, IndicatorType.BODY_RATIO, bodyRatio);
        putIfNotNull(m, IndicatorType.LOWER_WICK, lowerWick);
        putFlag(m, IndicatorType.IS_52W_HIGH, is52wHigh);
        putFlag(m, IndicatorType.IS_52W_LOW, is52wLow);
        putFlag(m, IndicatorType.IS_20D_HIGH, is20dHigh);
        putFlag(m, IndicatorType.IS_20D_LOW, is20dLow);
        return m;
    }

    private static void putIfNotNull(Map<IndicatorType, Double> m, IndicatorType type, Float v) {
        if (v != null) m.put(type, v.doubleValue());
    }

    private static void putFlag(Map<IndicatorType, Double> m, IndicatorType type, Boolean v) {
        if (v != null) m.put(type, v ? 1.0 : 0.0);
    }
}
