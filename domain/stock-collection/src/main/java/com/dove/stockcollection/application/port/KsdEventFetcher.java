package com.dove.stockcollection.application.port;

import com.dove.stock.domain.enums.StockEventType;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * KIS 예탁원정보(KSD) 권리 이벤트 조회 포트. 인프라(KIS)에 대한 추상화.
 */
public interface KsdEventFetcher {

    /**
     * 지정 유형의 이벤트를 from~to 기간으로 조회한다. 각 원소는 한 건의 필드 맵.
     *
     * @param sht 종목코드. 공백("")이면 전 종목, 특정 종목코드면 그 종목만.
     */
    List<Map<String, Object>> fetch(StockEventType type, LocalDate from, LocalDate to, String sht);
}
