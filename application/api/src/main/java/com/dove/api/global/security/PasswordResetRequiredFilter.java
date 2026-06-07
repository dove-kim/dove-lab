package com.dove.api.global.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 비밀번호 초기화 상태인 사용자가 비밀번호 변경 외 API를 호출할 수 없도록 차단하는 필터.
 */
@RequiredArgsConstructor
public class PasswordResetRequiredFilter extends OncePerRequestFilter {

    private static final String ALLOWED_METHOD = "PATCH";
    private static final String ALLOWED_PATH   = "/account/password";

    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthenticatedUser user && user.mustChangePassword()) {
            boolean isAllowed = ALLOWED_METHOD.equalsIgnoreCase(request.getMethod())
                    && ALLOWED_PATH.equals(request.getRequestURI());
            if (!isAllowed) {
                ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                        HttpStatus.FORBIDDEN, "PASSWORD_RESET_REQUIRED");
                response.setStatus(HttpStatus.FORBIDDEN.value());
                response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
                objectMapper.writeValue(response.getWriter(), problem);
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
