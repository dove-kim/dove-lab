# 테스트 규칙 (TDD·구조·인증)

## 방법론

- **TDD Red → Green → Refactor**: 실패 테스트 먼저, 통과시킬 최소 코드, 그 후 리팩토링.
- **Tidy First**: 구조적 변경(rename/move/extract)과 행위적 변경(기능 추가·수정)을 **같은 커밋에 섞지 않음**. 둘 다 필요하면 구조적 먼저.
- 테스트 이름은 행위 기술: `shouldXxxWhenYyy`.
- 결함 수정은 실패 테스트 작성 → 수정.

## 테스트 구조

- **테스트는 실제 코드와 동일 패키지를 미러링한다**(`.controller`/`.service`까지). 상위 기능 패키지에 평평하게 두지 않음.
- **행위/엔드포인트 단위로 `@Nested` 내부 클래스로 묶는다.** 한 컨트롤러 메서드(또는 한 행위 묶음)당 `@Nested` 하나. `@DisplayName`을 계층화: 바깥 클래스=대상, `@Nested`=엔드포인트/행위, 메서드=구체 케이스.
- 빈 테스트 패키지(디렉터리)는 남기지 않는다.
- **Mockito는 strict로 쓴다 — `lenient(...)`·`Strictness.LENIENT`·`@MockitoSettings(strictness = LENIENT)` 금지.** 기본 `STRICT_STUBS` 유지. 'unnecessary stubbing'이 뜨면 느슨하게 풀지 말고 **불필요한 스텁을 제거하거나 케이스별로 테스트를 분리**한다. 공통 스텁 헬퍼는 그것을 호출하는 모든 테스트에서 실제로 쓰일 때만 둔다.

### api 통합 테스트 인증

- **인증은 `@WithApiUser`(test support: `com.dove.api.support`) 어노테이션으로 선언한다.** 클래스/메서드에 `@WithApiUser(role = "ROOT", features = {"STOCK_SEARCH"})` 식. JWT가 stateless라 인증/인가에 DB 계정·토큰 발급이 불필요 — principal을 SecurityContext에 직접 주입한다.
- 인증 없는(401) 케이스는 어노테이션을 생략한다.
- **호출자(principal) 계정을 DB에 저장하지 않는다.** `@WithApiUser(memberId = ...)`로 식별자를 고정하고, 그 회원이 소유하는 도메인 데이터는 같은 memberId로 시드한다.
- 단, 실제 로그인·토큰·자격증명 흐름 자체를 검증하는 테스트(로그인, 비밀번호 변경, `JwtFilter`)는 실제 토큰 경로를 유지한다.
- 권한 변경 대상 회원 등 **대상(target) 엔티티는 도메인 커맨드 서비스로 시드**한다(호출자와 구분).
- 외부 시스템(KIS/Redis 등) 의존 빈은 `@MockitoBean`으로 격리한다.
