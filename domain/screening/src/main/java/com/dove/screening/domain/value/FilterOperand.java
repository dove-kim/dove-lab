package com.dove.screening.domain.value;

/**
 * 검색식 조건이 비교하는 값의 출처.
 */
public sealed interface FilterOperand
        permits IndicatorOperand, PriceOperand, VolumeOperand, TurnoverOperand, ModelScoreOperand, RankOperand, CustomMetricOperand {

    /**
     * 평가 기준일로부터의 거래일 오프셋. 0=기준일, 양수=미래(SEQ+N), 음수=과거(SEQ−N).
     */
    int offset();
}
