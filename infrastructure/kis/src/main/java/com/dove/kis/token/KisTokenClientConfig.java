package com.dove.kis.token;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.cloud.openfeign.support.SpringDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

/**
 * KIS 토큰 Feign 클라이언트 설정.
 */
public class KisTokenClientConfig {
    @Bean
    public SpringDecoder kisTokenDecoder() {
        ObjectFactory<HttpMessageConverters> factory = () ->
                new HttpMessageConverters(new MappingJackson2HttpMessageConverter());
        return new SpringDecoder(factory);
    }
}
