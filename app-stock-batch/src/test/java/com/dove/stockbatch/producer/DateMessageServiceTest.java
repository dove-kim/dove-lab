package com.dove.stockbatch.producer;

import com.dove.stockbatch.dto.KrxDailyStockDataRequest;
import com.dove.stockdata.enums.MarketType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DateMessageServiceTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private DateMessageService dateMessageService;

    @Test
    @DisplayName("KOSPI와 KOSDAQ 메시지를 모두 전송한다")
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

        verify(kafkaTemplate, times(2))
                .send(topicCaptor.capture(), keyCaptor.capture(), valueCaptor.capture());

        // 토픽 검증
        List<String> topics = topicCaptor.getAllValues();
        assertThat(topics).containsOnly("KRX_DATA_REQUEST");

        // 키 검증
        List<String> keys = keyCaptor.getAllValues();
        LocalDate expectedDate = LocalDate.now().minusDays(1);
        String expectedKospiKey = String.format("kospi-%s", expectedDate);
        String expectedKosdaqKey = String.format("kosdaq-%s", expectedDate);
        assertThat(keys).containsExactlyInAnyOrder(expectedKospiKey, expectedKosdaqKey);

        // 메시지 내용 검증
        List<KrxDailyStockDataRequest> requests = valueCaptor.getAllValues();
        assertThat(requests).hasSize(2);

        // KOSPI와 KOSDAQ 메시지가 모두 있는지 확인
        assertThat(requests)
                .extracting(KrxDailyStockDataRequest::getMarketType)
                .containsExactlyInAnyOrder(MarketType.KOSPI, MarketType.KOSDAQ);

        // 모든 요청의 날짜가 전날인지 확인
        assertThat(requests)
                .extracting(KrxDailyStockDataRequest::getBaseDate)
                .allMatch(date -> date.equals(expectedDate));

        // 이벤트 버전 확인
        assertThat(requests)
                .extracting(KrxDailyStockDataRequest::getEventVersion)
                .allMatch(version -> version.equals(1));
    }

    @Test
    @DisplayName("Kafka 전송 실패 시 예외를 로깅하고 계속 진행한다")
    void sendDailyStockDataRequest_Fail() {
        // given
        // KOSPI 전송은 성공, KOSDAQ 전송은 실패하는 시나리오
        when(kafkaTemplate.send(eq("KRX_DATA_REQUEST"), contains("kospi"), any(KrxDailyStockDataRequest.class)))
                .thenReturn(null);
        when(kafkaTemplate.send(eq("KRX_DATA_REQUEST"), contains("kosdaq"), any(KrxDailyStockDataRequest.class)))
                .thenThrow(new RuntimeException("Kafka connection failed"));

        // when & then
        // 예외가 발생해도 메서드가 정상 완료되는지 확인
        dateMessageService.sendDailyStockDataRequest();

        // 두 번의 전송 시도가 있었는지 확인
        verify(kafkaTemplate, times(2))
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
        verify(kafkaTemplate, times(2))
                .send(any(String.class), any(String.class), valueCaptor.capture());

        List<KrxDailyStockDataRequest> requests = valueCaptor.getAllValues();
        assertThat(requests)
                .extracting(KrxDailyStockDataRequest::getBaseDate)
                .allMatch(date -> date.equals(expectedDate));
    }
}