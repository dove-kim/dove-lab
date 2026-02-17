package com.dove.stockkrxdata.client;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.support.SpringDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;


@Configuration
@EnableFeignClients
public class KrxStockClientConfig {
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


}

