package com.youthjob.api.auth.service;

import com.youthjob.api.auth.domain.Role;
import com.youthjob.api.auth.domain.User;
import com.youthjob.api.auth.dto.AuthDtos.AuthResponse;
import com.youthjob.api.auth.dto.AuthDtos.LoginRequest;
import com.youthjob.api.auth.dto.AuthDtos.RefreshRequest;
import com.youthjob.api.auth.dto.AuthDtos.SignUpRequest;
import com.youthjob.api.auth.jwt.JwtService;
import com.youthjob.api.auth.jwt.TokenBlacklist;
import com.youthjob.api.auth.repository.UserRepository;
import com.youthjob.common.exception.BadRequestException;
import com.youthjob.common.exception.UnauthorizedException;
import com.youthjob.common.response.ErrorStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository repo;
    private final PasswordEncoder encoder;
    private final AuthenticationManager authManager;
    private final JwtService jwt;
    private final TokenBlacklist blacklist;

    public void signUp(SignUpRequest req) {
        if (repo.existsByEmail(req.email())) {
            throw new BadRequestException(ErrorStatus.BAD_REQUEST_DUPLICATE_EMAIL.getMessage());
        }
        var encoded = encoder.encode(req.password());
        var user = User.createUser(req.email(), encoded, Role.USER);
        repo.save(user);
    }

    public AuthResponse login(LoginRequest req) {
        authManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.email(), req.password()));

        var user = repo.findByEmail(req.email()).orElseThrow(
                () -> new UnauthorizedException(ErrorStatus.UNAUTHORIZED_USER.getMessage())
        );

        String access = jwt.generateAccessToken(user);
        String refresh = jwt.generateRefreshToken(user.getEmail());

        user.issueRefreshToken(refresh);
        repo.save(user);

        return new AuthResponse(access, refresh, "Bearer");
    }

    public AuthResponse refresh(RefreshRequest req) {
        String token = req.refreshToken();
        if (!jwt.isRefreshToken(token)) {
            throw new UnauthorizedException(ErrorStatus.UNAUTHORIZED_INVALID_TOKEN.getMessage());
        }

        String email = jwt.extractUsername(token);
        var user = repo.findByEmail(email).orElseThrow(
                () -> new UnauthorizedException(ErrorStatus.UNAUTHORIZED_USER.getMessage())
        );

        if (user.getRefreshToken() == null || !user.getRefreshToken().equals(token)) {
            throw new UnauthorizedException(ErrorStatus.UNAUTHORIZED_INVALID_TOKEN.getMessage());
        }

        String newAccess = jwt.generateAccessToken(user);
        String newRefresh = jwt.generateRefreshToken(user.getEmail());

        user.issueRefreshToken(newRefresh);
        repo.save(user);

        return new AuthResponse(newAccess, newRefresh, "Bearer");
    }

    public void logout(String accessTokenFromHeader, String refreshTokenFromBody) {
        // 리프레시 무효화
        if (refreshTokenFromBody != null) {
            String email = jwt.extractUsername(refreshTokenFromBody);
            repo.findByEmail(email).ifPresent(u -> {
                if (refreshTokenFromBody.equals(u.getRefreshToken())) {
                    u.clearRefreshToken();
                    repo.save(u);
                }
            });
        }

        if (accessTokenFromHeader != null) {
            try {
                var exp = jwt.getExpirationInstant(accessTokenFromHeader);
                blacklist.add(accessTokenFromHeader, exp);
            } catch (Exception ignored) { }
        }
    }
}
