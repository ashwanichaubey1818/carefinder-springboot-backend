package com.carefinder.backend.hospital;

import com.carefinder.backend.insurance.InsuranceProvider;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.util.Locale;

final class HospitalSpecifications {

    private HospitalSpecifications() {
    }

    static Specification<Hospital> activeOnly() {
        return (root, query, builder) -> builder.isTrue(root.get("active"));
    }

    static Specification<Hospital> textContains(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String pattern = "%" + text.trim().toLowerCase(Locale.ROOT) + "%";
        return (root, query, builder) -> builder.or(
                builder.like(builder.lower(root.get("name")), pattern),
                builder.like(builder.lower(root.get("city")), pattern),
                builder.like(builder.lower(root.get("state")), pattern)
        );
    }

    static Specification<Hospital> locationContains(String location) {
        if (location == null || location.isBlank()) {
            return null;
        }
        String pattern = "%" + location.trim().toLowerCase(Locale.ROOT) + "%";
        return (root, query, builder) -> builder.or(
                builder.like(builder.lower(root.get("city")), pattern),
                builder.like(builder.lower(root.get("state")), pattern),
                builder.like(builder.lower(root.get("name")), pattern)
        );
    }

    static Specification<Hospital> insuranceContains(String insurance) {
        if (insurance == null || insurance.isBlank()) {
            return null;
        }
        String pattern = "%" + insurance.trim().toLowerCase(Locale.ROOT) + "%";
        return (root, query, builder) -> {
            Join<Hospital, InsuranceProvider> provider = root.join("insuranceProviders", JoinType.INNER);
            query.distinct(true);
            return builder.and(
                    builder.isTrue(provider.get("active")),
                    builder.like(builder.lower(provider.get("name")), pattern)
            );
        };
    }

    static Specification<Hospital> emergency(Boolean required) {
        return Boolean.TRUE.equals(required)
                ? (root, query, builder) -> builder.isTrue(root.get("emergency"))
                : null;
    }

    static Specification<Hospital> open24x7(Boolean required) {
        return Boolean.TRUE.equals(required)
                ? (root, query, builder) -> builder.isTrue(root.get("open24x7"))
                : null;
    }

    static Specification<Hospital> minimumRating(Double rating) {
        return rating == null
                ? null
                : (root, query, builder) -> builder.greaterThanOrEqualTo(root.get("rating"), rating);
    }
}
