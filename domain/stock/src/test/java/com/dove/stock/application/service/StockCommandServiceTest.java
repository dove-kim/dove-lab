package com.dove.stock.application.service;

import com.dove.market.domain.enums.MarketType;
import com.dove.stock.domain.entity.Stock;
import com.dove.stock.domain.entity.StockDetail;
import com.dove.stock.domain.repository.StockDetailRepository;
import com.dove.stock.domain.repository.StockRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * StockCommandService 통합 테스트.
 */
@DataJpaTest
@Import(StockCommandService.class)
class StockCommandServiceTest {

    private static final LocalDate LISTED = LocalDate.of(1975, 6, 11);

    @Autowired StockCommandService service;
    @Autowired StockRepository stockRepository;
    @Autowired StockDetailRepository stockDetailRepository;

    @Nested
    @DisplayName("upsertFromKrx — 종목 마스터 upsert")
    class UpsertFromKrx {

        @Test
        @DisplayName("종목이 없으면 신규 생성한다")
        void shouldInsertWhenAbsent() {
            service.upsertFromKrx("005930", "KR7005930003", MarketType.KOSPI, LISTED, "주권", "보통주");

            Stock s = stockRepository.findById("005930").orElseThrow();
            assertThat(s.getMarket()).isEqualTo(MarketType.KOSPI);
            assertThat(s.getSecugrpNm()).isEqualTo("주권");
        }

        @Test
        @DisplayName("이미 존재하면 ISIN·분류 정보를 갱신하고 행 수를 유지한다")
        void shouldUpdateWhenExists() {
            service.upsertFromKrx("005930", "OLD", MarketType.KOSPI, LISTED, "주권", "보통주");
            service.upsertFromKrx("005930", "KR7005930003", MarketType.KOSPI, LISTED, "주권", "우선주");

            assertThat(stockRepository.count()).isEqualTo(1);
            Stock s = stockRepository.findById("005930").orElseThrow();
            assertThat(s.getIsin()).isEqualTo("KR7005930003");
            assertThat(s.getKindStkCertTpNm()).isEqualTo("우선주");
        }
    }

    @Nested
    @DisplayName("insertIfAbsent — 신규 종목만 insert")
    class InsertIfAbsent {

        @Test
        @DisplayName("없는 종목만 추가하고 기존 종목은 변경하지 않는다")
        void shouldInsertOnlyAbsent() {
            stockRepository.save(new Stock("005930", "KR7005930003", MarketType.KOSPI, LISTED, "주권", "보통주"));

            service.insertIfAbsent(List.of(
                    new Stock("005930", "DUP", MarketType.KOSPI, LISTED, "주권", "보통주"),
                    new Stock("000660", "KR7000660001", MarketType.KOSPI, LISTED, "주권", "보통주")));

            assertThat(stockRepository.count()).isEqualTo(2);
            assertThat(stockRepository.findById("005930").orElseThrow().getIsin()).isEqualTo("KR7005930003");
        }

        @Test
        @DisplayName("전부 신규이면 모두 insert한다")
        void shouldInsertAllWhenAllAbsent() {
            service.insertIfAbsent(List.of(
                    new Stock("005930", "KR7005930003", MarketType.KOSPI, LISTED, "주권", "보통주"),
                    new Stock("000660", "KR7000660001", MarketType.KOSPI, LISTED, "주권", "보통주")));

            assertThat(stockRepository.count()).isEqualTo(2);
        }

        @Test
        @DisplayName("빈 목록이면 아무것도 insert하지 않는다")
        void shouldNotInsertWhenListIsEmpty() {
            service.insertIfAbsent(List.of());

            assertThat(stockRepository.count()).isZero();
        }
    }

    @Nested
    @DisplayName("applyStockInfo — 주식기본조회 upsert")
    class ApplyStockInfo {

        @Test
        @DisplayName("StockDetail이 없으면 생성 후 정보를 적용한다")
        void shouldCreateAndApplyWhenAbsent() {
            service.applyStockInfo("005930",
                    100_000_000L, 500_000_000L, 100L,
                    "101", null, null, "Y",
                    "1", "10", "100",
                    "전기전자", "반도체", "메모리반도체",
                    "C26", "전자부품 제조업",
                    null, "N", "N", null, "19750611");

            StockDetail d = stockDetailRepository.findById("005930").orElseThrow();
            assertThat(d.getIdxBztpLclsNm()).isEqualTo("전기전자");
            assertThat(d.getListedShares()).isEqualTo(100_000_000L);
        }

        @Test
        @DisplayName("이미 존재하면 정보를 갱신하고 행 수를 유지한다")
        void shouldUpdateWhenExists() {
            service.applyStockInfo("005930",
                    50_000_000L, 100_000_000L, 100L,
                    "101", null, null, "N",
                    "1", "10", "100", "통신", "통신장비", "유선통신",
                    "C27", "전자부품", null, "N", "N", null, null);
            service.applyStockInfo("005930",
                    100_000_000L, 500_000_000L, 100L,
                    "101", null, null, "Y",
                    "1", "10", "100", "전기전자", "반도체", "메모리반도체",
                    "C26", "전자부품 제조업", null, "N", "N", null, "19750611");

            assertThat(stockDetailRepository.count()).isEqualTo(1);
            assertThat(stockDetailRepository.findById("005930").orElseThrow().getListedShares())
                    .isEqualTo(100_000_000L);
        }
    }

    @Nested
    @DisplayName("applyProductInfo — 상품기본조회 upsert")
    class ApplyProductInfo {

        @Test
        @DisplayName("StockDetail이 없으면 생성 후 상품 정보를 적용한다")
        void shouldCreateAndApplyWhenAbsent() {
            service.applyProductInfo("005930",
                    "삼성전자", "삼성전자", "SAMSUNG ELECTRONICS",
                    "005930", "1", "201", "국내주식");

            StockDetail d = stockDetailRepository.findById("005930").orElseThrow();
            assertThat(d.getPrdtAbrvName()).isEqualTo("삼성전자");
            assertThat(d.getPrdtClsfNm()).isEqualTo("국내주식");
        }

        @Test
        @DisplayName("이미 존재하면 상품 정보를 갱신하고 행 수를 유지한다")
        void shouldUpdateWhenExists() {
            service.applyProductInfo("005930",
                    "삼성전자보통주", "삼성전자B", "SAMSUNG ELECTRONICS",
                    "005930", "2", "201", "구분류");
            service.applyProductInfo("005930",
                    "삼성전자", "삼성전자", "SAMSUNG ELECTRONICS",
                    "005930", "1", "201", "국내주식");

            assertThat(stockDetailRepository.count()).isEqualTo(1);
            assertThat(stockDetailRepository.findById("005930").orElseThrow().getPrdtClsfNm())
                    .isEqualTo("국내주식");
        }
    }
}
