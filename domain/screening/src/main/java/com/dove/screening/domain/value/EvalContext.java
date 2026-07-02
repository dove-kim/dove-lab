package com.dove.screening.domain.value;

import com.dove.indicator.domain.enums.IndicatorType;
import com.dove.indicator.domain.rank.enums.RankType;
import com.dove.market.domain.enums.MarketType;
import com.dove.stock.domain.entity.StockPrice;

import java.util.Map;

/**
 * 필터 식 평가 컨텍스트 — 한 종목의 시장·지표·당일 주가·순위·모델점수·당일 상승비율.
 *
 * @param market      시장
 * @param indicators  지표값 맵
 * @param price       당일 주가
 * @param ranks       횡단면 순위값 맵
 * @param modelScores 모델 식별자별 채점 점수 맵
 * @param breadth       당일 상승비율(universe 단일 스칼라). 없으면 null
 * @param tradingHalted 거래정지 여부
 * @param adminItem     관리종목 여부
 */
public record EvalContext(MarketType market,
                          Map<IndicatorType, Double> indicators,
                          StockPrice price,
                          Map<RankType, Double> ranks,
                          Map<Long, Double> modelScores,
                          Double breadth,
                          boolean tradingHalted,
                          boolean adminItem) {
}
