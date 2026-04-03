package com.restromind.gateway.filter;

import com.restromind.common.jwt.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter implements GlobalFilter, Ordered {

    private final JwtUtil jwtUtil;

    // Paths that don't require a JWT
    private static final Set<String> PUBLIC_PATHS = Set.of(
            "/auth/register", "/auth/login", "/auth/refresh", "/auth/oauth2/google"
    );

    // Paths that require ADMIN role
    private static final List<String> ADMIN_ONLY_PREFIXES = List.of(
            "/analytics", "/restaurants/admin"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        // Allow public paths through
        if (PUBLIC_PATHS.stream().anyMatch(path::startsWith)) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = authHeader.substring(7);
        if (!jwtUtil.isValid(token)) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        Claims claims = jwtUtil.parseToken(token);
        String role = claims.get("role", String.class);
        String userId = claims.getSubject();

        // Role-based access control for admin-only paths
        if (ADMIN_ONLY_PREFIXES.stream().anyMatch(path::startsWith) && !"ADMIN".equals(role)) {
            exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
            return exchange.getResponse().setComplete();
        }

        // Inject user context headers for downstream services
        ServerWebExchange mutated = exchange.mutate()
                .request(r -> r.headers(headers -> {
                    headers.set("X-User-Id", userId);
                    headers.set("X-User-Role", role);
                }))
                .build();

        return chain.filter(mutated);
    }

    @Override
    public int getOrder() {
        return -100; // Run before routing
    }
}
