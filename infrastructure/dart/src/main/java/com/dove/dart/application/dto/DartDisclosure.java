package com.dove.dart.application.dto;

import java.time.LocalDate;

/**
 * DART 정기공시 항목(신규·정정 감시용).
 *
 * @param corpCode   고유번호
 * @param stockCode  종목코드
 * @param rceptNo    접수번호
 * @param rceptDt    접수일
 * @param reportName 보고서명
 * @param amendment  정정 여부(보고서명에 '정정' 포함)
 */
public record DartDisclosure(
        String corpCode,
        String stockCode,
        String rceptNo,
        LocalDate rceptDt,
        String reportName,
        boolean amendment
) {
}
