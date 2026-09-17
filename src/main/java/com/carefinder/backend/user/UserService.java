package com.carefinder.backend.user;

import com.carefinder.backend.auth.RefreshTokenRepository;
import com.carefinder.backend.common.ApiMessage;
import com.carefinder.backend.common.BadRequestException;
import com.carefinder.backend.common.NotFoundException;
import com.carefinder.backend.personal.FavoriteRepository;
import com.carefinder.backend.personal.RecentlyViewedRepository;
import com.carefinder.backend.security.CurrentUserService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final FavoriteRepository favoriteRepository;
    private final RecentlyViewedRepository recentlyViewedRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final CurrentUserService currentUserService;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            UserRepository userRepository,
            FavoriteRepository favoriteRepository,
            RecentlyViewedRepository recentlyViewedRepository,
            RefreshTokenRepository refreshTokenRepository,
            CurrentUserService currentUserService,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.favoriteRepository = favoriteRepository;
        this.recentlyViewedRepository = recentlyViewedRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.currentUserService = currentUserService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public UserDtos.ProfileResponse profile() {
        return toProfile(currentUserService.requireCurrentUser());
    }

    @Transactional
    public UserDtos.ProfileResponse updateProfile(UserDtos.UpdateProfileRequest request) {
        UserAccount user = currentUserService.requireCurrentUser();
        user.setName(request.name().trim());
        user.setMobile(request.mobile().trim());
        user.setCity(clean(request.city()));
        user.setInsuranceProvider(clean(request.insuranceProvider()));
        return toProfile(user);
    }

    @Transactional
    public ApiMessage changePassword(UserDtos.ChangePasswordRequest request) {
        UserAccount user = currentUserService.requireCurrentUser();
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Current password is incorrect.");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new BadRequestException("New password must be different from the current password.");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.invalidateAccessTokens();
        refreshTokenRepository.deleteByUserId(user.getId());
        return new ApiMessage("Password changed. Please sign in again.");
    }

    @Transactional(readOnly = true)
    public List<UserDtos.AdminUserResponse> listUsers() {
        return userRepository.findAll().stream()
                .sorted(Comparator.comparing(UserAccount::getCreatedAt).reversed())
                .map(this::toAdminResponse)
                .toList();
    }

    @Transactional
    public UserDtos.AdminUserResponse updateUser(UUID id, UserDtos.AdminUserUpdateRequest request) {
        UserAccount actor = currentUserService.requireCurrentUser();
        UserAccount user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User account not found."));
        if (actor.getId().equals(user.getId()) && Boolean.FALSE.equals(request.enabled())) {
            throw new BadRequestException("You cannot disable your own administrator account.");
        }
        boolean securityChanged = false;
        if (request.role() != null && request.role() != user.getRole()) {
            user.setRole(request.role());
            securityChanged = true;
        }
        if (request.enabled() != null && request.enabled() != user.isEnabled()) {
            user.setEnabled(request.enabled());
            securityChanged = true;
        }
        if (securityChanged) {
            user.invalidateAccessTokens();
            refreshTokenRepository.deleteByUserId(user.getId());
        }
        return toAdminResponse(user);
    }

    private UserDtos.ProfileResponse toProfile(UserAccount user) {
        return new UserDtos.ProfileResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getMobile(),
                user.getCity(),
                user.getInsuranceProvider(),
                user.getRole(),
                user.getCreatedAt(),
                favoriteRepository.countByUserId(user.getId()),
                recentlyViewedRepository.countByUserId(user.getId())
        );
    }

    private UserDtos.AdminUserResponse toAdminResponse(UserAccount user) {
        return new UserDtos.AdminUserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getMobile(),
                user.getCity(),
                user.getInsuranceProvider(),
                user.getRole(),
                user.isEnabled(),
                user.getCreatedAt()
        );
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
