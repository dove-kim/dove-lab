package com.dove.modelserving.domain.meta;

import java.util.List;

/**
 * 모델 등록 검증에 필요한 meta.json 핵심 필드.
 *
 * @param name        모델 이름
 * @param version     모델 버전
 * @param outputType  출력 종류 문자열(예: "probability")
 * @param features    순서 있는 피처 이름 목록
 * @param featureHash 피처 목록 해시(sha256 16자리)
 * @param entryZone   진입존 정의(없을 수 있음)
 */
public record ModelMeta(String name, String version, String outputType,
                        List<String> features, String featureHash, ModelEntryZone entryZone) {
}
