package com.restromind.app.common;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.SecretKey;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

public class JwtUtil {

    private final SecretKey key;
    private final long accessTokenTtlMs;
    private final long refreshTokenTtlMs;

    public JwtUtil(String secret, long accessTokenTtlMs, long refreshTokenTtlMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenTtlMs = accessTokenTtlMs;
        this.refreshTokenTtlMs = refreshTokenTtlMs;
    }

    public String generateAccessToken(Long userId, String email, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("email", email);
        claims.put("role", role);
        claims.put("type", "ACCESS");
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
        try { parseToken(token); return true; } catch (Exception e) { return false; }
    }
}
