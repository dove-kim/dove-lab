package com.dove.modelserving.application.service;

import java.util.List;

/**
 * 모델 등록 시 meta.json 값을 덮어쓰는 사용자 입력(없으면 meta 값 유지).
 *
 * @param name           모델 이름(blank이면 meta의 name 유지)
 * @param version        버전(blank이면 meta의 version 유지)
 * @param zoneDesc       진입존 설명(null이면 meta의 entry_zone.desc 유지)
 * @param zoneConditions 진입존 조건식 목록(null이면 meta의 entry_zone.conditions 유지)
 */
public record ModelRegistrationOverrides(String name, String version, String zoneDesc,
                                         List<String> zoneConditions) {

    /**
     * 아무것도 덮어쓰지 않는 빈 오버라이드.
     */
    public static ModelRegistrationOverrides none() {
        return new ModelRegistrationOverrides(null, null, null, null);
    }
}
