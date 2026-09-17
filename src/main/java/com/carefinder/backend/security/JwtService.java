package com.carefinder.backend.security;

import com.carefinder.backend.config.AppProperties;
import com.carefinder.backend.user.UserAccount;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class JwtService {

    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();
    private static final String HEADER = ENCODER.encodeToString(
            "{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8)
    );

    private final ObjectMapper objectMapper;
    private final AppProperties properties;
    private final Environment environment;
    private byte[] signingKey;

    public JwtService(ObjectMapper objectMapper, AppProperties properties, Environment environment) {
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.environment = environment;
    }

    @PostConstruct
    void validateConfiguration() {
        signingKey = properties.auth().jwtSecret().getBytes(StandardCharsets.UTF_8);
        if (signingKey.length < 32) {
            throw new IllegalStateException("JWT_SECRET must contain at least 32 characters.");
        }
        String lowerSecret = properties.auth().jwtSecret().toLowerCase(Locale.ROOT);
        if (environment.acceptsProfiles(Profiles.of("prod"))
                && (lowerSecret.contains("change-me") || lowerSecret.contains("replace-with"))) {
            throw new IllegalStateException("A unique JWT_SECRET is required in the prod profile.");
        }
    }

    public String issue(UserAccount user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(properties.auth().accessTokenMinutes(), ChronoUnit.MINUTES);
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("sub", user.getId().toString());
        claims.put("iss", "carefinder-api");
        claims.put("aud", "carefinder-angular");
        claims.put("email", user.getEmail());
        claims.put("role", user.getRole().name());
        claims.put("ver", user.getTokenVersion());
        claims.put("iat", now.getEpochSecond());
        claims.put("exp", expiresAt.getEpochSecond());
        claims.put("jti", UUID.randomUUID().toString());

        try {
            String payload = ENCODER.encodeToString(objectMapper.writeValueAsBytes(claims));
            String unsignedToken = HEADER + "." + payload;
            return unsignedToken + "." + ENCODER.encodeToString(sign(unsignedToken));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to create access token.", exception);
        }
    }

    public JwtPrincipal parse(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3 || !HEADER.equals(parts[0])) {
                throw new IllegalArgumentException("Invalid token format.");
            }
            String unsignedToken = parts[0] + "." + parts[1];
            byte[] suppliedSignature = DECODER.decode(parts[2]);
            if (!MessageDigest.isEqual(sign(unsignedToken), suppliedSignature)) {
                throw new IllegalArgumentException("Invalid token signature.");
            }

            Map<String, Object> claims = objectMapper.readValue(
                    DECODER.decode(parts[1]),
                    new TypeReference<>() {
                    }
            );
            long expiresAt = numberClaim(claims, "exp");
            if (!"carefinder-api".equals(stringClaim(claims, "iss"))
                    || !"carefinder-angular".equals(stringClaim(claims, "aud"))) {
                throw new IllegalArgumentException("Invalid token issuer or audience.");
            }
            if (Instant.now().getEpochSecond() >= expiresAt) {
                throw new IllegalArgumentException("Access token has expired.");
            }
            return new JwtPrincipal(
                    UUID.fromString(stringClaim(claims, "sub")),
                    stringClaim(claims, "email"),
                    com.carefinder.backend.user.Role.valueOf(stringClaim(claims, "role")),
                    numberClaim(claims, "ver")
            );
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalArgumentException("Invalid access token.", exception);
        }
    }

    public long accessTokenSeconds() {
        return properties.auth().accessTokenMinutes() * 60;
    }

    private byte[] sign(String value) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(signingKey, "HmacSHA256"));
        return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
    }

    private String stringClaim(Map<String, Object> claims, String name) {
        Object value = claims.get(name);
        if (!(value instanceof String text) || text.isBlank()) {
            throw new IllegalArgumentException("Missing token claim: " + name);
        }
        return text;
    }

    private long numberClaim(Map<String, Object> claims, String name) {
        Object value = claims.get(name);
        if (!(value instanceof Number number)) {
            throw new IllegalArgumentException("Missing token claim: " + name);
        }
        return number.longValue();
    }
}
