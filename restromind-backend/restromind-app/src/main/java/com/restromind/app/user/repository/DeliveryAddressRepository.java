package com.restromind.app.user.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.restromind.app.user.entity.DeliveryAddress;

public interface DeliveryAddressRepository extends JpaRepository<DeliveryAddress, Long> {
    List<DeliveryAddress> findByUserProfileIdOrderByCreatedAtDesc(Long userProfileId);
    Optional<DeliveryAddress> findByIdAndUserProfileId(Long id, Long userProfileId);

    @Modifying @Transactional
    @Query("UPDATE DeliveryAddress a SET a.isDefault = false WHERE a.userProfile.id = :profileId")
    void clearDefaultForUser(@Param("profileId") Long profileId);
}
