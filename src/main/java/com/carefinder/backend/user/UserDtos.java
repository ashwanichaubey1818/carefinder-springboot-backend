package com.carefinder.backend.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public final class UserDtos {

    private static final String PASSWORD_PATTERN = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,72}$";

    private UserDtos() {
    }

    public record ProfileResponse(
            UUID id,
            String name,
            String email,
            String mobile,
            String city,
            String insuranceProvider,
            Role role,
            Instant createdAt,
            long favoriteCount,
            long recentlyViewedCount
    ) {
    }

    public record UpdateProfileRequest(
            @NotBlank @Size(max = 100) String name,
            @NotBlank @Pattern(regexp = "^[0-9+() -]{8,20}$", message = "must be a valid mobile number") String mobile,
            @Size(max = 100) String city,
            @Size(max = 120) String insuranceProvider
    ) {
    }

    public record ChangePasswordRequest(
            @NotBlank String currentPassword,
            @NotBlank @Pattern(regexp = PASSWORD_PATTERN, message = "must contain uppercase, lowercase and a number") String newPassword
    ) {
    }

    public record AdminUserResponse(
            UUID id,
            String name,
            String email,
            String mobile,
            String city,
            String insuranceProvider,
            Role role,
            boolean enabled,
            Instant createdAt
    ) {
    }

    public record AdminUserUpdateRequest(Role role, Boolean enabled) {
    }
}
