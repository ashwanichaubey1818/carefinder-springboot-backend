package com.carefinder.backend.insurance;

import com.carefinder.backend.common.ApiMessage;
import com.carefinder.backend.audit.AuditService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Tag(name = "Insurance Network")
public class InsuranceController {

    private final InsuranceService service;
    private final AuditService auditService;

    public InsuranceController(InsuranceService service, AuditService auditService) {
        this.service = service;
        this.auditService = auditService;
    }

    @GetMapping("/api/v1/insurers")
    List<InsuranceDtos.InsuranceResponse> listPublic() {
        return service.listPublic();
    }

    @GetMapping("/api/v1/admin/insurers")
    List<InsuranceDtos.InsuranceResponse> listAdmin() {
        return service.listAdmin();
    }

    @PostMapping("/api/v1/admin/insurers")
    @ResponseStatus(HttpStatus.CREATED)
    InsuranceDtos.InsuranceResponse create(@Valid @RequestBody InsuranceDtos.InsuranceRequest request) {
        InsuranceDtos.InsuranceResponse provider = service.create(request);
        auditService.record("CREATE", "INSURER", provider.id(), provider.name());
        return provider;
    }

    @PutMapping("/api/v1/admin/insurers/{id}")
    InsuranceDtos.InsuranceResponse update(
            @PathVariable Long id,
            @Valid @RequestBody InsuranceDtos.InsuranceRequest request
    ) {
        InsuranceDtos.InsuranceResponse provider = service.update(id, request);
        auditService.record("UPDATE", "INSURER", id, provider.name());
        return provider;
    }

    @DeleteMapping("/api/v1/admin/insurers/{id}")
    ApiMessage deactivate(@PathVariable Long id) {
        service.deactivate(id);
        auditService.record("DEACTIVATE", "INSURER", id, "Insurance provider deactivated");
        return new ApiMessage("Insurance provider deactivated.");
    }
}
