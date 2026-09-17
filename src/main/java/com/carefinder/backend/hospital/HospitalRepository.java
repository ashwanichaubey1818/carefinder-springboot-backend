package com.carefinder.backend.hospital;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface HospitalRepository extends JpaRepository<Hospital, Long>, JpaSpecificationExecutor<Hospital> {

    @Override
    @EntityGraph(attributePaths = {"insuranceProviders", "specialties"})
    Optional<Hospital> findById(Long id);

    @EntityGraph(attributePaths = {"insuranceProviders", "specialties"})
    List<Hospital> findAllByIdIn(Collection<Long> ids);

    @EntityGraph(attributePaths = {"insuranceProviders", "specialties"})
    List<Hospital> findTop8ByActiveTrueAndNameContainingIgnoreCaseOrderByRatingDesc(String name);

    boolean existsByNameIgnoreCase(String name);

    Optional<Hospital> findByNameIgnoreCase(String name);

    boolean existsBySlug(String slug);

    long countByActiveTrue();

    @Query("select count(distinct h.state) from Hospital h where h.active = true")
    long countActiveStates();

    @Query("select count(distinct h.city) from Hospital h where h.active = true")
    long countActiveCities();
}
