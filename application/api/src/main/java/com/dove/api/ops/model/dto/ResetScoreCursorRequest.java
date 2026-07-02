package com.dove.api.ops.model.dto;

import java.time.LocalDate;

/**
 * 채점 커서 리셋 요청.
 *
 * @param toDate 되돌릴 거래일. null이면 미시작(전 점수 삭제)으로 되돌린다.
 */
public record ResetScoreCursorRequest(LocalDate toDate) {}
