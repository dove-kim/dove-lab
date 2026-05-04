# 개발 규칙

## 방법론

- **TDD Red → Green → Refactor**: 실패 테스트 먼저, 통과시킬 최소 코드, 그 후 리팩토링.
- **Tidy First**: 구조적 변경(rename/move/extract)과 행위적 변경(기능 추가·수정)을 **같은 커밋에 섞지 않음**. 둘 다 필요하면 구조적 먼저.
- 테스트 이름은 행위 기술: `shouldXxxWhenYyy`.
- 결함 수정은 실패 테스트 작성 → 수정.

## 커밋

- Claude는 `git commit` 실행 금지. 사용자만 커밋.
- 작업 단위 완료 시 변경 내역 요약 보고만.

## 코드 품질

- 중복 제거. 이름으로 의도 표현. 의존성 명시.
- 메서드 작게·단일 책임. 상태/부수 효과 최소화. 가장 단순한 해결책.
- 각 리팩토링 후 테스트 실행.

## 아키텍처 계층

구조는 **헥사고날(Ports & Adapters) 4계층 멀티모듈**, 도메인 모델은 **DDD 전술 패턴**(Aggregate, Bounded Context, Port/ACL)으로 설계 (plan.md 기준):

```
application/           Driver adapter — @KafkaListener, @Scheduled 진입점
  └── service/         조합 유스케이스 (도메인 service + port만 주입)

domain/                Aggregate 단위 모듈 (entity + repo + JPA/QueryDSL + CQRS service)
  market, stock, stock-price, technical-indicator, krx-call-log, event-retry

infrastructure/        Driven adapter — 외부 시스템 연결
  krx                  KRX API 어댑터 (port 구현)
  redis                Redisson 클라이언트
  distributed-lock     Redisson 분산락 + 멱등 가드 AOP

library/               도메인 무관 공통 기술
  jpa, logging
```

각 Fetcher 포트는 자기 `Outcome`(sealed) + `Reason`(enum)을 nested로 소유 (Published Language).
event-retry는 reason을 `String`으로 저장 (ACL). 공용 타입 의존 없음.

## 계층 경계 원칙 (필수)

- **app 모듈은 Repository 직접 주입 금지**. 도메인 모듈의 Query/Command service 또는 port만 주입.
- 모든 모듈은 **CQRS 분리**. 조회 전용은 `*QueryService`, 변경은 `*CommandService`.
- 도메인 모듈은 자기 aggregate의 entity + repo + QueryDSL 구현을 함께 소유 (관심사 응집).
- 조합(여러 aggregate 엮는 유스케이스)은 app의 `service/` 패키지. 별도 application 모듈 없음.
- Port는 관련 aggregate의 domain 모듈에 위치 (`DailyPriceFetcher` → `stock-price`, `StockListingFetcher` → `stock`).

## 쿼리 규칙

- **native SQL / JPQL 문자열 사용 금지**. Spring Data JPA 메서드 또는 QueryDSL만 사용.

## 엔티티 규칙

- 모든 `@Table`에 목적에 맞는 `@Index` 또는 `uniqueConstraints` 명시.
- 쿼리 패턴에 맞는 복합 인덱스를 엔티티 정의에 포함 (PendingEventRetry의 `(EVENT_TYPE, EVENT_KEY)` unique + `NEXT_RETRY_AT` index 참고).
- 신규/변경 컬럼·테이블에 한글 `@Comment` 부착.

## DDL 배포

- 스키마 변경은 **`init.sql`을 단일 진실 원천**으로 유지. 배포는 이 파일로 수행.
- JPA `ddl-auto`는 개발·테스트(H2) 한정. 운영은 `init.sql`.

## 에러 처리

- 서버는 에러 코드(영문 대문자 스네이크케이스)를 `ResponseStatusException` reason으로 전달.
- `spring.mvc.problemdetails.enabled: true` 활성화 — 에러 코드는 RFC 7807 `detail` 필드로 전달됨.
- 웹(Next.js)은 `detail` 코드를 받아 한국어 메시지로 매핑. 메시지 문자열은 프론트엔드에서만 관리.
- 보안상 단일 메시지로 통합해야 하는 경우(로그인 등)는 프론트에서 하드코딩 허용.

## 프론트엔드

- CSS 직접 작성 금지. 스타일은 **Tailwind CSS**만 사용.

## 빌드/테스트 실행

- 사용자가 직접 수행. Claude는 필요 시 제안만.
- JAVA_HOME: `C:/Users/kimza/.jdks/corretto-21.0.7`
- Kafka는 테스트에서 Mockito로 모킹 (EmbeddedKafka 미사용).