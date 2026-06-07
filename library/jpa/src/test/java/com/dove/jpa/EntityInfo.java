package com.dove.jpa;

/**
 * init.sql 정합성 검증에 쓰는 엔티티 정보 (테이블명 매핑용 클래스명 + 소스).
 *
 * @param className 엔티티 단순 클래스명
 * @param src       엔티티 소스 전문
 */
record EntityInfo(String className, String src) {
}
