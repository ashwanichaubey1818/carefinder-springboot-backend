package com.carefinder.backend.hospital;

import com.carefinder.backend.common.ApiMessage;
import com.carefinder.backend.audit.AuditService;
import com.carefinder.backend.security.CurrentUserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/hospitals")
@Tag(name = "Admin Hospitals")
public class AdminHospitalController {

    private final HospitalService hospitalService;
    private final CurrentUserService currentUserService;
    private final AuditService auditService;

    public AdminHospitalController(
            HospitalService hospitalService,
            CurrentUserService currentUserService,
            AuditService auditService
    ) {
        this.hospitalService = hospitalService;
        this.currentUserService = currentUserService;
        this.auditService = auditService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    HospitalDtos.HospitalResponse create(@Valid @RequestBody HospitalDtos.HospitalRequest request) {
        HospitalDtos.HospitalResponse hospital = hospitalService.create(request);
        auditService.record("CREATE", "HOSPITAL", hospital.id(), hospital.name());
        return hospital;
    }

    @PutMapping("/{id}")
    HospitalDtos.HospitalResponse update(
            @PathVariable Long id,
            @Valid @RequestBody HospitalDtos.HospitalRequest request
    ) {
        HospitalDtos.HospitalResponse hospital = hospitalService.update(
                id,
                request,
                currentUserService.requireCurrentUser()
        );
        auditService.record("UPDATE", "HOSPITAL", id, hospital.name());
        return hospital;
    }

    @DeleteMapping("/{id}")
    ApiMessage deactivate(@PathVariable Long id) {
        hospitalService.deactivate(id);
        auditService.record("DEACTIVATE", "HOSPITAL", id, "Hospital profile deactivated");
        return new ApiMessage("Hospital profile deactivated.");
    }
}
