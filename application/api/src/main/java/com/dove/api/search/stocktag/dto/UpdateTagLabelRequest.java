package com.dove.api.search.stocktag.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 분류 값 표시 라벨 수정 요청.
 *
 * @param label 표시 라벨
 */
public record UpdateTagLabelRequest(
        @NotBlank @Size(max = 130) String label
) {
}
