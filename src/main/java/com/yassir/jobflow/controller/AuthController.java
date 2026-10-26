package com.yassir.jobflow.controller;

import com.yassir.jobflow.dto.RegisterRequest;
import com.yassir.jobflow.dto.UserResponse;
import com.yassir.jobflow.entity.User;
import com.yassir.jobflow.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.yassir.jobflow.dto.LoginRequest;
import com.yassir.jobflow.dto.LoginResponse;
import com.yassir.jobflow.service.JwtService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final JwtService jwtService;

    public AuthController(UserService userService, JwtService jwtService) {
        this.userService = userService;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(
            @Valid @RequestBody RegisterRequest request) {

        User user = userService.registerUser(request);

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

   @PostMapping("/login")
public ResponseEntity<LoginResponse> login(
        @Valid @RequestBody LoginRequest request) {

    User user = userService.loginUser(request);

    String token = jwtService.generateToken(user);

    UserResponse userResponse = new UserResponse(
            user.getId(),
            user.getFirstName(),
            user.getLastName(),
            user.getEmail(),
            user.getRole(),
            user.isEnabled(),
            user.getCreatedAt()
    );

    LoginResponse response = new LoginResponse(token, userResponse);

    return ResponseEntity.ok(response);
}
}