package com.dove.technicalindicator.domain.calculator;

import com.dove.stockdata.domain.entity.StockData;
import com.dove.technicalindicator.domain.enums.IndicatorType;

import java.util.List;
import java.util.Map;

/**
 * 기술적 지표 계산기 인터페이스.
 * 지표별로 필요한 데이터 수(requiredDataSize)를 선언하고, 종가 기반으로 지표를 계산한다.
 */
public interface TechnicalIndicatorCalculator {

    String getName();

    int requiredDataSize();

    Map<IndicatorType, Double> calculate(List<StockData> stockDataList);
}
