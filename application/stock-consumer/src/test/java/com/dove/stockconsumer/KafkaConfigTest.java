package com.dove.stockconsumer;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.CommitFailedException;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("KafkaConfig 테스트")
class KafkaConfigTest {

    private KafkaConfig kafkaConfig;

    @BeforeEach
    void setUp() {
        kafkaConfig = new KafkaConfig();
        ReflectionTestUtils.setField(kafkaConfig, "bootstrapServers", "localhost:9092");
    }

    @Test
    @DisplayName("DefaultErrorHandler Bean을 등록한다")
    void shouldRegisterCustomErrorHandler() {
        DefaultErrorHandler errorHandler = kafkaConfig.errorHandler();

        assertThat(errorHandler).isNotNull();
    }

    @Test
    @DisplayName("CommitFailedException은 재시도하지 않는다")
    @SuppressWarnings("unchecked")
    void shouldMarkCommitFailedExceptionAsNonRetryable() {
        DefaultErrorHandler errorHandler = kafkaConfig.errorHandler();

        Consumer<Object, Object> consumer = mock(Consumer.class);
        MessageListenerContainer container = mock(MessageListenerContainer.class);
        ConsumerRecord<Object, Object> record = new ConsumerRecord<>("test", 0, 0L, "key", "value");

        errorHandler.handleRemaining(
                new CommitFailedException(),
                List.of(record),
                consumer,
                container
        );

        // non-retryable 예외는 seek 없이 즉시 recovery한다 (재시도 안 함)
        verify(consumer, never()).seek(any(TopicPartition.class), anyLong());
    }
}
