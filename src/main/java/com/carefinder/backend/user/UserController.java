package com.carefinder.backend.user;

import com.carefinder.backend.common.ApiMessage;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/me")
@Tag(name = "User Profile")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    UserDtos.ProfileResponse profile() {
        return userService.profile();
    }

    @PutMapping
    UserDtos.ProfileResponse update(@Valid @RequestBody UserDtos.UpdateProfileRequest request) {
        return userService.updateProfile(request);
    }

    @PatchMapping("/password")
    ApiMessage changePassword(@Valid @RequestBody UserDtos.ChangePasswordRequest request) {
        return userService.changePassword(request);
    }
}
