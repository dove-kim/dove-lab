package com.dove.dart.infrastructure.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * DART 공시검색(list) 응답.
 *
 * @param status    결과 코드
 * @param message   결과 메시지
 * @param totalPage 전체 페이지 수
 * @param list      공시 항목 목록
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DartListResponse(
        @JsonProperty("status") String status,
        @JsonProperty("message") String message,
        @JsonProperty("total_page") Integer totalPage,
        @JsonProperty("list") List<DartListItem> list
) {
}
