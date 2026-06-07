package com.dove.stock.domain.entity;

import com.dove.stock.domain.enums.StockEventType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 종목 권리 이벤트 (배당·유무상증자·합병/분할·액면교체·감자). KIS 예탁원정보(KSD)에서 수집.
 * (ticker, event_type, event_date) 조합으로 중복 방지.
 */
@Getter
@Entity
@Table(name = "STOCK_EVENT",
        uniqueConstraints = @UniqueConstraint(
                name = "UK_SE_TICKER_TYPE_DATE",
                columnNames = {"TICKER", "EVENT_TYPE", "EVENT_DATE"}),
        indexes = {
                @Index(name = "IDX_SE_TICKER_DATE", columnList = "TICKER, EVENT_DATE"),
                @Index(name = "IDX_SE_EVENT_DATE", columnList = "EVENT_DATE")
        })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "TICKER", nullable = false, length = 20)
    @Comment("종목코드")
    private String ticker;

    @Enumerated(EnumType.STRING)
    @Column(name = "EVENT_TYPE", nullable = false, length = 20)
    @Comment("이벤트 유형 (StockEventType)")
    private StockEventType eventType;

    @Column(name = "EVENT_DATE", nullable = false)
    @Comment("이벤트 기준일 (KSD record_date)")
    private LocalDate eventDate;

    @Column(name = "SUMMARY", length = 200)
    @Comment("표시용 요약 (예: 현금배당 600원, 무상증자 배정율 100%)")
    private String summary;

    @Column(name = "DETAIL", columnDefinition = "TEXT")
    @Comment("KSD 원본 응답 JSON")
    private String detail;

    @Column(name = "SOURCE", length = 20)
    @Comment("수집 출처 (예: KIS_KSD)")
    private String source;

    @CreationTimestamp
    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    @Comment("DB 최초 등록 일시")
    private LocalDateTime createdAt;

    public StockEvent(String ticker, StockEventType eventType, LocalDate eventDate,
                      String summary, String detail, String source) {
        this.ticker = ticker;
        this.eventType = eventType;
        this.eventDate = eventDate;
        this.summary = summary;
        this.detail = detail;
        this.source = source;
    }
}
