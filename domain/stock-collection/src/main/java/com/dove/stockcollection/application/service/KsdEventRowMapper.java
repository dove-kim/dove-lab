package com.dove.stockcollection.application.service;

import com.dove.stock.domain.enums.StockEventType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * KSD 권리 이벤트 원시 행(Map)을 도메인 저장 형태(종목코드·기준일·요약·원본 JSON)로 변환한다.
 */
@Component
@RequiredArgsConstructor
public class KsdEventRowMapper {

    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final ObjectMapper objectMapper;

    /**
     * 종목 코드(sht_cd).
     */
    public String ticker(Map<String, Object> row) {
        return str(row, "sht_cd");
    }

    /**
     * 기준일(record_date). 형식이 어긋나면 null.
     */
    public LocalDate recordDate(Map<String, Object> row) {
        return parseDate(str(row, "record_date"));
    }

    /**
     * 이벤트 유형별 사람이 읽는 요약 문자열.
     */
    public String summary(StockEventType type, Map<String, Object> r) {
        return switch (type) {
            case DIVIDEND -> "배당 %s 주당 %s원 (액면배당률 %s%%)".formatted(
                    str(r, "divi_kind"), num(r, "per_sto_divi_amt"), str(r, "divi_rate"));
            case RIGHTS_ISSUE -> "유상증자 배정율 %s%% 발행가 %s원".formatted(
                    str(r, "fix_rate"), num(r, "fix_price"));
            case BONUS_ISSUE -> "무상증자 배정율 %s%%".formatted(str(r, "fix_rate"));
            case MERGER_SPLIT -> "%s 비율 %s (%s ↔ %s)".formatted(
                    str(r, "merge_type"), str(r, "merge_rate"), str(r, "cust_nm"), str(r, "opp_cust_nm"));
            case PAR_CHANGE -> "액면교체 %s원 → %s원".formatted(
                    num(r, "inter_bf_face_amt"), num(r, "inter_af_face_amt"));
            case CAP_REDUCTION -> "%s 감자율 %s".formatted(
                    str(r, "reduce_cap_type"), str(r, "reduce_cap_rate"));
        };
    }

    /**
     * 원본 행을 JSON 문자열로 직렬화한다. 실패 시 null.
     */
    public String toJson(Map<String, Object> row) {
        try {
            return objectMapper.writeValueAsString(row);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private static String str(Map<String, Object> r, String key) {
        Object v = r.get(key);
        return v == null ? "" : v.toString().trim();
    }

    /**
     * 0-padding 숫자 문자열을 사람이 읽기 좋게 정리 (예: "000000000600" → "600").
     */
    private static String num(Map<String, Object> r, String key) {
        String s = str(r, key);
        if (s.isBlank()) return "-";
        try {
            return Long.toString(Long.parseLong(s.replace(",", "")));
        } catch (NumberFormatException e) {
            return s;
        }
    }

    private static LocalDate parseDate(String ymd) {
        if (ymd == null || ymd.length() != 8 || !ymd.chars().allMatch(Character::isDigit)) return null;
        try {
            return LocalDate.parse(ymd, YMD);
        } catch (Exception e) {
            return null;
        }
    }
}
