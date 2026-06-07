package com.dove.api.global.security;

import com.dove.auth.infrastructure.security.JwtProvider;
import com.dove.auth.application.service.ForcedLogoutService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * Bearer access token 을 검증하고 SecurityContext 에 인증 객체를 주입하는 필터.
 */
@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final ForcedLogoutService forcedLogoutService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String token = extractToken(request);
        if (token != null && jwtProvider.validate(token)) {
            String tokenType = jwtProvider.extractTokenType(token);
            if (JwtProvider.TOKEN_TYPE_ACCESS.equals(tokenType)) {
                Long memberId = jwtProvider.extractMemberId(token);
                Instant issuedAt = jwtProvider.extractIssuedAt(token);
                if (!forcedLogoutService.isLoggedOut(memberId, issuedAt)) {
                    String username = jwtProvider.extractUsername(token);
                    String role = jwtProvider.extractRole(token);
                    boolean mustChangePassword = jwtProvider.extractMustChangePassword(token);
                    Set<String> features = jwtProvider.extractFeatures(token);

                    AuthenticatedUser principal = new AuthenticatedUser(
                            memberId, username, role, mustChangePassword, features);
                    SimpleGrantedAuthority authority = new SimpleGrantedAuthority(role);
                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(principal, null, List.of(authority));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            }
        }
        chain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
