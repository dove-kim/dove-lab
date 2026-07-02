package com.dove.dart.infrastructure.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * DART 전체 재무제표(fnlttSinglAcntAll) 응답.
 *
 * @param status  결과 코드(000 정상, 013 무자료, 020 사용한도초과)
 * @param message 결과 메시지
 * @param list    계정 항목 목록
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record FnlttResponse(
        @JsonProperty("status") String status,
        @JsonProperty("message") String message,
        @JsonProperty("list") List<FnlttItem> list
) {
}
