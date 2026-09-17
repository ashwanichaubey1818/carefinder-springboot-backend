package com.carefinder.backend.hospital;

import com.carefinder.backend.common.PageResponse;
import com.carefinder.backend.personal.RecentlyViewedService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1/hospitals")
@Tag(name = "Hospitals")
public class HospitalController {

    private final HospitalService hospitalService;
    private final ComparisonReportService comparisonReportService;
    private final RecentlyViewedService recentlyViewedService;

    public HospitalController(
            HospitalService hospitalService,
            ComparisonReportService comparisonReportService,
            RecentlyViewedService recentlyViewedService
    ) {
        this.hospitalService = hospitalService;
        this.comparisonReportService = comparisonReportService;
        this.recentlyViewedService = recentlyViewedService;
    }

    @GetMapping
    @Operation(summary = "Search hospitals with location, insurer, service and GPS filters")
    PageResponse<HospitalDtos.HospitalResponse> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String insurance,
            @RequestParam(required = false) Boolean emergency,
            @RequestParam(required = false) Boolean open24x7,
            @RequestParam(required = false) Double minimumRating,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            @RequestParam(required = false) Double radiusKm,
            @RequestParam(defaultValue = "recommended") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        return hospitalService.search(
                query,
                location,
                insurance,
                emergency,
                open24x7,
                minimumRating,
                latitude,
                longitude,
                radiusKm,
                sort,
                page,
                size
        );
    }

    @GetMapping("/{id}")
    HospitalDtos.HospitalResponse get(@PathVariable Long id) {
        HospitalDtos.HospitalResponse response = hospitalService.get(id);
        recentlyViewedService.recordIfAuthenticated(id);
        return response;
    }

    @GetMapping("/compare")
    HospitalDtos.CompareResponse compare(@RequestParam List<Long> ids) {
        return hospitalService.compare(ids);
    }

    @GetMapping(value = "/compare/report.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    ResponseEntity<byte[]> comparisonReport(@RequestParam List<Long> ids) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=carefinder-comparison.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(comparisonReportService.create(ids));
    }
}
