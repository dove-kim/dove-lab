package com.dove.api.account.auth.controller;

import com.dove.api.account.auth.dto.*;
import com.dove.api.account.auth.service.AuthService;
import com.dove.auth.application.service.ForcedLogoutService;
import com.dove.api.global.security.AuthenticatedUser;
import com.dove.auth.infrastructure.security.JwtProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/**
 * 인증·회원가입·토큰 재발급 API.
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtProvider jwtProvider;
    private final ForcedLogoutService forcedLogoutService;

    /**
     * 아이디/비밀번호로 로그인하고 access/refresh 토큰을 발급한다.
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody @Valid LoginRequest request) {
        LoginResult result = authService.login(request.username(), request.password(), request.rememberMe());
        LoginResponse body = new LoginResponse(
                result.accessToken(), result.refreshToken(),
                result.username(), result.name(),
                result.role().name(), result.rememberMe());
        return ResponseEntity.ok(body);
    }

    /**
     * 초대 코드로 신규 회원을 등록하고 토큰을 발급한다.
     */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<RegisterResponse> register(@RequestBody @Valid RegisterRequest request) {
        try {
            LoginResult result = authService.register(
                    request.inviteCode(), request.username(), request.password(), request.email(), request.name());
            RegisterResponse body = new RegisterResponse(
                    result.accessToken(), result.refreshToken(),
                    result.username(), result.name(), result.role().name());
            return ResponseEntity.status(HttpStatus.CREATED).body(body);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVITE_CODE_INVALID");
        }
    }

    /**
     * refresh token으로 access/refresh 한 쌍을 재발급한다(rolling).
     */
    @PostMapping("/refresh")
    public ResponseEntity<RefreshResponse> refresh(@RequestBody @Valid RefreshRequest request) {
        String refreshToken = request.refreshToken();
        if (!jwtProvider.validate(refreshToken)
                || !JwtProvider.TOKEN_TYPE_REFRESH.equals(jwtProvider.extractTokenType(refreshToken))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "REFRESH_TOKEN_INVALID");
        }
        Long memberId = jwtProvider.extractMemberId(refreshToken);
        LoginResult result = authService.refresh(memberId);
        return ResponseEntity.ok(new RefreshResponse(result.accessToken(), result.refreshToken()));
    }

    /**
     * 로그아웃 — 사용자의 강제 로그아웃 cutoff를 설정해 기존 토큰을 즉시 무효화한다.
     */
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@AuthenticationPrincipal AuthenticatedUser user) {
        if (user != null) {
            try {
                forcedLogoutService.markLogoutNow(user.memberId());
            } catch (RuntimeException ignored) {
                // 실패해도 로그아웃 성공 처리
            }
        }
    }
}
