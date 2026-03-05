package com.dove.technicalindicator.domain.enums;

/** 기술적 지표 유형. SMA, RSI, MACD, 볼린저밴드, 스토캐스틱, ADX, 거래량 지표 등. */
public enum IndicatorType {
    SMA_5, SMA_20, SMA_50, SMA_60, SMA_120, SMA_200,
    RSI_14,
    MACD_LINE, MACD_SIGNAL, MACD_HISTOGRAM,
    STOCHASTIC_K, STOCHASTIC_D,
    ADX, PLUS_DI, MINUS_DI,
    VOLUME_RATIO, OBV,
    BB_UPPER, BB_MIDDLE, BB_LOWER,
    ATR,
    MFI,
    CCI,
    WILLIAMS_R
}
