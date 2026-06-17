package com.dove.screening.infrastructure.repository;

import com.dove.market.domain.enums.MarketType;
import com.dove.stock.domain.enums.NumericField;
import com.dove.stock.domain.enums.TagField;
import com.dove.screening.domain.value.NamePatternCondition;
import com.dove.screening.domain.value.NumericCondition;
import com.dove.screening.domain.value.StockCondition;
import com.dove.screening.domain.value.TagCondition;
import com.dove.stock.domain.entity.QStock;
import com.dove.stock.domain.entity.QStockDetail;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.core.types.dsl.StringExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 종목 분류(태그)·수치 조건을 QueryDSL WHERE 로 변환해 통과 종목 ticker를 조회한다.
 */
@Repository
@RequiredArgsConstructor
public class StockTagFilterRepository {

    private static final String INCLUDE = "INCLUDE";
    private static final String EXCLUDE = "EXCLUDE";

    private final JPAQueryFactory queryFactory;

    /**
     * 태그·수치·종목 조건과 시장 제한을 모두 적용해 통과 ticker 집합을 반환한다.
     * 모든 조건이 비어 있으면 시장 제한만 적용한 전체 종목을 반환한다.
     */
    public Set<String> findTickers(List<TagCondition> tagConds,
                                   List<NumericCondition> numConds,
                                   List<StockCondition> stockConds,
                                   List<MarketType> markets) {
        return findTickers(tagConds, numConds, stockConds, List.of(), markets);
    }

    /**
     * 태그·수치·이름패턴·종목 조건과 시장 제한을 모두 적용해 통과 ticker 집합을 반환한다.
     * 모든 조건이 비어 있으면 시장 제한만 적용한 전체 종목을 반환한다.
     */
    public Set<String> findTickers(List<TagCondition> tagConds,
                                   List<NumericCondition> numConds,
                                   List<StockCondition> stockConds,
                                   List<NamePatternCondition> nameConds,
                                   List<MarketType> markets) {
        QStock stock = QStock.stock;
        QStockDetail detail = QStockDetail.stockDetail;
        BooleanBuilder where = new BooleanBuilder();

        if (markets != null && !markets.isEmpty()) {
            where.and(stock.market.in(markets));
        }

        applyTagConditions(where, tagConds, stock, detail);
        applyNumericConditions(where, numConds, detail);
        applyNamePatternConditions(where, nameConds, detail);

        Set<String> result = new HashSet<>(
                queryFactory.select(stock.ticker)
                        .from(stock)
                        .leftJoin(detail).on(detail.ticker.eq(stock.ticker))
                        .where(where)
                        .fetch());

        applyStockConditions(result, stockConds);
        return result;
    }

    /**
     * INCLUDE: 같은 field 내 IN(OR), field 간 AND. EXCLUDE: ≠value(NULL 보존).
     */
    private void applyTagConditions(BooleanBuilder where, List<TagCondition> tagConds,
                                    QStock stock, QStockDetail detail) {
        if (tagConds == null || tagConds.isEmpty()) return;

        Map<TagField, List<String>> includeByField = new EnumMap<>(TagField.class);
        for (TagCondition c : tagConds) {
            if (!INCLUDE.equals(c.mode())) continue;
            TagField f = TagField.valueOf(c.field());
            includeByField.computeIfAbsent(f, k -> new ArrayList<>()).add(c.value());
        }
        for (Map.Entry<TagField, List<String>> e : includeByField.entrySet()) {
            StringExpression path = tagPath(e.getKey(), stock, detail);
            where.and(path.in(e.getValue()));
        }

        for (TagCondition c : tagConds) {
            if (!EXCLUDE.equals(c.mode())) continue;
            StringExpression path = tagPath(TagField.valueOf(c.field()), stock, detail);
            where.and(path.isNull().or(path.ne(c.value())));
        }
    }

    private void applyNumericConditions(BooleanBuilder where, List<NumericCondition> numConds,
                                        QStockDetail detail) {
        if (numConds == null) return;
        for (NumericCondition c : numConds) {
            NumberPath<Long> path = numPath(NumericField.valueOf(c.field()), detail);
            if (c.min() != null) where.and(path.goe(c.min()));
            if (c.max() != null) where.and(path.loe(c.max()));
        }
    }

    /**
     * INCLUDE: 패턴 중 하나라도 종목명에 포함(OR). EXCLUDE: 패턴 포함 종목 제거(NULL 보존).
     */
    private void applyNamePatternConditions(BooleanBuilder where, List<NamePatternCondition> nameConds,
                                            QStockDetail detail) {
        if (nameConds == null || nameConds.isEmpty()) return;

        BooleanBuilder includeOr = new BooleanBuilder();
        boolean hasInclude = false;
        for (NamePatternCondition c : nameConds) {
            if (INCLUDE.equals(c.mode())) {
                includeOr.or(nameMatches(detail, c));
                hasInclude = true;
            }
        }
        if (hasInclude) where.and(includeOr);

        for (NamePatternCondition c : nameConds) {
            if (EXCLUDE.equals(c.mode())) {
                where.and(detail.prdtAbrvName.isNull().or(nameMatches(detail, c).not()));
            }
        }
    }

    /**
     * 매칭 방식(CONTAINS/STARTS_WITH/ENDS_WITH)에 따른 종목명 술어. 기본은 포함.
     */
    private BooleanExpression nameMatches(QStockDetail detail, NamePatternCondition c) {
        StringExpression name = detail.prdtAbrvName;
        return switch (c.matchType()) {
            case "STARTS_WITH" -> name.startsWith(c.pattern());
            case "ENDS_WITH" -> name.endsWith(c.pattern());
            default -> name.contains(c.pattern());
        };
    }

    /**
     * 종목 조건: EXCLUDE ticker 제거 후 INCLUDE ticker 추가.
     */
    private void applyStockConditions(Set<String> result, List<StockCondition> stockConds) {
        if (stockConds == null) return;
        for (StockCondition c : stockConds) {
            if (EXCLUDE.equals(c.mode())) result.remove(c.stockCode());
        }
        for (StockCondition c : stockConds) {
            if (INCLUDE.equals(c.mode())) result.add(c.stockCode());
        }
    }

    private StringExpression tagPath(TagField f, QStock stock, QStockDetail detail) {
        return switch (f) {
            case SECUGRP -> stock.secugrpNm;
            case STOCK_TYPE -> stock.kindStkCertTpNm;
            case INDUSTRY_LCLS -> detail.idxBztpLclsNm;
            case INDUSTRY_MCLS -> detail.idxBztpMclsNm;
            case INDUSTRY_SCLS -> detail.idxBztpSclsNm;
            case STD_INDUSTRY -> detail.stdIdstClsfNm;
            case PRDT_CLSF -> detail.prdtClsfNm;
            case KOSPI200 -> detail.kospi200ItemYn;
            case TR_STOP -> detail.trStopYn;
            case ADMIN_ITEM -> detail.admnItemYn;
        };
    }

    private NumberPath<Long> numPath(NumericField f, QStockDetail detail) {
        return switch (f) {
            case CAPITAL_AMOUNT -> detail.capitalAmount;
            case FACE_VALUE -> detail.faceValue;
            case LISTED_SHARES -> detail.listedShares;
        };
    }
}
