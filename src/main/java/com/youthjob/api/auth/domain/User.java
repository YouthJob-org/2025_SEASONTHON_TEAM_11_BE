package com.youthjob.api.auth.domain;

import com.youthjob.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA 프록시/리플렉션용
@Table(name = "users",
        indexes = @Index(name="uk_users_email", columnList="email", unique = true))
public class User extends BaseTimeEntity implements UserDetails {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false, length=120, unique=true)
    private String email;

    @Column(nullable=false, length=200)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false, length=20)
    private Role role;

    @Column(length=500)
    private String refreshToken;

    // ===== 마이페이지에서 수정할 이름 -> 선택사항 =====
    @Column(length = 30)
    private String name;

    @Builder(access = AccessLevel.PRIVATE)
    private User(String email, String password, Role role) {
        this.email = email;
        this.password = password;
        this.role = role;
    }

    public static User createUser(String email, String encodedPassword, Role role) {
        if (email == null || email.isBlank()) throw new IllegalArgumentException("email required");
        if (encodedPassword == null || encodedPassword.isBlank()) throw new IllegalArgumentException("password required");
        if (role == null) role = Role.USER;
        return User.builder()
                .email(email)
                .password(encodedPassword)
                .role(role)
                .build();
    }

    /** 리프레시 토큰 발급/회전(rotate) */
    public void issueRefreshToken(String newRefreshToken) {
        this.refreshToken = newRefreshToken;
    }

    /** 리프레시 토큰 무효화(로그아웃) */
    public void clearRefreshToken() {
        this.refreshToken = null;
    }

    /** 비밀번호 변경 */
    public void changePassword(String encodedPassword) {
        if (encodedPassword == null || encodedPassword.isBlank()) {
            throw new IllegalArgumentException("encodedPassword required");
        }
        this.password = encodedPassword;
    }

    /** 마이페이지: 이름만 부분 업데이트(null은 미변경) */
    public void updateName(String name) {
        if (name != null) this.name = name;
    }

    // === UserDetails ===
    @Override public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of((GrantedAuthority) () -> "ROLE_" + role.name());
    }
    @Override public String getUsername() { return email; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
}
