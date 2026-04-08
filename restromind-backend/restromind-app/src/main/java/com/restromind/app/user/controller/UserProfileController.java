package com.restromind.app.user.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.restromind.app.user.dto.AddressRequest;
import com.restromind.app.user.dto.AddressResponse;
import com.restromind.app.user.dto.UpdateProfileRequest;
import com.restromind.app.user.dto.UserProfileResponse;
import com.restromind.app.user.service.UserProfileService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/users/me")
@RequiredArgsConstructor
@Tag(name = "User Profile")
@SecurityRequirement(name = "bearerAuth")
public class UserProfileController {

    private final UserProfileService userProfileService;

    private Long userId(HttpServletRequest req) {
        Object id = req.getAttribute("userId");
        if (id == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        return (Long) id;
    }

    // ── Profile ───────────────────────────────────────────────────────────────

    @Operation(summary = "Get my profile")
    @GetMapping
    public ResponseEntity<UserProfileResponse> getProfile(HttpServletRequest req) {
        return ResponseEntity.ok(userProfileService.getProfile(userId(req)));
    }

    @Operation(summary = "Update my profile (displayName, photo, phone)")
    @PutMapping
    public ResponseEntity<UserProfileResponse> updateProfile(
            @Valid @RequestBody UpdateProfileRequest body,
            HttpServletRequest req) {
        return ResponseEntity.ok(userProfileService.updateProfile(userId(req), body));
    }

    // ── Addresses ─────────────────────────────────────────────────────────────

    @Operation(summary = "Get my saved delivery addresses")
    @GetMapping("/addresses")
    public ResponseEntity<List<AddressResponse>> getAddresses(HttpServletRequest req) {
        return ResponseEntity.ok(userProfileService.getAddresses(userId(req)));
    }

    @Operation(summary = "Add a new delivery address")
    @PostMapping("/addresses")
    public ResponseEntity<AddressResponse> addAddress(
            @Valid @RequestBody AddressRequest body,
            HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(userProfileService.addAddress(userId(req), body));
    }

    @Operation(summary = "Delete a delivery address")
    @DeleteMapping("/addresses/{id}")
    public ResponseEntity<Void> deleteAddress(
            @PathVariable Long id,
            HttpServletRequest req) {
        userProfileService.deleteAddress(userId(req), id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Set an address as default")
    @PatchMapping("/addresses/{id}/default")
    public ResponseEntity<AddressResponse> setDefault(
            @PathVariable Long id,
            HttpServletRequest req) {
        return ResponseEntity.ok(userProfileService.setDefaultAddress(userId(req), id));
    }
}
