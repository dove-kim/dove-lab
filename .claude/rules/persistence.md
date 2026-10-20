# 쿼리·엔티티·DDL 규칙

## 쿼리 규칙

- **native SQL / JPQL 문자열 사용 금지**. Spring Data JPA 메서드 또는 QueryDSL만 사용. (조건부 update 등도 QueryDSL로 — 예: 커서 CAS 전진.)

## 엔티티 규칙

- 모든 `@Table`에 목적에 맞는 `@Index` 또는 `uniqueConstraints` 명시.
- 쿼리 패턴에 맞는 복합 인덱스를 엔티티 정의에 포함.
- 신규/변경 컬럼·테이블에 한글 `@Comment` 부착.

## DDL 배포

- 스키마 변경은 **`init.sql`을 단일 진실 원천**으로 유지. 배포는 이 파일로 수행.
- JPA `ddl-auto`는 개발·테스트(H2) 한정. 운영은 `init.sql`.
