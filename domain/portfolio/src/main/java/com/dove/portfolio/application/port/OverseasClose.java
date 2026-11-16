package com.dove.portfolio.application.port;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 해외 종목의 일자별 종가 한 건.
 *
 * @param asOf  종가 기준 일자
 * @param close 종가(원통화)
 */
public record OverseasClose(LocalDate asOf, BigDecimal close) {}
