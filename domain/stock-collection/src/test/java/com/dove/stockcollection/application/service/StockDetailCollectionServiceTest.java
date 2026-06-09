package com.dove.stockcollection.application.service;

import com.dove.concurrent.ParallelException;
import com.dove.stock.application.service.StockCommandService;
import com.dove.stock.application.service.StockQueryService;
import com.dove.stock.application.service.StockTagValueService;
import com.dove.stock.domain.enums.TagField;
import com.dove.stockcollection.application.port.StockDetailFetcher;
import com.dove.stockcollection.application.port.StockInfoData;
import com.dove.stockcollection.application.port.StockProductData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * {@link StockDetailCollectionService} 단위 테스트.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("StockDetailCollectionService")
class StockDetailCollectionServiceTest {

    @Mock StockDetailFetcher fetcher;
    @Mock StockQueryService stockQueryService;
    @Mock StockCommandService stockCommandService;
    @Mock StockTagValueService tagValueService;
    @InjectMocks StockDetailCollectionService service;

    @BeforeEach
    void setUp() {
        // @Value 미주입 시 0 → Parallel의 Semaphore(0)로 영구 블로킹되므로 직접 설정
        ReflectionTestUtils.setField(service, "concurrency", 4);
    }

    private StockInfoData stockInfoWith(String idxLclsNm) {
        return new StockInfoData(
                100L, 5000L, 5000L,
                "1", null, null, "N",
                "001", "002", "003",
                idxLclsNm, "반도체", "메모리반도체",
                "C26", "전자부품",
                "0.5", "N", "N", null, "20000101");
    }

    private StockProductData productDataWith(String clsfName) {
        return new StockProductData("삼성전자", "삼성전자", "Samsung", "005930", "1", "P001", clsfName);
    }

    @Nested
    @DisplayName("updateAll — 정상 수집")
    class UpdateAllSuccess {

        @Test
        @DisplayName("종목별 상세를 적용하고 진행률을 통보하며 태그를 distinct로 등록한다")
        void shouldApplyDetailsAndRegisterDistinctTagsWhenSuccess() {
            given(stockQueryService.findAllTickers()).willReturn(List.of("005930", "000660"));

            given(fetcher.fetchStockInfo("005930")).willReturn(Optional.of(stockInfoWith("전기전자")));
            given(fetcher.fetchStockInfo("000660")).willReturn(Optional.empty());
            given(fetcher.fetchProductInfo("005930")).willReturn(Optional.of(productDataWith("주식")));
            given(fetcher.fetchProductInfo("000660")).willReturn(Optional.empty());

            CollectionProgress progress = new CollectionProgress() {
                public void onTotal(int t) {}
                public void onProgress(int d) {}
            };

            service.updateAll(progress);

            verify(stockCommandService).applyStockInfo(eq("005930"),
                    any(), any(), any(),
                    any(), any(), any(), any(),
                    any(), any(), any(),
                    any(), any(), any(),
                    any(), any(),
                    any(), any(), any(),
                    any(), any());
            verify(stockCommandService, never()).applyStockInfo(eq("000660"),
                    any(), any(), any(),
                    any(), any(), any(), any(),
                    any(), any(), any(),
                    any(), any(), any(),
                    any(), any(),
                    any(), any(), any(),
                    any(), any());
            verify(tagValueService).registerIfAbsent(TagField.INDUSTRY_LCLS.name(), "전기전자");
            verify(tagValueService).registerIfAbsent(TagField.PRDT_CLSF.name(), "주식");
        }

        @Test
        @DisplayName("같은 태그값은 중복 등록하지 않는다")
        void shouldNotRegisterDuplicateTagValues() {
            given(stockQueryService.findAllTickers()).willReturn(List.of("005930", "000660"));

            // 두 종목 모두 같은 업종
            given(fetcher.fetchStockInfo("005930")).willReturn(Optional.of(stockInfoWith("전기전자")));
            given(fetcher.fetchStockInfo("000660")).willReturn(Optional.of(stockInfoWith("전기전자")));
            given(fetcher.fetchProductInfo(anyString())).willReturn(Optional.empty());

            service.updateAll(CollectionProgress.NOOP);

            // 같은 업종명은 1번만 등록
            verify(tagValueService).registerIfAbsent(TagField.INDUSTRY_LCLS.name(), "전기전자");
        }
    }

    @Nested
    @DisplayName("updateAll — fetch 오류")
    class UpdateAllFailure {

        @Test
        @DisplayName("fetch 예외 시 ParallelException을 rethrow한다")
        void shouldRethrowParallelExceptionWhenFetchThrows() {
            given(stockQueryService.findAllTickers()).willReturn(List.of("005930"));
            given(fetcher.fetchStockInfo("005930")).willThrow(new RuntimeException("KIS down"));

            assertThatThrownBy(() -> service.updateAll(CollectionProgress.NOOP))
                    .isInstanceOf(ParallelException.class);
        }
    }

}
