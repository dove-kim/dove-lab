# infrastructure/kis

한국투자증권(KIS) Open API 어댑터 모듈.

베이스 URL: `https://openapi.koreainvestment.com:9443`  
인증: OAuth 2.0 Bearer Token (`KisTokenManager` 가 자동 갱신)

> **사용 범위**: 당일·역사적 주가 수집과 수정주가 적용(`PriceCollectionService`), 종목 상세정보(`StockDetailService`), 투자자매매동향(`InvestorCollectService`), 포트폴리오 해외 보유 종가(`KisOverseasPriceAdapter`)에 사용된다.  
> 종목 목록 조회는 KRX(infrastructure/krx) 어댑터가 담당한다. 모든 KIS 호출은 `KisGate`(초당 20회)를 통과한다.

## KRX vs KIS 역할 분리

| 항목 | KRX | KIS |
|---|---|---|
| 종목 목록 | 조회 시점 기준 상장 종목 목록 제공 (이력 없음) | — |
| 종목 생애주기 | — | `CTPF1002R`로 상장일·상장폐지일·액면가·상장주식수 관리 |
| 일별 주가 | 해당 날짜 시가·고가·저가·종가·거래량 | 기간별 봉 (최대 100건/회) + 수정주가 |
| 거래일 판단 | 종목·주가 응답 유무로 개장/휴장 역추론 | 휴장일 row 자체를 생략 |

→ **종목 목록의 정확한 이력(상장폐지 여부 등)은 KIS가 단일 진실 원천이다.**  
→ `StockSyncJob`(08:05)은 KRX로 당일 종목 목록을 수집하고, `StockDetailJob`(12:00)이 KIS 상세정보(상장폐지일·액면가·상장주식수 등)를 보강한다.

---

## 엔드포인트

| TR_ID | 용도 | 경로 |
|-------|------|------|
| FHKST01010100 | 주식현재가 시세 | `/uapi/domestic-stock/v1/quotations/inquire-price` |
| FHKST03010100 | 국내주식기간별시세 | `/uapi/domestic-stock/v1/quotations/inquire-daily-itemchartprice` |
| CTPF1002R | 주식기본정보 (상장폐지일·액면가·상장주식수) | `/uapi/domestic-stock/v1/quotations/search-stock-info` |
| CTPF1604R | 상품기본정보 (종목명·분류 등) | `/uapi/domestic-stock/v1/quotations/search-info` |
| FHKST01010900 | 종목별 투자자매매동향 | `/uapi/domestic-stock/v1/quotations/inquire-investor` |
| HHDFS00000300 | 해외주식 현재가 (포트폴리오 해외 보유 평가) | `/uapi/overseas-price/v1/quotations/price` |

공통 파라미터 `FID_COND_MRKT_DIV_CODE`: `J`=KRX(KOSPI/KOSDAQ/KONEX), `NX`=NXT, `UN`=통합
해외 파라미터 `EXCD`: `NAS`=나스닥, `NYS`=뉴욕, `AMS`=아멕스(NYSE Arca ETF 포함) 등

---

## API 응답 정책 (실측·문서 기반)

### 공통 응답 구조

```json
{
  "rt_cd": "0",       // "0" = 성공, "1" = 실패
  "msg_cd": "XXXXX",  // 메시지 코드
  "msg1": "...",      // 메시지 텍스트
  "output": { ... }   // 또는 "output1" / "output2"
}
```

`rt_cd != "0"` 이면 `KisApiException(msg_cd, msg1)` 을 던진다.

---

### 수정주가 vs 비수정주가

기간별시세 API(`FHKST03010100`)는 `FID_ORG_ADJ_PRC` 파라미터로 수정주가 여부를 제어한다.

`PriceCollectionService`는 `KisPeriodChartFetcher.fetchDaily(exchange, ticker, from, to, priceType)` 하나로 `PriceType`에 따라 `FID_ORG_ADJ_PRC`를 정한다.

| `FID_ORG_ADJ_PRC` | `PriceType` | 의미 |
|---|---|---|
| `"1"` | `RAW` | 비수정주가 (원주가 그대로, 거슬러 올라가도 변하지 않음) |
| `"0"` | `ADJUSTED` | 수정주가 (권리락·배당락 소급 반영) |

- RAW·ADJUSTED를 함께 수집하며, RAW 봉에서 수정주가 이벤트(배당락 등)를 감지하면 해당 종목 ADJUSTED 전체를 재조회하고 지표 커서를 되돌린다.

---

### 기간별시세 API (FHKST03010100)

| 조회 조건 | rt_cd | output2 |
|----------|-------|---------|
| 과거 영업일 포함 범위 | `"0"` | 영업일 행만 포함 (주말·공휴일 행은 자동 생략) |
| 범위 전체가 휴장일 (주말·공휴일) | `"0"` | `null` 또는 `[]` |
| 장 중 (당일 포함 범위) | `"0"` | 당일 행 포함 (장 중 임시 데이터) |
| 미래 날짜 범위 | `"0"` | `null` 또는 `[]` |
| 존재하지 않는 종목코드 | `"1"` | — (KisApiException 발생) |

**핵심**: 휴장일은 에러가 아니라 **해당 날짜 행 부재**로 표현된다.  
→ 수집 측은 빈 리스트를 "정상"으로 처리해야 한다.

#### 거래정지 종목 일봉

거래정지(매매거래정지) 종목은 **해당 날짜 row가 존재**하며, 시가·고가·저가가 모두 직전 종가와 동일한 값으로 채워진다.

> **실측 확인**: HLB(028300) 2005-03-09~17 거래정지 구간 실측.  
> 정지 기간 내 모든 row: `open = high = low = close = 직전 종가(1975)`, `vol = 0`.  
> 정지 해제일(2005-03-18): 거래량 정상 재개.

| 필드 | KRX 응답 | KIS 실측 응답 | 저장값 |
|---|---|---|---|
| 종가 (`stck_clpr`) | 직전 종가 | 직전 종가 | 정상 저장 |
| 시가 (`stck_oprc`) | `null` | 직전 종가 (종가와 동일) | 정상 저장 |
| 고가 (`stck_hgpr`) | `null` | 직전 종가 (종가와 동일) | 정상 저장 |
| 저가 (`stck_lwpr`) | `null` | 직전 종가 (종가와 동일) | 정상 저장 |
| 거래량 (`acml_vol`) | `0` | `"0"` | `0` |

**거래정지 판별 기준**: `vol = 0` AND `open = high = low = close` → 거래정지일.

- `KisDailyCandle`로 봉을 받아 `StockPrice`로 저장한다. 거래정지일도 row가 존재하므로 그대로 보존된다(거래량 0).

---

### 현재가 API (FHKST01010100)

| 조회 조건 | rt_cd | 반환 데이터 |
|----------|-------|-----------|
| 장 중 | `"0"` | 실시간 가격, 누적거래량 갱신 중 |
| 장 마감 후 (당일 영업일) | `"0"` | 당일 종가 기준 최종 데이터 |
| 휴장일 (주말·공휴일) | `"0"` | **직전 영업일 종가** (`acml_vol` = 0 또는 당일 거래 없음으로 표시) |
| 존재하지 않는 종목코드 | `"1"` | — (KisApiException 발생) |

**핵심**: 휴장일에도 `rt_cd = "0"` 정상 응답이 온다. 단, 거래량이 0이고 가격은 stale.  
→ 현재가 API로 휴장 여부를 판단하는 것은 신뢰할 수 없다. `ExchangeTradingDate` 기반으로 판단해야 한다.

---

## KOSPI / KOSDAQ / KONEX 거래일 독립성

**결론: 세 시장은 항상 같은 날 개장·휴장한다.**

- KOSPI, KOSDAQ, KONEX 모두 KRX(한국거래소)가 운영하며, 금융위원회가 고시하는 동일한 공휴일·휴장일 규정을 따른다.
- 개별 시장이 독립적으로 휴장하는 사례는 없다 (시스템 장애 등 비정형 이벤트 제외).
- KIS API의 `FID_COND_MRKT_DIV_CODE` 파라미터에서도 `J` 코드 하나로 KRX 전체(KOSPI/KOSDAQ/KONEX)를 대표한다.

→ `Exchange` 단위(`KRX`)로 거래일 캘린더를 단일 관리한다(`ExchangeTradingDate`). KOSPI/KOSDAQ/KONEX별 행 분리는 구조적 중복이므로 제거됨.  
→ 향후 미국장 등 별도 캘린더가 필요한 시장을 추가할 때 `Exchange` enum에 `NYSE`, `NASDAQ` 등을 추가하면 된다.

---

## 인증 (토큰 관리)

- `KisTokenManager`: 토큰 만료 1분 전 자동 갱신, 재시도 포함.
- `KisStockClientConfig`: Feign 요청 인터셉터에서 `Authorization: Bearer {token}` 헤더 삽입.
- 실전 계정과 모의투자 계정은 베이스 URL이 다르다 (`kis.base-url` 프로퍼티로 구분).

---

## Rate Limit

- 모든 KIS 호출은 **`KisGate.call(supplier)`** 를 통과한다 — 호출 직전 초당 슬롯을 획득(블로킹)한다. 스레드(동시성) 수와 무관하게 초당 호출이 제한된다.
- 슬롯 제어는 `RateLimiter` 구현으로 분리: 단일 프로세스는 `LocalRateLimiter`(Semaphore), 분산은 `RedissonRateLimiter`(`kis.api.quota.distributed=true`). 기본 `kis.api.stock-per-second`=20.

---

## 에러 처리 요약

| 상황 | 발생 예외 |
|------|----------|
| HTTP 4xx/5xx (네트워크·인증) | `FeignException` |
| `rt_cd != "0"` (API 비즈니스 에러) | `KisApiException(code, message)` |

상위 수집 서비스(`PriceCollectionService`, `StockDetailService`, `InvestorCollectService`)는 `Parallel` 병렬 처리 중 KIS 오류가 나면 수집을 중단하고 `SystemEventCommandService.recordKisApiFailure()` 로 이력을 기록한다.

---

## 어댑터 클래스

| 클래스 | 역할 |
|---|---|
| `KisGate` | 모든 KIS 호출의 초당 율제한 게이트 (`call(supplier)`) |
| `KisPeriodChartFetcher` | 기간별시세 조회 `fetchDaily(exchange, ticker, from, to, priceType)` — `PriceType`로 수정/비수정 결정 |
| `KisStockClient` | Feign 클라이언트 — 주식기본조회·상품기본조회·투자자매매동향 |
| `KisOverseasPriceAdapter` | 해외주식 현재가 조회 `fetchClose(market, ticker)` — 포트폴리오 해외 보유 평가용 (`OverseasPricePort`) |
| `KisTokenManager` | OAuth 토큰 자동 갱신 |
