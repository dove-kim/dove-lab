package com.dove.stockdata.entity;

import com.dove.commonjpa.converter.LocalDateToTimestampConverter;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@Entity
@Table(name = "STOCK_DATE")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class StockDate {
    @Id
    @Column(name = "DATE", nullable = false, unique = true)
    @Convert(converter = LocalDateToTimestampConverter.class)
    private LocalDate date;
}
