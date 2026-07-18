package com.dove.portfolio.application.port;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 원통화 1단위당 원화 환율 조회 결과.
 *
 * @param currency  원통화 코드(예: USD)
 * @param rateToKrw 원통화 1단위당 원화
 * @param rateDate  환율 고시 일자
 */
public record FxQuote(String currency, BigDecimal rateToKrw, LocalDate rateDate) {}
