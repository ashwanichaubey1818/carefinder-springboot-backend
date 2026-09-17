package com.carefinder.backend.security;

import com.carefinder.backend.common.NotFoundException;
import com.carefinder.backend.user.UserAccount;
import com.carefinder.backend.user.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CurrentUserService {

    private final UserRepository userRepository;

    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserAccount requireCurrentUser() {
        JwtPrincipal principal = principal()
                .orElseThrow(() -> new NotFoundException("Authenticated user was not found."));
        return userRepository.findById(principal.userId())
                .orElseThrow(() -> new NotFoundException("Authenticated user was not found."));
    }

    public Optional<JwtPrincipal> principal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof JwtPrincipal principal) {
            return Optional.of(principal);
        }
        return Optional.empty();
    }
}
