package com.dove.portfolio.application.service;

import java.time.LocalDate;

/**
 * XIRR 계산용 외부 현금흐름 1건 — 투자자 관점 부호(납입=음수, 인출=양수).
 *
 * @param date      발생 일자
 * @param amountKrw 금액(원화, 부호 포함)
 */
public record ExternalFlow(LocalDate date, long amountKrw) {}
