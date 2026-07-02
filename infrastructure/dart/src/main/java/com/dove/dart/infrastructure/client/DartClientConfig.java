package com.dove.dart.infrastructure.client;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.cloud.openfeign.support.SpringDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import java.util.List;

/**
 * DART 클라이언트 디코더 설정 — DART가 text/json 으로 응답해도 JSON으로 역직렬화한다.
 */
public class DartClientConfig {

    @Bean
    public SpringDecoder dartDecoder() {
        MappingJackson2HttpMessageConverter jackson = new MappingJackson2HttpMessageConverter();
        jackson.setSupportedMediaTypes(List.of(
                MediaType.APPLICATION_JSON,
                MediaType.valueOf("text/json"),
                new MediaType("text", "json"),
                MediaType.TEXT_PLAIN));
        ObjectFactory<HttpMessageConverters> factory = () -> new HttpMessageConverters(jackson);
        return new SpringDecoder(factory);
    }
}
