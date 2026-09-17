package com.carefinder.backend.hospital;

import com.carefinder.backend.audit.AuditService;
import com.carefinder.backend.security.CurrentUserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/staff/hospitals")
@Tag(name = "Hospital Staff")
public class StaffHospitalController {

    private final HospitalService hospitalService;
    private final CurrentUserService currentUserService;
    private final AuditService auditService;

    public StaffHospitalController(
            HospitalService hospitalService,
            CurrentUserService currentUserService,
            AuditService auditService
    ) {
        this.hospitalService = hospitalService;
        this.currentUserService = currentUserService;
        this.auditService = auditService;
    }

    @PutMapping("/{id}")
    HospitalDtos.HospitalResponse updateAssignedHospital(
            @PathVariable Long id,
            @Valid @RequestBody HospitalDtos.HospitalRequest request
    ) {
        HospitalDtos.HospitalResponse hospital = hospitalService.update(
                id,
                request,
                currentUserService.requireCurrentUser()
        );
        auditService.record("UPDATE", "HOSPITAL", id, "Hospital staff updated assigned profile");
        return hospital;
    }
}
