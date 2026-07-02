package com.dove.api.ops.model.dto;

import java.time.LocalDate;

/**
 * 모델 점수 삭제 요청. 전체·기간·종목 중 하나를 지정하며, 실수 방지를 위해 확인 플래그가 필요하다.
 *
 * @param from    거래일 구간 시작(inclusive). null이면 무제한 하한.
 * @param to      거래일 구간 끝(inclusive). null이면 무제한 상한.
 * @param ticker  특정 종목으로 한정할 때의 티커. null이면 종목 무관.
 * @param confirm 삭제 확인 플래그. true가 아니면 거부한다.
 */
public record DeleteScoresRequest(
        LocalDate from,
        LocalDate to,
        String ticker,
        boolean confirm
) {}
