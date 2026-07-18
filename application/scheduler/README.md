# scheduler

KRX/KIS/DART에서 종목·주가·투자자동향·재무를 수집하고 지표·순위·밸류에이션·모델 점수를 계산하는 배치 애플리케이션.

## 스케줄

각 `@Scheduled` 잡은 **독립 스레드**에서 실행된다(`spring.task.scheduling.pool.size`, 기본 5). 한 잡이 오래 걸려도 다른 잡을 막지 않으며, cron 잡은 자기 자신과 겹치지 않는다(완료 후 다음 발화 계산). 늦게 밀려 실행돼도 커서 멱등이라 안전하다.

| 시각 (KST) | 잡 | 설명 |
|---|---|---|
| 07:30 | `PortfolioMarketDataJob` | 미국장 마감 후 — 보유 해외 종목 종가(KIS) + 원통화 환율(Frankfurter) 스냅샷 갱신 (`PORTFOLIO_QUOTE`/`PORTFOLIO_FX_RATE`) |
| 08:05 | `StockSyncJob` | KRX 당일 종목 스냅샷을 최신 상태로 upsert (신규 종목 포함) |
| 12:00 | `StockDetailJob` | 전 종목 KIS 상세정보(`STOCK_DETAIL`) + 투자자매매동향(`INVESTOR_DAILY`) + 당일 권리이벤트(`STOCK_EVENT`, KSD) 수집 |
| 21:00 | `DailyPipelineOrchestrator` | 당일 주가 수집(하드 게이트) → **병렬** {① 지표 → 순위(rank) → 커스텀 지표(custom-metric) ∥ ② DART 공시 폴링 → 상장주식수 → 밸류에이션} → 모델 채점. 각 단계 실패는 시스템 이벤트로 격리(다음 단계 계속). |
| 일 06:00 | `FundamentalScheduledJobs` | DART 고유번호(corp_code) 주간 동기화 (신규 상장 반영) |

> **KIS 데이터 가용 시각**: 장 마감(15:30) 후 약 20:00 KST부터 당일 주가 조회 가능 → 파이프라인은 21:00.
> 과거 재조회(백필)는 ROOT 전용 비동기 API로 처리(`PendingCollectionJob`). DART 재무 과거 백필·상장주식수·투자자동향 대량 과거 데이터는 별도 스크립트로 채운다.

## 동시성·자원

- 병렬은 `Parallel`(library/concurrent, 가상 스레드 + 세마포어로 동시 수만 제한). 율제한은 `KisGate`가 별도 — KIS 초당 20회는 **Redis 분산**(`RedissonRateLimiter`, 50ms당 1건 균등)이라 api·scheduler를 동시에 띄워도 전체 합산으로 20/초를 지킨다.
- API 작업 동시 수 `collection.concurrency`(기본 40, 대부분 게이트에서 park) / 비-API 지표 `indicator.concurrency`(기본 10, DB·CPU 병목).
- 저사양 공유 서버 가정(약 4코어·코어당 저성능): 지표 동시성·DB 커넥션 풀은 작게, JVM `-Xmx`는 앱당 작게 캡(scheduler 예 768m). 단일 분석 쿼리는 1코어로 처리된다고 보고 풀스캔 의존 대신 인덱스로 범위를 좁힌다.

## 환경변수

| 환경변수 | 설명 | 기본값 |
|---|---|---|
| `DB_HOST` / `DB_PORT` | MySQL 호스트/포트 | `127.0.0.1` / `3307` |
| `DB_USERNAME` / `DB_PASSWORD` | DB 계정 | `dove_app` / `dove1234` |
| `REDIS_HOST` / `REDIS_PORT` | Redis 호스트/포트 | `127.0.0.1` / `6380` |
| `KRX_API_AUTH_KEY` | KRX Open API 인증키 (주가·상장주식수) | — (필수) |
| `KIS_APP_KEY` / `KIS_APP_SECRET` | KIS API 앱키/시크릿 | — (필수) |
| `DART_API_KEY` | DART OpenAPI 인증키 (재무제표 수집) | — (재무 기능 시 필수) |
| `DART_DAILY_QUOTA` | DART 일일 호출 한도 (백필+폴링+corp동기화 공유) | `18000` |
| `DOVE_WORK_DIR` | 임시 작업파일 루트 (DART 원본·ML 아티팩트, 작업 전후 자동 정리). 비면 OS 임시폴더 하위 | (비움) |
| `MARKET_INITIAL_DATE` | 시장 데이터 시작일 | `2010-01-01` |
| `SCHEDULER_POOL_SIZE` | @Scheduled 스레드 풀 크기 | `5` |
| `COLLECTION_CONCURRENCY` | 수집(API) 동시 실행 수 | `40` |
| `INDICATOR_CONCURRENCY` | 지표 계산 동시 그룹 수 | `10` |
| `INDICATOR_START_DATE` | 지표 계산 시작일 하한 (비우면 전체 이력). 최초 계산량 제한용 — 예: `2026-01-01` | (비움) |
| `RANK_CONCURRENCY` | 순위 유닛(universe×가격유형) 동시 계산 수. DB 공유 병목·OOM 고려해 낮게 | `3` |

## 로컬 실행

`local` 프로파일이면 스케줄이 꺼지고 `JOB` 환경변수로 지정한 잡을 즉시 1회 실행 후 종료(`LocalJobRunner`).

| 환경변수 | 값 |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `local` (필수 — 없으면 잡 안 돎, 웹서버만 대기) |
| `JOB` | `pipeline`(전체) \| `derived`(지표→rank→custom-metric) \| `indicator` \| `rank` \| `custom-metric` \| `model-score` \| `stock-sync` \| `stock-detail` \| `fund-corp-sync` \| `fund-backfill` \| `fund-poll` \| `share-count` \| `share-count-range` \| `valuation` \| `valuation-range` |
| `FUND_FROM_YEAR` / `FUND_TO_YEAR` | `fund-backfill`·`valuation-range`·`share-count-range` 의 연도 범위 (기본 2015~2024) |

> DART 잡(`fund-*`)은 `DART_API_KEY`, 상장주식수 잡(`share-count*`)은 `KRX_API_AUTH_KEY` 필요.

```powershell
# Windows (PowerShell) — 예: 종목 동기화
$env:SPRING_PROFILES_ACTIVE="local"; $env:JOB="stock-sync"; $env:KRX_API_AUTH_KEY="<key>"; $env:KIS_APP_KEY="<key>"; $env:KIS_APP_SECRET="<secret>"; .\gw.ps1 :scheduler:bootRun
```

```bash
# Linux / macOS — 예: 지표 계산
SPRING_PROFILES_ACTIVE=local JOB=indicator \
  KIS_APP_KEY=<key> KIS_APP_SECRET=<secret> \
  ./gradlew :scheduler:bootRun
```

## 테스트

```powershell
.\gw.ps1 :scheduler:test    # Windows
```
```bash
./gradlew :scheduler:test   # Linux / macOS
```
단위 테스트는 Mockito로 외부 의존 없이 실행된다.
