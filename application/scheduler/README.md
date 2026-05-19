# scheduler

매일 KRX(한국거래소)에서 종목·주가 데이터를 수집하고 기술적 지표를 계산하는 배치 애플리케이션.

## 스케줄

| 시각 (KST) | 잡 | 설명 |
|---|---|---|
| 08:05 (평일) | `DailyMarketJob` | 커서 기준 다음 날부터 전일까지 KRX 수집 → 지표 계산 |
| 12:00 (매일) | `CompletionScanJob` | 2010-01-01 ~ 전일 전체 이력 누락 보정 (시장 데이터 + 지표) |

> **KRX T+1 정책**: D일 데이터는 D+1일 약 08:00 KST 이후 제공된다.

### DailyMarketJob (08:05)

1. `MarketDataCompletionService.syncRange()` — KRX에서 종목·주가 수집·저장
2. OPEN으로 확정된 날짜별 `IndicatorDailyCalculateService.calculateAndSave()` — 기술적 지표 계산·저장

### CompletionScanJob (12:00)

1. `MarketDataCompletionService.scan()` — 누락 시장 데이터 보정 (시장당 최대 처리 날짜 수 제한)
2. `IndicatorCompletionService.scan()` — 주가는 있으나 지표 없는 (종목, 날짜) 재계산

## 환경변수

| 환경변수 | 설명 | 기본값 |
|---|---|---|
| `DB_HOST` | MySQL 호스트 | `127.0.0.1` |
| `DB_PORT` | MySQL 포트 | `3307` |
| `DB_USERNAME` | DB 사용자명 | `dove_app` |
| `DB_PASSWORD` | DB 비밀번호 | `dove1234` |
| `KRX_API_AUTH_KEY` | 한국거래소 API 인증키 | — (필수) |
| `KRX_TARGET_MARKETS` | 조회 시장 CSV | `KOSPI,KOSDAQ` |
| `MARKET_INITIAL_DATE` | 누락 보정 탐색 시작일 | `2010-01-01` |
| `SCHEDULER_THREAD_POOL_SIZE` | 수집·계산 병렬 스레드 수 | `6` |

## 로컬 실행

`local` 프로파일을 활성화하면 스케줄이 비활성화되고 `JOB` 환경변수로 지정한 잡을 즉시 실행 후 프로세스가 종료된다.

### 환경변수 (로컬 전용)

| 환경변수 | 값 | 설명 |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `local` | LocalJobRunner 활성화 (필수) |
| `JOB` | `daily` \| `completion` | 실행할 잡 (필수) |
| `KRX_API_AUTH_KEY` | 발급받은 인증키 | KRX API 인증키 (필수) |
| `MARKET_INITIAL_DATE` | e.g. `2025-11-18` | 조회 시작일 — 미설정 시 `2010-01-01`부터 전체 수집 |
| `KRX_TARGET_MARKETS` | `KOSPI,KOSDAQ` | 대상 시장 (기본값 그대로 사용 가능) |

> `MARKET_INITIAL_DATE`는 범위를 좁혀 API 호출 횟수를 줄인다. 로컬 테스트 시 반드시 지정 권장.

### Linux / macOS

```bash
# CompletionScanJob — 6개월치 누락 보정
SPRING_PROFILES_ACTIVE=local JOB=completion \
  MARKET_INITIAL_DATE=2025-11-18 \
  KRX_API_AUTH_KEY=<key> \
  ./gradlew :scheduler:bootRun

# DailyMarketJob — 최근 1개월 수집
SPRING_PROFILES_ACTIVE=local JOB=daily \
  MARKET_INITIAL_DATE=2026-04-18 \
  KRX_API_AUTH_KEY=<key> \
  ./gradlew :scheduler:bootRun
```

### Windows (PowerShell)

```powershell
# CompletionScanJob — 6개월치 누락 보정
$env:SPRING_PROFILES_ACTIVE="local"; $env:JOB="completion"; $env:MARKET_INITIAL_DATE="2025-11-18"; $env:KRX_API_AUTH_KEY="<key>"; .\gw.ps1 :scheduler:bootRun

# DailyMarketJob — 최근 1개월 수집
$env:SPRING_PROFILES_ACTIVE="local"; $env:JOB="daily"; $env:MARKET_INITIAL_DATE="2026-04-18"; $env:KRX_API_AUTH_KEY="<key>"; .\gw.ps1 :scheduler:bootRun
```

> **Windows 한글 로그**: `gw.ps1`에 UTF-8 인코딩이 설정되어 있어 별도 조치 불필요.

## 테스트 실행

```bash
# Linux / macOS
./gradlew :scheduler:test
```

```powershell
# Windows
.\gw.ps1 :scheduler:test
```

단위 테스트는 Mockito로 외부 의존 없이 실행된다.
통합 테스트(`*IntegrationTest`)는 H2 인메모리 DB를 사용하며 KRX 어댑터를 모킹한다.
