package com.carefinder.backend.personal;

import com.carefinder.backend.hospital.Hospital;
import com.carefinder.backend.hospital.HospitalMapper;
import com.carefinder.backend.hospital.HospitalRepository;
import com.carefinder.backend.hospital.HospitalService;
import com.carefinder.backend.security.CurrentUserService;
import com.carefinder.backend.user.UserAccount;
import com.carefinder.backend.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RecentlyViewedService {

    private final RecentlyViewedRepository repository;
    private final HospitalRepository hospitalRepository;
    private final UserRepository userRepository;
    private final HospitalService hospitalService;
    private final HospitalMapper mapper;
    private final CurrentUserService currentUserService;

    public RecentlyViewedService(
            RecentlyViewedRepository repository,
            HospitalRepository hospitalRepository,
            UserRepository userRepository,
            HospitalService hospitalService,
            HospitalMapper mapper,
            CurrentUserService currentUserService
    ) {
        this.repository = repository;
        this.hospitalRepository = hospitalRepository;
        this.userRepository = userRepository;
        this.hospitalService = hospitalService;
        this.mapper = mapper;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public void recordIfAuthenticated(Long hospitalId) {
        currentUserService.principal().ifPresent(principal -> {
            UserAccount user = userRepository.getReferenceById(principal.userId());
            Hospital hospital = hospitalRepository.getReferenceById(hospitalId);
            touch(user, hospital);
        });
    }

    @Transactional
    public PersonalDtos.RecentResponse record(Long hospitalId) {
        UserAccount user = currentUserService.requireCurrentUser();
        Hospital hospital = hospitalService.requireHospital(hospitalId);
        RecentlyViewed recent = touch(user, hospital);
        return toResponse(recent);
    }

    @Transactional(readOnly = true)
    public List<PersonalDtos.RecentResponse> list() {
        UserAccount user = currentUserService.requireCurrentUser();
        return repository.findTop20ByUserIdOrderByLastViewedAtDesc(user.getId()).stream()
                .filter(recent -> recent.getHospital().isActive())
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void remove(Long hospitalId) {
        repository.deleteByUserIdAndHospitalId(currentUserService.requireCurrentUser().getId(), hospitalId);
    }

    @Transactional
    public void clear() {
        repository.deleteByUserId(currentUserService.requireCurrentUser().getId());
    }

    private RecentlyViewed touch(UserAccount user, Hospital hospital) {
        RecentlyViewed recent = repository.findByUserIdAndHospitalId(user.getId(), hospital.getId())
                .map(existing -> {
                    existing.touch();
                    return existing;
                })
                .orElseGet(() -> new RecentlyViewed(user, hospital));
        return repository.save(recent);
    }

    private PersonalDtos.RecentResponse toResponse(RecentlyViewed recent) {
        return new PersonalDtos.RecentResponse(
                mapper.toResponse(recent.getHospital(), null),
                recent.getLastViewedAt(),
                recent.getViewCount()
        );
    }
}
