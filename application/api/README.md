# api

회원 인증, 주식 데이터 조회, 사용자 기능 권한 관리를 제공하는 REST API 서버.

## 엔드포인트 구조

```
/auth/login              POST  로그인
/auth/register           POST  초대 코드로 회원 가입

/stocks                  GET   종목 목록
/stocks/{code}/prices    GET   일별 주가
                               ?source=KRX|NXT|CONSOLIDATED (기본값 KRX)
                               ?adjusted=true|false          (기본값 true, ROOT 계정은 항상 false)
                               ?limit=N                      (기본값 60)
/stocks/{code}/indicators GET  기술적 지표
                               ?source=KRX|NXT|CONSOLIDATED (기본값 KRX)
                               ?adjusted=true|false          (기본값 true, ROOT 계정은 항상 false)
                               ?limit=N                      (기본값 120)
                               ?types=SMA_5,EMA_20,...       (IndicatorType 이름 복수 가능)
/stocks/{code}/detail    GET   종목 상세 (기본 + STOCK_DETAIL)
/stocks/{code}/events    GET   권리 이벤트 목록 (배당·증자·감자·합병/분할·액면)
/stocks/{code}/invest-opinion GET  종목투자의견 (on-demand, KIS 즉시 호출)
/stocks/{code}/estimate  GET   종목추정실적 (on-demand, KIS 즉시 호출)
/stocks/{code}/investor-flow GET  투자자별 일별 순매수 (개인·기관·외국인)
                               ?source=KRX|NXT|CONSOLIDATED
                               ?limit=N                      (기본값 60, 최신순)
/stocks/{code}/fundamentals   GET  DART 재무제표 이력 (매출·이익·자산·자본 등)
/stocks/{code}/valuations     GET  일별 밸류에이션 (시총·PER·PBR·PSR·GPA)
/stocks/{code}/valuation/latest GET 최신 밸류에이션 (없으면 204)
/stocks/{code}/scores    GET   종목 모델 채점 점수 (차트 오버레이)
/stocks/models           GET   활성 모델 요약 목록

/market/trading-days     GET   거래일 목록

/filters                 CRUD  종목 검색 필터 (저장·조회·실행, 불리언 트리 + 순서 파이프라인)
/filters/{id}/execute    POST  필터 실행 (FILTER·RANK 단계 순차, 등락/시총/거래량 정렬·top-N)
/custom-metrics          GET   접근 허용된 커스텀 지표 요약 (필터 빌더용, CUSTOM_INDICATOR)

/stock-tags              GET   종목 분류(태그) 차원·값 목록 + 수치 필드 (검색·필터 UI 공통)
/admin/stock-tags/{id}/label  PATCH  분류 값 표시명 편집 (ROOT)

/indicator-presets       CRUD  지표 프리셋

/stock-sets              CRUD  종목 세트

/me/menu                 GET / PATCH  내 메뉴 조회·설정

# 포트폴리오 (PORTFOLIO_LEDGER)
/portfolio/accounts          CRUD  계좌
/portfolio/transactions      CRUD  거래 (매수·매도·입출금·배당·이자, 수수료 포함) — 보유·평단·라운드트립은 거래 fold로 파생
/portfolio/holdings          GET/POST/DELETE  보유종목 식별 매핑 + 배당률(/{id}/dividend)·배당추적(/{id}/tracking) 설정
/portfolio/summary           GET   요약 (총자산·순납입·누적손익·XIRR·통화별 현금)
/portfolio/positions         GET   보유 포지션 (평가액·손익·비중, 현재환율 원화 환산)
/portfolio/roundtrips        GET   청산 성과 (라운드트립: 승률·평균수익·보유일)
/portfolio/all/{summary|positions|transactions|roundtrips}  GET  전 계좌 합산 뷰
/portfolio/rebalance-plans   CRUD  리밸런싱 프리셋 (종목·목표비중 저장) — PORTFOLIO_REBALANCE
/portfolio/stock-search      GET   보유·국내 종목 검색 / /overseas  해외 티커 검증 (KIS 즉시 호출)
/portfolio/shares            CRUD  계좌 공유 grant (READ)
/portfolio/shared/{accountId}/{summary|positions|transactions|roundtrips}  GET  공유받은 계좌 열람
/portfolio/shared/{accountId}/transactions  POST  공유받은 계좌에 거래 추가

/admin/users                    GET          회원 목록 (활성만, ADMIN↑)
/admin/users/{id}/capabilities  GET / PATCH  capability 권한 조회·부여·회수 (ADMIN↑)
/admin/users/{id}/menu          GET          사용자 메뉴 미리보기 (ADMIN↑)
/admin/users/{id}/role          PATCH        역할 변경 (ROOT)
/admin/users/{id}/reset-password POST        비밀번호 초기화 → 임시 비밀번호 (ROOT)
/admin/users/{id}               DELETE       회원 탈퇴(soft delete — 행·참조 보존, 로그인 차단·강제 로그아웃) (ROOT)
/admin/custom-metric-grants     GET / PATCH  사용자별 커스텀 지표 접근 부여·회수 (ADMIN↑)

/root/invite-codes       GET / POST  초대 코드 관리 (ROOT)

# 운영 관리 (ROOT) — /admin/ops
/admin/ops/collection/price   POST  주가 재조회(기간) 시작 → 작업ID (범위 ≤어제, 수정주가 재조회는 ADJUSTED_TOTAL/DONE에 별도 표시)
/admin/ops/collection/stock   POST  종목 재조회(기간, KRX) 시작 → 작업ID
/admin/ops/collection/event   POST  권리이벤트(KSD, 기간) 재조회 시작 → 작업ID (백필=종목별 전구간, 일일=날짜범위+캡 시 종목별 보완 → 완전 수집)
/admin/ops/collection/fundamental POST  재무제표(DART, 연도범위) 재조회 시작 → 작업ID
/admin/ops/collection/valuation   POST  일별 밸류에이션 재계산 시작 → 작업ID (외부 API 없이 DB 재계산)
/admin/ops/collection/tasks   GET   수집 작업 목록·상태 폴링
/admin/ops/models             CRUD  ML 모델 등록·조회·활성/비활성·커서리셋·점수삭제
/admin/ops/custom-metrics     CRUD  커스텀 지표 정의(DSL) 등록·활성/비활성·재계산·삭제·미리보기 (ROOT)
/admin/ops/scheduler/status   GET   스케줄러/백필 진행률 (대시보드)
/admin/ops/system-events      GET   수집·계산 운영 이벤트 (KRX/KIS 실패 등)
/admin/ops/api-quota          GET   API 호출 할당량 현황
```

> 투자자동향 재조회는 KIS가 최근 구간만 제공해 백필이 무의미하므로 제거됨 — 과거 데이터는 별도 스크립트(pykrx)로 채운다.

## 환경변수

| 환경변수 | 설명 | 기본값 |
|---|---|---|
| `DB_HOST` | MySQL 호스트 | `127.0.0.1` |
| `DB_PORT` | MySQL 포트 | `3307` |
| `DB_USERNAME` | DB 사용자명 | `dove_app` |
| `DB_PASSWORD` | DB 비밀번호 | `dove1234` |
| `JWT_SECRET` * | JWT 서명 키 (32자 이상) | 로컬용 기본값 |
| `JWT_EXPIRATION_MS` | 액세스 토큰 만료 (ms) | `3600000` (1h) |
| `JWT_REMEMBER_ME_EXPIRATION_MS` | 자동 로그인 토큰 만료 (ms) | `2592000000` (30d) |
| `INIT_ADMIN_USERNAME` | 최초 ROOT 계정 아이디 | — (선택) |
| `INIT_ADMIN_PASSWORD` | 최초 ROOT 계정 비밀번호 | — (선택) |
| `SERVER_PORT` | 서버 포트 | `8081` |

`*` 운영 배포 시 반드시 변경.

## 로컬 실행

```bash
# Linux / macOS — 최초 실행 (ROOT 계정 생성)
INIT_ADMIN_USERNAME=admin INIT_ADMIN_PASSWORD=<pw> ./gradlew :api:bootRun

# Linux / macOS — 이후 실행
./gradlew :api:bootRun
```

```powershell
# Windows — 최초 실행 (ROOT 계정 생성)
$env:INIT_ADMIN_USERNAME="admin"; $env:INIT_ADMIN_PASSWORD="<pw>"; .\gw.ps1 :api:bootRun

# Windows — 이후 실행
.\gw.ps1 :api:bootRun
```

인프라(MySQL) 기동은 루트의 [README.md](../../README.md)를 참고한다.

## 테스트 실행

```bash
# Linux / macOS
./gradlew :api:test
```

```powershell
# Windows
.\gw.ps1 :api:test
```
