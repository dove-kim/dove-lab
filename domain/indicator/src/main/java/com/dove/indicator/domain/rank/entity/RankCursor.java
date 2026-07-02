package com.dove.indicator.domain.rank.entity;

import com.dove.stock.domain.converter.MarketUniverseCodeConverter;
import com.dove.stock.domain.converter.PriceTypeCodeConverter;
import com.dove.stock.domain.enums.MarketUniverse;
import com.dove.stock.domain.enums.PriceType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * (universe, 가격유형) 단위로 횡단면 순위 계산이 완료된 마지막 거래일을 추적하는 날짜축 커서.
 */
@Getter
@Entity
@Table(name = "RANK_CURSOR",
        uniqueConstraints = @UniqueConstraint(
                name = "UK_RC_UNIVERSE_PRICETYPE",
                columnNames = {"UNIVERSE", "PRICE_TYPE"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RankCursor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Convert(converter = MarketUniverseCodeConverter.class)
    @Column(name = "UNIVERSE", nullable = false)
    @Comment("universe 코드(KRX/KONEX)")
    private MarketUniverse universe;

    @Convert(converter = PriceTypeCodeConverter.class)
    @Column(name = "PRICE_TYPE", nullable = false)
    @Comment("주가 유형 (RAW/ADJUSTED)")
    private PriceType priceType;

    @Column(name = "CURSOR_DATE")
    @Comment("순위 계산이 완료된 마지막 거래일. NULL이면 처음부터 계산")
    private LocalDate cursorDate;

    @Column(name = "UPDATED_AT", nullable = false)
    @Comment("최종 갱신 일시")
    private LocalDateTime updatedAt;

    public RankCursor(MarketUniverse universe, PriceType priceType) {
        this.universe = universe;
        this.priceType = priceType;
        this.cursorDate = null;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 커서를 지정 거래일로 전진시키고 갱신 일시를 기록한다.
     */
    public void advance(LocalDate newCursorDate) {
        this.cursorDate = newCursorDate;
        this.updatedAt = LocalDateTime.now();
    }
}
