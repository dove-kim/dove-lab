package com.dove.api.search.stock.dto;

import com.dove.modelserving.domain.entity.StockModelScore;

/**
 * 일자별 모델 채점 점수.
 *
 * @param date  거래일
 * @param score 모델 출력값(0~1 보정 확률 또는 연속값)
 */
public record ModelScoreBar(String date, Double score) {

    /**
     * 점수 엔티티를 차트용 봉으로 변환한다.
     */
    public static ModelScoreBar from(StockModelScore score) {
        return new ModelScoreBar(score.getId().getTradeDate().toString(), score.getScore());
    }
}
