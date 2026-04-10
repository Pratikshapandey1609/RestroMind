package com.restromind.app.auth.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.restromind.app.auth.dto.AuthResponse;
import com.restromind.app.auth.dto.ChangePasswordRequest;
import com.restromind.app.auth.dto.LoginRequest;
import com.restromind.app.auth.dto.RegisterRequest;
import com.restromind.app.auth.dto.UpdateAccountRequest;
import com.restromind.app.auth.service.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Auth")
public class AuthController {

    private final AuthService authService;

    private Long userId(HttpServletRequest req) {
        Object id = req.getAttribute("userId");
        if (id == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        return (Long) id;
    }

    @Operation(summary = "Register a new user")
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(req));
    }

    @Operation(summary = "Login")
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(authService.login(req));
    }

    @Operation(summary = "Refresh access token")
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, String>> refresh(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(Map.of("accessToken", authService.refresh(body.get("refreshToken"))));
    }

    @Operation(summary = "Logout — revokes all refresh tokens")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest req) {
        authService.logout(userId(req));
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Change password (Settings page)")
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/password")
    public ResponseEntity<Void> changePassword(
            @Valid @RequestBody ChangePasswordRequest req,
            HttpServletRequest httpReq) {
        authService.changePassword(userId(httpReq), req);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Update account info — name and/or email (Settings page)")
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/account")
    public ResponseEntity<AuthResponse> updateAccount(
            @Valid @RequestBody UpdateAccountRequest req,
            HttpServletRequest httpReq) {
        return ResponseEntity.ok(authService.updateAccount(userId(httpReq), req));
    }
}
