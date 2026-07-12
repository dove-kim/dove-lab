package com.dove.screening.domain.entity;

import com.dove.market.domain.enums.MarketType;
import com.dove.screening.domain.converter.FilterExpressionConverter;
import com.dove.screening.domain.converter.MarketTypeListConverter;
import com.dove.screening.domain.enums.DateRule;
import com.dove.screening.domain.enums.FilterVenue;
import com.dove.screening.domain.value.FilterExpression;
import com.dove.stock.domain.enums.PriceType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 회원별 지표 검색 필터.
 */
@Entity
@Table(
    name = "SEARCH_FILTER",
    uniqueConstraints = {
        @UniqueConstraint(name = "UK_SEARCH_FILTER_MEMBER_NAME", columnNames = {"MEMBER_ID", "NAME"})
    },
    indexes = {
        @Index(name = "IDX_SEARCH_FILTER_MEMBER_ID", columnList = "MEMBER_ID"),
        @Index(name = "IDX_SEARCH_FILTER_SF_ID", columnList = "STOCK_FILTER_ID")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SearchFilter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    @Comment("검색 필터 고유 ID")
    private Long id;

    @Column(name = "MEMBER_ID", nullable = false)
    @Comment("소유 회원 ID")
    private Long memberId;

    @Column(name = "NAME", nullable = false, length = 100)
    @Comment("필터 이름 (사용자 내 고유)")
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "DATE_RULE", nullable = false, length = 20)
    @Comment("날짜 규칙 (LATEST/SPECIFIC_DATE/PREV_1D/PREV_3D/PREV_5D/PREV_10D)")
    private DateRule dateRule;

    @Convert(converter = MarketTypeListConverter.class)
    @Column(name = "MARKETS", nullable = false, columnDefinition = "JSON")
    @Comment("대상 시장 (JSON 배열, 예: [\"KOSPI\",\"KOSDAQ\"])")
    private List<MarketType> markets;

    @Enumerated(EnumType.STRING)
    @Column(name = "PRICE_TYPE", nullable = false, length = 10)
    @Comment("주가 유형 (RAW=비수정/ADJUSTED=수정)")
    private PriceType priceType;

    @Enumerated(EnumType.STRING)
    @Column(name = "EXCHANGE", nullable = false, length = 20)
    @Comment("지표 데이터 거래소 (KRX/NXT/INTEGRATED)")
    private FilterVenue exchange;

    @Convert(converter = FilterExpressionConverter.class)
    @Column(name = "EXPRESSION", nullable = false, columnDefinition = "TEXT")
    @Comment("지표 검색 식 (JSON 트리)")
    private FilterExpression expression;

    @Column(name = "PIPELINE", columnDefinition = "TEXT")
    @Comment("순서 단계 목록(JSON, null=단순 필터)")
    private String pipeline;

    @Column(name = "STOCK_FILTER_ID")
    @Comment("적용할 종목 필터 ID (null=필터 없음)")
    private Long stockFilterId;

    @Column(name = "DISPLAY_ORDER", nullable = false)
    @Comment("목록 노출 순서 (낮을수록 위)")
    private int displayOrder;

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    @Comment("생성일시")
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT", nullable = false)
    @Comment("수정일시")
    private LocalDateTime updatedAt;

    /**
     * 새 검색 필터를 생성한다. 거래소는 KRX로 둔다.
     */
    public static SearchFilter create(Long memberId, String name, DateRule dateRule,
                                       List<MarketType> markets, PriceType priceType, FilterExpression expression,
                                       Long stockFilterId) {
        return create(memberId, name, dateRule, markets, priceType, FilterVenue.KRX, expression, stockFilterId, null);
    }

    /**
     * 새 검색 필터를 생성한다.
     */
    public static SearchFilter create(Long memberId, String name, DateRule dateRule,
                                       List<MarketType> markets, PriceType priceType, FilterVenue exchange,
                                       FilterExpression expression, Long stockFilterId, String pipeline) {
        SearchFilter f = new SearchFilter();
        f.memberId = memberId;
        f.name = name;
        f.dateRule = dateRule;
        f.markets = markets;
        f.priceType = priceType != null ? priceType : PriceType.RAW;
        f.exchange = exchange != null ? exchange : FilterVenue.KRX;
        f.expression = expression;
        f.pipeline = pipeline;
        f.stockFilterId = stockFilterId;
        f.createdAt = LocalDateTime.now();
        f.updatedAt = LocalDateTime.now();
        return f;
    }

    /**
     * 필터 속성을 갱신한다. 거래소는 KRX로 둔다.
     */
    public void update(String name, DateRule dateRule, List<MarketType> markets, PriceType priceType,
                       FilterExpression expression, Long stockFilterId) {
        update(name, dateRule, markets, priceType, FilterVenue.KRX, expression, stockFilterId, null);
    }

    /**
     * 필터 이름·날짜 규칙·시장·주가 유형·거래소·표현식·파이프라인·종목 필터 연결을 갱신한다.
     */
    public void update(String name, DateRule dateRule, List<MarketType> markets, PriceType priceType,
                       FilterVenue exchange, FilterExpression expression, Long stockFilterId, String pipeline) {
        this.name = name;
        this.dateRule = dateRule;
        this.markets = markets;
        this.priceType = priceType != null ? priceType : PriceType.RAW;
        this.exchange = exchange != null ? exchange : FilterVenue.KRX;
        this.expression = expression;
        this.pipeline = pipeline;
        this.stockFilterId = stockFilterId;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateDisplayOrder(int order) {
        this.displayOrder = order;
    }
}
