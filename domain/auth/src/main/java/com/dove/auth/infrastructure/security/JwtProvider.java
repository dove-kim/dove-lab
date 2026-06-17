package com.dove.auth.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * access·refresh JWT 토큰을 생성·검증한다.
 */
@Component
public class JwtProvider {

    public static final String TOKEN_TYPE_ACCESS = "ACCESS";
    public static final String TOKEN_TYPE_REFRESH = "REFRESH";

    private final SecretKey secretKey;
    private final long accessExpirationMs;
    private final long refreshExpirationMs;

    public JwtProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-expiration-ms:900000}") long accessExpirationMs,
            @Value("${jwt.refresh-expiration-ms:2592000000}") long refreshExpirationMs) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpirationMs = accessExpirationMs;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    /**
     * 회원 정보·권한·capability를 담은 access 토큰을 생성한다.
     */
    public String generateAccessToken(Long memberId, String username, String name, String role,
                                      boolean mustChangePassword, Set<String> capabilities) {
        return Jwts.builder()
                .subject(String.valueOf(memberId))
                .claim("tokenType", TOKEN_TYPE_ACCESS)
                .claim("username", username)
                .claim("name", name)
                .claim("role", role)
                .claim("capabilities", capabilities == null ? List.of() : List.copyOf(capabilities))
                .claim("mustChangePassword", mustChangePassword)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessExpirationMs))
                .signWith(secretKey)
                .compact();
    }

    /**
     * 회원 식별자만 담은 refresh 토큰을 생성한다.
     */
    public String generateRefreshToken(Long memberId) {
        return Jwts.builder()
                .subject(String.valueOf(memberId))
                .claim("tokenType", TOKEN_TYPE_REFRESH)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshExpirationMs))
                .signWith(secretKey)
                .compact();
    }

    public Long extractMemberId(String token) {
        return Long.valueOf(getClaims(token).getSubject());
    }

    public String extractUsername(String token) {
        return getClaims(token).get("username", String.class);
    }

    public String extractRole(String token) {
        return getClaims(token).get("role", String.class);
    }

    public boolean extractMustChangePassword(String token) {
        return Boolean.TRUE.equals(getClaims(token).get("mustChangePassword", Boolean.class));
    }

    public Set<String> extractCapabilities(String token) {
        return extractStringSet(token, "capabilities");
    }

    private Set<String> extractStringSet(String token, String claim) {
        Object raw = getClaims(token).get(claim);
        if (raw instanceof List<?> list) {
            return list.stream().map(String::valueOf).collect(Collectors.toUnmodifiableSet());
        }
        return Set.of();
    }

    public String extractTokenType(String token) {
        return getClaims(token).get("tokenType", String.class);
    }

    public Instant extractIssuedAt(String token) {
        Date iat = getClaims(token).getIssuedAt();
        return iat == null ? null : iat.toInstant();
    }

    /**
     * 토큰 서명·만료가 유효하면 true.
     */
    public boolean validate(String token) {
        try {
            getClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
