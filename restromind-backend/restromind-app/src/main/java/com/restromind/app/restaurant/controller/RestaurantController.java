package com.restromind.app.restaurant.controller;

import com.restromind.app.restaurant.dto.*;
import com.restromind.app.restaurant.service.RestaurantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/restaurants")
@RequiredArgsConstructor
public class RestaurantController {

    private final RestaurantService restaurantService;

    /** Extracts the authenticated user's ID from the JWT filter attribute. */
    private Long userId(HttpServletRequest req) {
        Object id = req.getAttribute("userId");
        if (id == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        return (Long) id;
    }

    /** Throws 403 if the authenticated user is not ADMIN. */
    private void requireAdmin(HttpServletRequest req) {
        Object role = req.getAttribute("role");
        if (!"ADMIN".equals(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin role required");
        }
    }

    // ── Admin Onboarding ──────────────────────────────────────────────────────

    @Tag(name = "Restaurant Management")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Onboarding Step 1 — submit restaurant name and logo")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Restaurant created"),
        @ApiResponse(responseCode = "200", description = "Restaurant updated"),
        @ApiResponse(responseCode = "400", description = "Validation error"),
        @ApiResponse(responseCode = "401", description = "Not authenticated"),
        @ApiResponse(responseCode = "403", description = "Admin role required")
    })
    @PostMapping("/onboarding/step1")
    public ResponseEntity<OnboardingStepResponse> step1(
            HttpServletRequest req,
            @Valid @RequestBody OnboardingStep1Request body) {
        requireAdmin(req);
        return restaurantService.onboardStep1(userId(req), body);
    }

    @Tag(name = "Restaurant Management")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Onboarding Step 2 — submit cuisine type and description")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Updated"),
        @ApiResponse(responseCode = "400", description = "Validation error"),
        @ApiResponse(responseCode = "403", description = "Admin role required"),
        @ApiResponse(responseCode = "404", description = "Complete Step 1 first")
    })
    @PostMapping("/onboarding/step2")
    public ResponseEntity<OnboardingStepResponse> step2(
            HttpServletRequest req,
            @Valid @RequestBody OnboardingStep2Request body) {
        requireAdmin(req);
        return ResponseEntity.ok(restaurantService.onboardStep2(userId(req), body));
    }

    @Tag(name = "Restaurant Management")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Onboarding Step 3 — submit address, hours, location (activates restaurant)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Restaurant activated"),
        @ApiResponse(responseCode = "400", description = "Validation error"),
        @ApiResponse(responseCode = "403", description = "Admin role required"),
        @ApiResponse(responseCode = "404", description = "Complete Step 1 first")
    })
    @PostMapping("/onboarding/step3")
    public ResponseEntity<OnboardingStepResponse> step3(
            HttpServletRequest req,
            @Valid @RequestBody OnboardingStep3Request body) {
        requireAdmin(req);
        return ResponseEntity.ok(restaurantService.onboardStep3(userId(req), body));
    }

    @Tag(name = "Restaurant Management")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get onboarding status")
    @GetMapping("/onboarding/status")
    public ResponseEntity<OnboardingStatusResponse> onboardingStatus(HttpServletRequest req) {
        requireAdmin(req);
        return ResponseEntity.ok(restaurantService.getOnboardingStatus(userId(req)));
    }

    // ── Admin Profile ─────────────────────────────────────────────────────────

    @Tag(name = "Restaurant Management")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get own restaurant profile")
    @GetMapping("/me")
    public ResponseEntity<RestaurantProfileResponse> getProfile(HttpServletRequest req) {
        requireAdmin(req);
        return ResponseEntity.ok(restaurantService.getProfile(userId(req)));
    }

    @Tag(name = "Restaurant Management")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update own restaurant profile")
    @PutMapping("/me")
    public ResponseEntity<RestaurantProfileResponse> updateProfile(
            HttpServletRequest req,
            @Valid @RequestBody UpdateProfileRequest body) {
        requireAdmin(req);
        return ResponseEntity.ok(restaurantService.updateProfile(userId(req), body));
    }

    @Tag(name = "Restaurant Management")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Override restaurant status (OPEN or CLOSED)")
    @PatchMapping("/me/status")
    public ResponseEntity<StatusUpdateResponse> updateStatus(
            HttpServletRequest req,
            @Valid @RequestBody StatusUpdateRequest body) {
        requireAdmin(req);
        return ResponseEntity.ok(restaurantService.updateStatus(userId(req), body));
    }

    // ── User Discovery ────────────────────────────────────────────────────────

    @Tag(name = "Restaurant Discovery")
    @Operation(summary = "List available restaurants with optional filtering and sorting")
    @GetMapping
    public ResponseEntity<PageResponse<RestaurantListItemResponse>> listRestaurants(
            @RequestParam(required = false) String cuisineType,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude) {
        RestaurantService.RestaurantFilterParams params =
            new RestaurantService.RestaurantFilterParams(
                cuisineType, sortBy, latitude, longitude, page, Math.min(size, 100));
        return ResponseEntity.ok(restaurantService.listRestaurants(params));
    }

    @Tag(name = "Restaurant Discovery")
    @Operation(summary = "Search restaurants by name (min 2 chars)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Results returned"),
        @ApiResponse(responseCode = "400", description = "Query too short")
    })
    @GetMapping("/search")
    public ResponseEntity<List<RestaurantListItemResponse>> search(@RequestParam String query) {
        return ResponseEntity.ok(restaurantService.search(query));
    }

    @Tag(name = "Restaurant Discovery")
    @Operation(summary = "Get restaurant detail by ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Restaurant found"),
        @ApiResponse(responseCode = "404", description = "Not found or not published")
    })
    @GetMapping("/{id}")
    public ResponseEntity<RestaurantProfileResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(restaurantService.getRestaurantById(id));
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    @Tag(name = "Internal")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Internal restaurant lookup by ID")
    @GetMapping("/{id}/internal")
    public ResponseEntity<RestaurantInternalResponse> getInternal(@PathVariable Long id) {
        return ResponseEntity.ok(restaurantService.getInternal(id));
    }

    @Tag(name = "Internal")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update restaurant average rating")
    @PatchMapping("/{id}/rating")
    public ResponseEntity<Void> updateRating(
            @PathVariable Long id,
            @RequestBody RatingUpdateRequest body) {
        restaurantService.updateRating(id, body.averageRating());
        return ResponseEntity.ok().build();
    }
}
