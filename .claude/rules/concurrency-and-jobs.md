# 동시성·배치·수집/지표 파이프라인

## 동시성·병렬 실행

- 병렬 처리는 **`Parallel`(library/concurrent)** 하나로 통일. 가상 스레드 + 세마포어로 **동시 실행 수만** 제한(fail-fast, lazy). 별도 풀/유틸 새로 만들지 않음.
- **외부 API 율제한은 게이트로 분리.** KIS 초당 20회는 `KisGate`가 **호출부에서** 강제 — 스레드 수와 무관. (동시 수 제한 ≠ 율제한, 둘은 직교.) 율제한은 **Redis 분산**(`RedissonRateLimiter`, OVERALL, 50ms당 1건 균등)이라 api·scheduler 다중 인스턴스가 합산으로 한도를 공유한다.
- 동시 수는 **워크로드 병목 기준**으로 정함:
  - API 작업(대부분 게이트·네트워크에서 park): `collection.concurrency`(기본 40).
  - 비-API 계산(DB/CPU 병목): `indicator.concurrency`(기본 10). **단일 코어에선 더 올려도 효과 거의 없고** 경합만 늘어남.
- 가상 스레드 개수 자체는 관리 안 함(싸다). 메모리는 *동시 수 × 작업당 데이터*로 결정 → 대용량은 **청크 스트리밍**으로 작업당 메모리를 한 청크로 고정.

## 배치·스케줄러

- 잡마다 독립 스레드: `spring.task.scheduling.pool.size`(기본 5) ≥ 잡 수. 한 잡이 오래 걸려도 다른 잡을 막지 않음.
- cron `@Scheduled`는 **자기 자신과 겹치지 않음**(완료 후 다음 발화 계산). 밀려서 늦게 돌아도 **커서 멱등**이라 재계산 안전 → "밀림 허용".
- 잡 이름은 `SchedulerJobName` enum 사용(문자열 리터럴 금지).
- 진행률은 **`JobStatusRegistry`(Redis, best-effort)** 에 기록 → ROOT 대시보드 `GET /admin/ops/scheduler/status`. Redis 오류는 삼켜 작업을 막지 않음.

## 주가 수집·지표 (커서 기반, wide 저장)

- 지표는 **wide 테이블 `STOCK_FEATURE_DAILY`** 에 저장 — 한 거래일 = 한 행, 지표는 컬럼(`IndicatorType` 값별). warmup 미달 지표는 그 행에서 NULL. (EAV 금지 — 행수·인덱스 폭증으로 용량 예산 초과.)
- 지표 커서 단위 = **그룹 `(ticker, exchange, priceType)`** (지표별 아님 — 한 행에 전 지표가 함께 들어가므로). 저장 시작일 = 커서일 + 1.
- 지표 계산은 **100행 청크 + 직전 maxRequired행 lookback** 스트리밍(메모리 = 한 청크치, 이력 길이 무관). `SEQ`(그룹 거래일 순번)를 부여해 전일·N일전 비교(SEQ-N)·백테스트 미래수익(SEQ+N)을 휴장일 무관하게 self-join. 저장 + 커서 전진은 **한 트랜잭션**이며, 전진은 **CAS**(계산 시점 커서값과 일치할 때만) — 불일치 = 그 사이 수집 rewind 발생 → 롤백·해당 그룹 중단, 다음 배치가 rewound 지점부터 재계산.
- 차트 등 소량 조회는 동적 계산도 충분히 빠름(계산은 병목 아님). wide 저장의 목적은 **임의 검색식의 전 이력 스캔을 "조회"로** 만드는 것(매 검색 재계산 회피).
- 수집(`PriceCollectionService`)이 가격 변경/조정 감지 시 `rewindAllBefore`/`clearAdjusted`로 지표 커서를 되돌림. 지표는 행의 **연속성을 가정**하고 계산만 함 — 누락/보정은 수집 책임.
- **백필 재조회는 ≤어제로 제한**(`CollectionLauncher`에서 캡). 오늘은 일일 잡 전담 → 두 경로가 같은 날짜를 안 건드려 충돌 없음.
