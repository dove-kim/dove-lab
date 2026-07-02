package com.dove.modelserving.infrastructure.scorer;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * 채점기 stdout JSON 결과.
 *
 * @param status "ok" 또는 "error"
 * @param scores 채점 결과 행들(status=ok일 때)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PredictOutput(
        @JsonProperty("status") String status,
        @JsonProperty("scores") List<ScoredRow> scores) {
}
