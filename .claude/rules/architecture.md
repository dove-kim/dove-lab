# 아키텍처·모듈 구조

구조는 **헥사고날(Ports & Adapters) 멀티모듈**, 도메인 모델은 **DDD 전술 패턴**(Aggregate, Bounded Context)으로 설계:

```
application/           Driver adapter — Spring Boot 실행 단위
  scheduler            @Scheduled 진입점 — 수집·지표 계산·보정 일괄 처리
  api                  REST API 서버 — 인증·주식 데이터·사용자 권한 관리

domain/                Aggregate 단위 모듈 (entity + repo + JPA/QueryDSL + CQRS service)
  auth                 Credential, InviteCode
  user                 MemberProfile, MemberRole
  user-feature         Capability(권한 enum), MemberCapabilityGrant(사용자 권한), MemberCustomIndicatorGrant(지표별 접근)
  market               MarketType, Exchange, ExchangeTradingDate, MarketListingSync
  stock                Stock, StockDetail, StockEvent(권리이벤트), StockPrice, StockTagValue
  stock-collection     KIS 주가/KSD 권리이벤트 수집 코어 + KRX 종목 동기화(StockSyncService) + TradingDayPort/ShareCountFetcher(KRX 포트) + 수집 태스크(CollectionLauncher, CollectionTask)
  indicator            StockFeatureDaily(지표 wide) + 지표 계산기 + 횡단면 순위(rank) 계산 + 각 커서(IndicatorCursor·RankCursor, CAS)
  fundamental          StockFundamental(DART 재무) + StockValuationDaily(PIT 일별 밸류에이션) + StockShareCount(상장주식수 변경이력)
  investor-flow        InvestorDaily (기관·외국인·개인 매매동향)
  model-serving        ML 모델 레지스트리(ML_MODEL) + 채점 점수(STOCK_MODEL_SCORE) + 아티팩트 실행(score.py)
  custom-metric        ROOT 정의 커스텀 지표(DSL 계산식 JSON) — CustomMetricDef/Daily + 순수 평가기 + SERIES 시장지표(레짐·상승비율 등, 야간 증분 계산·저장). 전략을 코드 아닌 데이터로 은닉
  system-event         수집·계산 운영 이벤트(KRX/KIS 실패 등) — ROOT 모니터링
  screening            사용자 정의 종목 필터(불리언 트리 + 순서 파이프라인: FILTER·RANK 단계, 등락/시총/거래량 정렬·top-N) + 종목 세트 + 지표 프리셋
  portfolio            국내·해외 자산 포트폴리오 — 계좌·거래(PortfolioTransaction)를 fold해 보유·평단·라운드트립 파생. 보유종목 식별·배당추적(PortfolioHolding), 해외 종가·환율 스냅샷(PortfolioQuote/PortfolioFxRate), 환전(PortfolioFxConversion — 통화별 현금 이동, 순납입 제외), 리밸런싱 프리셋(PortfolioRebalancePlan), 계좌 공유(PortfolioShare). 금액은 원통화 저장·현재환율 원화 환산(파생)

infrastructure/        Driven adapter — 외부 시스템 연결
  krx                  KRX API 어댑터 (Feign) — 종목·일별시세(상장주식수)
  kis                  KIS API 어댑터 (Feign) + KisGate(초당 20회 율제한) — 국내·해외 주가
  dart                 DART OpenAPI 어댑터 (Feign) — 재무제표·고유번호·정기공시
  frankfurter          Frankfurter API 어댑터 (Feign) — 원통화→KRW 환율

library/               도메인 무관 공통 기술
  jpa, logging, api-quota, concurrent(Parallel), datetime, job-status(Redis 진행률), workspace(임시 작업파일 관리)
```

## 계층 경계 원칙 (필수)

- **app 모듈은 Repository 직접 주입 금지**. 도메인 모듈의 Query/Command service 또는 port만 주입.
- 모든 모듈은 **CQRS 분리**. 조회 전용은 `*QueryService`, 변경은 `*CommandService`. **단, 조회/변경 로직이 유의미하게 다르지 않은 단순 위임 aggregate는 단일 `*Service`로 두고 조회 메서드에만 `@Transactional(readOnly = true)`를 단다**(불필요한 클래스 분리 회피).
- 도메인 모듈은 자기 aggregate의 entity + repo + QueryDSL 구현을 함께 소유 (관심사 응집).
- 조합(여러 aggregate 엮는 유스케이스)은 `scheduler/service/` 또는 `api/service/` 패키지에서 담당.
- 도메인 횡단 유스케이스(시장 데이터 수집·지표 계산 등)는 `scheduler/service/` 에서 조합.
