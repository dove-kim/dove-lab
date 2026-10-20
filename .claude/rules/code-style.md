# 코드 스타일·주석 규칙

## 코드 품질

- 중복 제거. 이름으로 의도 표현. 의존성 명시.
- 메서드 작게·단일 책임. 상태/부수 효과 최소화. 가장 단순한 해결책.
- 각 리팩토링 후 테스트 실행.

### 주석

- **Javadoc은 항상 여러 줄 블록 형식**. 한 줄짜리 `/** ... */` 금지.
  ```java
  /**
   * 무엇을 하는지 한 줄로.
   */
  ```
- **모든 클래스·인터페이스·enum, 그리고 public 메서드에 Javadoc을 단다**(없으면 추가). 단 자명한 것(getter/setter, 단순 오버라이드, 1줄 위임 등)은 생략.
- **클래스·인터페이스 Javadoc은 "무엇을 하는 객체"만 1줄.** 협력 객체·호출 흐름·구현 세부·다른 시스템(예: 프론트엔드) 언급 금지.
- 메서드 Javadoc에는 **"무엇"을 하는지만** 적는다. 내부 로직 절차 설명 금지.
- 예외는 `@throws`로 문서화 (unchecked라도).
- **DTO/record**: 용도 1줄 + 각 컴포넌트를 `@param` 한 줄로 설명.
  ```java
  /**
   * 로그인 요청.
   *
   * @param username   아이디
   * @param password   비밀번호
   * @param rememberMe 로그인 상태 유지 여부
   */
  public record LoginRequest(String username, String password, boolean rememberMe) {}
  ```
- 메서드 내부의 핵심 로직은 **한 줄 인라인 주석**으로만 보충. 자명한 코드엔 주석 달지 않음. 객체·메서드 이름이 모든 걸 설명하게 두고, 주석으로 전부 서술하려 하지 않는다.
- 엔티티 컬럼·테이블의 한글 `@Comment`는 주석이 아니라 스키마 메타데이터이므로 별도 유지.

### 내부 클래스/record 금지

- `private record`, `private static class` 등 **내부(중첩) 타입 정의 금지**.
- 보조 record/VO가 필요하면 **같은 패키지에 최상위 파일**(필요 시 package-private)로 분리한다.

### 서비스의 DTO 변환 금지 (`to~` 금지)

- 서비스는 **DTO/Response 객체로 변환하지 않는다**. `XxxResponse.from(...)`·`toDto(...)` 같은 변환은 **presentation(컨트롤러) 책임**.
- 서비스는 도메인 엔티티/도메인 데이터를 반환하고, 컨트롤러가 DTO로 매핑한다.
- (도메인 엔티티를 생성·저장하는 것은 변환이 아니므로 허용 — 예: 저장 대상 aggregate 구성.)
