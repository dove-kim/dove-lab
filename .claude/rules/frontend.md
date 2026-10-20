# 프론트엔드 규칙 (Next.js)

- CSS 직접 작성 금지. 스타일은 **Tailwind CSS**만 사용.

## 디렉터리 구조 (Next.js App Router)

```
src/
  app/           라우팅만. Server Component로 데이터 fetch → containers 컴포넌트에 props 전달
  containers/    기능(메뉴) 단위 폴더. URL 구조가 아닌 도메인/기능 단위로 구성.
                 해당 기능에서만 쓰는 Client Component, 상수, 로직.
  components/    진짜 공통 컴포넌트만 (여러 페이지에서 재사용되는 것)
  services/      외부 통신 (backend.ts — backendFetch, unauthorized, safeJson)
  utils/         순수 함수 유틸 (cx.ts, jwt.ts, filter.ts 등)
  types/         타입 정의만. 함수/로직은 utils/로
  styles/        globals.css (Tailwind import)
  hooks/         공통 커스텀 훅 (필요 시)
  states/        전역 상태 (필요 시)
```

## containers/ 구조

`app/` URL 계층과 별개로, **메뉴/기능 단위**로 구성한다. 동일 기능의 하위 화면은 같은 폴더 아래에 모은다.

```
containers/
  dashboard/              # 대시보드
  stock-search/           # STOCK_SEARCH 기능 전체
    main/                 # /stock-search (종목 검색 화면)
    filters/              # /search-filters (필터 관리)
    stock-sets/           # /stock-sets (종목 세트 관리)
  settings/               # /settings (메뉴 설정)
  admin/                  # /admin (기능 권한 관리, ADMIN 이상)
  root/                   # /root (사용자 관리·초대 코드, ROOT 전용)
```

새 기능이 추가될 때 (`STOCK_LEDGER`, `BUDGET` 등) 동일 패턴으로 최상위 폴더를 추가한다.

## 컴포넌트 규칙

- `app/` 페이지는 Server Component. 쿠키 확인·데이터 fetch 후 `containers/` 컴포넌트에 props 전달.
- 인증 필요 페이지: `cookies().get("token")` 없으면 `redirect("/login")`.
- 클라이언트 상태·이벤트가 필요한 컴포넌트만 `"use client"` + `containers/`.
- 여러 페이지에서 쓰지 않으면 `components/`에 넣지 않음.

## API 라우트 규칙

- Next.js API 라우트(`app/api/`)는 백엔드 프록시 역할. 쿠키에서 JWT를 읽어 `Authorization: Bearer` 헤더로 백엔드 전달.
- 인증: `backendFetch`가 토큰 없으면 null 반환 → `unauthorized()` 응답.
- ADMIN 전용 라우트: JWT 디코딩 후 role 확인 (`decodeJwtPayload`). 403 반환.
- 에러 코드는 영문 대문자 스네이크케이스로 JSON body에 포함.

## JWT 처리

- JWT 디코드: `utils/jwt.ts`의 `decodeJwtPayload(token)` 단일 함수 사용. 인라인 중복 금지.
- 클라이언트에서는 httpOnly 쿠키를 직접 읽을 수 없음 → 서버 컴포넌트 / API 라우트에서만 디코드.

## UI 가시성·조작성 원칙

- **모든 화면 크기에서** 요소가 눈에 띄고 누르기 쉬워야 한다.
- 터치 타겟은 충분히 크게 (최소 44×44px 수준).
- 선택 컨트롤은 `<select>` 또는 명확한 크기의 버튼 그룹 사용. 소형 SVG 아이콘 버튼 단독 사용 금지.
- 모달/드로어처럼 화면 전체를 활용하는 UI 패턴을 적극 사용 — 작은 팝오버보다 고정 크기 모달이 낫다.
- 의심스러우면 더 크게, 더 여백 있게.

## 스타일 토큰

- `utils/cx.ts`의 `cx` 객체에서 공통 Tailwind 클래스 조합 관리 (`cx.input`, `cx.btnPrimary`, `cx.table.*` 등).
- 반복되는 클래스 조합이 생기면 `cx`에 추가. 인라인 중복 금지.
