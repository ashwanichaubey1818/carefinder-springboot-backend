package com.carefinder.backend.auth;

import com.carefinder.backend.common.ApiMessage;
import com.carefinder.backend.common.BadRequestException;
import com.carefinder.backend.common.ConflictException;
import com.carefinder.backend.common.UnauthorizedException;
import com.carefinder.backend.config.AppProperties;
import com.carefinder.backend.security.JwtService;
import com.carefinder.backend.user.Role;
import com.carefinder.backend.user.UserAccount;
import com.carefinder.backend.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

@Service
public class AuthService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String RESET_MESSAGE =
            "If that email exists, a password reset instruction has been created.";

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AppProperties properties;

    public AuthService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordResetTokenRepository resetTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AppProperties properties
    ) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.resetTokenRepository = resetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.properties = properties;
    }

    @Transactional
    public AuthDtos.AuthResponse register(AuthDtos.RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("An account with this email already exists.");
        }
        UserAccount user = new UserAccount(
                request.name().trim(),
                email,
                request.mobile().trim(),
                passwordEncoder.encode(request.password()),
                Role.USER
        );
        user.setCity(clean(request.city()));
        user.setInsuranceProvider(clean(request.insuranceProvider()));
        return createSession(userRepository.save(user));
    }

    @Transactional
    public AuthDtos.AuthResponse login(AuthDtos.LoginRequest request) {
        UserAccount user = userRepository.findByEmailIgnoreCase(normalizeEmail(request.email()))
                .orElseThrow(() -> new UnauthorizedException("Email or password is incorrect."));
        if (!user.isEnabled() || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Email or password is incorrect.");
        }
        return createSession(user);
    }

    @Transactional
    public AuthDtos.AuthResponse refresh(AuthDtos.RefreshRequest request) {
        RefreshToken stored = refreshTokenRepository
                .findByTokenHashAndRevokedAtIsNull(hash(request.refreshToken()))
                .orElseThrow(() -> new UnauthorizedException("Refresh token is invalid."));
        if (stored.getExpiresAt().isBefore(Instant.now())) {
            stored.revoke();
            throw new UnauthorizedException("Refresh token has expired. Please sign in again.");
        }
        UserAccount user = stored.getUser();
        if (!user.isEnabled()) {
            stored.revoke();
            throw new UnauthorizedException("This account is disabled.");
        }
        stored.revoke();
        return createSession(user);
    }

    @Transactional
    public ApiMessage logout(AuthDtos.LogoutRequest request) {
        refreshTokenRepository.findByTokenHashAndRevokedAtIsNull(hash(request.refreshToken()))
                .ifPresent(RefreshToken::revoke);
        return new ApiMessage("Signed out successfully.");
    }

    @Transactional
    public AuthDtos.PasswordResetStartResponse forgotPassword(AuthDtos.ForgotPasswordRequest request) {
        String rawToken = userRepository.findByEmailIgnoreCase(normalizeEmail(request.email()))
                .map(user -> {
                    resetTokenRepository.deleteByUserId(user.getId());
                    String generated = randomToken();
                    resetTokenRepository.save(new PasswordResetToken(
                            hash(generated),
                            user,
                            Instant.now().plus(properties.auth().resetTokenMinutes(), ChronoUnit.MINUTES)
                    ));
                    return generated;
                })
                .orElse(null);
        String developmentToken = properties.auth().exposeDevResetToken() ? rawToken : null;
        return new AuthDtos.PasswordResetStartResponse(RESET_MESSAGE, developmentToken);
    }

    @Transactional
    public ApiMessage resetPassword(AuthDtos.ResetPasswordRequest request) {
        PasswordResetToken stored = resetTokenRepository
                .findByTokenHashAndUsedAtIsNull(hash(request.token()))
                .orElseThrow(() -> new BadRequestException("Password reset token is invalid."));
        if (stored.getExpiresAt().isBefore(Instant.now())) {
            stored.markUsed();
            throw new BadRequestException("Password reset token has expired.");
        }
        UserAccount user = stored.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.invalidateAccessTokens();
        stored.markUsed();
        refreshTokenRepository.deleteByUserId(user.getId());
        return new ApiMessage("Password updated. Please sign in with your new password.");
    }

    public static AuthDtos.UserView toUserView(UserAccount user) {
        return new AuthDtos.UserView(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getMobile(),
                user.getCity(),
                user.getInsuranceProvider(),
                user.getRole(),
                user.getCreatedAt()
        );
    }

    private AuthDtos.AuthResponse createSession(UserAccount user) {
        String rawRefreshToken = randomToken();
        refreshTokenRepository.save(new RefreshToken(
                hash(rawRefreshToken),
                user,
                Instant.now().plus(properties.auth().refreshTokenDays(), ChronoUnit.DAYS)
        ));
        return new AuthDtos.AuthResponse(
                jwtService.issue(user),
                rawRefreshToken,
                "Bearer",
                jwtService.accessTokenSeconds(),
                toUserView(user)
        );
    }

    private String randomToken() {
        byte[] bytes = new byte[48];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))
            );
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable.", exception);
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
