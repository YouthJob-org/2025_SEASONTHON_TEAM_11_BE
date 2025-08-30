package com.youthjob.auth.controller;

import com.youthjob.auth.dto.AuthDtos.*;
import com.youthjob.auth.service.AuthService;
import com.youthjob.common.response.ApiResponse;
import com.youthjob.common.response.SuccessStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<Void>> signUp(@RequestBody @Valid SignUpRequest req) {
        authService.signUp(req);
        return ApiResponse.successOnly(SuccessStatus.MEMBER_SIGNUP_SUCCESS);
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@RequestBody @Valid LoginRequest req) {
        return ApiResponse.success(SuccessStatus.LOGIN_SUCCESS, authService.login(req));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@RequestBody @Valid RefreshRequest req) {
        return ApiResponse.success(SuccessStatus.AUTH_SUCCESS, authService.refresh(req));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader,
            @RequestBody(required = false) RefreshRequest body
    ) {
        String access = (authHeader != null && authHeader.startsWith("Bearer "))
                ? authHeader.substring(7) : null;
        String refresh = (body != null) ? body.refreshToken() : null;
        authService.logout(access, refresh);
        return ApiResponse.successOnly(SuccessStatus.LOGOUT_SUCCESS);
    }
}
