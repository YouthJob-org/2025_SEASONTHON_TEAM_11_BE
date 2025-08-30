package com.youthjob.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

public class AuthDtos {

    public record SignUpRequest(
            @Email @NotBlank String email,
            @NotBlank String password
    ) {}

    public record LoginRequest(
            @Email @NotBlank String email,
            @NotBlank String password
    ) {}

    @Builder
    public record AuthResponse(
            String accessToken,
            String refreshToken,
            String tokenType // "Bearer"
    ) {}

    public record RefreshRequest(@NotBlank String refreshToken) {}

    public record LogoutRequest(String accessToken) {} // 선택: 블랙리스트에 올릴 때 사용
}
