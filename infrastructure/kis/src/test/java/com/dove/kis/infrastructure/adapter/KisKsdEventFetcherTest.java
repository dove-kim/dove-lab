package com.dove.kis.infrastructure.adapter;

import com.dove.kis.infrastructure.client.KisStockClient;
import com.dove.kis.infrastructure.client.dto.KisKsdResponse;
import com.dove.kis.quota.KisGate;
import com.dove.stock.domain.enums.StockEventType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * KSD 어댑터가 게이트에 위임하는지 검증. (재시도는 KisGate가 담당)
 */
class KisKsdEventFetcherTest {

    private static final LocalDate D = LocalDate.of(2024, 1, 5);

    private final KisStockClient client = mock(KisStockClient.class);
    private final KisGate gate = mock(KisGate.class);
    private final KisKsdEventFetcher fetcher = new KisKsdEventFetcher(client, gate);

    private KisKsdResponse emptyOk() {
        KisKsdResponse resp = mock(KisKsdResponse.class);
        given(resp.isSuccess()).willReturn(true);
        given(resp.rows()).willReturn(List.of());
        return resp;
    }

    @Test
    @DisplayName("정상 응답이면 1회 호출로 끝난다")
    void shouldCallOnceWhenSuccess() {
        KisKsdResponse resp = emptyOk();
        given(gate.call(any())).willReturn(resp);

        fetcher.fetch(StockEventType.DIVIDEND, D, D, "");

        verify(gate, times(1)).call(any());
    }

    @Test
    @DisplayName("게이트가 던진 예외는 어댑터 자체 재시도 없이 그대로 전파한다")
    void shouldPropagateWhenGateThrows() {
        given(gate.call(any())).willThrow(new RuntimeException("KIS down"));

        assertThatThrownBy(() -> fetcher.fetch(StockEventType.DIVIDEND, D, D, ""))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("KIS down");

        verify(gate, times(1)).call(any());
    }
}
