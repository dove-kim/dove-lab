package com.dove.modelserving.domain.entity;

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

/**
 * 한 (종목, 거래소, 가격유형, 거래일, 모델)의 모델 채점 점수를 담는 행.
 */
@Getter
@Entity
@Table(name = "STOCK_MODEL_SCORE",
        indexes = {
                @Index(name = "IDX_SMS_CHART", columnList = "TICKER, EXCHANGE, PRICE_TYPE, MODEL_ID, TRADE_DATE"),
                @Index(name = "IDX_SMS_SEARCH", columnList = "TRADE_DATE, MODEL_ID, SCORE")
        })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockModelScore {

    @EmbeddedId
    private StockModelScoreId id;

    @Column(name = "SCORE", nullable = false)
    @Comment("모델 출력값(0~1 보정 확률 또는 연속값)")
    private Float score;

    @Column(name = "SCORED_AT", nullable = false)
    @Comment("채점 일시")
    private LocalDateTime scoredAt;

    public StockModelScore(StockModelScoreId id, Float score, LocalDateTime scoredAt) {
        this.id = id;
        this.score = score;
        this.scoredAt = scoredAt;
    }
}
