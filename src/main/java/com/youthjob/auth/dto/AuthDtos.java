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
            String tokenType
    ) {}

    public record RefreshRequest(@NotBlank String refreshToken) {}

    public record LogoutRequest(String accessToken) {} // 블랙리스트
}
