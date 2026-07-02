package com.dove.systemevent.application.service;

import com.dove.market.domain.enums.MarketType;
import com.dove.systemevent.domain.entity.SystemEvent;
import com.dove.systemevent.domain.enums.SystemEventType;
import com.dove.systemevent.domain.repository.SystemEventRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("SystemEventService")
class SystemEventServiceTest {

    @Mock
    private SystemEventRepository repository;

    @InjectMocks
    private SystemEventService service;

    private SystemEvent captureSaved() {
        ArgumentCaptor<SystemEvent> captor = ArgumentCaptor.forClass(SystemEvent.class);
        verify(repository).save(captor.capture());
        return captor.getValue();
    }

    @Nested
    @DisplayName("실패 기록")
    class RecordFailures {

        @Test
        @DisplayName("recordKisApiFailure — KIS 유형·source/error 상세를 저장한다")
        void shouldRecordKisFailure() {
            service.recordKisApiFailure("INVESTOR", "타임아웃");

            SystemEvent e = captureSaved();
            assertThat(e.getEventType()).isEqualTo(SystemEventType.KIS_API_FAILURE);
            assertThat(e.getMarketType()).isNull();
            assertThat(e.getDetail()).containsEntry("source", "INVESTOR").containsEntry("error", "타임아웃");
        }

        @Test
        @DisplayName("recordKisApiFailure — errorMessage가 null이면 빈 문자열로 저장(NPE 없음)")
        void shouldHandleNullKisMessage() {
            service.recordKisApiFailure("INVESTOR", null);

            assertThat(captureSaved().getDetail()).containsEntry("error", "");
        }

        @Test
        @DisplayName("recordKrxApiFailure — KRX 유형·시장·error를 저장하고 null 메시지를 빈 문자열로(NPE 없음)")
        void shouldRecordKrxFailureAndGuardNull() {
            service.recordKrxApiFailure(MarketType.KOSPI, null);

            SystemEvent e = captureSaved();
            assertThat(e.getEventType()).isEqualTo(SystemEventType.KRX_API_FAILURE);
            assertThat(e.getMarketType()).isEqualTo(MarketType.KOSPI);
            assertThat(e.getDetail()).containsEntry("error", "");
        }

        @Test
        @DisplayName("recordModelScoringFailure — 모델 유형·modelId/errorCode/message 상세를 저장한다")
        void shouldRecordModelScoringFailure() {
            service.recordModelScoringFailure(7L, "FEATURE_MISMATCH", "피처 불일치");

            SystemEvent e = captureSaved();
            assertThat(e.getEventType()).isEqualTo(SystemEventType.MODEL_SCORING_FAILURE);
            assertThat(e.getMarketType()).isNull();
            assertThat(e.getDetail())
                    .containsEntry("modelId", "7")
                    .containsEntry("errorCode", "FEATURE_MISMATCH")
                    .containsEntry("message", "피처 불일치");
        }

        @Test
        @DisplayName("recordKrxRateLimit — 응답 본문을 200자로 잘라 저장한다")
        void shouldTruncateRateLimitBodyTo200() {
            String body = "x".repeat(300);

            service.recordKrxRateLimit(MarketType.KOSDAQ, LocalDate.of(2024, 5, 30), body);

            SystemEvent e = captureSaved();
            assertThat(e.getEventType()).isEqualTo(SystemEventType.KRX_RATE_LIMIT_EXCEEDED);
            assertThat(e.getMarketType()).isEqualTo(MarketType.KOSDAQ);
            assertThat(e.getDetail()).containsEntry("date", "2024-05-30");
            assertThat(e.getDetail().get("responseBodySnippet")).hasSize(200);
        }
    }

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("저장 행을 그대로 페이지로 반환한다")
        void shouldReturnEntityPage() {
            SystemEvent e = SystemEvent.of(SystemEventType.KIS_API_FAILURE, null, java.util.Map.of("error", "x"));
            given(repository.findAllByOrderByOccurredAtDesc(PageRequest.of(0, 10)))
                    .willReturn(new PageImpl<>(List.of(e)));

            Page<SystemEvent> page = service.findAll(PageRequest.of(0, 10));

            assertThat(page.getContent()).hasSize(1);
            assertThat(page.getContent().get(0).getEventType()).isEqualTo(SystemEventType.KIS_API_FAILURE);
        }
    }
}
