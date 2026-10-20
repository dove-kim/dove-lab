# dove-lab 개발 규칙 (진입점)

개인용 주식 데이터·전략 플랫폼. **헥사고날 멀티모듈 Spring Boot(api·scheduler) + Next.js 웹**,
**저사양 공유 서버** 위에서 돌아간다. 이 두 가지가 거의 모든 설계 결정의 이유다.

## 항상 지키는 것 (요약)

1. **TDD Red → Green → Refactor.** 결함 수정도 실패 테스트부터. 구조적 변경과 행위적 변경을 섞지 않는다(Tidy First).
2. **커밋은 사용자만.** Claude는 `git commit` 실행 금지 — 작업 단위 완료 시 변경 내역 요약만 보고.
3. **빌드·테스트도 사용자가 직접 실행.** Claude는 필요 시 명령을 제안만 한다.
   JAVA_HOME: `C:/Users/kimza/.jdks/corretto-21.0.7`
4. **자원 예산 안에서 설계한다** — DB 100GB·앱 메모리 768MB·버퍼풀 1.5GB. 풀스캔 의존 기능 금지.
5. **native SQL / JPQL 문자열 금지.** Spring Data JPA 메서드 또는 QueryDSL만.
6. **단순함 우선.** 개인용 도구에 엔터프라이즈급 장치를 넣지 않는다. 최소 장치부터.

## 규칙 문서 지도

작업 성격에 맞는 문서를 **열어서 읽고** 따른다. 이 진입점은 요약일 뿐, 세부는 아래에 있다.

| 문서 | 언제 읽나 |
|---|---|
| [rules/architecture.md](rules/architecture.md) | 새 기능을 어느 모듈에 둘지, 계층 경계(app↔domain↔infra)를 넘을 때 |
| [rules/testing.md](rules/testing.md) | 테스트를 쓸 때 — 패키지 미러링·`@Nested` 구조·strict Mockito·`@WithApiUser` 인증 |
| [rules/code-style.md](rules/code-style.md) | 클래스·메서드를 쓸 때 — Javadoc, 중첩 타입 금지, 서비스의 DTO 변환 금지 |
| [rules/persistence.md](rules/persistence.md) | 쿼리·엔티티·인덱스를 건드리거나 스키마를 바꿀 때(`init.sql`) |
| [rules/resource-budget.md](rules/resource-budget.md) | **대량 테이블·대량 스캔·새 배치를 설계할 때 (필수)** |
| [rules/concurrency-and-jobs.md](rules/concurrency-and-jobs.md) | 병렬 실행·외부 API 호출·스케줄러 잡·수집/지표 커서를 다룰 때 |
| [rules/capability.md](rules/capability.md) | 권한을 추가·게이트할 때(백엔드=capability, 프론트=메뉴 표시) |
| [rules/error-handling.md](rules/error-handling.md) | API 에러 응답을 추가·변경할 때 |
| [rules/frontend.md](rules/frontend.md) | 웹(Next.js)을 건드릴 때 — 디렉터리 구조·Tailwind·JWT·UI 가시성 |

## 모듈 한눈에

```
application/   api(REST) · scheduler(@Scheduled 배치)        ← Driver adapter
domain/        aggregate 단위 모듈(entity+repo+QueryDSL+CQRS service)
infrastructure/ krx · kis · dart · frankfurter               ← Driven adapter
library/       jpa · logging · api-quota · concurrent · datetime · job-status · workspace
```

도메인 모듈 목록과 각 모듈이 소유한 aggregate는 [rules/architecture.md](rules/architecture.md) 참고.
