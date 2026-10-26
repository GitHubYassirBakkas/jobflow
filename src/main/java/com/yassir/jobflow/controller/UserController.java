package com.yassir.jobflow.controller;

import com.yassir.jobflow.dto.UserResponse;
import com.yassir.jobflow.entity.User;
import com.yassir.jobflow.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(
            @AuthenticationPrincipal Jwt jwt) {

        String email = jwt.getSubject();

        User user = userService.getUserByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found")
                );

        UserResponse response = new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getRole(),
                user.isEnabled(),
                user.getCreatedAt()
        );

        return ResponseEntity.ok(response);
    }
}