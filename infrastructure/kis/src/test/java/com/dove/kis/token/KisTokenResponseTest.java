package com.dove.kis.token;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KisTokenResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldParseTokenResponse() throws Exception {
        String json = """
                {
                  "access_token": "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9.eyJzdWIi",
                  "access_token_token_expired": "2023-12-22 08:16:59",
                  "token_type": "Bearer",
                  "expires_in": 86400
                }
                """;

        KisTokenResponse response = objectMapper.readValue(json, KisTokenResponse.class);

        assertThat(response.getAccessToken()).isEqualTo("eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9.eyJzdWIi");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getExpiresIn()).isEqualTo(86400L);
        assertThat(response.getAccessTokenExpired()).isEqualTo("2023-12-22 08:16:59");
    }

    @Test
    void shouldIgnoreUnknownFields() throws Exception {
        String json = """
                {
                  "access_token": "sometoken",
                  "token_type": "Bearer",
                  "expires_in": 86400,
                  "access_token_token_expired": "2023-12-22 08:16:59",
                  "unknown_field": "value"
                }
                """;

        KisTokenResponse response = objectMapper.readValue(json, KisTokenResponse.class);

        assertThat(response.getAccessToken()).isEqualTo("sometoken");
    }
}
