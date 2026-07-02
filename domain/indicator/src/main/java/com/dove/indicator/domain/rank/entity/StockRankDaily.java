package com.dove.indicator.domain.rank.entity;

import com.dove.indicator.domain.rank.enums.RankType;
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
 * 한 (종목, 거래소, 가격유형, 거래일)의 횡단면 percentile 순위들을 한 행에 담는 wide 순위 행.
 */
@Getter
@Entity
@Table(name = "STOCK_RANK_DAILY",
        indexes = {
                @Index(name = "IDX_SRD_EXCHANGE_DATE", columnList = "EXCHANGE, PRICE_TYPE, TRADE_DATE"),
                @Index(name = "IDX_SRD_TICKER_DATE", columnList = "TICKER, EXCHANGE, PRICE_TYPE, TRADE_DATE")
        })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockRankDaily {

    @EmbeddedId
    private StockRankDailyId id;

    @Column(name = "RANK_RET_1D")
    @Comment("1일 수익률 횡단면 percentile(0~1)")
    private Float rankRet1d;

    @Column(name = "RANK_RET_5D")
    @Comment("5일 수익률 횡단면 percentile(0~1)")
    private Float rankRet5d;

    @Column(name = "RANK_RET_10D")
    @Comment("10일 수익률 횡단면 percentile(0~1)")
    private Float rankRet10d;

    @Column(name = "RANK_VOLUME_RATIO_20")
    @Comment("20일 거래량비율 횡단면 percentile(0~1)")
    private Float rankVolumeRatio20;

    @Column(name = "RANK_RSI_14")
    @Comment("RSI(14) 횡단면 percentile(0~1)")
    private Float rankRsi14;

    @Column(name = "RANK_MACD_HISTOGRAM")
    @Comment("MACD 히스토그램 횡단면 percentile(0~1)")
    private Float rankMacdHistogram;

    @Column(name = "RANK_HIGH_52W_RATIO")
    @Comment("52주 고가대비비율 횡단면 percentile(0~1)")
    private Float rankHigh52wRatio;

    @Column(name = "RANK_VOLATILITY_20D")
    @Comment("20일 변동성 횡단면 percentile(0~1)")
    private Float rankVolatility20d;

    @Column(name = "RANK_TURNOVER")
    @Comment("거래대금 횡단면 percentile(0~1)")
    private Float rankTurnover;

    @Column(name = "CALCULATED_AT", nullable = false)
    @Comment("계산 일시")
    private LocalDateTime calculatedAt;

    public StockRankDaily(StockRankDailyId id, LocalDateTime calculatedAt) {
        this.id = id;
        this.calculatedAt = calculatedAt;
    }

    /**
     * 순위 종류별 percentile 값을 해당 컬럼에 채운다. null은 무시한다.
     *
     * @throws IllegalArgumentException 매핑되지 않은 순위 종류
     */
    public void set(RankType type, Double value) {
        if (value == null) return;
        Float v = value.floatValue();
        switch (type) {
            case RANK_RET_1D -> rankRet1d = v;
            case RANK_RET_5D -> rankRet5d = v;
            case RANK_RET_10D -> rankRet10d = v;
            case RANK_VOLUME_RATIO_20 -> rankVolumeRatio20 = v;
            case RANK_RSI_14 -> rankRsi14 = v;
            case RANK_MACD_HISTOGRAM -> rankMacdHistogram = v;
            case RANK_HIGH_52W_RATIO -> rankHigh52wRatio = v;
            case RANK_VOLATILITY_20D -> rankVolatility20d = v;
            case RANK_TURNOVER -> rankTurnover = v;
        }
    }

    /**
     * 값이 채워진(NULL 아닌) 순위만 담은 맵을 반환한다.
     */
    public Map<RankType, Double> toRankMap() {
        Map<RankType, Double> m = new EnumMap<>(RankType.class);
        putIfNotNull(m, RankType.RANK_RET_1D, rankRet1d);
        putIfNotNull(m, RankType.RANK_RET_5D, rankRet5d);
        putIfNotNull(m, RankType.RANK_RET_10D, rankRet10d);
        putIfNotNull(m, RankType.RANK_VOLUME_RATIO_20, rankVolumeRatio20);
        putIfNotNull(m, RankType.RANK_RSI_14, rankRsi14);
        putIfNotNull(m, RankType.RANK_MACD_HISTOGRAM, rankMacdHistogram);
        putIfNotNull(m, RankType.RANK_HIGH_52W_RATIO, rankHigh52wRatio);
        putIfNotNull(m, RankType.RANK_VOLATILITY_20D, rankVolatility20d);
        putIfNotNull(m, RankType.RANK_TURNOVER, rankTurnover);
        return m;
    }

    private static void putIfNotNull(Map<RankType, Double> m, RankType type, Float v) {
        if (v != null) m.put(type, v.doubleValue());
    }
}
