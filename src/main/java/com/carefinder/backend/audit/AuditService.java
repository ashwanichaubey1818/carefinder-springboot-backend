package com.carefinder.backend.audit;

import com.carefinder.backend.security.CurrentUserService;
import com.carefinder.backend.security.JwtPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class AuditService {

    private final AuditLogRepository repository;
    private final CurrentUserService currentUserService;

    public AuditService(AuditLogRepository repository, CurrentUserService currentUserService) {
        this.repository = repository;
        this.currentUserService = currentUserService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String action, String resource, Object resourceId, String details) {
        JwtPrincipal actor = currentUserService.principal().orElse(null);
        repository.save(new AuditLog(
                actor == null ? null : actor.userId(),
                actor == null ? null : actor.email(),
                limit(action, 80),
                limit(resource, 80),
                resourceId == null ? null : limit(resourceId.toString(), 80),
                limit(details, 1000)
        ));
    }

    @Transactional(readOnly = true)
    public List<AuditResponse> recent() {
        return repository.findTop100ByOrderByCreatedAtDesc().stream()
                .map(log -> new AuditResponse(
                        log.getId(),
                        log.getActorUserId(),
                        log.getActorEmail(),
                        log.getAction(),
                        log.getResource(),
                        log.getResourceId(),
                        log.getDetails(),
                        log.getCreatedAt()
                ))
                .toList();
    }

    private String limit(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }

    public record AuditResponse(
            Long id,
            java.util.UUID actorUserId,
            String actorEmail,
            String action,
            String resource,
            String resourceId,
            String details,
            Instant createdAt
    ) {
    }
}
