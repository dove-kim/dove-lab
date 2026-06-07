package com.dove.screening.domain.entity;

import com.dove.market.domain.enums.MarketType;
import com.dove.screening.domain.enums.DateRule;
import com.dove.screening.domain.value.FilterExpression;
import com.dove.stock.domain.enums.PriceType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SearchFilterTest {

    private SearchFilter create() {
        return SearchFilter.create(1L, "필터1", DateRule.LATEST,
                List.of(MarketType.KOSPI, MarketType.KOSDAQ), PriceType.RAW,
                FilterExpression.empty(), null);
    }

    @Test
    @DisplayName("create — 필드 정상 설정")
    void shouldSetFieldsOnCreate() {
        SearchFilter f = create();

        assertThat(f.getMemberId()).isEqualTo(1L);
        assertThat(f.getName()).isEqualTo("필터1");
        assertThat(f.getDateRule()).isEqualTo(DateRule.LATEST);
        assertThat(f.getPriceType()).isEqualTo(PriceType.RAW);
        assertThat(f.getExpression()).isNotNull();
        assertThat(f.getStockFilterId()).isNull();
        assertThat(f.getCreatedAt()).isNotNull();
        assertThat(f.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("getMarkets — List<MarketType> 직접 반환")
    void shouldReturnMarketList() {
        SearchFilter f = create();

        assertThat(f.getMarkets()).containsExactlyInAnyOrder(MarketType.KOSPI, MarketType.KOSDAQ);
    }

    @Test
    @DisplayName("getMarkets — 단일 시장")
    void shouldReturnSingleMarket() {
        SearchFilter f = SearchFilter.create(1L, "필터", DateRule.SPECIFIC_DATE,
                List.of(MarketType.KOSPI), PriceType.RAW, FilterExpression.empty(), null);

        assertThat(f.getMarkets()).containsExactly(MarketType.KOSPI);
    }

    @Test
    @DisplayName("update — 모든 필드 갱신")
    void shouldUpdateAllFields() {
        SearchFilter f = create();

        f.update("새이름", DateRule.PREV_5D,
                List.of(MarketType.KOSDAQ), PriceType.ADJUSTED, FilterExpression.parse("{\"updated\":true}"), 10L);

        assertThat(f.getName()).isEqualTo("새이름");
        assertThat(f.getDateRule()).isEqualTo(DateRule.PREV_5D);
        assertThat(f.getMarkets()).containsExactly(MarketType.KOSDAQ);
        assertThat(f.getPriceType()).isEqualTo(PriceType.ADJUSTED);
        assertThat(f.getExpression().toJson()).isEqualTo("{\"updated\":true}");
        assertThat(f.getStockFilterId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("updateDisplayOrder — displayOrder 갱신")
    void shouldUpdateDisplayOrder() {
        SearchFilter f = create();

        f.updateDisplayOrder(5);

        assertThat(f.getDisplayOrder()).isEqualTo(5);
    }
}
