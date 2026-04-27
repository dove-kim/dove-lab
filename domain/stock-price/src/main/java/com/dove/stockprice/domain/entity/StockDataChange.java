package com.dove.stockprice.domain.entity;

import com.dove.market.domain.enums.MarketType;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 주가 데이터 변경을 기록하는 Dirty Flag 엔티티.
 * 주가 저장 시 기록되며, 배치가 주기적으로 조회하여 기술적 지표 계산 이벤트를 발행한 뒤 삭제한다.
 * 복합키(시장/종목/거래일)로 동일 데이터의 중복 변경을 자연스럽게 병합한다.
 */
@Getter
@Entity
@Table(name = "STOCK_DATA_CHANGE",
        indexes = @Index(name = "IDX_STOCK_DATA_CHANGE_CREATED_AT", columnList = "CREATED_AT"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockDataChange {
    @EmbeddedId
    private StockDataChangeId id;

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;

    public StockDataChange(MarketType marketType, String stockCode, LocalDate tradeDate) {
        this.id = new StockDataChangeId(marketType, stockCode, tradeDate);
        this.createdAt = LocalDateTime.now();
    }
}
