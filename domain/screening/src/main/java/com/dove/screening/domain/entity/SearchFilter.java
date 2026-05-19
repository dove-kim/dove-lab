package com.dove.screening.domain.entity;

import com.dove.market.domain.enums.MarketType;
import com.dove.screening.domain.enums.DateRule;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Table(
    name = "SEARCH_FILTER",
    uniqueConstraints = {
        @UniqueConstraint(name = "UK_SEARCH_FILTER_MEMBER_NAME", columnNames = {"MEMBER_ID", "NAME"})
    },
    indexes = {
        @Index(name = "IDX_SEARCH_FILTER_MEMBER_ID", columnList = "MEMBER_ID")
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

    @Column(name = "MARKETS", nullable = false, length = 50)
    @Comment("대상 시장 (쉼표 구분, 예: KOSPI,KOSDAQ)")
    private String markets;

    @Column(name = "EXPRESSION", nullable = false, columnDefinition = "TEXT")
    @Comment("검색 식 (JSON 트리)")
    private String expression;

    @Column(name = "INCLUDE_STOCK_SET_ID")
    @Comment("포함 종목 세트 ID (nullable)")
    private Long includeStockSetId;

    @Column(name = "EXCLUDE_STOCK_SET_ID")
    @Comment("제외 종목 세트 ID (nullable)")
    private Long excludeStockSetId;

    @Column(name = "DISPLAY_ORDER", nullable = false)
    @Comment("목록 노출 순서 (낮을수록 위)")
    private int displayOrder;

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    @Comment("생성일시")
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT", nullable = false)
    @Comment("수정일시")
    private LocalDateTime updatedAt;

    public List<MarketType> getMarketList() {
        return Arrays.stream(markets.split(","))
                .map(String::trim)
                .map(MarketType::valueOf)
                .collect(Collectors.toList());
    }

    public static SearchFilter create(Long memberId, String name, DateRule dateRule,
                                       List<MarketType> marketList, String expression,
                                       Long includeStockSetId, Long excludeStockSetId) {
        SearchFilter f = new SearchFilter();
        f.memberId = memberId;
        f.name = name;
        f.dateRule = dateRule;
        f.markets = marketList.stream().map(MarketType::name).collect(Collectors.joining(","));
        f.expression = expression;
        f.includeStockSetId = includeStockSetId;
        f.excludeStockSetId = excludeStockSetId;
        f.createdAt = LocalDateTime.now();
        f.updatedAt = LocalDateTime.now();
        return f;
    }

    public void update(String name, DateRule dateRule, List<MarketType> marketList, String expression,
                       Long includeStockSetId, Long excludeStockSetId) {
        this.name = name;
        this.dateRule = dateRule;
        this.markets = marketList.stream().map(MarketType::name).collect(Collectors.joining(","));
        this.expression = expression;
        this.includeStockSetId = includeStockSetId;
        this.excludeStockSetId = excludeStockSetId;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateDisplayOrder(int order) {
        this.displayOrder = order;
    }
}
