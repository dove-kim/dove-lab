package com.dove.krx.infrastructure.client;

import com.dove.krx.quota.KrxApiQuotaService;
import feign.RequestInterceptor;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.cloud.openfeign.support.SpringDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

/**
 * KrxStockClient 전용 Feign 설정.
 *
 * <p>@Configuration 없이 정의 — 전역 등록 시 인터셉터가 KIS 등 다른 Feign 클라이언트에도 적용된다.
 */
public class KrxStockClientConfig {
    /**
     * Jackson 기반 HTTP 메시지 컨버터를 사용하는 Feign 디코더를 등록한다.
     */
    @Bean
    public SpringDecoder feignDecoder() {
        ObjectFactory<HttpMessageConverters> messageConverters = () -> {
            HttpMessageConverters converters = new HttpMessageConverters(
                    new MappingJackson2HttpMessageConverter()
            );
            return converters;
        };
        return new SpringDecoder(messageConverters);
    }

    /**
     * KRX API 호출 직전 일일 한도를 확인하는 인터셉터.
     */
    @Bean
    @ConditionalOnBean(KrxApiQuotaService.class)
    public RequestInterceptor krxQuotaInterceptor(KrxApiQuotaService quotaService) {
        return template -> quotaService.tryAcquire();
    }
}
