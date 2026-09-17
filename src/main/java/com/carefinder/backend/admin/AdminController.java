package com.carefinder.backend.admin;

import com.carefinder.backend.audit.AuditService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Tag(name = "Administration")
public class AdminController {

    private final AnalyticsService analyticsService;
    private final AuditService auditService;

    public AdminController(AnalyticsService analyticsService, AuditService auditService) {
        this.analyticsService = analyticsService;
        this.auditService = auditService;
    }

    @GetMapping("/api/v1/meta/stats")
    AnalyticsService.PublicStats publicStats() {
        return analyticsService.publicStats();
    }

    @GetMapping("/api/v1/admin/analytics/summary")
    AnalyticsService.AdminStats adminStats() {
        return analyticsService.adminStats();
    }

    @GetMapping("/api/v1/admin/audit-logs")
    List<AuditService.AuditResponse> auditLogs() {
        return auditService.recent();
    }
}
