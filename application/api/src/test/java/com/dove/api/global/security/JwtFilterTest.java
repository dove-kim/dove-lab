package com.dove.api.global.security;

import com.dove.auth.infrastructure.security.JwtProvider;
import com.dove.auth.application.service.ForcedLogoutService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JwtFilterTest {

    private static final String SECRET = "test-secret-key-that-is-32bytes!!";

    private JwtProvider jwtProvider;
    private ForcedLogoutService forcedLogoutService;
    private JwtFilter jwtFilter;

    @BeforeEach
    void setUp() {
        jwtProvider = new JwtProvider(SECRET, 900_000L, 2_592_000_000L);
        forcedLogoutService = mock(ForcedLogoutService.class);
        when(forcedLogoutService.isLoggedOut(anyLong(), any())).thenReturn(false);
        jwtFilter = new JwtFilter(jwtProvider, forcedLogoutService);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("인증 설정")
    class Authenticates {

        @Test
        @DisplayName("유효한 access token → SecurityContext에 AuthenticatedUser 주입 (features 포함)")
        void shouldSetAuthenticationForValidAccessToken() throws ServletException, IOException {
            String token = jwtProvider.generateAccessToken(7L, "alice", "Alice", "USER", false, Set.of("STOCK_SEARCH"));
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("Authorization", "Bearer " + token);
            MockHttpServletResponse response = new MockHttpServletResponse();
            FilterChain chain = mock(FilterChain.class);

            jwtFilter.doFilter(request, response, chain);

            var auth = SecurityContextHolder.getContext().getAuthentication();
            assertThat(auth).isNotNull();
            AuthenticatedUser user = (AuthenticatedUser) auth.getPrincipal();
            assertThat(user.memberId()).isEqualTo(7L);
            assertThat(user.username()).isEqualTo("alice");
            assertThat(user.role()).isEqualTo("USER");
            assertThat(user.capabilities()).containsExactly("STOCK_SEARCH");

            verify(chain).doFilter(request, response);
        }

        @Test
        @DisplayName("ADMIN access token → authority에 ADMIN 포함")
        void shouldSetAdminAuthorityForAdminToken() throws ServletException, IOException {
            String token = jwtProvider.generateAccessToken(99L, "admin", "Admin", "ADMIN", false, Set.of());
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("Authorization", "Bearer " + token);
            MockHttpServletResponse response = new MockHttpServletResponse();
            FilterChain chain = mock(FilterChain.class);

            jwtFilter.doFilter(request, response, chain);

            var auth = SecurityContextHolder.getContext().getAuthentication();
            assertThat(auth).isNotNull();
            assertThat(auth.getAuthorities())
                    .extracting(a -> a.getAuthority())
                    .containsExactly("ADMIN");
        }
    }

    @Nested
    @DisplayName("인증 미설정")
    class DoesNotAuthenticate {

        @Test
        @DisplayName("refresh token 으로 일반 API 호출 시 SecurityContext 미세팅")
        void shouldNotAuthenticateRefreshToken() throws ServletException, IOException {
            String refresh = jwtProvider.generateRefreshToken(7L);
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("Authorization", "Bearer " + refresh);
            MockHttpServletResponse response = new MockHttpServletResponse();
            FilterChain chain = mock(FilterChain.class);

            jwtFilter.doFilter(request, response, chain);

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            verify(chain).doFilter(request, response);
        }

        @Test
        @DisplayName("ForcedLogout cutoff 적용 시 인증 미설정")
        void shouldNotAuthenticateWhenForcedLoggedOut() throws ServletException, IOException {
            String token = jwtProvider.generateAccessToken(7L, "alice", "Alice", "USER", false, Set.of());
            when(forcedLogoutService.isLoggedOut(eq(7L), any(Instant.class))).thenReturn(true);
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("Authorization", "Bearer " + token);
            MockHttpServletResponse response = new MockHttpServletResponse();
            FilterChain chain = mock(FilterChain.class);

            jwtFilter.doFilter(request, response, chain);

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }

        @Test
        @DisplayName("Authorization 헤더 없음 → SecurityContext 비어있고 체인 계속")
        void shouldNotSetAuthenticationWhenNoHeader() throws ServletException, IOException {
            MockHttpServletRequest request = new MockHttpServletRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();
            FilterChain chain = mock(FilterChain.class);

            jwtFilter.doFilter(request, response, chain);

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            verify(chain).doFilter(request, response);
        }

        @Test
        @DisplayName("변조된 토큰 → SecurityContext 비어있고 체인 계속")
        void shouldNotSetAuthenticationForTamperedToken() throws ServletException, IOException {
            String token = jwtProvider.generateAccessToken(1L, "alice", "Alice", "USER", false, Set.of());
            String tampered = token.substring(0, token.length() - 6) + "TAMPER";
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("Authorization", "Bearer " + tampered);
            MockHttpServletResponse response = new MockHttpServletResponse();
            FilterChain chain = mock(FilterChain.class);

            jwtFilter.doFilter(request, response, chain);

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            verify(chain).doFilter(request, response);
        }

        @Test
        @DisplayName("만료된 토큰 → SecurityContext 비어있고 체인 계속")
        void shouldNotSetAuthenticationForExpiredToken() throws ServletException, IOException, InterruptedException {
            JwtProvider shortLived = new JwtProvider(SECRET, 1L, 1L);
            String token = shortLived.generateAccessToken(1L, "alice", "Alice", "USER", false, Set.of());
            Thread.sleep(20);

            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("Authorization", "Bearer " + token);
            MockHttpServletResponse response = new MockHttpServletResponse();
            FilterChain chain = mock(FilterChain.class);

            jwtFilter.doFilter(request, response, chain);

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            verify(chain).doFilter(request, response);
        }
    }
}
