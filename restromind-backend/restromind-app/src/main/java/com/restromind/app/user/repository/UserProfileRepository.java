package com.restromind.app.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.restromind.app.user.entity.UserProfile;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {
    Optional<UserProfile> findByAuthUserId(Long authUserId);
}
