package com.dove.modelserving.domain.feature;

/**
 * 한 피처가 어느 wide 테이블의 어느 컬럼에서 읽히는지.
 *
 * @param table  피처 값이 저장된 테이블
 * @param column 그 테이블의 컬럼명(대문자 스네이크케이스)
 */
public record FeatureSource(FeatureTable table, String column) {
}
