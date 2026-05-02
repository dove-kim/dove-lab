package com.dove.stockconsumer;

import com.dove.market.application.dto.DailyMarketDataQuery;
import com.dove.technicalindicator.application.dto.IndicatorCalcRequest;
import com.dove.technicalindicator.application.dto.IndicatorCalcTrigger;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.CommitFailedException;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.CooperativeStickyAssignor;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    // ── Producer ─────────────────────────────────────────────────────────────

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(ObjectMapper objectMapper) {
        JsonSerializer<Object> valueSerializer = new JsonSerializer<>(objectMapper);
        valueSerializer.configure(Map.of(JsonSerializer.ADD_TYPE_INFO_HEADERS, false), false);
        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(
                producerProps(), new StringSerializer(), valueSerializer));
    }

    private Map<String, Object> producerProps() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        return props;
    }

    // ── Consumer ─────────────────────────────────────────────────────────────

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, DailyMarketDataQuery> dailyMarketDataFactory(ObjectMapper objectMapper) {
        return typedFactory(DailyMarketDataQuery.class, objectMapper);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, IndicatorCalcTrigger> indicatorCalcTriggerFactory(ObjectMapper objectMapper) {
        return typedFactory(IndicatorCalcTrigger.class, objectMapper);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, IndicatorCalcRequest> indicatorCalcFactory(ObjectMapper objectMapper) {
        return typedFactory(IndicatorCalcRequest.class, objectMapper);
    }

    private <T> ConcurrentKafkaListenerContainerFactory<String, T> typedFactory(Class<T> type, ObjectMapper objectMapper) {
        JsonDeserializer<T> deserializer = new JsonDeserializer<>(type, objectMapper);
        deserializer.addTrustedPackages("com.dove.*");
        ConcurrentKafkaListenerContainerFactory<String, T> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(new DefaultKafkaConsumerFactory<>(baseConsumerProps(), new StringDeserializer(), deserializer));
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        factory.setCommonErrorHandler(errorHandler());
        return factory;
    }

    private Map<String, Object> baseConsumerProps() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.PARTITION_ASSIGNMENT_STRATEGY_CONFIG, List.of(CooperativeStickyAssignor.class));
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, 1024);
        props.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 500);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 10);
        return props;
    }

    @Bean
    public DefaultErrorHandler errorHandler() {
        DefaultErrorHandler handler = new DefaultErrorHandler(new FixedBackOff(1000, 3));
        handler.addNotRetryableExceptions(CommitFailedException.class);
        return handler;
    }
}
