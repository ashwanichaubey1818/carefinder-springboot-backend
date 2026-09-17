package com.carefinder.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        List<String> corsOrigins,
        boolean seedData,
        boolean bootstrapAdmin,
        String adminEmail,
        String adminPassword,
        String staffEmail,
        String staffPassword,
        Auth auth
) {
    public record Auth(
            String jwtSecret,
            long accessTokenMinutes,
            long refreshTokenDays,
            long resetTokenMinutes,
            boolean exposeDevResetToken
    ) {
    }
}
