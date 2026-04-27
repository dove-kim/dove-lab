package com.dove.stock.domain.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 종목 상장 이력. 개장일별 1:N — 행 존재 = 해당 날짜에 상장 상태였음.
 */
@Getter
@Entity
@Table(name = "STOCK_LISTED_DATE",
        indexes = @Index(name = "IDX_STOCK_LISTED_DATE_MARKET_DATE",
                columnList = "MARKET_TYPE, DATE"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class StockListedDate {
    @EmbeddedId
    private StockListedDateId id;
}
