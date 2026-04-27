package com.dove.krx.infrastructure.client;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("KrxDailyPriceResponse 테스트")
class KrxDailyPriceResponseTest {

    @Nested
    @DisplayName("Data 타입 변환")
    class DataTypeConversion {

        private final KrxDailyPriceResponse.Data data = new KrxDailyPriceResponse.Data(
                "20240115", "005930", "삼성전자", "KOSPI", "",
                "71000", "1000", "1.43",
                "70000", "72000", "69000",
                "1500000", "106500000000", "424000000000000", "5969782550"
        );

        @Test
        @DisplayName("baseDateStr을 LocalDate로 변환한다")
        void shouldParseBaseDate() {
            assertThat(data.getBaseDate()).isEqualTo(LocalDate.of(2024, 1, 15));
        }

        @Test
        @DisplayName("가격 문자열을 Long으로 변환한다")
        void shouldParsePriceStrings() {
            assertThat(data.getClosingPrice()).isEqualTo(71000L);
            assertThat(data.getOpeningPrice()).isEqualTo(70000L);
            assertThat(data.getHighPrice()).isEqualTo(72000L);
            assertThat(data.getLowPrice()).isEqualTo(69000L);
        }

        @Test
        @DisplayName("거래량과 거래대금을 Long으로 변환한다")
        void shouldParseVolumeAndValue() {
            assertThat(data.getTradingVolume()).isEqualTo(1500000L);
            assertThat(data.getTradingValue()).isEqualTo(106500000000L);
        }

        @Test
        @DisplayName("시가총액과 상장주식수를 Long으로 변환한다")
        void shouldParseMarketCapAndShares() {
            assertThat(data.getMarketCap()).isEqualTo(424000000000000L);
            assertThat(data.getListedShares()).isEqualTo(5969782550L);
        }

        @Test
        @DisplayName("대비를 Long으로 변환한다")
        void shouldParsePriceChange() {
            assertThat(data.getPriceChange()).isEqualTo(1000L);
        }
    }

    @Nested
    @DisplayName("toJson 변환")
    class ToJson {

        @Test
        @DisplayName("dataList가 null이면 빈 배열을 반환한다")
        void shouldReturnEmptyArrayWhenNull() {
            KrxDailyPriceResponse response = new KrxDailyPriceResponse(null);
            assertThat(response.toJson()).isEqualTo("[]");
        }

        @Test
        @DisplayName("dataList가 비어있으면 빈 배열을 반환한다")
        void shouldReturnEmptyArrayWhenEmpty() {
            KrxDailyPriceResponse response = new KrxDailyPriceResponse(List.of());
            assertThat(response.toJson()).isEqualTo("[]");
        }

        @Test
        @DisplayName("데이터가 있으면 JSON 문자열로 변환한다")
        void shouldConvertDataToJson() {
            KrxDailyPriceResponse.Data data = new KrxDailyPriceResponse.Data(
                    "20240115", "005930", "삼성전자", "KOSPI", "",
                    "71000", "1000", "1.43",
                    "70000", "72000", "69000",
                    "1500000", "106500000000", "424000000000000", "5969782550"
            );
            KrxDailyPriceResponse response = new KrxDailyPriceResponse(List.of(data));

            String json = response.toJson();

            assertThat(json).startsWith("{\"OutBlock_1\":[");
            assertThat(json).contains("\"ISU_CD\":\"005930\"");
            assertThat(json).contains("\"ISU_NM\":\"삼성전자\"");
            assertThat(json).endsWith("]}");
        }

        @Test
        @DisplayName("여러 데이터가 있으면 쉼표로 구분된 JSON 배열을 반환한다")
        void shouldJoinMultipleDataWithComma() {
            KrxDailyPriceResponse.Data data1 = new KrxDailyPriceResponse.Data(
                    "20240115", "005930", "삼성전자", "KOSPI", "",
                    "71000", "1000", "1.43", "70000", "72000", "69000",
                    "1500000", "0", "0", "0"
            );
            KrxDailyPriceResponse.Data data2 = new KrxDailyPriceResponse.Data(
                    "20240115", "000660", "SK하이닉스", "KOSPI", "",
                    "130000", "2000", "1.56", "128000", "131000", "127000",
                    "500000", "0", "0", "0"
            );
            KrxDailyPriceResponse response = new KrxDailyPriceResponse(List.of(data1, data2));

            String json = response.toJson();

            assertThat(json).contains("\"ISU_CD\":\"005930\"");
            assertThat(json).contains("\"ISU_CD\":\"000660\"");
        }
    }
}
