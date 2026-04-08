package com.restromind.app.user.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.restromind.app.auth.repository.UserRepository;
import com.restromind.app.user.dto.AddressRequest;
import com.restromind.app.user.dto.AddressResponse;
import com.restromind.app.user.dto.UpdateProfileRequest;
import com.restromind.app.user.dto.UserProfileResponse;
import com.restromind.app.user.entity.DeliveryAddress;
import com.restromind.app.user.entity.UserProfile;
import com.restromind.app.user.repository.DeliveryAddressRepository;
import com.restromind.app.user.repository.UserProfileRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserProfileRepository profileRepository;
    private final DeliveryAddressRepository addressRepository;
    private final UserRepository userRepository;

    // ── Profile ───────────────────────────────────────────────────────────────

    public UserProfileResponse getProfile(Long authUserId) {
        UserProfile profile = getOrCreate(authUserId);
        return toResponse(profile);
    }

    @Transactional
    public UserProfileResponse updateProfile(Long authUserId, UpdateProfileRequest req) {
        UserProfile profile = getOrCreate(authUserId);
        if (req.displayName() != null) profile.setDisplayName(req.displayName());
        if (req.profilePhoto() != null) profile.setProfilePhoto(req.profilePhoto());
        if (req.phone() != null) profile.setPhone(req.phone());
        return toResponse(profileRepository.save(profile));
    }

    // ── Addresses ─────────────────────────────────────────────────────────────

    public List<AddressResponse> getAddresses(Long authUserId) {
        UserProfile profile = getOrCreate(authUserId);
        return addressRepository.findByUserProfileIdOrderByCreatedAtDesc(profile.getId())
            .stream().map(this::toAddressResponse).toList();
    }

    @Transactional
    public AddressResponse addAddress(Long authUserId, AddressRequest req) {
        UserProfile profile = getOrCreate(authUserId);

        // If new address is default, clear existing default first
        if (req.isDefault()) {
            addressRepository.clearDefaultForUser(profile.getId());
        }

        DeliveryAddress addr = new DeliveryAddress();
        addr.setUserProfile(profile);
        addr.setLabel(req.label());
        addr.setAddress(req.address());
        addr.setCity(req.city());
        addr.setDefault(req.isDefault());
        return toAddressResponse(addressRepository.save(addr));
    }

    @Transactional
    public void deleteAddress(Long authUserId, Long addressId) {
        UserProfile profile = getOrCreate(authUserId);
        DeliveryAddress addr = addressRepository
            .findByIdAndUserProfileId(addressId, profile.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Address not found"));
        addressRepository.delete(addr);
    }

    @Transactional
    public AddressResponse setDefaultAddress(Long authUserId, Long addressId) {
        UserProfile profile = getOrCreate(authUserId);
        DeliveryAddress addr = addressRepository
            .findByIdAndUserProfileId(addressId, profile.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Address not found"));

        // Clear existing default, then set new one
        addressRepository.clearDefaultForUser(profile.getId());
        addr.setDefault(true);
        return toAddressResponse(addressRepository.save(addr));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Gets existing profile or auto-creates one on first access. */
    private UserProfile getOrCreate(Long authUserId) {
        return profileRepository.findByAuthUserId(authUserId).orElseGet(() -> {
            // Verify the auth user actually exists
            userRepository.findById(authUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
            UserProfile p = new UserProfile();
            p.setAuthUserId(authUserId);
            return profileRepository.save(p);
        });
    }

    private UserProfileResponse toResponse(UserProfile p) {
        List<AddressResponse> addresses = addressRepository
            .findByUserProfileIdOrderByCreatedAtDesc(p.getId())
            .stream().map(this::toAddressResponse).toList();
        return new UserProfileResponse(p.getId(), p.getAuthUserId(),
            p.getDisplayName(), p.getProfilePhoto(), p.getPhone(), addresses);
    }

    private AddressResponse toAddressResponse(DeliveryAddress a) {
        return new AddressResponse(a.getId(), a.getLabel(), a.getAddress(), a.getCity(), a.isDefault());
    }
}
