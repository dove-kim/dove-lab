package com.dove.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 토큰 생성·검증 인프라 컴포넌트.
 *
 * <p>토큰 클레임 구조:
 * <ul>
 *   <li>{@code sub} = memberId (Long → String 직렬화)</li>
 *   <li>{@code username} = 로그인 식별자</li>
 *   <li>{@code name} = 표시명</li>
 *   <li>{@code role} = 권한 (USER/ADMIN, enum.name())</li>
 *   <li>{@code mustChangePassword} = 비밀번호 변경 강제 여부</li>
 * </ul>
 *
 * <p>이 클래스는 도메인 객체(예: MemberRole enum)를 직접 다루지 않는다. 호출자가 String
 * 형태로 role을 전달해야 하며, 토큰에서 추출 시에도 String을 반환한다. 인프라 레이어는
 * 도메인 결합을 가지지 않는다는 onion 원칙을 따른다.
 */
@Component
public class JwtProvider {

    private final SecretKey secretKey;
    private final long expirationMs;
    private final long rememberMeExpirationMs;

    public JwtProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-ms:3600000}") long expirationMs,
            @Value("${jwt.remember-me-expiration-ms:2592000000}") long rememberMeExpirationMs) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
        this.rememberMeExpirationMs = rememberMeExpirationMs;
    }

    /**
     * 액세스 토큰을 발급한다.
     *
     * @param memberId   회원 ID (sub claim)
     * @param username   로그인 식별자 (username claim)
     * @param name       표시명 (name claim)
     * @param role       권한 문자열 (role claim) — enum.name() 같이 호출자가 직렬화하여 전달
     * @param rememberMe true면 장기 만료 사용
     */
    public String generate(Long memberId, String username, String name, String role,
                           boolean rememberMe, boolean mustChangePassword) {
        long expMs = rememberMe ? rememberMeExpirationMs : expirationMs;
        return Jwts.builder()
                .subject(String.valueOf(memberId))
                .claim("username", username)
                .claim("name", name)
                .claim("role", role)
                .claim("mustChangePassword", mustChangePassword)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expMs))
                .signWith(secretKey)
                .compact();
    }

    public Long extractMemberId(String token) {
        return Long.valueOf(getClaims(token).getSubject());
    }

    public String extractUsername(String token) {
        return getClaims(token).get("username", String.class);
    }

    public String extractName(String token) {
        return getClaims(token).get("name", String.class);
    }

    public String extractRole(String token) {
        return getClaims(token).get("role", String.class);
    }

    public boolean extractMustChangePassword(String token) {
        return Boolean.TRUE.equals(getClaims(token).get("mustChangePassword", Boolean.class));
    }

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
