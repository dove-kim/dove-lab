package com.dove.security;

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
import java.util.List;

/**
 * Authorization 헤더의 Bearer 토큰을 검증하고 SecurityContext에 인증 객체를 주입한다.
 *
 * <p>Principal로 {@link AuthenticatedUser} record를 박아서 컨트롤러가
 * {@code @AuthenticationPrincipal AuthenticatedUser user}로 직접 받을 수 있다.
 * 매 요청마다 username으로 DB 조회 없이 토큰 자체에서 memberId/username/role을 확보한다.
 */
@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String token = extractToken(request);
        if (token != null && jwtProvider.validate(token)) {
            Long memberId = jwtProvider.extractMemberId(token);
            String username = jwtProvider.extractUsername(token);
            String role = jwtProvider.extractRole(token);
            boolean mustChangePassword = jwtProvider.extractMustChangePassword(token);

            AuthenticatedUser principal = new AuthenticatedUser(memberId, username, role, mustChangePassword);
            SimpleGrantedAuthority authority = new SimpleGrantedAuthority(role);
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(principal, null, List.of(authority));
            SecurityContextHolder.getContext().setAuthentication(auth);
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
