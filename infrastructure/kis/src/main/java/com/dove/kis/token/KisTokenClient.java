package com.dove.kis.token;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "kis-token",
        url = "${kis.base-url:https://openapi.koreainvestment.com:9443}",
        configuration = KisTokenClientConfig.class
)
/**
 * KIS OAuth2 접근토큰 발급·폐기 Feign 클라이언트.
 */
public interface KisTokenClient {
    /**
     * 접근토큰을 발급한다.
     */
    @PostMapping("/oauth2/tokenP")
    KisTokenResponse getToken(@RequestBody KisTokenRequest request);

    /**
     * 접근토큰을 폐기한다.
     */
    @PostMapping("/oauth2/revokeP")
    KisTokenRevokeResponse revokeToken(@RequestBody KisTokenRevokeRequest request);
}
