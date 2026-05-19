# api

회원 인증, 주식 데이터 조회, 사용자 기능 권한 관리를 제공하는 REST API 서버.

## 엔드포인트 구조

```
/auth/login              POST  로그인
/auth/register           POST  초대 코드로 회원 가입

/stocks                  GET   종목 목록
/stocks/{code}/prices    GET   일별 주가
/stocks/{code}/indicators GET  기술적 지표

/market/trading-days     GET   거래일 목록

/filters                 CRUD  종목 검색 필터 (저장·조회·실행)
/filters/{id}/execute    POST  필터 실행

/indicator-presets       CRUD  지표 프리셋

/stock-sets              CRUD  종목 세트

/me/menu                 GET / PATCH  내 메뉴 조회·설정

/admin/users/{id}/features  PATCH  기능 권한 부여·회수 (ADMIN↑)
/admin/users/{id}/menu      GET    사용자 메뉴 미리보기 (ADMIN↑)

/root/invite-codes       GET / POST  초대 코드 관리 (ROOT)
/root/users              GET / PATCH 사용자 관리 (ROOT)
```

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
