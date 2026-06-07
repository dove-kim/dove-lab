package com.dove.indicator.domain.entity;

import com.dove.stock.domain.converter.PriceTypeCodeConverter;
import com.dove.stock.domain.converter.StockExchangeCodeConverter;
import com.dove.stock.domain.enums.PriceType;
import com.dove.stock.domain.enums.StockExchange;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 그룹 (종목, 거래소, 가격유형) 단위의 기술적 지표 계산 커서.
 */
@Getter
@Entity
@Table(name = "INDICATOR_CURSOR",
        uniqueConstraints = @UniqueConstraint(
                name = "UK_IC_TICKER_EXCHANGE_PRICETYPE",
                columnNames = {"TICKER", "EXCHANGE", "PRICE_TYPE"}),
        indexes = @Index(name = "IDX_IC_TICKER", columnList = "TICKER"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IndicatorCursor {

    /** 커서가 없을 때 계산을 시작하는 최초 기준일. */
    public static final LocalDate EARLIEST_DATE = LocalDate.of(1985, 1, 1);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "TICKER", nullable = false, length = 20)
    @Comment("종목코드")
    private String ticker;

    @Convert(converter = StockExchangeCodeConverter.class)
    @Column(name = "EXCHANGE", nullable = false)
    @Comment("거래소")
    private StockExchange exchange;

    @Convert(converter = PriceTypeCodeConverter.class)
    @Column(name = "PRICE_TYPE", nullable = false)
    @Comment("주가 유형 (RAW/ADJUSTED)")
    private PriceType priceType;

    @Column(name = "CURSOR_DATE")
    @Comment("마지막으로 계산 완료된 날짜. NULL이면 처음부터 계산")
    private LocalDate cursorDate;

    @Column(name = "UPDATED_AT", nullable = false)
    @Comment("최종 갱신 일시")
    private LocalDateTime updatedAt;

    public IndicatorCursor(String ticker, StockExchange exchange, PriceType priceType) {
        this.ticker = ticker;
        this.exchange = exchange;
        this.priceType = priceType;
        this.cursorDate = null;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 커서를 지정 날짜로 전진시키고 갱신 일시를 기록한다.
     */
    public void advance(LocalDate newCursorDate) {
        this.cursorDate = newCursorDate;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 커서값으로부터 다음 계산 시작일을 반환한다. null이면 최초 기준일, 있으면 그 다음 날.
     */
    public static LocalDate firstSaveDate(LocalDate cursorDate) {
        return cursorDate == null ? EARLIEST_DATE : cursorDate.plusDays(1);
    }
}
