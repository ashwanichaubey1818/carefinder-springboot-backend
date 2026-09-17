package com.carefinder.backend.hospital;

import com.carefinder.backend.common.BadRequestException;
import com.carefinder.backend.common.ConflictException;
import com.carefinder.backend.common.NotFoundException;
import com.carefinder.backend.common.PageResponse;
import com.carefinder.backend.insurance.InsuranceProvider;
import com.carefinder.backend.insurance.InsuranceProviderRepository;
import com.carefinder.backend.user.Role;
import com.carefinder.backend.user.UserAccount;
import com.carefinder.backend.user.UserRepository;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class HospitalService {

    private static final int MAX_PAGE_SIZE = 50;

    private final HospitalRepository hospitalRepository;
    private final InsuranceProviderRepository insuranceRepository;
    private final UserRepository userRepository;
    private final HospitalMapper mapper;

    public HospitalService(
            HospitalRepository hospitalRepository,
            InsuranceProviderRepository insuranceRepository,
            UserRepository userRepository,
            HospitalMapper mapper
    ) {
        this.hospitalRepository = hospitalRepository;
        this.insuranceRepository = insuranceRepository;
        this.userRepository = userRepository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<HospitalDtos.HospitalResponse> search(
            String query,
            String location,
            String insurance,
            Boolean emergency,
            Boolean open24x7,
            Double minimumRating,
            Double latitude,
            Double longitude,
            Double radiusKm,
            String sort,
            int page,
            int size
    ) {
        validatePagination(page, size);
        validateCoordinates(latitude, longitude, radiusKm);

        Specification<Hospital> specification = HospitalSpecifications.activeOnly()
                .and(HospitalSpecifications.textContains(query))
                .and(HospitalSpecifications.locationContains(location))
                .and(HospitalSpecifications.insuranceContains(insurance))
                .and(HospitalSpecifications.emergency(emergency))
                .and(HospitalSpecifications.open24x7(open24x7))
                .and(HospitalSpecifications.minimumRating(minimumRating));

        List<HospitalDistance> matches = hospitalRepository.findAll(specification).stream()
                .map(hospital -> new HospitalDistance(
                        hospital,
                        latitude == null ? null : distanceKm(
                                latitude,
                                longitude,
                                hospital.getLatitude(),
                                hospital.getLongitude()
                        )
                ))
                .filter(item -> radiusKm == null || item.distanceKm() <= radiusKm)
                .sorted(comparator(sort, latitude != null))
                .toList();

        List<HospitalDtos.HospitalResponse> responses = matches.stream()
                .map(item -> mapper.toResponse(item.hospital(), roundDistance(item.distanceKm())))
                .toList();
        return PageResponse.of(responses, page, size);
    }

    @Transactional(readOnly = true)
    public HospitalDtos.HospitalResponse get(Long id) {
        return mapper.toResponse(requireHospital(id), null);
    }

    @Transactional(readOnly = true)
    public HospitalDtos.CompareResponse compare(List<Long> ids) {
        List<Hospital> hospitals = hospitalsInRequestedOrder(ids);
        List<HospitalDtos.HospitalResponse> responses = hospitals.stream()
                .map(hospital -> mapper.toResponse(hospital, null))
                .toList();
        Long highestRated = hospitals.stream()
                .max(Comparator.comparingDouble(Hospital::getRating))
                .map(Hospital::getId)
                .orElse(null);
        Long largest = hospitals.stream()
                .max(Comparator.comparingInt(Hospital::getBeds))
                .map(Hospital::getId)
                .orElse(null);
        return new HospitalDtos.CompareResponse(
                responses,
                new HospitalDtos.ComparisonHighlights(
                        highestRated,
                        largest,
                        hospitals.stream().filter(Hospital::isEmergency).map(Hospital::getId).toList(),
                        hospitals.stream().filter(Hospital::isOpen24x7).map(Hospital::getId).toList()
                )
        );
    }

    @Transactional(readOnly = true)
    public List<Hospital> hospitalsInRequestedOrder(List<Long> ids) {
        if (ids == null || ids.size() < 2 || ids.size() > 3) {
            throw new BadRequestException("Select 2 or 3 hospitals for comparison.");
        }
        List<Long> distinctIds = ids.stream().distinct().toList();
        if (distinctIds.size() != ids.size()) {
            throw new BadRequestException("Each compared hospital must be different.");
        }
        Map<Long, Hospital> found = hospitalRepository.findAllByIdIn(distinctIds).stream()
                .filter(Hospital::isActive)
                .collect(Collectors.toMap(Hospital::getId, Function.identity()));
        if (found.size() != distinctIds.size()) {
            throw new NotFoundException("One or more selected hospitals were not found.");
        }
        return distinctIds.stream().map(found::get).toList();
    }

    @Transactional
    public HospitalDtos.HospitalResponse create(HospitalDtos.HospitalRequest request) {
        if (hospitalRepository.existsByNameIgnoreCase(request.name().trim())) {
            throw new ConflictException("A hospital with this name already exists.");
        }
        Hospital hospital = new Hospital(
                request.name().trim(),
                uniqueSlug(request.name()),
                request.city().trim(),
                request.state().trim()
        );
        apply(hospital, request, true);
        return mapper.toResponse(hospitalRepository.save(hospital), null);
    }

    @Transactional
    public HospitalDtos.HospitalResponse update(
            Long id,
            HospitalDtos.HospitalRequest request,
            UserAccount actor
    ) {
        Hospital hospital = requireHospital(id);
        assertCanManage(hospital, actor);
        hospitalRepository.findByNameIgnoreCase(request.name().trim())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new ConflictException("A hospital with this name already exists.");
                });
        boolean administrator = actor.getRole() == Role.ADMIN;
        apply(hospital, request, administrator);
        return mapper.toResponse(hospital, null);
    }

    @Transactional
    public void deactivate(Long id) {
        requireHospital(id).setActive(false);
    }

    @Transactional(readOnly = true)
    public Hospital requireHospital(Long id) {
        return hospitalRepository.findById(id)
                .filter(Hospital::isActive)
                .orElseThrow(() -> new NotFoundException("Hospital profile not found."));
    }

    private void apply(Hospital hospital, HospitalDtos.HospitalRequest request, boolean allowAdministration) {
        hospital.setName(request.name().trim());
        hospital.setCity(request.city().trim());
        hospital.setState(request.state().trim());
        hospital.setLatitude(request.latitude());
        hospital.setLongitude(request.longitude());
        hospital.setRating(request.rating());
        hospital.setReviews(request.reviews());
        hospital.setEmergency(request.emergency());
        hospital.setOpen24x7(request.open24x7());
        hospital.setType(request.type().trim());
        hospital.setBeds(request.beds());
        hospital.setAccreditation(request.accreditation().trim());
        hospital.setAddress(clean(request.address()));
        hospital.setPhone(clean(request.phone()));
        hospital.setWebsite(clean(request.website()));
        hospital.setDescription(clean(request.description()));
        hospital.getSpecialties().clear();
        hospital.getSpecialties().addAll(request.specialties().stream()
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList());
        hospital.getInsuranceProviders().clear();
        hospital.getInsuranceProviders().addAll(resolveInsuranceProviders(request.insuranceProviderIds()));

        if (allowAdministration) {
            if (request.active() != null) {
                hospital.setActive(request.active());
            }
            if (request.verified() != null) {
                hospital.setVerified(request.verified());
            }
            hospital.setManager(resolveManager(request.managerUserId()));
        }
    }

    private Set<InsuranceProvider> resolveInsuranceProviders(Set<Long> ids) {
        if (ids.isEmpty()) {
            return Set.of();
        }
        List<InsuranceProvider> providers = insuranceRepository.findAllByIdInAndActiveTrue(ids);
        if (providers.size() != ids.size()) {
            throw new BadRequestException("One or more insurance provider IDs are invalid or inactive.");
        }
        return new LinkedHashSet<>(providers);
    }

    private UserAccount resolveManager(UUID managerId) {
        if (managerId == null) {
            return null;
        }
        UserAccount manager = userRepository.findById(managerId)
                .orElseThrow(() -> new BadRequestException("Hospital manager user was not found."));
        if (manager.getRole() != Role.HOSPITAL_STAFF || !manager.isEnabled()) {
            throw new BadRequestException("Hospital manager must be an active HOSPITAL_STAFF user.");
        }
        return manager;
    }

    private void assertCanManage(Hospital hospital, UserAccount actor) {
        if (actor.getRole() == Role.ADMIN) {
            return;
        }
        if (actor.getRole() != Role.HOSPITAL_STAFF
                || hospital.getManager() == null
                || !hospital.getManager().getId().equals(actor.getId())) {
            throw new AccessDeniedException("Hospital staff can update only their assigned hospital.");
        }
    }

    private Comparator<HospitalDistance> comparator(String requestedSort, boolean hasCoordinates) {
        String sort = requestedSort == null ? "recommended" : requestedSort.toLowerCase(Locale.ROOT);
        return switch (sort) {
            case "distance" -> hasCoordinates
                    ? Comparator.comparingDouble(HospitalDistance::distanceKm)
                    : recommendedComparator();
            case "rating" -> Comparator
                    .comparingDouble((HospitalDistance item) -> item.hospital().getRating()).reversed()
                    .thenComparing(item -> item.hospital().getReviews(), Comparator.reverseOrder());
            case "name" -> Comparator.comparing(
                    item -> item.hospital().getName(),
                    String.CASE_INSENSITIVE_ORDER
            );
            default -> recommendedComparator();
        };
    }

    private Comparator<HospitalDistance> recommendedComparator() {
        return Comparator.comparingDouble((HospitalDistance item) ->
                item.hospital().getRating() * 100 + Math.min(item.hospital().getReviews(), 2000) / 100.0
        ).reversed();
    }

    private double distanceKm(double lat1, double lon1, double lat2, double lon2) {
        double earthRadius = 6371;
        double latitudeDifference = Math.toRadians(lat2 - lat1);
        double longitudeDifference = Math.toRadians(lon2 - lon1);
        double haversine = Math.pow(Math.sin(latitudeDifference / 2), 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.pow(Math.sin(longitudeDifference / 2), 2);
        return earthRadius * 2 * Math.atan2(Math.sqrt(haversine), Math.sqrt(1 - haversine));
    }

    private Double roundDistance(Double distance) {
        return distance == null ? null : Math.round(distance * 10.0) / 10.0;
    }

    private void validatePagination(int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new BadRequestException("page must be 0 or greater and size must be between 1 and 50.");
        }
    }

    private void validateCoordinates(Double latitude, Double longitude, Double radiusKm) {
        if ((latitude == null) != (longitude == null)) {
            throw new BadRequestException("latitude and longitude must be supplied together.");
        }
        if (latitude != null && (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180)) {
            throw new BadRequestException("Coordinates are outside the valid range.");
        }
        if (radiusKm != null && (latitude == null || radiusKm <= 0 || radiusKm > 500)) {
            throw new BadRequestException("radiusKm requires coordinates and must be between 0 and 500.");
        }
    }

    private String uniqueSlug(String name) {
        String base = Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        String candidate = base;
        int suffix = 2;
        while (hospitalRepository.existsBySlug(candidate)) {
            candidate = base + "-" + suffix++;
        }
        return candidate;
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private record HospitalDistance(Hospital hospital, Double distanceKm) {
    }
}
