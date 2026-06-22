# 개발 규칙

## 방법론

- **TDD Red → Green → Refactor**: 실패 테스트 먼저, 통과시킬 최소 코드, 그 후 리팩토링.
- **Tidy First**: 구조적 변경(rename/move/extract)과 행위적 변경(기능 추가·수정)을 **같은 커밋에 섞지 않음**. 둘 다 필요하면 구조적 먼저.
- 테스트 이름은 행위 기술: `shouldXxxWhenYyy`.
- 결함 수정은 실패 테스트 작성 → 수정.

## 테스트 구조

- **테스트는 실제 코드와 동일 패키지를 미러링한다**(`.controller`/`.service`까지). 상위 기능 패키지에 평평하게 두지 않음.
- **행위/엔드포인트 단위로 `@Nested` 내부 클래스로 묶는다.** 한 컨트롤러 메서드(또는 한 행위 묶음)당 `@Nested` 하나. `@DisplayName`을 계층화: 바깥 클래스=대상, `@Nested`=엔드포인트/행위, 메서드=구체 케이스.
- 빈 테스트 패키지(디렉터리)는 남기지 않는다.
- **Mockito는 strict로 쓴다 — `lenient(...)`·`Strictness.LENIENT`·`@MockitoSettings(strictness = LENIENT)` 금지.** 기본 `STRICT_STUBS` 유지. 'unnecessary stubbing'이 뜨면 느슨하게 풀지 말고 **불필요한 스텁을 제거하거나 케이스별로 테스트를 분리**한다. 공통 스텁 헬퍼는 그것을 호출하는 모든 테스트에서 실제로 쓰일 때만 둔다.

### api 통합 테스트 인증

- **인증은 `@WithApiUser`(test support: `com.dove.api.support`) 어노테이션으로 선언한다.** 클래스/메서드에 `@WithApiUser(role = "ROOT", features = {"STOCK_SEARCH"})` 식. JWT가 stateless라 인증/인가에 DB 계정·토큰 발급이 불필요 — principal을 SecurityContext에 직접 주입한다.
- 인증 없는(401) 케이스는 어노테이션을 생략한다.
- **호출자(principal) 계정을 DB에 저장하지 않는다.** `@WithApiUser(memberId = ...)`로 식별자를 고정하고, 그 회원이 소유하는 도메인 데이터는 같은 memberId로 시드한다.
- 단, 실제 로그인·토큰·자격증명 흐름 자체를 검증하는 테스트(로그인, 비밀번호 변경, `JwtFilter`)는 실제 토큰 경로를 유지한다.
- 권한 변경 대상 회원 등 **대상(target) 엔티티는 도메인 커맨드 서비스로 시드**한다(호출자와 구분).
- 외부 시스템(KIS/Redis 등) 의존 빈은 `@MockitoBean`으로 격리한다.

## 커밋

- Claude는 `git commit` 실행 금지. 사용자만 커밋.
- 작업 단위 완료 시 변경 내역 요약 보고만.

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

## 아키텍처 계층

구조는 **헥사고날(Ports & Adapters) 멀티모듈**, 도메인 모델은 **DDD 전술 패턴**(Aggregate, Bounded Context)으로 설계:

```
application/           Driver adapter — Spring Boot 실행 단위
  scheduler            @Scheduled 진입점 — 수집·지표 계산·보정 일괄 처리
  api                  REST API 서버 — 인증·주식 데이터·사용자 권한 관리

domain/                Aggregate 단위 모듈 (entity + repo + JPA/QueryDSL + CQRS service)
  auth                 Credential, InviteCode
  user                 MemberProfile, MemberRole
  user-feature         UserFeatureGrant, UserModuleDisplay, UserFeatureDisplay
  market               MarketType, Exchange, ExchangeTradingDate, MarketListingSync
  stock                Stock, StockDetail, StockEvent(권리이벤트), StockPrice, StockTagValue
  stock-collection     KIS 주가/KSD 권리이벤트 수집 코어 + KRX 종목 동기화(StockSyncService) + TradingDayPort(KRX 포트) + 수집 태스크(CollectionLauncher, CollectionTask)
  indicator            TechnicalIndicator + 지표 계산기 + IndicatorCursor(+CAS)
  investor-flow        InvestorDaily (기관·외국인·개인 매매동향)
  system-event         수집·계산 운영 이벤트(KRX/KIS 실패 등) — ROOT 모니터링
  ml-export            ML 학습 데이터 내보내기 프리셋
  screening            사용자 정의 종목 필터 + 종목 세트 + 지표 프리셋

infrastructure/        Driven adapter — 외부 시스템 연결
  krx                  KRX API 어댑터 (Feign)
  kis                  KIS API 어댑터 (Feign) + KisGate(초당 20회 율제한)

library/               도메인 무관 공통 기술
  jpa, logging, api-quota, concurrent(Parallel), datetime, job-status(Redis 진행률)
```

## 계층 경계 원칙 (필수)

- **app 모듈은 Repository 직접 주입 금지**. 도메인 모듈의 Query/Command service 또는 port만 주입.
- 모든 모듈은 **CQRS 분리**. 조회 전용은 `*QueryService`, 변경은 `*CommandService`. **단, 조회/변경 로직이 유의미하게 다르지 않은 단순 위임 aggregate는 단일 `*Service`로 두고 조회 메서드에만 `@Transactional(readOnly = true)`를 단다**(불필요한 클래스 분리 회피).
- 도메인 모듈은 자기 aggregate의 entity + repo + QueryDSL 구현을 함께 소유 (관심사 응집).
- 조합(여러 aggregate 엮는 유스케이스)은 `scheduler/service/` 또는 `api/service/` 패키지에서 담당.
- 도메인 횡단 유스케이스(시장 데이터 수집·지표 계산 등)는 `scheduler/service/` 에서 조합.

## 권한·메뉴 (capability 기반)

**권한(백엔드)과 메뉴 표시(프론트)는 완전히 분리한다.** 백엔드는 메뉴를 모르고, 프론트가 권한을 읽어 표시만 정한다.

### 백엔드 — 권한만

- **매니페스트** = `Capability` enum (`domain/user-feature .../domain/capability`). **권한 키만** 선언한다. capability는 코드가 정의(엔드포인트가 강제하기에 존재) → 런타임 생성 없음 → enum. **메뉴 위치·라벨·잠금/숨김 정책은 여기 두지 않는다**(프론트 책임).
- **권한(grant)** = `MEMBER_CAPABILITY_GRANT` (사용자별). ADMIN/ROOT가 부여, 회수=행 삭제. 활성 capability 집합은 **JWT claim**에 실어 API가 JWT로 검사(매 요청 DB 조회 안 함, 변경 시 강제 로그아웃).
- **강제는 항상 API** — `@RequireCapability(STOCK_SEARCH)` → 없으면 403. 메뉴는 보안 경계가 아니다.
- **민감 데이터는 응답에서 제거** — 예: `INDICATOR_ML` 없으면 ML 예측 필드를 응답에서 뺀다(UI 숨김만으론 부족). "메뉴인지"는 서버가 몰라도 됨 — 필드 단위 권한일 뿐.

### 프론트 — 메뉴/표시

- **menu manifest**(프론트 소유) = `{ capability, route, group, order, LOCK|HIDE }`. 라벨·라우트·잠금정책 전부 프론트.
- 프론트는 JWT의 capability 집합을 읽어 **API를 때려보기 전에** 미리 렌더 결정:
  - **LOCK**: 권한 없어도 **보이되 잠금**(영역 단위). 첫 탭이 잠겼으면 다음 접근 가능한 탭으로 자동 이동.
  - **HIDE**: 권한 없으면 **숨김**(예: ML). 데이터도 서버가 안 내려줌.
- **메뉴 노출(전역)·순서(사용자)** 는 권한이 아니라 표시 설정 — 프론트 menu-id로 키잉(필요 시 별도 설정 저장).

### 역할(ROLE)

- **USER**: 메뉴 순서만 변경. **ADMIN**: 사용자별 권한(capability) 부여 + 전역 노출. **ROOT**: 전부 + ROOT 고유 불변.
- **관리 화면·ROOT 고유 기능은 capability가 아니라 ROLE로 게이트**(capability 레지스트리에 넣지 않음). ROLE 체계 개선은 별도 과제.

새 권한 추가: `Capability`에 키 한 줄 추가 → 게이트할 API에 `@RequireCapability`(HIDE면 응답 필드도 제거) → 프론트 manifest에 표시 정책 추가.

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

## 운영 자원 제약 (저사양·공유 환경)

이 프로젝트는 아래 **자원 예산 안에서 동작하도록** 만든다. 서버 실측이 아니라 설계 상한이며, 이 한계를 넘는 전제(대용량 상주·풀스캔 의존 등)를 두지 않는다.

### 목표 동작 한계 (budget)

- **CPU**: 약 4코어 이하, 코어당 성능 낮음 가정. **단일 쿼리·단일 작업은 1코어로 처리**된다고 보고 설계(한 작업이 한 코어를 오래 점유하지 않게).
- **앱 메모리**: JVM `-Xmx`를 앱당 약 768MB 이하로 캡(api·scheduler 각각). 기본 = RAM 25% 방치 금지.
- **앱+DB 합산 메모리**: 약 3GB 이내 목표(공유 환경, 나머지는 타 서비스·OS 몫).
- **DB 핫 워킹셋**: 버퍼풀 약 1.5-2GB 안에 들어오게 설계 — 자주 읽는 인덱스·테이블이 이 안에 맞도록. 전 이력은 안 들어온다고 가정.

### 저장 용량 예산 (하드 한계)

- 전체 DB(데이터+인덱스+로그) **현재 100GB 이하** 유지.
- **향후 50년 누적 증가에도 약 120GB 이내.** → 이 한계가 설계를 강제한다: 파생 대량 데이터는 **EAV 금지, wide + 압축 필수**(EAV 지표 한 종류만으로도 한계 초과). 무한 누적 로그(binlog)는 보존기간 캡.
- 대량 테이블은 `ROW_FORMAT=COMPRESSED`. **신규 대량 테이블 추가 시 50년 누적 용량을 추정·기록**한다.

### DB 설정 예산 (도커 MySQL)

- 버퍼풀 약 1.5GB. **컨테이너 메모리 한도는 버퍼풀 + 약 20% 이상**(아니면 부하 시 OOM/스왑). 버퍼풀 크기는 chunk×instances(기본 1GB) 단위로 올림되니 의도한 값과 실제값 확인.
- `max_connections`는 작게(앱 Hikari 풀 합 + 여유 ≈ 60-80). 앱 풀 합이 이를 넘지 않게.
- `innodb_flush_method=O_DIRECT`(이중 캐시 방지로 RAM 절약) 유지.
- **대량 배치 쓰기 가속**(재생성 가능한 데이터라 허용): `innodb_flush_log_at_trx_commit=2`(매 커밋 fsync 회피), `innodb_redo_log_capacity` 넉넉히(예: 1GB — 작으면 체크포인트 스톨로 대량 insert 저하).
- **binlog: 복제·PITR 미사용이면 `--skip-log-bin`으로 비활성**(공간·쓰기 이중 I/O 절약). 켜둘 거면 보존기간 짧게(기본 30일은 길다) + `max_binlog_size` 캡.
- `innodb_file_per_table=ON`(테이블별 압축·공간 반납). 기본 `innodb_default_row_format=dynamic`이라 **대량 테이블은 `ROW_FORMAT=COMPRESSED`를 명시**.

### 설계 규칙

- **전 테이블 풀스캔 의존 기능 금지** — 인덱스로 스캔 대상 먼저 축소.
- 비-API 동시성·DB 커넥션 풀은 작게(키워도 처리량 안 늘고 경합만 증가).
- 대용량 순차 스캔이 필요한 기능은 **비동기 + 진행률 + 결과 상한** UX로.

### 대용량 분석 데이터 설계 원칙

- 파생 대량 데이터(지표 등)는 **EAV(행 폭증) 금지, wide(가로) 레이아웃**으로 행수·인덱스 최소화.
- 캐시(약 1.5GB) 초과 테이블의 전 이력 스캔 기능은 **인덱스 범위 축소**를 전제로 설계. 반복 풀스캔이 불가피하면 **컬럼스토어 미러(열 선택 읽기·압축·다코어 활용)** 를 검토.

## 쿼리 규칙

- **native SQL / JPQL 문자열 사용 금지**. Spring Data JPA 메서드 또는 QueryDSL만 사용. (조건부 update 등도 QueryDSL로 — 예: 커서 CAS 전진.)

## 엔티티 규칙

- 모든 `@Table`에 목적에 맞는 `@Index` 또는 `uniqueConstraints` 명시.
- 쿼리 패턴에 맞는 복합 인덱스를 엔티티 정의에 포함.
- 신규/변경 컬럼·테이블에 한글 `@Comment` 부착.

## DDL 배포

- 스키마 변경은 **`init.sql`을 단일 진실 원천**으로 유지. 배포는 이 파일로 수행.
- JPA `ddl-auto`는 개발·테스트(H2) 한정. 운영은 `init.sql`.

## 에러 처리

- 서버는 에러 코드(영문 대문자 스네이크케이스)를 `ResponseStatusException` reason으로 전달.
- `spring.mvc.problemdetails.enabled: true` 활성화 — 에러 코드는 RFC 7807 `detail` 필드로 전달됨.
- 웹(Next.js)은 `detail` 코드를 받아 한국어 메시지로 매핑. 메시지 문자열은 프론트엔드에서만 관리.
- 보안상 단일 메시지로 통합해야 하는 경우(로그인 등)는 프론트에서 하드코딩 허용.

## 프론트엔드

- CSS 직접 작성 금지. 스타일은 **Tailwind CSS**만 사용.

### 디렉터리 구조 (Next.js App Router)

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

### containers/ 구조

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

### 컴포넌트 규칙

- `app/` 페이지는 Server Component. 쿠키 확인·데이터 fetch 후 `containers/` 컴포넌트에 props 전달.
- 인증 필요 페이지: `cookies().get("token")` 없으면 `redirect("/login")`.
- 클라이언트 상태·이벤트가 필요한 컴포넌트만 `"use client"` + `containers/`.
- 여러 페이지에서 쓰지 않으면 `components/`에 넣지 않음.

### API 라우트 규칙

- Next.js API 라우트(`app/api/`)는 백엔드 프록시 역할. 쿠키에서 JWT를 읽어 `Authorization: Bearer` 헤더로 백엔드 전달.
- 인증: `backendFetch`가 토큰 없으면 null 반환 → `unauthorized()` 응답.
- ADMIN 전용 라우트: JWT 디코딩 후 role 확인 (`decodeJwtPayload`). 403 반환.
- 에러 코드는 영문 대문자 스네이크케이스로 JSON body에 포함.

### JWT 처리

- JWT 디코드: `utils/jwt.ts`의 `decodeJwtPayload(token)` 단일 함수 사용. 인라인 중복 금지.
- 클라이언트에서는 httpOnly 쿠키를 직접 읽을 수 없음 → 서버 컴포넌트 / API 라우트에서만 디코드.

### UI 가시성·조작성 원칙

- **모든 화면 크기에서** 요소가 눈에 띄고 누르기 쉬워야 한다.
- 터치 타겟은 충분히 크게 (최소 44×44px 수준).
- 선택 컨트롤은 `<select>` 또는 명확한 크기의 버튼 그룹 사용. 소형 SVG 아이콘 버튼 단독 사용 금지.
- 모달/드로어처럼 화면 전체를 활용하는 UI 패턴을 적극 사용 — 작은 팝오버보다 고정 크기 모달이 낫다.
- 의심스러우면 더 크게, 더 여백 있게.

### 스타일 토큰

- `utils/cx.ts`의 `cx` 객체에서 공통 Tailwind 클래스 조합 관리 (`cx.input`, `cx.btnPrimary`, `cx.table.*` 등).
- 반복되는 클래스 조합이 생기면 `cx`에 추가. 인라인 중복 금지.

## 빌드/테스트 실행

- 사용자가 직접 수행. Claude는 필요 시 제안만.
- JAVA_HOME: `C:/Users/kimza/.jdks/corretto-21.0.7`