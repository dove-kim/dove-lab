package com.dove.modelserving.domain.meta;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

/**
 * 피처 목록으로부터 결정적 해시를 계산하는 유틸.
 */
public final class FeatureHasher {

    private FeatureHasher() {
    }

    /**
     * 피처 이름을 "|"로 이어 sha256한 뒤 앞 16자리(hex)를 반환한다.
     * 아티팩트가 만든 meta.json의 feature_hash와 동일한 규칙이다.
     *
     * @throws IllegalStateException SHA-256 알고리즘을 쓸 수 없을 때
     */
    public static String hash(List<String> features) {
        String joined = String.join("|", features);
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(joined.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.substring(0, 16);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 미지원", e);
        }
    }
}
