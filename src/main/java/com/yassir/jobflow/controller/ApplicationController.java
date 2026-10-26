package com.yassir.jobflow.controller;

import com.yassir.jobflow.dto.ApplicationResponse;
import com.yassir.jobflow.entity.Application;
import com.yassir.jobflow.entity.User;
import com.yassir.jobflow.service.ApplicationService;
import com.yassir.jobflow.service.UserService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/jobs")
public class ApplicationController {

    private final ApplicationService applicationService;
    private final UserService userService;

    public ApplicationController(
            ApplicationService applicationService,
            UserService userService) {

        this.applicationService = applicationService;
        this.userService = userService;
    }

    @PostMapping("/{jobId}/apply")
public ResponseEntity<ApplicationResponse> applyToJob(
        @PathVariable Long jobId,
        @RequestParam(
                value = "file",
                required = false
        ) MultipartFile file,
        @AuthenticationPrincipal Jwt jwt) {

    String email = jwt.getSubject();

    User candidate = userService.getUserByEmail(email)
            .orElseThrow(() ->
                    new IllegalArgumentException("User not found"));

    Application application =
            applicationService.applyToJob(
                    jobId,
                    candidate,
                    file
            );

    ApplicationResponse response =
            applicationService.toApplicationResponse(application);

    return ResponseEntity.ok(response);
}
}
