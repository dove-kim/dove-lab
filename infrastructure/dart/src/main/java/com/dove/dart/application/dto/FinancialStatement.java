package com.dove.dart.application.dto;

import java.time.LocalDate;
import java.util.Map;

/**
 * DART에서 파싱한 한 공시의 재무제표 — 표준계정코드→금액 맵으로 도메인에 전달.
 *
 * @param corpCode   DART 고유번호
 * @param rceptNo    접수번호
 * @param rceptDt    공시일(접수번호 앞 8자리)
 * @param reportCode 보고서 코드(11011 등)
 * @param fsDiv      재무구분(CFS/OFS)
 * @param amounts    표준계정코드(account_id) → 당기금액
 */
public record FinancialStatement(
        String corpCode,
        String rceptNo,
        LocalDate rceptDt,
        String reportCode,
        String fsDiv,
        Map<String, Long> amounts
) {
}
