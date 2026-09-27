package com.carefinder.backend.chat;

import com.carefinder.backend.hospital.Hospital;
import com.carefinder.backend.hospital.HospitalRepository;
import com.carefinder.backend.insurance.InsuranceProvider;
import com.carefinder.backend.insurance.InsuranceProviderRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

@Service
public class HospitalChatQueryService {

    private final HospitalRepository hospitalRepository;
    private final InsuranceProviderRepository insuranceRepository;

    public HospitalChatQueryService(
            HospitalRepository hospitalRepository,
            InsuranceProviderRepository insuranceRepository) {
        this.hospitalRepository = hospitalRepository;
        this.insuranceRepository = insuranceRepository;
    }

    @Transactional(readOnly = true)
    public SearchResult search(ChatQueryPlan plan) {
        Specification<Hospital> specification = buildSpecification(plan);

        long total = hospitalRepository.count(specification);

        List<Hospital> hospitals = hospitalRepository
                .findAll(
                        specification,
                        PageRequest.of(
                                0,
                                plan.limit(),
                                sortFor(plan.sortBy())))
                .getContent();

        return new SearchResult(total, hospitals);
    }

    @Transactional(readOnly = true)
    public String databaseVocabulary() {
        List<Hospital> hospitals = activeHospitals();

        Set<String> names = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

        Set<String> cities = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

        Set<String> states = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

        Set<String> specialties = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

        Set<String> types = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

        Set<String> accreditations = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

        for (Hospital hospital : hospitals) {
            addIfPresent(names, hospital.getName());
            addIfPresent(cities, hospital.getCity());
            addIfPresent(states, hospital.getState());
            addIfPresent(types, hospital.getType());
            addIfPresent(
                    accreditations,
                    hospital.getAccreditation());

            hospital.getSpecialties()
                    .forEach(value -> addIfPresent(specialties, value));
        }

        List<String> insurers = insuranceRepository
                .findAllByActiveTrueOrderByNameAsc()
                .stream()
                .map(InsuranceProvider::getName)
                .toList();

        return """
                Hospital names: %s
                Cities: %s
                States: %s
                Specialties: %s
                Insurance providers: %s
                Hospital types: %s
                Accreditations: %s
                """.formatted(
                String.join(", ", names),
                String.join(", ", cities),
                String.join(", ", states),
                String.join(", ", specialties),
                String.join(", ", insurers),
                String.join(", ", types),
                String.join(", ", accreditations));
    }

    @Transactional(readOnly = true)
    public List<String> cities() {
        Set<String> values = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

        activeHospitals().forEach(
                hospital -> addIfPresent(
                        values,
                        hospital.getCity()));

        return new ArrayList<>(values);
    }

    @Transactional(readOnly = true)
    public List<String> specialties() {
        Set<String> values = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

        activeHospitals().forEach(
                hospital -> hospital
                        .getSpecialties()
                        .forEach(
                                specialty -> addIfPresent(
                                        values,
                                        specialty)));

        return new ArrayList<>(values);
    }

    @Transactional(readOnly = true)
    public List<String> insurers() {
        return insuranceRepository
                .findAllByActiveTrueOrderByNameAsc()
                .stream()
                .map(InsuranceProvider::getName)
                .toList();
    }

    private Specification<Hospital> buildSpecification(
            ChatQueryPlan plan) {
        Specification<Hospital> specification = activeSpecification();

        if (plan.intent() == ChatQueryPlan.Intent.COMPARE
                && hasText(plan.hospitalName())
                && hasText(plan.secondHospitalName())) {

            specification = specification.and(
                    (root, query, builder) -> builder.or(
                            builder.like(
                                    builder.lower(
                                            root.get("name")),
                                    likePattern(
                                            plan.hospitalName()),
                                    '\\'),
                            builder.like(
                                    builder.lower(
                                            root.get("name")),
                                    likePattern(
                                            plan.secondHospitalName()),
                                    '\\')));

        } else if (hasText(plan.hospitalName())) {
            specification = specification.and(
                    textContains("name", plan.hospitalName()));
        }

        if (hasText(plan.city())) {
            specification = specification.and(
                    textContains("city", plan.city()));
        }

        if (hasText(plan.state())) {
            specification = specification.and(
                    textContains("state", plan.state()));
        }

        if (hasText(plan.accreditation())) {
            specification = specification.and(
                    textContains(
                            "accreditation",
                            plan.accreditation()));
        }

        if (hasText(plan.hospitalType())) {
            specification = specification.and(
                    textContains(
                            "type",
                            plan.hospitalType()));
        }

        if (hasText(plan.specialty())) {
            specification = specification.and(
                    specialtyContains(plan.specialty()));
        }

        if (hasText(plan.insuranceProvider())) {
            specification = specification.and(
                    insuranceContains(
                            plan.insuranceProvider()));
        }

        if (plan.emergencyOnly()) {
            specification = specification.and(
                    (root, query, builder) -> builder.isTrue(
                            root.get("emergency")));
        }

        if (plan.open24x7Only()) {
            specification = specification.and(
                    (root, query, builder) -> builder.isTrue(
                            root.get("open24x7")));
        }

        if (plan.verifiedOnly()) {
            specification = specification.and(
                    (root, query, builder) -> builder.isTrue(
                            root.get("verified")));
        }

        if (plan.minimumRating() != null
                && plan.minimumRating() > 0) {

            specification = specification.and(
                    (root, query, builder) -> builder.greaterThanOrEqualTo(
                            root.<Double>get("rating"),
                            plan.minimumRating()));
        }

        if (plan.minimumBeds() != null
                && plan.minimumBeds() > 0) {

            specification = specification.and(
                    (root, query, builder) -> builder.greaterThanOrEqualTo(
                            root.<Integer>get("beds"),
                            plan.minimumBeds()));
        }

        return specification;
    }

    private Specification<Hospital> activeSpecification() {
        return (root, query, builder) -> builder.isTrue(root.get("active"));
    }

    private Specification<Hospital> textContains(
            String field,
            String value) {
        return (root, query, builder) -> builder.like(
                builder.lower(
                        root.get(field)),
                likePattern(value),
                '\\');
    }

    private Specification<Hospital> specialtyContains(
            String specialty) {
        return (root, query, builder) -> {
            query.distinct(true);

            Join<Hospital, String> join = root.join(
                    "specialties",
                    JoinType.LEFT);

            return builder.like(
                    builder.lower(join),
                    likePattern(specialty),
                    '\\');
        };
    }

    private Specification<Hospital> insuranceContains(
            String insuranceProvider) {
        return (root, query, builder) -> {
            query.distinct(true);

            Join<Hospital, InsuranceProvider> join = root.join(
                    "insuranceProviders",
                    JoinType.LEFT);

            return builder.like(
                    builder.lower(
                            join.get("name")),
                    likePattern(insuranceProvider),
                    '\\');
        };
    }

    private Sort sortFor(ChatQueryPlan.SortBy sortBy) {
        return switch (sortBy) {
            case NAME -> Sort.by(
                    Sort.Order.asc("name"));

            case BEDS -> Sort.by(
                    Sort.Order.desc("beds"),
                    Sort.Order.desc("rating"));

            case REVIEWS -> Sort.by(
                    Sort.Order.desc("reviews"),
                    Sort.Order.desc("rating"));

            case RATING, RELEVANCE -> Sort.by(
                    Sort.Order.desc("rating"),
                    Sort.Order.desc("reviews"));
        };
    }

    private List<Hospital> activeHospitals() {
        return hospitalRepository
                .findAll()
                .stream()
                .filter(Hospital::isActive)
                .toList();
    }

    private String likePattern(String value) {
        String escaped = value
                .trim()
                .toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");

        return "%" + escaped + "%";
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private void addIfPresent(
            Set<String> values,
            String value) {
        if (hasText(value)) {
            values.add(value.trim());
        }
    }

    public record SearchResult(
            long total,
            List<Hospital> hospitals) {
    }
}