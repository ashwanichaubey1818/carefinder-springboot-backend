package com.carefinder.backend.personal;

import com.carefinder.backend.hospital.Hospital;
import com.carefinder.backend.hospital.HospitalMapper;
import com.carefinder.backend.hospital.HospitalService;
import com.carefinder.backend.security.CurrentUserService;
import com.carefinder.backend.user.UserAccount;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FavoriteService {

    private final FavoriteRepository repository;
    private final HospitalService hospitalService;
    private final HospitalMapper mapper;
    private final CurrentUserService currentUserService;

    public FavoriteService(
            FavoriteRepository repository,
            HospitalService hospitalService,
            HospitalMapper mapper,
            CurrentUserService currentUserService
    ) {
        this.repository = repository;
        this.hospitalService = hospitalService;
        this.mapper = mapper;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public List<PersonalDtos.FavoriteResponse> list() {
        UserAccount user = currentUserService.requireCurrentUser();
        return repository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .filter(favorite -> favorite.getHospital().isActive())
                .map(favorite -> new PersonalDtos.FavoriteResponse(
                        mapper.toResponse(favorite.getHospital(), null),
                        favorite.getCreatedAt()
                ))
                .toList();
    }

    @Transactional
    public PersonalDtos.FavoriteResponse add(Long hospitalId) {
        UserAccount user = currentUserService.requireCurrentUser();
        Hospital hospital = hospitalService.requireHospital(hospitalId);
        Favorite favorite = repository.findByUserIdAndHospitalId(user.getId(), hospitalId)
                .orElseGet(() -> repository.save(new Favorite(user, hospital)));
        return new PersonalDtos.FavoriteResponse(mapper.toResponse(hospital, null), favorite.getCreatedAt());
    }

    @Transactional
    public void remove(Long hospitalId) {
        UserAccount user = currentUserService.requireCurrentUser();
        repository.findByUserIdAndHospitalId(user.getId(), hospitalId).ifPresent(repository::delete);
    }

    @Transactional
    public void clear() {
        repository.deleteByUserId(currentUserService.requireCurrentUser().getId());
    }
}
