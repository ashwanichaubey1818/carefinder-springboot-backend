package com.carefinder.backend.personal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RecentlyViewedRepository extends JpaRepository<RecentlyViewed, Long> {

    List<RecentlyViewed> findTop20ByUserIdOrderByLastViewedAtDesc(UUID userId);

    Optional<RecentlyViewed> findByUserIdAndHospitalId(UUID userId, Long hospitalId);

    long countByUserId(UUID userId);

    void deleteByUserIdAndHospitalId(UUID userId, Long hospitalId);

    void deleteByUserId(UUID userId);
}
