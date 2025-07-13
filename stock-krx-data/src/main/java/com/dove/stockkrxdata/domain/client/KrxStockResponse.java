package com.dove.stockkrxdata.domain.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class KrxStockResponse {
    @JsonProperty("OutBlock_1")
    private List<Data> dataList;

    public String toJson() {
        if (dataList == null || dataList.isEmpty()) {
            return "[]";
        }

        return "{\"OutBlock_1\":[" +
                dataList.stream()
                        .map(Data::toJson)
                        .collect(Collectors.joining(",")) +
                "]}";
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Data {
        @JsonProperty("BAS_DD")
        private String baseDateStr;      // 원본 날짜 문자열

        @JsonProperty("ISU_CD")
        private String stockCode;        // 종목코드

        @JsonProperty("ISU_NM")
        private String stockName;        // 종목명

        @JsonProperty("MKT_NM")
        private String marketName;       // 시장구분

        @JsonProperty("SECT_TP_NM")
        private String sectorType;       // 소속부

        @JsonProperty("TDD_CLSPRC")
        private String closingPriceStr;  // 종가 문자열

        @JsonProperty("CMPPREVDD_PRC")
        private String priceChangeStr;   // 대비 문자열

        @JsonProperty("FLUC_RT")
        private String fluctuationRateStr; // 등락률 문자열

        @JsonProperty("TDD_OPNPRC")
        private String openingPriceStr;  // 시가 문자열

        @JsonProperty("TDD_HGPRC")
        private String highPriceStr;     // 고가 문자열

        @JsonProperty("TDD_LWPRC")
        private String lowPriceStr;      // 저가 문자열

        @JsonProperty("ACC_TRDVOL")
        private String tradingVolumeStr; // 거래량 문자열

        @JsonProperty("ACC_TRDVAL")
        private String tradingValueStr;  // 거래대금 문자열

        @JsonProperty("MKTCAP")
        private String marketCapStr;     // 시가총액 문자열

        @JsonProperty("LIST_SHRS")
        private String listedSharesStr;  // 상장주식수 문자열

        public LocalDate getBaseDate() {
            return LocalDate.parse(baseDateStr, DateTimeFormatter.BASIC_ISO_DATE);
        }

        public BigDecimal getClosingPrice() {
            return new BigDecimal(closingPriceStr);
        }

        public BigDecimal getPriceChange() {
            return new BigDecimal(priceChangeStr);
        }

        public BigDecimal getFluctuationRate() {
            return new BigDecimal(fluctuationRateStr);
        }

        public BigDecimal getOpeningPrice() {
            return new BigDecimal(openingPriceStr);
        }

        public BigDecimal getHighPrice() {
            return new BigDecimal(highPriceStr);
        }

        public BigDecimal getLowPrice() {
            return new BigDecimal(lowPriceStr);
        }

        public BigDecimal getTradingVolume() {
            return new BigDecimal(tradingVolumeStr);
        }

        public BigDecimal getTradingValue() {
            return new BigDecimal(tradingValueStr);
        }

        public BigDecimal getMarketCap() {
            return new BigDecimal(marketCapStr);
        }

        public BigDecimal getListedShares() {
            return new BigDecimal(listedSharesStr);
        }

        public String toJson() {
            return String.format(
                    "{\"BAS_DD\":\"%s\"," +
                            "\"ISU_CD\":\"%s\"," +
                            "\"ISU_NM\":\"%s\"," +
                            "\"MKT_NM\":\"%s\"," +
                            "\"SECT_TP_NM\":\"%s\"," +
                            "\"TDD_CLSPRC\":\"%s\"," +
                            "\"CMPPREVDD_PRC\":\"%s\"," +
                            "\"FLUC_RT\":\"%s\"," +
                            "\"TDD_OPNPRC\":\"%s\"," +
                            "\"TDD_HGPRC\":\"%s\"," +
                            "\"TDD_LWPRC\":\"%s\"," +
                            "\"ACC_TRDVOL\":\"%s\"," +
                            "\"ACC_TRDVAL\":\"%s\"," +
                            "\"MKTCAP\":\"%s\"," +
                            "\"LIST_SHRS\":\"%s\"}",
                    baseDateStr, stockCode, stockName, marketName, sectorType,
                    closingPriceStr, priceChangeStr, fluctuationRateStr, openingPriceStr,
                    highPriceStr, lowPriceStr, tradingVolumeStr, tradingValueStr,
                    marketCapStr, listedSharesStr
            );
        }

    }

}

