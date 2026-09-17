package com.carefinder.backend.user;

import com.carefinder.backend.audit.AuditService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/users")
@Tag(name = "Admin Users")
public class AdminUserController {

    private final UserService userService;
    private final AuditService auditService;

    public AdminUserController(UserService userService, AuditService auditService) {
        this.userService = userService;
        this.auditService = auditService;
    }

    @GetMapping
    List<UserDtos.AdminUserResponse> list() {
        return userService.listUsers();
    }

    @PatchMapping("/{id}")
    UserDtos.AdminUserResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody UserDtos.AdminUserUpdateRequest request
    ) {
        UserDtos.AdminUserResponse user = userService.updateUser(id, request);
        auditService.record("UPDATE", "USER", id, "Role/status updated for " + user.email());
        return user;
    }
}
