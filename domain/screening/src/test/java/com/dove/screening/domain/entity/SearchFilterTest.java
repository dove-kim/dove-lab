package com.dove.screening.domain.entity;

import com.dove.market.domain.enums.MarketType;
import com.dove.screening.domain.enums.DateRule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SearchFilterTest {

    private SearchFilter create() {
        return SearchFilter.create(1L, "필터1", DateRule.LATEST,
                List.of(MarketType.KOSPI, MarketType.KOSDAQ),
                "{}", null, null);
    }

    @Test
    @DisplayName("create — 필드 정상 설정")
    void shouldSetFieldsOnCreate() {
        SearchFilter f = create();

        assertThat(f.getMemberId()).isEqualTo(1L);
        assertThat(f.getName()).isEqualTo("필터1");
        assertThat(f.getDateRule()).isEqualTo(DateRule.LATEST);
        assertThat(f.getExpression()).isEqualTo("{}");
        assertThat(f.getIncludeStockSetId()).isNull();
        assertThat(f.getExcludeStockSetId()).isNull();
        assertThat(f.getCreatedAt()).isNotNull();
        assertThat(f.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("getMarketList — 쉼표 구분 문자열 → MarketType 리스트 파싱")
    void shouldParseMarketList() {
        SearchFilter f = create();

        List<MarketType> markets = f.getMarketList();

        assertThat(markets).containsExactlyInAnyOrder(MarketType.KOSPI, MarketType.KOSDAQ);
    }

    @Test
    @DisplayName("getMarketList — 단일 시장")
    void shouldParseSingleMarket() {
        SearchFilter f = SearchFilter.create(1L, "필터", DateRule.SPECIFIC_DATE,
                List.of(MarketType.KOSPI), "{}", null, null);

        assertThat(f.getMarketList()).containsExactly(MarketType.KOSPI);
    }

    @Test
    @DisplayName("update — 모든 필드 갱신")
    void shouldUpdateAllFields() {
        SearchFilter f = create();

        f.update("새이름", DateRule.PREV_5D,
                List.of(MarketType.KOSDAQ), "{\"updated\":true}", 10L, 20L);

        assertThat(f.getName()).isEqualTo("새이름");
        assertThat(f.getDateRule()).isEqualTo(DateRule.PREV_5D);
        assertThat(f.getMarketList()).containsExactly(MarketType.KOSDAQ);
        assertThat(f.getExpression()).isEqualTo("{\"updated\":true}");
        assertThat(f.getIncludeStockSetId()).isEqualTo(10L);
        assertThat(f.getExcludeStockSetId()).isEqualTo(20L);
    }

    @Test
    @DisplayName("updateDisplayOrder — displayOrder 갱신")
    void shouldUpdateDisplayOrder() {
        SearchFilter f = create();

        f.updateDisplayOrder(5);

        assertThat(f.getDisplayOrder()).isEqualTo(5);
    }
}
