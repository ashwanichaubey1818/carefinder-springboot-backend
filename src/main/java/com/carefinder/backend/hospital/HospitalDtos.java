package com.carefinder.backend.hospital;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class HospitalDtos {

    private HospitalDtos() {
    }

    public record HospitalResponse(
            Long id,
            String name,
            String city,
            String state,
            double latitude,
            double longitude,
            double rating,
            int reviews,
            boolean emergency,
            boolean open24x7,
            List<String> insurance,
            List<String> specialties,
            String type,
            int beds,
            String accreditation,
            String address,
            String phone,
            String website,
            String description,
            boolean verified,
            Double distanceKm
    ) {
    }

    public record HospitalRequest(
            @NotBlank @Size(max = 220) String name,
            @NotBlank @Size(max = 100) String city,
            @NotBlank @Size(max = 100) String state,
            @DecimalMin("-90.0") @DecimalMax("90.0") double latitude,
            @DecimalMin("-180.0") @DecimalMax("180.0") double longitude,
            @DecimalMin("0.0") @DecimalMax("5.0") double rating,
            @Min(0) int reviews,
            boolean emergency,
            boolean open24x7,
            @NotEmpty @Size(max = 12) List<@NotBlank @Size(max = 120) String> specialties,
            @NotBlank @Size(max = 140) String type,
            @Min(1) @Max(10000) int beds,
            @NotBlank @Size(max = 80) String accreditation,
            @Size(max = 500) String address,
            @Pattern(regexp = "^$|^[0-9+() -]{8,25}$", message = "must be a valid phone number") String phone,
            @Size(max = 250) String website,
            @Size(max = 1500) String description,
            Boolean active,
            Boolean verified,
            @NotNull Set<Long> insuranceProviderIds,
            UUID managerUserId
    ) {
    }

    public record CompareResponse(List<HospitalResponse> hospitals, ComparisonHighlights highlights) {
    }

    public record ComparisonHighlights(
            Long highestRatedHospitalId,
            Long largestHospitalId,
            List<Long> emergencyHospitalIds,
            List<Long> open24x7HospitalIds
    ) {
    }
}
