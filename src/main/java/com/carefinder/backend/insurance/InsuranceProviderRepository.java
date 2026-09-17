package com.carefinder.backend.insurance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface InsuranceProviderRepository extends JpaRepository<InsuranceProvider, Long> {

    List<InsuranceProvider> findAllByActiveTrueOrderByNameAsc();

    List<InsuranceProvider> findAllByIdInAndActiveTrue(Collection<Long> ids);

    Optional<InsuranceProvider> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);

    boolean existsBySlug(String slug);
}
