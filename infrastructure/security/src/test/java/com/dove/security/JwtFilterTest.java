package com.dove.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class JwtFilterTest {

    private static final String SECRET = "test-secret-key-that-is-32bytes!!";

    private JwtProvider jwtProvider;
    private JwtFilter jwtFilter;

    @BeforeEach
    void setUp() {
        jwtProvider = new JwtProvider(SECRET, 3_600_000L, 2_592_000_000L);
        jwtFilter = new JwtFilter(jwtProvider);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("유효한 Bearer 토큰 → SecurityContext에 AuthenticatedUser 주입")
    void shouldSetAuthenticationForValidToken() throws ServletException, IOException {
        String token = jwtProvider.generate(7L, "alice", "Alice", "USER", false, false);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        jwtFilter.doFilter(request, response, chain);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getPrincipal()).isInstanceOf(AuthenticatedUser.class);

        AuthenticatedUser user = (AuthenticatedUser) auth.getPrincipal();
        assertThat(user.memberId()).isEqualTo(7L);
        assertThat(user.username()).isEqualTo("alice");
        assertThat(user.role()).isEqualTo("USER");

        verify(chain).doFilter(request, response);
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
    @DisplayName("Bearer 접두어 없는 헤더 → SecurityContext 비어있고 체인 계속")
    void shouldNotSetAuthenticationWhenNoBearerPrefix() throws ServletException, IOException {
        String token = jwtProvider.generate(1L, "alice", "Alice", "USER", false, false);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", token); // Bearer 없음
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        jwtFilter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("변조된 토큰 → SecurityContext 비어있고 체인 계속")
    void shouldNotSetAuthenticationForTamperedToken() throws ServletException, IOException {
        String token = jwtProvider.generate(1L, "alice", "Alice", "USER", false, false);
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
        String token = shortLived.generate(1L, "alice", "Alice", "USER", false, false);
        Thread.sleep(20);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        jwtFilter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("ADMIN 토큰 → authority에 ADMIN 포함")
    void shouldSetAdminAuthorityForAdminToken() throws ServletException, IOException {
        String token = jwtProvider.generate(99L, "admin", "Admin", "ADMIN", false, false);
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
