package com.carefinder.backend.personal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    List<Favorite> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<Favorite> findByUserIdAndHospitalId(UUID userId, Long hospitalId);

    long countByUserId(UUID userId);

    void deleteByUserId(UUID userId);
}
