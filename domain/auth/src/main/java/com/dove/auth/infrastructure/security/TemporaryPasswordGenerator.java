package com.dove.auth.infrastructure.security;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * 임시 비밀번호 생성 정책 — 혼동 문자(I·O·0·1·l)를 제외한 문자로 고정 길이 무작위 비밀번호를 만든다.
 */
@Component
public class TemporaryPasswordGenerator {

    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
    private static final int LENGTH = 10;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * 혼동 문자를 제외한 무작위 임시 비밀번호(평문)를 생성한다.
     */
    public String generate() {
        StringBuilder sb = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            sb.append(CHARS.charAt(SECURE_RANDOM.nextInt(CHARS.length())));
        }
        return sb.toString();
    }
}
