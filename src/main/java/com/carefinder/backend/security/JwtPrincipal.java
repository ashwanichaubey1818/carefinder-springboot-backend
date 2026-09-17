package com.carefinder.backend.security;

import com.carefinder.backend.user.Role;

import java.util.UUID;

public record JwtPrincipal(UUID userId, String email, Role role, long tokenVersion) {
}
