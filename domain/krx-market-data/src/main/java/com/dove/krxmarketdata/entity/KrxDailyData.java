package com.dove.krxmarketdata.entity;

import com.dove.krxmarketdata.enums.KrxDailyDataStatus;
import com.dove.stockdata.enums.MarketType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

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
    private LocalDate baseDate;

    @Column(name = "MARKET_TYPE", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private MarketType marketType;

    @Column(name = "RAW_DATA", columnDefinition = "json")
    private String rawData;

    @Column(name = "DATA_STATUS", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private KrxDailyDataStatus status;

    @Column(name = "API_CALL_AT", nullable = false, updatable = false)
    private LocalDate apiCallAt;


    @Builder
    private KrxDailyData(LocalDate baseDate, MarketType marketType, String rawData, KrxDailyDataStatus status, LocalDate apiCallAt) {
        this.baseDate = baseDate;
        this.marketType = marketType;
        this.rawData = rawData;
        this.status = status;
        this.apiCallAt = apiCallAt;
    }

    public static KrxDailyData success(
            LocalDate baseDate,
            MarketType marketType,
            String rawData,
            LocalDate apiCallAt
    ) {
        return KrxDailyData.builder()
                .baseDate(baseDate)
                .marketType(marketType)
                .rawData(rawData)
                .status(KrxDailyDataStatus.SUCCESS)
                .apiCallAt(apiCallAt)
                .build();
    }

    public static KrxDailyData failed(
            LocalDate baseDate,
            MarketType marketType,
            LocalDate apiCallAt
    ) {
        return KrxDailyData.builder()
                .baseDate(baseDate)
                .marketType(marketType)
                .status(KrxDailyDataStatus.API_FAILED)
                .apiCallAt(apiCallAt)
                .build();
    }

    public static KrxDailyData authFailed(
            LocalDate baseDate,
            MarketType marketType,
            LocalDate apiCallAt
    ) {
        return KrxDailyData.builder()
                .baseDate(baseDate)
                .marketType(marketType)
                .status(KrxDailyDataStatus.API_AUTH_FAILED)
                .apiCallAt(apiCallAt)
                .build();
    }

    public static KrxDailyData responseParseError(
            LocalDate baseDate,
            MarketType marketType,
            LocalDate apiCallAt
    ) {
        return KrxDailyData.builder()
                .baseDate(baseDate)
                .marketType(marketType)
                .status(KrxDailyDataStatus.BODY_ERROR)
                .apiCallAt(apiCallAt)
                .build();
    }

    public static KrxDailyData responseNull(
            LocalDate baseDate,
            MarketType marketType,
            LocalDate apiCallAt
    ) {
        return KrxDailyData.builder()
                .baseDate(baseDate)
                .marketType(marketType)
                .status(KrxDailyDataStatus.BODY_NULL)
                .apiCallAt(apiCallAt)
                .build();
    }

    public static KrxDailyData unsupportedMarket(LocalDate baseDate, MarketType marketType, LocalDate apiCallAt) {
        return KrxDailyData.builder()
                .baseDate(baseDate)
                .marketType(marketType)
                .status(KrxDailyDataStatus.UNSUPPORTED_MARKET_TYPE)
                .apiCallAt(apiCallAt)
                .build();
    }
}
