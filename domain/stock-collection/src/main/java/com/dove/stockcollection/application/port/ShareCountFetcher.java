package com.dove.stockcollection.application.port;

import java.time.LocalDate;
import java.util.List;

/**
 * 거래소 상장주식수 조회 포트.
 */
public interface ShareCountFetcher {

    /**
     * 해당 거래일의 전 종목 상장주식수를 조회한다.
     * 데이터가 없는 날짜(휴장·미래·오류)는 빈 리스트를 반환한다.
     */
    List<ShareCountRow> fetch(LocalDate date);
}
