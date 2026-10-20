# 권한·메뉴 (capability 기반)

**권한(백엔드)과 메뉴 표시(프론트)는 완전히 분리한다.** 백엔드는 메뉴를 모르고, 프론트가 권한을 읽어 표시만 정한다.

## 백엔드 — 권한만

- **매니페스트** = `Capability` enum (`domain/user-feature .../domain/capability`). **권한 키만** 선언한다. capability는 코드가 정의(엔드포인트가 강제하기에 존재) → 런타임 생성 없음 → enum. **메뉴 위치·라벨·잠금/숨김 정책은 여기 두지 않는다**(프론트 책임).
- **권한(grant)** = `MEMBER_CAPABILITY_GRANT` (사용자별). ADMIN/ROOT가 부여, 회수=행 삭제. 활성 capability 집합은 **JWT claim**에 실어 API가 JWT로 검사(매 요청 DB 조회 안 함, 변경 시 강제 로그아웃).
- **강제는 항상 API** — `@RequireCapability(STOCK_SEARCH)` → 없으면 403. 메뉴는 보안 경계가 아니다.
- **민감 데이터는 응답에서 제거** — 예: `INDICATOR_ML` 없으면 ML 예측 필드를 응답에서 뺀다(UI 숨김만으론 부족). "메뉴인지"는 서버가 몰라도 됨 — 필드 단위 권한일 뿐.

## 프론트 — 메뉴/표시

- **menu manifest**(프론트 소유) = `{ capability, route, group, order, LOCK|HIDE }`. 라벨·라우트·잠금정책 전부 프론트.
- 프론트는 JWT의 capability 집합을 읽어 **API를 때려보기 전에** 미리 렌더 결정:
  - **LOCK**: 권한 없어도 **보이되 잠금**(영역 단위). 첫 탭이 잠겼으면 다음 접근 가능한 탭으로 자동 이동.
  - **HIDE**: 권한 없으면 **숨김**(예: ML). 데이터도 서버가 안 내려줌.
- **메뉴 노출(전역)·순서(사용자)** 는 권한이 아니라 표시 설정 — 프론트 menu-id로 키잉(필요 시 별도 설정 저장).

## 역할(ROLE)

- **USER**: 메뉴 순서만 변경. **ADMIN**: 사용자별 권한(capability) 부여 + 전역 노출. **ROOT**: 전부 + ROOT 고유 불변.
- **관리 화면·ROOT 고유 기능은 capability가 아니라 ROLE로 게이트**(capability 레지스트리에 넣지 않음). ROLE 체계 개선은 별도 과제.

새 권한 추가: `Capability`에 키 한 줄 추가 → 게이트할 API에 `@RequireCapability`(HIDE면 응답 필드도 제거) → 프론트 manifest에 표시 정책 추가.
