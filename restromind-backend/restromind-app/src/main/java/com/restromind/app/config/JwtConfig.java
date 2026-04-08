package com.restromind.app.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.restromind.app.common.JwtUtil;

@Configuration
public class JwtConfig {

    @Bean
    public JwtUtil jwtUtil(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-ttl-ms}") long accessTtl,
            @Value("${jwt.refresh-token-ttl-ms}") long refreshTtl) {
        return new JwtUtil(secret, accessTtl, refreshTtl);
    }
}
