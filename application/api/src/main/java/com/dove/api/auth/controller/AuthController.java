package com.dove.api.auth.controller;

import com.dove.api.auth.dto.LoginRequest;
import com.dove.api.auth.dto.LoginResponse;
import com.dove.api.auth.dto.LoginResult;
import com.dove.api.auth.dto.RegisterRequest;
import com.dove.api.auth.dto.RegisterResponse;
import com.dove.api.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public LoginResponse login(@RequestBody @Valid LoginRequest request) {
        LoginResult result = authService.login(request.username(), request.password(), request.rememberMe());
        return new LoginResponse(result.accessToken(), result.username(), result.name(), result.role().name(), result.rememberMe());
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponse register(@RequestBody @Valid RegisterRequest request) {
        try {
            LoginResult result = authService.register(
                    request.inviteCode(), request.username(), request.password(), request.email(), request.name());
            return new RegisterResponse(result.accessToken(), result.username(), result.name(), result.role().name());
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVITE_CODE_INVALID");
        }
    }
}
