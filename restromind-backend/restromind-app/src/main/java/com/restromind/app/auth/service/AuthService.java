package com.restromind.app.auth.service;

import com.restromind.app.auth.dto.AuthResponse;
import com.restromind.app.auth.dto.LoginRequest;
import com.restromind.app.auth.dto.RegisterRequest;
import com.restromind.app.auth.entity.RefreshToken;
import com.restromind.app.auth.entity.User;
import com.restromind.app.auth.repository.RefreshTokenRepository;
import com.restromind.app.auth.repository.UserRepository;
import com.restromind.app.common.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }
        User user = new User();
        user.setEmail(req.getEmail());
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        user.setFullName(req.getFullName());
        user.setRole(User.Role.valueOf(req.getRole()));
        userRepository.save(user);
        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByEmail(req.getEmail())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
        return buildAuthResponse(user);
    }

    @Transactional
    public String refresh(String refreshToken) {
        Claims claims;
        try {
            claims = jwtUtil.parseToken(refreshToken);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
        }
        if (!"REFRESH".equals(claims.get("type"))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token type");
        }
        UUID jti = UUID.fromString(claims.get("jti", String.class));
        RefreshToken stored = refreshTokenRepository.findByJti(jti)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token not found"));
        if (stored.isRevoked() || stored.getExpiresAt().isBefore(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token expired or revoked");
        }
        User user = stored.getUser();
        return jwtUtil.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name());
    }

    @Transactional
    public void logout(Long userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
    }

    private AuthResponse buildAuthResponse(User user) {
        String jti = UUID.randomUUID().toString();
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name());
        String refreshTokenStr = jwtUtil.generateRefreshToken(user.getId(), jti);

        RefreshToken rt = new RefreshToken();
        rt.setJti(UUID.fromString(jti));
        rt.setUser(user);
        rt.setExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));
        refreshTokenRepository.save(rt);

        return new AuthResponse(accessToken, refreshTokenStr, user.getRole().name(), user.getId());
    }
}
