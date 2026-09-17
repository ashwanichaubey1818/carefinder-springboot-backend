package com.carefinder.backend.insurance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class InsuranceDtos {

    private InsuranceDtos() {
    }

    public record InsuranceResponse(Long id, String name, String slug, String description, boolean active) {
    }

    public record InsuranceRequest(
            @NotBlank @Size(max = 120) String name,
            @Size(max = 500) String description,
            Boolean active
    ) {
    }
}
