package com.dove.stockbatch.producer;

import com.dove.stockbatch.dto.KrxDailyStockDataRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DateMessageServiceTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private DateMessageService dateMessageService;

    @Test
    @DisplayName("전일 날짜로 단일 메시지를 전송한다")
    void sendDailyStockDataRequest_Success() {
        // given
        when(kafkaTemplate.send(any(String.class), any(String.class), any(KrxDailyStockDataRequest.class)))
                .thenReturn(null);

        // when
        dateMessageService.sendDailyStockDataRequest();

        // then
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<KrxDailyStockDataRequest> valueCaptor = ArgumentCaptor.forClass(KrxDailyStockDataRequest.class);

        verify(kafkaTemplate, times(1))
                .send(topicCaptor.capture(), keyCaptor.capture(), valueCaptor.capture());

        // 토픽 검증
        assertThat(topicCaptor.getValue()).isEqualTo("KRX_DATA_REQUEST");

        // 키 검증
        LocalDate expectedDate = LocalDate.now().minusDays(1);
        assertThat(keyCaptor.getValue()).isEqualTo(String.format("daily-%s", expectedDate));

        // 메시지 내용 검증
        KrxDailyStockDataRequest request = valueCaptor.getValue();
        assertThat(request.getBaseDate()).isEqualTo(expectedDate);
        assertThat(request.getEventVersion()).isEqualTo(2);
    }

    @Test
    @DisplayName("Kafka 전송 실패 시 예외를 로깅하고 계속 진행한다")
    void sendDailyStockDataRequest_Fail() {
        // given
        when(kafkaTemplate.send(eq("KRX_DATA_REQUEST"), any(String.class), any(KrxDailyStockDataRequest.class)))
                .thenThrow(new RuntimeException("Kafka connection failed"));

        // when & then — 예외가 발생해도 메서드가 정상 완료되는지 확인
        dateMessageService.sendDailyStockDataRequest();

        verify(kafkaTemplate, times(1))
                .send(eq("KRX_DATA_REQUEST"), any(String.class), any(KrxDailyStockDataRequest.class));
    }

    @Test
    @DisplayName("전날 날짜로 메시지를 생성하는지 검사")
    void sendDailyStockDataRequest_ValidateDate() {
        // given
        when(kafkaTemplate.send(any(String.class), any(String.class), any(KrxDailyStockDataRequest.class)))
                .thenReturn(null);

        LocalDate expectedDate = LocalDate.now().minusDays(1);

        // when
        dateMessageService.sendDailyStockDataRequest();

        // then
        ArgumentCaptor<KrxDailyStockDataRequest> valueCaptor = ArgumentCaptor.forClass(KrxDailyStockDataRequest.class);
        verify(kafkaTemplate, times(1))
                .send(any(String.class), any(String.class), valueCaptor.capture());

        assertThat(valueCaptor.getValue().getBaseDate()).isEqualTo(expectedDate);
    }
}
