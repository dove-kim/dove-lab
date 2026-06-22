package com.dove.stockcollection.application.service;

import com.dove.stock.application.service.StockEventService;
import com.dove.stock.application.service.StockQueryService;
import com.dove.stock.domain.enums.StockEventType;
import com.dove.stockcollection.application.port.KsdEventFetcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * KSD 권리 이벤트 수집 코어의 일일·백필 분기와 100행 캡 보완, 무효 행 스킵을 검증한다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("StockEventCollectionService")
class StockEventCollectionServiceTest {

    private static final LocalDate DAY = LocalDate.of(2026, 5, 31);
    private static final int TYPE_COUNT = StockEventType.values().length;

    @Mock
    private KsdEventFetcher fetcher;

    @Mock
    private StockEventService eventCommandService;

    @Mock
    private StockQueryService stockQueryService;

    @Mock
    private KsdEventRowMapper rowMapper;

    @InjectMocks
    private StockEventCollectionService service;

    @BeforeEach
    void setUp() {
        // @Value 미주입 방어: Parallel Semaphore(0) 데드락 회피
        ReflectionTestUtils.setField(service, "concurrency", 4);
    }

    /**
     * 저장 분기를 통과시키는 유효한 행 매퍼 스텁(ticker·recordDate 채움).
     */
    private void stubValidRow() {
        when(rowMapper.ticker(any())).thenReturn("005930");
        when(rowMapper.recordDate(any())).thenReturn(DAY);
        when(rowMapper.summary(any(), any())).thenReturn("요약");
        when(rowMapper.toJson(any())).thenReturn("{}");
    }

    private static List<Map<String, Object>> rows(int count) {
        List<Map<String, Object>> result = new ArrayList<>();
        IntStream.range(0, count).forEach(i -> result.add(Map.of("idx", i)));
        return result;
    }

    @Nested
    @DisplayName("collect — 일일(from==to)")
    class DailyCollect {

        @Test
        @DisplayName("100행 미만이면 종목별 보완 없이 수신 행을 그대로 저장한다")
        void shouldSaveRowsDirectlyWhenBelowPageLimit() {
            stubValidRow();
            when(fetcher.fetch(any(), eq(DAY), eq(DAY), eq(""))).thenReturn(rows(3));

            service.collect(DAY, DAY, CollectionProgress.NOOP);

            // 유형별 날짜범위 1콜(sht=""), 종목별 보완 없음
            verify(fetcher, times(TYPE_COUNT)).fetch(any(), eq(DAY), eq(DAY), eq(""));
            verify(fetcher, never()).fetch(any(), any(), any(), eq("005930"));
            verify(stockQueryService, never()).findAllTickers();
            // 유형 6종 × 행 3개 = 18회 저장
            verify(eventCommandService, times(TYPE_COUNT * 3))
                    .saveIfAbsent(eq("005930"), any(), eq(DAY), eq("요약"), eq("{}"));
        }
    }

    @Nested
    @DisplayName("collect — 일일 100행 캡 보완")
    class DailyCapBackfill {

        @Test
        @DisplayName("어떤 유형 첫 콜이 100행 이상이면 그 유형만 종목별 조회로 보완한다")
        void shouldFetchPerStockWhenTypeHitsPageLimit() {
            stubValidRow();
            StockEventType capped = StockEventType.DIVIDEND;
            when(stockQueryService.findAllTickers()).thenReturn(List.of("005930", "000660"));
            // 첫 콜(sht=""): DIVIDEND만 100행으로 캡 유발, 나머지 유형은 1행. 종목별 보완 콜은 종목당 1행.
            when(fetcher.fetch(any(), eq(DAY), eq(DAY), anyString()))
                    .thenAnswer(inv -> {
                        String sht = inv.getArgument(3);
                        if (sht.isEmpty()) return inv.getArgument(0) == capped ? rows(100) : rows(1);
                        return rows(1);
                    });

            service.collect(DAY, DAY, CollectionProgress.NOOP);

            // 캡 걸린 DIVIDEND 1종에 대해서만 findAllTickers 후 종목별 fetch
            verify(stockQueryService, times(1)).findAllTickers();
            verify(fetcher, times(1)).fetch(eq(capped), eq(DAY), eq(DAY), eq("005930"));
            verify(fetcher, times(1)).fetch(eq(capped), eq(DAY), eq(DAY), eq("000660"));
            // 비캡 유형은 종목별 조회 없음
            verify(fetcher, never()).fetch(eq(StockEventType.BONUS_ISSUE), any(), any(), eq("005930"));
        }
    }

    @Nested
    @DisplayName("collect — 백필(from!=to)")
    class RangeBackfill {

        @Test
        @DisplayName("종목별 전 구간을 조회하고 진행률 총량을 유형×종목으로 보고한다")
        void shouldFetchPerStockForAllTypesWhenRange() {
            stubValidRow();
            LocalDate from = LocalDate.of(2023, 1, 1);
            LocalDate to = LocalDate.of(2023, 12, 31);
            List<String> tickers = List.of("005930", "000660");
            when(stockQueryService.findAllTickers()).thenReturn(tickers);
            when(fetcher.fetch(any(), eq(from), eq(to), anyString())).thenReturn(rows(1));

            RecordingProgress progress = new RecordingProgress();
            service.collect(from, to, progress);

            // 날짜범위 1콜(sht="") 경로는 백필에서 타지 않음
            verify(fetcher, never()).fetch(any(), any(), any(), eq(""));
            // 유형×종목 종목별 조회
            verify(fetcher, times(TYPE_COUNT * tickers.size()))
                    .fetch(any(), eq(from), eq(to), anyString());
            // onTotal(types × tickers)
            assertThat(progress.total).isEqualTo(TYPE_COUNT * tickers.size());
        }
    }

    @Nested
    @DisplayName("save — 무효 행 스킵")
    class SkipInvalidRows {

        @Test
        @DisplayName("ticker가 공백이거나 recordDate가 null인 행은 저장하지 않는다")
        void shouldSkipWhenTickerBlankOrRecordDateNull() {
            // DIVIDEND 한 유형만 2행 반환(나머지 유형은 빈 목록) — 첫 행 유효, 둘째 행 무효
            when(fetcher.fetch(any(), eq(DAY), eq(DAY), eq(""))).thenReturn(List.of());
            when(fetcher.fetch(eq(StockEventType.DIVIDEND), eq(DAY), eq(DAY), eq(""))).thenReturn(rows(2));
            when(rowMapper.ticker(any())).thenReturn("005930", "");        // 둘째 행 공백 ticker
            when(rowMapper.recordDate(any())).thenReturn(DAY, (LocalDate) null);
            when(rowMapper.summary(any(), any())).thenReturn("요약");
            when(rowMapper.toJson(any())).thenReturn("{}");

            service.collect(DAY, DAY, CollectionProgress.NOOP);

            // 유효 1행만 저장, 무효 행(공백 ticker·null date)은 saveIfAbsent 미호출
            verify(eventCommandService, times(1))
                    .saveIfAbsent(eq("005930"), any(), eq(DAY), any(), any());
            verify(eventCommandService, never())
                    .saveIfAbsent(eq(""), any(), any(), any(), any());
        }
    }

    /**
     * onTotal로 전달된 총량을 기록하는 테스트용 진행 리스너.
     */
    static final class RecordingProgress implements CollectionProgress {
        int total;

        @Override
        public void onTotal(int total) {
            this.total = total;
        }

        @Override
        public void onProgress(int done) {}
    }
}
