package com.dove.stockkrxdata.entity;

import com.dove.commonjpa.converter.LocalDateTimeToTimestampConverter;
import com.dove.stockkrxdata.enums.KrxDailyDataStatus;
import com.dove.stockkrxdata.enums.KrxMarketType;
import com.dove.commonjpa.converter.LocalDateToTimestampConverter;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "KRX_DAILY_DATA",
        schema = "DOVE_STOCK",
        indexes = {
                @Index(name = "IDX_BASE_DATE", columnList = "BASE_DATE")
        }
)
@NoArgsConstructor
@AllArgsConstructor
public class KrxDailyData {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "BASE_DATE", nullable = false)
    @Convert(converter = LocalDateToTimestampConverter.class)
    private LocalDate baseDate;

    @Column(name = "MARKET_TYPE", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private KrxMarketType krxMarketType;

    @Column(name = "RAW_DATA", columnDefinition = "json")
    private String rawData;

    @Column(name = "DATA_STATUS", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private KrxDailyDataStatus status;

    @Column(name = "API_CALL_AT", nullable = false, updatable = false)
    @Convert(converter = LocalDateTimeToTimestampConverter.class)
    private LocalDateTime apiCallAt;


    @Builder
    private KrxDailyData(LocalDate baseDate, KrxMarketType krxMarketType, String rawData, KrxDailyDataStatus status, LocalDateTime apiCallAt) {
        this.baseDate = baseDate;
        this.krxMarketType = krxMarketType;
        this.rawData = rawData;
        this.status = status;
        this.apiCallAt = apiCallAt;
    }

    public static KrxDailyData success(
            LocalDate baseDate,
            KrxMarketType krxMarketType,
            String rawData,
            LocalDateTime apiCallAt
    ) {
        return KrxDailyData.builder()
                .baseDate(baseDate)
                .krxMarketType(krxMarketType)
                .rawData(rawData)
                .status(KrxDailyDataStatus.SUCCESS)
                .apiCallAt(apiCallAt)
                .build();
    }

    public static KrxDailyData failed(
            LocalDate baseDate,
            KrxMarketType krxMarketType,
            LocalDateTime apiCallAt
    ) {
        return KrxDailyData.builder()
                .baseDate(baseDate)
                .krxMarketType(krxMarketType)
                .status(KrxDailyDataStatus.API_FAILED)
                .apiCallAt(apiCallAt)
                .build();
    }

    public static KrxDailyData authFailed(
            LocalDate baseDate,
            KrxMarketType krxMarketType,
            LocalDateTime apiCallAt
    ) {
        return KrxDailyData.builder()
                .baseDate(baseDate)
                .krxMarketType(krxMarketType)
                .status(KrxDailyDataStatus.API_AUTH_FAILED)
                .apiCallAt(apiCallAt)
                .build();
    }

    public static KrxDailyData responseParseError(
            LocalDate baseDate,
            KrxMarketType krxMarketType,
            LocalDateTime apiCallAt
    ) {
        return KrxDailyData.builder()
                .baseDate(baseDate)
                .krxMarketType(krxMarketType)
                .status(KrxDailyDataStatus.BODY_ERROR)
                .apiCallAt(apiCallAt)
                .build();
    }

    public static KrxDailyData responseNull(
            LocalDate baseDate,
            KrxMarketType krxMarketType,
            LocalDateTime apiCallAt
    ) {
        return KrxDailyData.builder()
                .baseDate(baseDate)
                .krxMarketType(krxMarketType)
                .status(KrxDailyDataStatus.BODY_NULL)
                .apiCallAt(apiCallAt)
                .build();
    }

    public static KrxDailyData unsupportedMarket(LocalDate baseDate, KrxMarketType krxMarketType, LocalDateTime apiCallAt) {
        return KrxDailyData.builder()
                .baseDate(baseDate)
                .krxMarketType(krxMarketType)
                .status(KrxDailyDataStatus.UNSUPPORTED_MARKET_TYPE)
                .apiCallAt(apiCallAt)
                .build();
    }
}
