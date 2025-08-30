package com.youthjob.auth.service;

import com.youthjob.auth.domain.Role;
import com.youthjob.auth.domain.User;
import com.youthjob.auth.dto.AuthDtos.*;
import com.youthjob.auth.jwt.JwtService;
import com.youthjob.auth.jwt.TokenBlacklist;
import com.youthjob.auth.repository.UserRepository;
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
    private final TokenBlacklist blacklist; // 메모리/Redis 구현체로 대체 가능

    public void signUp(SignUpRequest req) {
        if (repo.existsByEmail(req.email())) {
            throw new IllegalStateException("이미 가입된 이메일입니다.");
        }
        var encoded = encoder.encode(req.password());
        var user = User.createUser(req.email(), encoded, Role.USER);
        repo.save(user);
    }

    public AuthResponse login(LoginRequest req) {
        // 인증(비밀번호 일치 검증)
        authManager.authenticate(new UsernamePasswordAuthenticationToken(req.email(), req.password()));

        var user = repo.findByEmail(req.email()).orElseThrow();

        String access = jwt.generateAccessToken(user);
        String refresh = jwt.generateRefreshToken(user.getEmail());

        // 명시적 동작 메서드로 상태 변경
        user.issueRefreshToken(refresh);
        repo.save(user);

        return new AuthResponse(access, refresh, "Bearer");
    }

    public AuthResponse refresh(RefreshRequest req) {
        String token = req.refreshToken();
        if (!jwt.isRefreshToken(token)) throw new IllegalArgumentException("리프레시 토큰이 아닙니다.");
        String email = jwt.extractUsername(token);
        var user = repo.findByEmail(email).orElseThrow();

        // 서버 저장본과 일치 여부 확인(회전/탈취 방지)
        if (user.getRefreshToken() == null || !user.getRefreshToken().equals(token)) {
            throw new IllegalStateException("유효하지 않은 리프레시 토큰입니다.");
        }

        // 회전
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
        // 액세스 블랙리스트(선택)
        if (accessTokenFromHeader != null) {
            try {
                var exp = jwt.getExpirationInstant(accessTokenFromHeader);
                blacklist.add(accessTokenFromHeader, exp);
            } catch (Exception ignored) {}
        }
    }
}
