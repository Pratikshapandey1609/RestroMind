package com.restromind.auth.service;

import com.restromind.auth.dto.LoginRequest;
import com.restromind.auth.dto.RegisterRequest;
import com.restromind.auth.entity.RefreshToken;
import com.restromind.auth.entity.User;
import com.restromind.auth.repository.RefreshTokenRepository;
import com.restromind.auth.repository.UserRepository;
import com.restromind.common.dto.AuthResponse;
import com.restromind.common.jwt.JwtUtil;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtUtil jwtUtil;

    @InjectMocks AuthService authService;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setEmail("test@example.com");
        mockUser.setPasswordHash("hashed");
        mockUser.setFullName("Test User");
        mockUser.setRole(User.Role.USER);
    }

    // ── register ──────────────────────────────────────────────────────────────

    @Test
    void register_happyPath_returns201WithTokens() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("test@example.com");
        req.setPassword("password123");
        req.setFullName("Test User");
        req.setRole("USER");

        when(userRepository.existsByEmail(req.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(req.getPassword())).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenReturn(mockUser);
        when(jwtUtil.generateAccessToken(any(), any(), any(), any())).thenReturn("access-token");
        when(jwtUtil.generateRefreshToken(any(), any())).thenReturn("refresh-token");

        AuthResponse response = authService.register(req);

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(response.getRole()).isEqualTo("USER");
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void register_duplicateEmail_throws409() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("test@example.com");
        req.setPassword("password123");
        req.setFullName("Test User");
        req.setRole("USER");

        when(userRepository.existsByEmail(req.getEmail())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.CONFLICT));

        verify(userRepository, never()).save(any());
        verify(refreshTokenRepository, never()).save(any());
    }

    // ── login ─────────────────────────────────────────────────────────────────

    @Test
    void login_happyPath_returns200WithTokens() {
        LoginRequest req = new LoginRequest();
        req.setEmail("test@example.com");
        req.setPassword("password123");

        when(userRepository.findByEmail(req.getEmail())).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches(req.getPassword(), mockUser.getPasswordHash())).thenReturn(true);
        when(jwtUtil.generateAccessToken(any(), any(), any(), any())).thenReturn("access-token");
        when(jwtUtil.generateRefreshToken(any(), any())).thenReturn("refresh-token");

        AuthResponse response = authService.login(req);

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getUserId()).isEqualTo(1L);
    }

    @Test
    void login_wrongPassword_throws401() {
        LoginRequest req = new LoginRequest();
        req.setEmail("test@example.com");
        req.setPassword("wrongpass");

        when(userRepository.findByEmail(req.getEmail())).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches(req.getPassword(), mockUser.getPasswordHash())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    assertThat(rse.getReason()).isEqualTo("Invalid credentials");
                });
    }

    @Test
    void login_unknownEmail_throws401WithSameMessage() {
        LoginRequest req = new LoginRequest();
        req.setEmail("unknown@example.com");
        req.setPassword("password123");

        when(userRepository.findByEmail(req.getEmail())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    assertThat(rse.getReason()).isEqualTo("Invalid credentials");
                });
    }

    // ── refresh ───────────────────────────────────────────────────────────────

    @Test
    void refresh_validToken_returnsNewAccessToken() {
        UUID jti = UUID.randomUUID();
        Claims claims = mock(Claims.class);
        when(claims.get("type")).thenReturn("REFRESH");
        when(claims.get("jti", String.class)).thenReturn(jti.toString());

        RefreshToken rt = new RefreshToken();
        rt.setJti(jti);
        rt.setRevoked(false);
        rt.setExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));
        rt.setUser(mockUser);

        when(jwtUtil.parseToken(anyString())).thenReturn(claims);
        when(refreshTokenRepository.findByJti(jti)).thenReturn(Optional.of(rt));
        when(jwtUtil.generateAccessToken(any(), any(), any(), any())).thenReturn("new-access-token");

        String result = authService.refresh("valid-refresh-token");
        assertThat(result).isEqualTo("new-access-token");
    }

    @Test
    void refresh_wrongTypeClaim_throws401() {
        Claims claims = mock(Claims.class);
        when(claims.get("type")).thenReturn("ACCESS");
        when(jwtUtil.parseToken(anyString())).thenReturn(claims);

        assertThatThrownBy(() -> authService.refresh("bad-token"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    void refresh_jtiNotFound_throws401() {
        UUID jti = UUID.randomUUID();
        Claims claims = mock(Claims.class);
        when(claims.get("type")).thenReturn("REFRESH");
        when(claims.get("jti", String.class)).thenReturn(jti.toString());
        when(jwtUtil.parseToken(anyString())).thenReturn(claims);
        when(refreshTokenRepository.findByJti(jti)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh("token"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    void refresh_revokedToken_throws401() {
        UUID jti = UUID.randomUUID();
        Claims claims = mock(Claims.class);
        when(claims.get("type")).thenReturn("REFRESH");
        when(claims.get("jti", String.class)).thenReturn(jti.toString());

        RefreshToken rt = new RefreshToken();
        rt.setJti(jti);
        rt.setRevoked(true);
        rt.setExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));

        when(jwtUtil.parseToken(anyString())).thenReturn(claims);
        when(refreshTokenRepository.findByJti(jti)).thenReturn(Optional.of(rt));

        assertThatThrownBy(() -> authService.refresh("token"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    void refresh_expiredToken_throws401() {
        UUID jti = UUID.randomUUID();
        Claims claims = mock(Claims.class);
        when(claims.get("type")).thenReturn("REFRESH");
        when(claims.get("jti", String.class)).thenReturn(jti.toString());

        RefreshToken rt = new RefreshToken();
        rt.setJti(jti);
        rt.setRevoked(false);
        rt.setExpiresAt(Instant.now().minus(1, ChronoUnit.DAYS));

        when(jwtUtil.parseToken(anyString())).thenReturn(claims);
        when(refreshTokenRepository.findByJti(jti)).thenReturn(Optional.of(rt));

        assertThatThrownBy(() -> authService.refresh("token"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    // ── logout ────────────────────────────────────────────────────────────────

    @Test
    void logout_callsRevokeAllByUserId() {
        authService.logout(1L);
        verify(refreshTokenRepository).revokeAllByUserId(1L);
    }
}
