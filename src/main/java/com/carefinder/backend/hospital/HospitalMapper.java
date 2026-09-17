package com.carefinder.backend.hospital;

import com.carefinder.backend.insurance.InsuranceProvider;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class HospitalMapper {

    public HospitalDtos.HospitalResponse toResponse(Hospital hospital, Double distanceKm) {
        return new HospitalDtos.HospitalResponse(
                hospital.getId(),
                hospital.getName(),
                hospital.getCity(),
                hospital.getState(),
                hospital.getLatitude(),
                hospital.getLongitude(),
                hospital.getRating(),
                hospital.getReviews(),
                hospital.isEmergency(),
                hospital.isOpen24x7(),
                hospital.getInsuranceProviders().stream()
                        .filter(InsuranceProvider::isActive)
                        .map(InsuranceProvider::getName)
                        .sorted()
                        .toList(),
                List.copyOf(hospital.getSpecialties()),
                hospital.getType(),
                hospital.getBeds(),
                hospital.getAccreditation(),
                hospital.getAddress(),
                hospital.getPhone(),
                hospital.getWebsite(),
                hospital.getDescription(),
                hospital.isVerified(),
                distanceKm
        );
    }
}
