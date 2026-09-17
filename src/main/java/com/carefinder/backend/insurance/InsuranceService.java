package com.carefinder.backend.insurance;

import com.carefinder.backend.common.ConflictException;
import com.carefinder.backend.common.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

@Service
public class InsuranceService {

    private final InsuranceProviderRepository repository;

    public InsuranceService(InsuranceProviderRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<InsuranceDtos.InsuranceResponse> listPublic() {
        return repository.findAllByActiveTrueOrderByNameAsc().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<InsuranceDtos.InsuranceResponse> listAdmin() {
        return repository.findAll().stream()
                .sorted(java.util.Comparator.comparing(InsuranceProvider::getName))
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public InsuranceDtos.InsuranceResponse create(InsuranceDtos.InsuranceRequest request) {
        String name = request.name().trim();
        if (repository.existsByNameIgnoreCase(name)) {
            throw new ConflictException("This insurance provider already exists.");
        }
        String slug = uniqueSlug(name);
        InsuranceProvider provider = new InsuranceProvider(name, slug, clean(request.description()));
        provider.setActive(request.active() == null || request.active());
        return toResponse(repository.save(provider));
    }

    @Transactional
    public InsuranceDtos.InsuranceResponse update(Long id, InsuranceDtos.InsuranceRequest request) {
        InsuranceProvider provider = requireProvider(id);
        String name = request.name().trim();
        repository.findByNameIgnoreCase(name)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new ConflictException("This insurance provider already exists.");
                });
        provider.setName(name);
        provider.setDescription(clean(request.description()));
        if (request.active() != null) {
            provider.setActive(request.active());
        }
        return toResponse(provider);
    }

    @Transactional
    public void deactivate(Long id) {
        requireProvider(id).setActive(false);
    }

    private InsuranceProvider requireProvider(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Insurance provider not found."));
    }

    private InsuranceDtos.InsuranceResponse toResponse(InsuranceProvider provider) {
        return new InsuranceDtos.InsuranceResponse(
                provider.getId(),
                provider.getName(),
                provider.getSlug(),
                provider.getDescription(),
                provider.isActive()
        );
    }

    private String uniqueSlug(String name) {
        String base = Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        String candidate = base;
        int suffix = 2;
        while (repository.existsBySlug(candidate)) {
            candidate = base + "-" + suffix++;
        }
        return candidate;
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
