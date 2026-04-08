package com.restromind.app.restaurant;

import com.restromind.app.restaurant.dto.*;
import com.restromind.app.restaurant.entity.*;
import com.restromind.app.restaurant.repository.OperatingHoursRepository;
import com.restromind.app.restaurant.repository.RestaurantRepository;
import com.restromind.app.restaurant.service.RestaurantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RestaurantServiceTest {

    @Mock RestaurantRepository restaurantRepository;
    @Mock OperatingHoursRepository operatingHoursRepository;

    @InjectMocks RestaurantService restaurantService;

    private Restaurant existingRestaurant;

    @BeforeEach
    void setUp() {
        existingRestaurant = new Restaurant();
        existingRestaurant.setId(1L);
        existingRestaurant.setOwnerId(10L);
        existingRestaurant.setName("Pizza Palace");
        existingRestaurant.setStatus(RestaurantStatus.DRAFT);
        existingRestaurant.setCuisineType(CuisineType.ITALIAN);
        existingRestaurant.setAverageRating(BigDecimal.ZERO);
        existingRestaurant.setOnboardingStep(1);
    }

    // ── Onboarding Step 1 ─────────────────────────────────────────────────────

    @Test
    void step1_newRestaurant_returns201() {
        when(restaurantRepository.findByOwnerId(10L)).thenReturn(Optional.empty());
        when(restaurantRepository.save(any())).thenReturn(existingRestaurant);

        OnboardingStep1Request req = new OnboardingStep1Request("Pizza Palace", "https://logo.png");
        ResponseEntity<OnboardingStepResponse> resp = restaurantService.onboardStep1(10L, req);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getBody()).isNotNull();
        verify(restaurantRepository).save(any(Restaurant.class));
    }

    @Test
    void step1_existingRestaurant_returns200() {
        existingRestaurant.setOnboardingStep(2);
        when(restaurantRepository.findByOwnerId(10L)).thenReturn(Optional.of(existingRestaurant));
        when(restaurantRepository.save(any())).thenReturn(existingRestaurant);

        OnboardingStep1Request req = new OnboardingStep1Request("Updated Name", null);
        ResponseEntity<OnboardingStepResponse> resp = restaurantService.onboardStep1(10L, req);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        // Step should not regress — stays at 2
        assertThat(existingRestaurant.getOnboardingStep()).isEqualTo(2);
    }

    // ── Onboarding Step 2 ─────────────────────────────────────────────────────

    @Test
    void step2_noRestaurant_throws404() {
        when(restaurantRepository.findByOwnerId(10L)).thenReturn(Optional.empty());

        OnboardingStep2Request req = new OnboardingStep2Request(CuisineType.INDIAN, "Great food");
        assertThatThrownBy(() -> restaurantService.onboardStep2(10L, req))
            .isInstanceOf(ResponseStatusException.class)
            .extracting(e -> ((ResponseStatusException) e).getStatusCode())
            .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void step2_valid_advancesStep() {
        existingRestaurant.setOnboardingStep(1);
        when(restaurantRepository.findByOwnerId(10L)).thenReturn(Optional.of(existingRestaurant));
        when(restaurantRepository.save(any())).thenReturn(existingRestaurant);

        OnboardingStep2Request req = new OnboardingStep2Request(CuisineType.INDIAN, "Great food");
        OnboardingStepResponse resp = restaurantService.onboardStep2(10L, req);

        assertThat(existingRestaurant.getOnboardingStep()).isEqualTo(2);
        assertThat(existingRestaurant.getCuisineType()).isEqualTo(CuisineType.INDIAN);
    }

    // ── Onboarding Step 3 ─────────────────────────────────────────────────────

    @Test
    void step3_valid_activatesRestaurant() {
        existingRestaurant.setOnboardingStep(2);
        when(restaurantRepository.findByOwnerId(10L)).thenReturn(Optional.of(existingRestaurant));
        when(restaurantRepository.save(any())).thenReturn(existingRestaurant);

        OnboardingStep3Request req = new OnboardingStep3Request(
            "123 Main St", null, "Mumbai", "MH", "400001", "India",
            "9876543210", new BigDecimal("19.07"), new BigDecimal("72.87"), 30,
            List.of(new OperatingHoursRequest(java.time.DayOfWeek.MONDAY,
                LocalTime.of(9, 0), LocalTime.of(22, 0)))
        );

        OnboardingStepResponse resp = restaurantService.onboardStep3(10L, req);

        assertThat(existingRestaurant.getStatus()).isEqualTo(RestaurantStatus.ACTIVE);
        assertThat(existingRestaurant.getOnboardingStep()).isEqualTo(3);
        verify(operatingHoursRepository).deleteByRestaurantId(1L);
    }

    @Test
    void step3_invalidHours_throws400() {
        when(restaurantRepository.findByOwnerId(10L)).thenReturn(Optional.of(existingRestaurant));

        // closeTime before openTime
        OnboardingStep3Request req = new OnboardingStep3Request(
            "123 Main St", null, "Mumbai", "MH", "400001", "India",
            "9876543210", new BigDecimal("19.07"), new BigDecimal("72.87"), 30,
            List.of(new OperatingHoursRequest(java.time.DayOfWeek.MONDAY,
                LocalTime.of(22, 0), LocalTime.of(9, 0)))
        );

        assertThatThrownBy(() -> restaurantService.onboardStep3(10L, req))
            .isInstanceOf(ResponseStatusException.class)
            .extracting(e -> ((ResponseStatusException) e).getStatusCode())
            .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ── Profile ───────────────────────────────────────────────────────────────

    @Test
    void getProfile_found_returnsResponse() {
        existingRestaurant.setStatus(RestaurantStatus.ACTIVE);
        when(restaurantRepository.findByOwnerId(10L)).thenReturn(Optional.of(existingRestaurant));

        RestaurantProfileResponse resp = restaurantService.getProfile(10L);

        assertThat(resp.name()).isEqualTo("Pizza Palace");
        assertThat(resp.status()).isEqualTo("ACTIVE");
    }

    @Test
    void getProfile_notFound_throws404() {
        when(restaurantRepository.findByOwnerId(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> restaurantService.getProfile(10L))
            .isInstanceOf(ResponseStatusException.class)
            .extracting(e -> ((ResponseStatusException) e).getStatusCode())
            .isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ── Status Update ─────────────────────────────────────────────────────────

    @Test
    void updateStatus_open_succeeds() {
        existingRestaurant.setStatus(RestaurantStatus.ACTIVE);
        when(restaurantRepository.findByOwnerId(10L)).thenReturn(Optional.of(existingRestaurant));
        when(restaurantRepository.save(any())).thenReturn(existingRestaurant);

        StatusUpdateResponse resp = restaurantService.updateStatus(10L, new StatusUpdateRequest("OPEN"));

        assertThat(existingRestaurant.getStatus()).isEqualTo(RestaurantStatus.OPEN);
    }

    @Test
    void updateStatus_closed_succeeds() {
        existingRestaurant.setStatus(RestaurantStatus.OPEN);
        when(restaurantRepository.findByOwnerId(10L)).thenReturn(Optional.of(existingRestaurant));
        when(restaurantRepository.save(any())).thenReturn(existingRestaurant);

        restaurantService.updateStatus(10L, new StatusUpdateRequest("CLOSED"));

        assertThat(existingRestaurant.getStatus()).isEqualTo(RestaurantStatus.CLOSED);
    }

    @Test
    void updateStatus_invalidValue_throws400() {
        assertThatThrownBy(() -> restaurantService.updateStatus(10L, new StatusUpdateRequest("ACTIVE")))
            .isInstanceOf(ResponseStatusException.class)
            .extracting(e -> ((ResponseStatusException) e).getStatusCode())
            .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ── Search ────────────────────────────────────────────────────────────────

    @Test
    void search_shortQuery_throws400() {
        assertThatThrownBy(() -> restaurantService.search("p"))
            .isInstanceOf(ResponseStatusException.class)
            .extracting(e -> ((ResponseStatusException) e).getStatusCode())
            .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void search_validQuery_returnsResults() {
        existingRestaurant.setStatus(RestaurantStatus.ACTIVE);
        when(restaurantRepository.searchByName("pizza")).thenReturn(List.of(existingRestaurant));

        List<RestaurantListItemResponse> results = restaurantService.search("pizza");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).name()).isEqualTo("Pizza Palace");
    }

    // ── Rating ────────────────────────────────────────────────────────────────

    @Test
    void updateRating_valid_saves() {
        when(restaurantRepository.findById(1L)).thenReturn(Optional.of(existingRestaurant));
        when(restaurantRepository.save(any())).thenReturn(existingRestaurant);

        restaurantService.updateRating(1L, new BigDecimal("4.5"));

        assertThat(existingRestaurant.getAverageRating()).isEqualByComparingTo("4.5");
    }

    @Test
    void updateRating_outOfRange_throws400() {
        when(restaurantRepository.findById(1L)).thenReturn(Optional.of(existingRestaurant));

        assertThatThrownBy(() -> restaurantService.updateRating(1L, new BigDecimal("5.5")))
            .isInstanceOf(ResponseStatusException.class)
            .extracting(e -> ((ResponseStatusException) e).getStatusCode())
            .isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
