package com.carefinder.backend.auth;

import com.carefinder.backend.user.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public final class AuthDtos {

    private static final String PASSWORD_PATTERN = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,72}$";

    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank @Size(max = 100) String name,
            @NotBlank @Email @Size(max = 190) String email,
            @NotBlank @Pattern(regexp = "^[0-9+() -]{8,20}$", message = "must be a valid mobile number") String mobile,
            @NotBlank @Pattern(regexp = PASSWORD_PATTERN, message = "must contain uppercase, lowercase and a number") String password,
            @Size(max = 100) String city,
            @Size(max = 120) String insuranceProvider
    ) {
    }

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password
    ) {
    }

    public record RefreshRequest(@NotBlank String refreshToken) {
    }

    public record LogoutRequest(@NotBlank String refreshToken) {
    }

    public record ForgotPasswordRequest(@NotBlank @Email String email) {
    }

    public record ResetPasswordRequest(
            @NotBlank String token,
            @NotBlank @Pattern(regexp = PASSWORD_PATTERN, message = "must contain uppercase, lowercase and a number") String newPassword
    ) {
    }

    public record UserView(
            UUID id,
            String name,
            String email,
            String mobile,
            String city,
            String insuranceProvider,
            Role role,
            Instant createdAt
    ) {
    }

    public record AuthResponse(
            String accessToken,
            String refreshToken,
            String tokenType,
            long expiresInSeconds,
            UserView user
    ) {
    }

    public record PasswordResetStartResponse(String message, String developmentResetToken) {
    }
}
