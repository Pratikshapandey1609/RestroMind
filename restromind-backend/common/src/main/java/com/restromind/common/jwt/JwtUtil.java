package com.restromind.common.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

/**
 * Shared JWT utility used by api-gateway and auth-service.
 * Secret is injected via constructor — each service reads it from application.yml.
 */
public class JwtUtil {

    private final SecretKey key;
    private final long accessTokenTtlMs;
    private final long refreshTokenTtlMs;

    public JwtUtil(String secret, long accessTokenTtlMs, long refreshTokenTtlMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenTtlMs = accessTokenTtlMs;
        this.refreshTokenTtlMs = refreshTokenTtlMs;
    }

    public String generateAccessToken(Long userId, String email, String role, Long restaurantId) {
        Map<String, Object> claims = new java.util.HashMap<>();
        claims.put("email", email);
        claims.put("role", role);
        claims.put("type", "ACCESS");
        if (restaurantId != null) claims.put("restaurantId", restaurantId);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claims(claims)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessTokenTtlMs))
                .signWith(key)
                .compact();
    }

    public String generateRefreshToken(Long userId, String jti) {
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("jti", jti)
                .claim("type", "REFRESH")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshTokenTtlMs))
                .signWith(key)
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isValid(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
