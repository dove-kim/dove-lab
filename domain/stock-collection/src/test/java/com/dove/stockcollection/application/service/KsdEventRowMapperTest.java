package com.dove.stockcollection.application.service;

import com.dove.stock.domain.enums.StockEventType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class KsdEventRowMapperTest {

    private final KsdEventRowMapper mapper = new KsdEventRowMapper(new ObjectMapper());

    private static Map<String, Object> row(String... kv) {
        Map<String, Object> m = new HashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put(kv[i], kv[i + 1]);
        return m;
    }

    @Nested
    @DisplayName("식별 필드 추출")
    class Identity {

        @Test
        @DisplayName("ticker/recordDate를 원시 키에서 뽑는다")
        void shouldExtractTickerAndDate() {
            Map<String, Object> r = row("sht_cd", "005930", "record_date", "20240105");

            assertThat(mapper.ticker(r)).isEqualTo("005930");
            assertThat(mapper.recordDate(r)).isEqualTo(LocalDate.of(2024, 1, 5));
        }

        @Test
        @DisplayName("ticker 없으면 빈 문자열")
        void shouldReturnEmptyTickerWhenMissing() {
            assertThat(mapper.ticker(row())).isEmpty();
        }

        @Test
        @DisplayName("record_date 형식이 어긋나면 null")
        void shouldReturnNullDateWhenMalformed() {
            assertThat(mapper.recordDate(row("record_date", "2024"))).isNull();
            assertThat(mapper.recordDate(row("record_date", "20241345"))).isNull(); // 13월
            assertThat(mapper.recordDate(row())).isNull();
        }
    }

    @Nested
    @DisplayName("요약 문자열")
    class Summary {

        @Test
        @DisplayName("배당 — 0-padding 금액을 사람이 읽기 좋게 정리한다")
        void shouldFormatDividendAndStripZeroPadding() {
            Map<String, Object> r = row("divi_kind", "현금", "per_sto_divi_amt", "000000000600", "divi_rate", "12");

            assertThat(mapper.summary(StockEventType.DIVIDEND, r))
                    .isEqualTo("배당 현금 주당 600원 (액면배당률 12%)");
        }

        @Test
        @DisplayName("액면교체 — 변경 전/후 금액")
        void shouldFormatParChange() {
            Map<String, Object> r = row("inter_bf_face_amt", "000005000", "inter_af_face_amt", "000000500");

            assertThat(mapper.summary(StockEventType.PAR_CHANGE, r))
                    .isEqualTo("액면교체 5000원 → 500원");
        }

        @Test
        @DisplayName("빈 금액은 '-'로 표시")
        void shouldShowDashForBlankNumber() {
            assertThat(mapper.summary(StockEventType.RIGHTS_ISSUE, row("fix_rate", "30")))
                    .isEqualTo("유상증자 배정율 30% 발행가 -원");
        }
    }

    @Nested
    @DisplayName("원본 JSON")
    class Json {

        @Test
        @DisplayName("원본 행을 JSON 문자열로 직렬화한다")
        void shouldSerializeRowToJson() {
            String json = mapper.toJson(row("sht_cd", "005930", "divi_rate", "12"));

            assertThat(json).contains("\"sht_cd\":\"005930\"").contains("\"divi_rate\":\"12\"");
        }
    }
}
