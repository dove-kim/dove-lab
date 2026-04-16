# Consumer Death Spiral 해결 계획

## 배경

2026-04-15 로그 분석 결과, `stock-consumer`가 데스 스파이럴에 빠져 모든 메시지 소비가 중단됨.

### 문제 1 — KrxStockDailyDataEventListener (KRX_STOCK_PRICE_QUERY 토픽)

1. offset 5043 처리 중 `MARKET_CALENDAR` duplicate key 예외 (`2023-10-20-KOSPI`)
2. `DefaultErrorHandler` 기본값 `FixedBackOff(interval=0, maxAttempts=9)`로 딜레이 없이 10번 즉시 재시도
3. 처리 지연 → `max.poll.interval.ms` (기본 5분) 초과 → 컨슈머 그룹 kick-out
4. `finally { acknowledge() }`의 `commitSync()` → `CommitFailedException`
5. 오프셋 미커밋 → 같은 메시지 재polled → 무한 루프
6. **결과**: CURRENT-OFFSET 5111 고정, LAG 841, 타겟(5950~5951: 2026-04-14 KOSPI/KOSDAQ) 미도달

### 문제 2 — StockIndicatorCalcEventListener (TECHNICAL_INDICATOR_CALC 토픽)

1. offset 1174 메시지: stockCode=103140, **tradeDate=2010-01-04**
2. `findTradeDatesFrom()`이 2010-01-04 ~ 현재까지 **약 4,000거래일** 반환
3. `calculateForStock()`을 4,000회 루프 → 5분 안에 완료 불가
4. **비즈니스 예외는 없음** (에러 로그 미존재). 순수 처리시간 초과 문제
5. kick-out → CommitFailedException → DefaultErrorHandler 10회 재시도 → 모두 실패 후 포기
6. **결과**: `stockIndicatorCalc-1` 그룹도 rebalancing 상태, TECHNICAL_INDICATOR_CALC 소비 중단

## 목표

컨슈머 설정을 튜닝하여 death spiral을 방지하고,
StockIndicatorCalcEventListener의 처리 시간 폭주를 해결하여 정상 소비를 복구한다.

## 원칙

- **순수 쿼리(native SQL / JPQL) 사용 금지** — Spring Data JPA 메서드 또는 QueryDSL만 사용
- **TDD Red → Green → Refactor 엄수**: 테스트 먼저, 최소 구현, 그 다음 리팩토링
- **Tidy First**: 구조적 변경과 행위적 변경 커밋 분리

## 제외 사항 (다음 브랜치에서 Redis 분산락으로 해결 예정)

- MARKET_CALENDAR 멱등 저장 (조건부 UPSERT / HOLIDAY→TRADING 단방향 승격)
- MarketCalendar 비관적 락 조회
- StockDataSaveService 멱등성 보강
- 기타 at-least-once 재처리 시 duplicate 관련 모든 작업

## 검증

테스트 실행은 사용자가 직접 수행. Claude가 실행해도 된다고 허용 시 아래 명령:

```bash
JAVA_HOME="C:/Users/kimza/.jdks/corretto-21.0.7" \
  ./gradlew :app:app-stock-consumer:test
```

---

## 태스크 상세 (TDD)

### 태스크 1 — 컨슈머 설정 튜닝: max.poll.records 축소 + DefaultErrorHandler 재설정

**행위적 변경**. kick-out 예방과 에러 핸들링 개선.

현재 상태:
- `MAX_POLL_RECORDS_CONFIG = 100` → 한 poll에 100건, 총 처리시간이 5분 넘을 수 있음
- `DefaultErrorHandler`는 Spring Kafka 기본값 `FixedBackOff(interval=0, maxAttempts=9)` 사용
  → 딜레이 없이 즉시 10번 재시도 → poll 타임아웃 악화

변경:
- `MAX_POLL_RECORDS_CONFIG` → **10**으로 축소 (한 poll 처리 부담 감소)
- `DefaultErrorHandler` Bean을 명시적으로 등록:
  - `FixedBackOff(1000, 3)` — 1초 간격, 최대 3회 재시도
  - `CommitFailedException`을 non-retryable로 지정 (재시도해도 의미 없음)

> `DataIntegrityViolationException` non-retryable 지정은 분산락 도입 전까지 유효하나,
> 분산락 이후엔 발생 자체가 없어지므로 **지금 넣어도 무방**.

#### 테스트 (`app/app-stock-consumer/src/test/java/com/dove/stockconsumer/KafkaConfigTest.java` 신규)

- [x] `shouldConfigureMaxPollRecordsTo10` — consumerConfig()의 MAX_POLL_RECORDS_CONFIG가 10인지 검증
- [x] `shouldRegisterCustomErrorHandler` — DefaultErrorHandler Bean이 등록되고, BackOff 설정이 의도대로인지 검증
- [x] `shouldMarkCommitFailedExceptionAsNonRetryable` — CommitFailedException 발생 시 재시도 없이 즉시 포기하는지 검증

#### 구현

`app-stock-consumer/src/main/java/com/dove/stockconsumer/KafkaConfig.java` 수정.

---

### 태스크 2 — StockIndicatorCalcEventListener 처리시간 제한

**행위적 변경**. 한 메시지의 처리 대상 거래일 수를 제한하여 max.poll.interval.ms 안에 완료 가능하도록 한다.

현재 문제:
- offset 1174: tradeDate=2010-01-04 → 현재까지 약 4,000일 for 루프
- max.poll.interval.ms=5분 (기본값) 안에 완료 불가 → kick-out

변경 방안:
- `StockIndicatorCalcEventListener.onIndicatorCalcEventRequest()`에서
  `dates` 리스트의 처리를 **최대 N일(예: 200일)**로 제한
- 남은 날짜가 있으면 마지막 처리 날짜+1일을 `tradeDate`로 갱신한 **새 메시지를 Kafka에 재발행**하여
  다음 poll에서 이어서 처리 (자기 자신에게 continuation 메시지 발행)
- 현재 메시지는 정상 ack → 오프셋 전진

> N=200일 근거: `calculateForStock` 1건당 약 50ms 가정 시, 200건 = 10초.
> max.poll.records=10일 때 10건 × 10초 = 100초. 5분 이내 안전 마진 확보.
> N값은 실측 후 조정 가능.

이 방식의 장점:
- 기존 메시지 포맷 변경 없음 (`IndicatorCalcEventRequest` 그대로)
- `message_publish.sh`로 과거 날짜 백필해도 안전
- 처리 진행률이 오프셋으로 추적 가능 (각 chunk가 독립 메시지)

#### 테스트 (`app/app-stock-consumer/src/test/java/com/dove/stockconsumer/listener/StockIndicatorCalcEventListenerTest.java` 확장)

- [x] `shouldProcessAllDatesWhenUnderLimit` — 대상 거래일이 N일 이하면 전부 처리하고 추가 메시지 발행 없음
- [x] `shouldSplitProcessingWhenOverLimit` — 대상 거래일이 N일 초과 시, 첫 N일만 처리하고 나머지에 대한 continuation 메시지 발행
- [x] `shouldPublishContinuationMessageWithCorrectTradeDate` — continuation 메시지의 tradeDate가 마지막 처리 날짜+1일인지 검증
- [x] `shouldAcknowledgeAfterPartialProcessing` — 부분 처리 후에도 정상 ack 수행

#### 구현

`StockIndicatorCalcEventListener`에 `KafkaTemplate` 의존성 추가.
for 루프를 최대 N건으로 제한. 남은 분량이 있으면 동일 토픽에 continuation 메시지 발행.

---

## 커밋 전략

각 태스크마다:

1. **테스트 추가 커밋** — 실패 테스트만 (빨간 상태). 커밋 메시지 prefix `test:`
2. **구현 커밋** — 테스트 통과하는 최소 코드. prefix `feat:` (행위적 변경)
3. **리팩토링 커밋** (필요 시) — 테스트 그대로 통과. prefix `refactor:` (구조적 변경)

구조적 변경과 행위적 변경은 **절대 같은 커밋에 섞지 않는다**.

## 완료 조건

- [x] 태스크 1~2의 모든 테스트가 통과
- [x] 기존 테스트가 모두 통과 (리그레션 없음)
- [ ] 재배포 후 컨슈머 그룹 상태 정상화:
  - `krxDailyData-1`: CONSUMER-ID 활성, LAG → 0 수렴
  - `stockIndicatorCalc-1`: CONSUMER-ID 활성, LAG → 0 수렴
- [ ] `KRX_DAILY_DATA` 테이블에 `base_date=2026-04-14` / (KOSPI, KOSDAQ) row 존재
