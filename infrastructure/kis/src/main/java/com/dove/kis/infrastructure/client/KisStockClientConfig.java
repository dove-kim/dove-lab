package com.dove.kis.infrastructure.client;

import com.dove.kis.config.KisProperties;
import com.dove.kis.token.KisTokenManager;
import feign.RequestInterceptor;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.cloud.openfeign.support.SpringDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

/**
 * KIS 주식 Feign 클라이언트 설정.
 */
public class KisStockClientConfig {

    @Bean
    public SpringDecoder kisStockDecoder() {
        ObjectFactory<HttpMessageConverters> factory = () ->
                new HttpMessageConverters(new MappingJackson2HttpMessageConverter());
        return new SpringDecoder(factory);
    }

    @Bean
    public RequestInterceptor kisAuthInterceptor(KisTokenManager tokenManager, KisProperties properties) {
        return template -> {
            template.header("content-type", "application/json; charset=utf-8");
            template.header("authorization", "Bearer " + tokenManager.getToken());
            template.header("appkey", properties.getAppKey());
            template.header("appsecret", properties.getAppSecret());
            template.header("custtype", "P");
        };
    }
}
