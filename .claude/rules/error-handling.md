# 에러 처리 계약

## 에러 처리

- 서버는 에러 코드(영문 대문자 스네이크케이스)를 `ResponseStatusException` reason으로 전달.
- `spring.mvc.problemdetails.enabled: true` 활성화 — 에러 코드는 RFC 7807 `detail` 필드로 전달됨.
- 웹(Next.js)은 `detail` 코드를 받아 한국어 메시지로 매핑. 메시지 문자열은 프론트엔드에서만 관리.
- 보안상 단일 메시지로 통합해야 하는 경우(로그인 등)는 프론트에서 하드코딩 허용.
