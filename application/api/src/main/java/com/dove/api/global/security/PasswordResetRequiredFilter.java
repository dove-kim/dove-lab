package com.dove.api.global.security;

import com.dove.security.AuthenticatedUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 비밀번호 초기화 상태인 사용자가 비밀번호 변경 외 API를 호출할 수 없도록 차단한다.
 * JWT에 mustChangePassword=true 클레임이 있으면 PATCH /me/password 이외의 모든 요청을 403으로 거부.
 */
public class PasswordResetRequiredFilter extends OncePerRequestFilter {

    private static final String ALLOWED_METHOD = "PATCH";
    private static final String ALLOWED_PATH   = "/me/password";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthenticatedUser user && user.mustChangePassword()) {
            boolean isAllowed = ALLOWED_METHOD.equalsIgnoreCase(request.getMethod())
                    && ALLOWED_PATH.equals(request.getRequestURI());
            if (!isAllowed) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/problem+json");
                response.getWriter().write("{\"status\":403,\"detail\":\"PASSWORD_RESET_REQUIRED\"}");
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
