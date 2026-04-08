package com.restromind.app.auth;

import com.restromind.app.auth.dto.AuthResponse;
import com.restromind.app.auth.dto.LoginRequest;
import com.restromind.app.auth.dto.RegisterRequest;
import com.restromind.app.auth.entity.RefreshToken;
import com.restromind.app.auth.entity.User;
import com.restromind.app.auth.repository.RefreshTokenRepository;
import com.restromind.app.auth.repository.UserRepository;
import com.restromind.app.auth.service.AuthService;
import com.restromind.app.common.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtUtil jwtUtil;

    @InjectMocks AuthService authService;

    private RegisterRequest registerReq;
    private User savedUser;

    @BeforeEach
    void setUp() {
        registerReq = new RegisterRequest();
        registerReq.setEmail("admin@test.com");
        registerReq.setPassword("password123");
        registerReq.setFullName("Test Admin");
        registerReq.setRole("ADMIN");

        savedUser = new User();
        savedUser.setId(1L);
        savedUser.setEmail("admin@test.com");
        savedUser.setPasswordHash("$2a$12$hashed");
        savedUser.setFullName("Test Admin");
        savedUser.setRole(User.Role.ADMIN);
    }

    // ── Register ──────────────────────────────────────────────────────────────

    @Test
    void register_newUser_returnsTokens() {
        when(userRepository.existsByEmail("admin@test.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("$2a$12$hashed");
        when(userRepository.save(any())).thenReturn(savedUser);
        when(jwtUtil.generateAccessToken(any(), any(), any())).thenReturn("access-token");
        when(jwtUtil.generateRefreshToken(any(), any())).thenReturn("refresh-token");

        AuthResponse resp = authService.register(registerReq);

        assertThat(resp.getAccessToken()).isEqualTo("access-token");
        assertThat(resp.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(resp.getRole()).isEqualTo("ADMIN");
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void register_duplicateEmail_throws409() {
        when(userRepository.existsByEmail("admin@test.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerReq))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Email already registered");
    }

    // ── Login ─────────────────────────────────────────────────────────────────

    @Test
    void login_validCredentials_returnsTokens() {
        LoginRequest req = new LoginRequest();
        req.setEmail("admin@test.com");
        req.setPassword("password123");

        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(savedUser));
        when(passwordEncoder.matches("password123", "$2a$12$hashed")).thenReturn(true);
        when(jwtUtil.generateAccessToken(any(), any(), any())).thenReturn("access-token");
        when(jwtUtil.generateRefreshToken(any(), any())).thenReturn("refresh-token");

        AuthResponse resp = authService.login(req);

        assertThat(resp.getAccessToken()).isEqualTo("access-token");
        assertThat(resp.getUserId()).isEqualTo(1L);
    }

    @Test
    void login_wrongPassword_throws401() {
        LoginRequest req = new LoginRequest();
        req.setEmail("admin@test.com");
        req.setPassword("wrong");

        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(savedUser));
        when(passwordEncoder.matches("wrong", "$2a$12$hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(req))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Invalid credentials");
    }

    @Test
    void login_unknownEmail_throws401() {
        LoginRequest req = new LoginRequest();
        req.setEmail("nobody@test.com");
        req.setPassword("pass");

        when(userRepository.findByEmail("nobody@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(req))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Invalid credentials");
    }

    // ── Refresh ───────────────────────────────────────────────────────────────

    @Test
    void refresh_validToken_returnsNewAccessToken() {
        UUID jti = UUID.randomUUID();
        io.jsonwebtoken.Claims claims = mock(io.jsonwebtoken.Claims.class);
        when(claims.get("type")).thenReturn("REFRESH");
        when(claims.get("jti", String.class)).thenReturn(jti.toString());
        when(jwtUtil.parseToken("refresh-token")).thenReturn(claims);

        RefreshToken rt = new RefreshToken();
        rt.setJti(jti);
        rt.setUser(savedUser);
        rt.setExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));
        rt.setRevoked(false);

        when(refreshTokenRepository.findByJti(jti)).thenReturn(Optional.of(rt));
        when(jwtUtil.generateAccessToken(1L, "admin@test.com", "ADMIN")).thenReturn("new-access-token");

        String result = authService.refresh("refresh-token");

        assertThat(result).isEqualTo("new-access-token");
    }

    @Test
    void refresh_revokedToken_throws401() {
        UUID jti = UUID.randomUUID();
        io.jsonwebtoken.Claims claims = mock(io.jsonwebtoken.Claims.class);
        when(claims.get("type")).thenReturn("REFRESH");
        when(claims.get("jti", String.class)).thenReturn(jti.toString());
        when(jwtUtil.parseToken("revoked-token")).thenReturn(claims);

        RefreshToken rt = new RefreshToken();
        rt.setJti(jti);
        rt.setUser(savedUser);
        rt.setExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));
        rt.setRevoked(true);

        when(refreshTokenRepository.findByJti(jti)).thenReturn(Optional.of(rt));

        assertThatThrownBy(() -> authService.refresh("revoked-token"))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("expired or revoked");
    }

    // ── Logout ────────────────────────────────────────────────────────────────

    @Test
    void logout_revokesAllTokens() {
        authService.logout(1L);
        verify(refreshTokenRepository).revokeAllByUserId(1L);
    }
}
