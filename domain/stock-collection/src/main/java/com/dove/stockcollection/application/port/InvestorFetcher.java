package com.dove.stockcollection.application.port;

import java.time.LocalDate;
import java.util.List;

/**
 * 종목별 투자자매매동향 조회 포트. 인프라(KIS)에 대한 추상화.
 */
public interface InvestorFetcher {

    /**
     * ticker의 from~to 기간 투자자매매동향을 조회한다. 실패·데이터 없으면 빈 리스트.
     */
    List<InvestorDailyRow> fetch(String ticker, LocalDate from, LocalDate to);
}
