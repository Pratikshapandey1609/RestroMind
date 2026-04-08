package com.restromind.app.restaurant.service;

import com.restromind.app.restaurant.dto.*;
import com.restromind.app.restaurant.entity.CuisineType;
import com.restromind.app.restaurant.entity.OperatingHours;
import com.restromind.app.restaurant.entity.Restaurant;
import com.restromind.app.restaurant.entity.RestaurantStatus;
import com.restromind.app.restaurant.repository.OperatingHoursRepository;
import com.restromind.app.restaurant.repository.RestaurantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RestaurantService {

    private final RestaurantRepository restaurantRepository;
    private final OperatingHoursRepository operatingHoursRepository;

    public record RestaurantFilterParams(
        String cuisineType, String sortBy,
        Double latitude, Double longitude,
        int page, int size) {}

    // ── Onboarding ────────────────────────────────────────────────────────────

    @Transactional
    public ResponseEntity<OnboardingStepResponse> onboardStep1(Long ownerId, OnboardingStep1Request req) {
        Optional<Restaurant> existing = restaurantRepository.findByOwnerId(ownerId);
        if (existing.isEmpty()) {
            Restaurant r = new Restaurant();
            r.setOwnerId(ownerId);
            r.setName(req.name());
            r.setLogoUrl(req.logoUrl());
            r.setStatus(RestaurantStatus.DRAFT);
            r.setCuisineType(CuisineType.OTHER);
            r.setAverageRating(BigDecimal.ZERO);
            r.setOnboardingStep(1);
            Restaurant saved = restaurantRepository.save(r);
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(new OnboardingStepResponse(saved.getId(), saved.getOnboardingStep()));
        } else {
            Restaurant r = existing.get();
            r.setName(req.name());
            r.setLogoUrl(req.logoUrl());
            r.setOnboardingStep(Math.max(r.getOnboardingStep(), 1));
            Restaurant saved = restaurantRepository.save(r);
            return ResponseEntity.ok(new OnboardingStepResponse(saved.getId(), saved.getOnboardingStep()));
        }
    }

    @Transactional
    public OnboardingStepResponse onboardStep2(Long ownerId, OnboardingStep2Request req) {
        Restaurant r = restaurantRepository.findByOwnerId(ownerId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Restaurant profile not found. Complete Step 1 first."));
        r.setCuisineType(req.cuisineType());
        r.setDescription(req.description());
        r.setOnboardingStep(Math.max(r.getOnboardingStep(), 2));
        Restaurant saved = restaurantRepository.save(r);
        return new OnboardingStepResponse(saved.getId(), saved.getOnboardingStep());
    }

    @Transactional
    public OnboardingStepResponse onboardStep3(Long ownerId, OnboardingStep3Request req) {
        Restaurant r = restaurantRepository.findByOwnerId(ownerId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Restaurant profile not found. Complete Step 1 first."));

        validateOperatingHours(req.operatingHours());

        r.setAddressLine1(req.addressLine1());
        r.setAddressLine2(req.addressLine2());
        r.setCity(req.city());
        r.setState(req.state());
        r.setPostalCode(req.postalCode());
        r.setCountry(req.country());
        r.setPhone(req.phone());
        r.setLatitude(req.latitude());
        r.setLongitude(req.longitude());
        r.setEstimatedDeliveryTime(req.estimatedDeliveryTime());
        r.setOnboardingStep(3);
        r.setStatus(RestaurantStatus.ACTIVE);
        Restaurant saved = restaurantRepository.save(r);

        operatingHoursRepository.deleteByRestaurantId(saved.getId());
        insertOperatingHours(saved, req.operatingHours());

        return new OnboardingStepResponse(saved.getId(), saved.getOnboardingStep());
    }

    public OnboardingStatusResponse getOnboardingStatus(Long ownerId) {
        Restaurant r = restaurantRepository.findByOwnerId(ownerId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Restaurant profile not found."));
        return new OnboardingStatusResponse(r.getId(), r.getOnboardingStep(), r.getStatus().name());
    }

    // ── Admin Profile ─────────────────────────────────────────────────────────

    public RestaurantProfileResponse getProfile(Long ownerId) {
        Restaurant r = restaurantRepository.findByOwnerId(ownerId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Restaurant profile not found."));
        return toProfileResponse(r);
    }

    @Transactional
    public RestaurantProfileResponse updateProfile(Long ownerId, UpdateProfileRequest req) {
        Restaurant r = restaurantRepository.findByOwnerId(ownerId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Restaurant profile not found."));

        if (req.operatingHours() != null) validateOperatingHours(req.operatingHours());

        r.setName(req.name());
        r.setLogoUrl(req.logoUrl());
        r.setDescription(req.description());
        if (req.cuisineType() != null) r.setCuisineType(req.cuisineType());
        if (req.addressLine1() != null) r.setAddressLine1(req.addressLine1());
        if (req.addressLine2() != null) r.setAddressLine2(req.addressLine2());
        if (req.city() != null) r.setCity(req.city());
        if (req.state() != null) r.setState(req.state());
        if (req.postalCode() != null) r.setPostalCode(req.postalCode());
        if (req.country() != null) r.setCountry(req.country());
        if (req.phone() != null) r.setPhone(req.phone());
        if (req.latitude() != null) r.setLatitude(req.latitude());
        if (req.longitude() != null) r.setLongitude(req.longitude());
        if (req.estimatedDeliveryTime() != null) r.setEstimatedDeliveryTime(req.estimatedDeliveryTime());

        Restaurant saved = restaurantRepository.save(r);

        if (req.operatingHours() != null) {
            operatingHoursRepository.deleteByRestaurantId(saved.getId());
            insertOperatingHours(saved, req.operatingHours());
        }

        return toProfileResponse(restaurantRepository.findById(saved.getId()).orElse(saved));
    }

    @Transactional
    public StatusUpdateResponse updateStatus(Long ownerId, StatusUpdateRequest req) {
        String statusStr = req.status().toUpperCase();
        if (!"OPEN".equals(statusStr) && !"CLOSED".equals(statusStr)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status must be OPEN or CLOSED");
        }
        Restaurant r = restaurantRepository.findByOwnerId(ownerId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Restaurant profile not found."));
        r.setStatus(RestaurantStatus.valueOf(statusStr));
        Restaurant saved = restaurantRepository.save(r);
        return new StatusUpdateResponse(saved.getId(), saved.getStatus().name());
    }

    // ── User Discovery ────────────────────────────────────────────────────────

    public PageResponse<RestaurantListItemResponse> listRestaurants(RestaurantFilterParams params) {
        if ("distance".equalsIgnoreCase(params.sortBy())
                && (params.latitude() == null || params.longitude() == null)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "latitude and longitude are required for distance sorting");
        }

        ZonedDateTime now = ZonedDateTime.now();
        List<Restaurant> available = restaurantRepository
            .findAvailableRestaurants(now.getDayOfWeek(), now.toLocalTime());

        if (params.cuisineType() != null && !params.cuisineType().isBlank()) {
            String filter = params.cuisineType().toUpperCase();
            available = available.stream()
                .filter(r -> r.getCuisineType() != null
                    && r.getCuisineType().name().equalsIgnoreCase(filter))
                .toList();
        }

        if ("rating".equalsIgnoreCase(params.sortBy())) {
            available = available.stream()
                .sorted(Comparator.comparing(Restaurant::getAverageRating,
                    Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        } else if ("deliveryTime".equalsIgnoreCase(params.sortBy())) {
            available = available.stream()
                .sorted(Comparator.comparing(Restaurant::getEstimatedDeliveryTime,
                    Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        } else if ("distance".equalsIgnoreCase(params.sortBy())) {
            double lat = params.latitude(), lon = params.longitude();
            available = available.stream()
                .sorted(Comparator.comparingDouble(r -> {
                    if (r.getLatitude() == null || r.getLongitude() == null) return Double.MAX_VALUE;
                    return haversineKm(lat, lon,
                        r.getLatitude().doubleValue(), r.getLongitude().doubleValue());
                }))
                .toList();
        }

        long total = available.size();
        int totalPages = params.size() == 0 ? 0 : (int) Math.ceil((double) total / params.size());
        int from = params.page() * params.size();
        int to = Math.min(from + params.size(), (int) total);

        List<RestaurantListItemResponse> content = (from >= total)
            ? List.of()
            : available.subList(from, to).stream().map(this::toListItem).toList();

        return new PageResponse<>(content, total, totalPages, params.page(), params.size());
    }

    public List<RestaurantListItemResponse> search(String query) {
        if (query == null || query.length() < 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Search query must be at least 2 characters");
        }
        return restaurantRepository.searchByName(query).stream().map(this::toListItem).toList();
    }

    public RestaurantProfileResponse getRestaurantById(Long id) {
        Restaurant r = restaurantRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Restaurant not found"));
        if (r.getStatus() == RestaurantStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Restaurant not found");
        }
        return toProfileResponse(r);
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    public RestaurantInternalResponse getInternal(Long id) {
        Restaurant r = restaurantRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Restaurant not found"));
        return new RestaurantInternalResponse(r.getId(), r.getName(), r.getOwnerId(),
            r.getStatus().name(),
            r.getCuisineType() != null ? r.getCuisineType().name() : null);
    }

    @Transactional
    public void updateRating(Long id, BigDecimal averageRating) {
        Restaurant r = restaurantRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Restaurant not found"));
        if (averageRating.compareTo(BigDecimal.ZERO) < 0
                || averageRating.compareTo(new BigDecimal("5.00")) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rating must be between 0.00 and 5.00");
        }
        r.setAverageRating(averageRating);
        restaurantRepository.save(r);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    public static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1), dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
            + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
            * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private void validateOperatingHours(List<OperatingHoursRequest> hours) {
        for (OperatingHoursRequest h : hours) {
            if (h.openTime() != null && h.closeTime() != null && !h.openTime().isBefore(h.closeTime())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "close_time must be after open_time");
            }
        }
    }

    private void insertOperatingHours(Restaurant restaurant, List<OperatingHoursRequest> hours) {
        for (OperatingHoursRequest h : hours) {
            OperatingHours oh = new OperatingHours();
            oh.setRestaurant(restaurant);
            oh.setDayOfWeek(h.dayOfWeek());
            oh.setOpenTime(h.openTime());
            oh.setCloseTime(h.closeTime());
            operatingHoursRepository.save(oh);
        }
    }

    private RestaurantProfileResponse toProfileResponse(Restaurant r) {
        List<OperatingHoursResponse> hours = r.getOperatingHours().stream()
            .sorted(Comparator.comparingInt(oh -> oh.getDayOfWeek().getValue()))
            .map(oh -> new OperatingHoursResponse(
                oh.getDayOfWeek().name(), oh.getOpenTime().toString(), oh.getCloseTime().toString()))
            .toList();
        return new RestaurantProfileResponse(
            r.getId(), r.getName(), r.getLogoUrl(), r.getDescription(),
            r.getCuisineType() != null ? r.getCuisineType().name() : null,
            r.getAddressLine1(), r.getAddressLine2(), r.getCity(), r.getState(),
            r.getPostalCode(), r.getCountry(), r.getPhone(),
            r.getStatus().name(), r.getEstimatedDeliveryTime(), r.getAverageRating(),
            r.getOnboardingStep(), hours);
    }

    private RestaurantListItemResponse toListItem(Restaurant r) {
        return new RestaurantListItemResponse(r.getId(), r.getName(), r.getLogoUrl(),
            r.getCuisineType() != null ? r.getCuisineType().name() : null,
            r.getAverageRating(), r.getEstimatedDeliveryTime(), r.getCity());
    }
}
