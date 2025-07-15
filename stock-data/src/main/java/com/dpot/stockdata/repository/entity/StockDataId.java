package com.dpot.stockdata.repository.entity;

import com.dpot.commonjpa.converter.LocalDateToTimestampConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.time.LocalDate;

@Getter
@Embeddable
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockDataId {
    @Column(name = "STOCK_CODE", nullable = false, length = 20)
    private String stockCode;

    @Column(name = "TRADE_DATE", nullable = false)
    @Convert(converter = LocalDateToTimestampConverter.class)
    private LocalDate tradeDate;
}
