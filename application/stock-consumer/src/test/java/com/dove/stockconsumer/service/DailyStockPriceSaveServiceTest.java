package com.dove.stockconsumer.service;

import com.dove.market.domain.enums.MarketType;
import com.dove.stock.application.service.StockCommandService;
import com.dove.stock.application.service.StockQueryService;
import com.dove.stock.domain.entity.Stock;
import com.dove.stock.domain.enums.TradingStatus;
import com.dove.stockprice.application.service.DailyStockPriceCommandService;
import com.dove.stockprice.domain.entity.DailyStockPrice;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DailyStockPriceSaveServiceTest {

    @Mock
    private StockQueryService stockQueryService;
    @Mock
    private StockCommandService stockCommandService;
    @Mock
    private DailyStockPriceCommandService dailyStockPriceCommandService;

    private DailyStockPriceSaveService service;

    private final LocalDate baseDate = LocalDate.of(2026, 4, 21);

    @BeforeEach
    void setUp() {
        service = new DailyStockPriceSaveService(stockQueryService, stockCommandService, dailyStockPriceCommandService);
    }

    @Test
    @DisplayName("신규 종목 → Stock(ACTIVE) 생성 + DailyStockPrice 저장")
    void shouldCreateNewStockAndSaveData() {
        when(stockQueryService.findByMarketAndCode(MarketType.KOSPI, "005930"))
                .thenReturn(Optional.empty());

        service.update(baseDate, MarketType.KOSPI, "005930", "삼성전자",
                1000L, 70000L, 71000L, 69500L, 71500L);

        ArgumentCaptor<Stock> stockCaptor = ArgumentCaptor.forClass(Stock.class);
        verify(stockCommandService).save(stockCaptor.capture());
        assertThat(stockCaptor.getValue().getName()).isEqualTo("삼성전자");
        assertThat(stockCaptor.getValue().getTradingStatus()).isEqualTo(TradingStatus.ACTIVE);

        ArgumentCaptor<DailyStockPrice> dataCaptor = ArgumentCaptor.forClass(DailyStockPrice.class);
        verify(dailyStockPriceCommandService).save(dataCaptor.capture());
        assertThat(dataCaptor.getValue().getClosePrice()).isEqualTo(71000L);
    }

    @Test
    @DisplayName("기존 종목 → 이름 갱신 (기존 인스턴스 재사용)")
    void shouldUpdateExistingStockName() {
        Stock existing = new Stock(MarketType.KOSPI, "005930", "구이름", TradingStatus.ACTIVE);
        when(stockQueryService.findByMarketAndCode(MarketType.KOSPI, "005930"))
                .thenReturn(Optional.of(existing));

        service.update(baseDate, MarketType.KOSPI, "005930", "새이름",
                1000L, 70000L, 71000L, 69500L, 71500L);

        assertThat(existing.getName()).isEqualTo("새이름");
        verify(stockCommandService).save(existing);
    }

    @Test
    @DisplayName("필수 파라미터 누락 → 저장 건너뜀")
    void shouldSkipWhenRequiredParamMissing() {
        service.update(null, MarketType.KOSPI, "005930", "삼성전자",
                1000L, 70000L, 71000L, 69500L, 71500L);

        verify(stockCommandService, never()).save(any());
        verify(dailyStockPriceCommandService, never()).save(any());
    }

    @Test
    @DisplayName("빈 stockCode → 저장 건너뜀")
    void shouldSkipWhenStockCodeEmpty() {
        service.update(baseDate, MarketType.KOSPI, "", "삼성전자",
                1000L, 70000L, 71000L, 69500L, 71500L);

        verify(stockCommandService, never()).save(any());
        verify(dailyStockPriceCommandService, never()).save(any());
    }
}
