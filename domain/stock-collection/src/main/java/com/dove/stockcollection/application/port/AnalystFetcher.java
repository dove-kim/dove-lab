package com.dove.stockcollection.application.port;

import com.dove.stockcollection.domain.model.AnalystEstimate;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 종목 애널리스트 정보(투자의견·추정실적) 조회 포트. 인프라(KIS)에 대한 추상화.
 */
public interface AnalystFetcher {

    /**
     * 종목투자의견을 from~to 기간으로 조회한다. 각 원소는 회원사별 의견 한 건의 필드 맵.
     */
    List<Map<String, Object>> fetchInvestOpinion(String ticker, LocalDate from, LocalDate to);

    /**
     * 종목추정실적을 조회한다. 리서치 커버 종목만 데이터가 존재한다.
     */
    Optional<AnalystEstimate> fetchEstimate(String ticker);
}
