package com.dove.stockconsumer.service;

import com.dove.market.domain.enums.MarketType;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DailyStockPriceSaveServiceTest {

    @Mock
    private DailyStockPriceCommandService dailyStockPriceCommandService;

    private DailyStockPriceSaveService service;

    private final LocalDate baseDate = LocalDate.of(2026, 4, 21);

    @BeforeEach
    void setUp() {
        service = new DailyStockPriceSaveService(dailyStockPriceCommandService);
    }

    @Test
    @DisplayName("정상 입력 → DailyStockPrice 저장")
    void shouldSaveDailyStockPrice() {
        service.save(baseDate, MarketType.KOSPI, "005930",
                1000L, 70000L, 71000L, 69500L, 71500L);

        ArgumentCaptor<DailyStockPrice> captor = ArgumentCaptor.forClass(DailyStockPrice.class);
        verify(dailyStockPriceCommandService).save(captor.capture());
        assertThat(captor.getValue().getClosePrice()).isEqualTo(71000L);
        assertThat(captor.getValue().getId().getStockCode()).isEqualTo("005930");
    }

    @Test
    @DisplayName("baseDate null → 저장 건너뜀")
    void shouldSkipWhenRequiredParamMissing() {
        service.save(null, MarketType.KOSPI, "005930",
                1000L, 70000L, 71000L, 69500L, 71500L);

        verify(dailyStockPriceCommandService, never()).save(any());
    }

    @Test
    @DisplayName("빈 stockCode → 저장 건너뜀")
    void shouldSkipWhenStockCodeEmpty() {
        service.save(baseDate, MarketType.KOSPI, "",
                1000L, 70000L, 71000L, 69500L, 71500L);

        verify(dailyStockPriceCommandService, never()).save(any());
    }
}
