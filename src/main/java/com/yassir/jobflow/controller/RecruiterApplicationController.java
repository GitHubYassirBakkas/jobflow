package com.yassir.jobflow.controller;

import com.yassir.jobflow.dto.ApplicationResponse;
import com.yassir.jobflow.dto.UpdateApplicationStatusRequest;
import com.yassir.jobflow.entity.Application;
import com.yassir.jobflow.entity.User;
import com.yassir.jobflow.service.ApplicationService;
import com.yassir.jobflow.service.UserService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.List;

@RestController
@RequestMapping("/api/recruiter/jobs")
public class RecruiterApplicationController {

    private final ApplicationService applicationService;
    private final UserService userService;

    public RecruiterApplicationController(
            ApplicationService applicationService,
            UserService userService) {

        this.applicationService = applicationService;
        this.userService = userService;
    }

    @GetMapping("/{jobId}/applications")
    public ResponseEntity<List<ApplicationResponse>> getJobApplications(
            @PathVariable Long jobId,
            @AuthenticationPrincipal Jwt jwt) {

        String email = jwt.getSubject();

        User recruiter = userService.getUserByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found"));

        List<ApplicationResponse> applications =
                applicationService
                        .getApplicationsByJob(jobId, recruiter)
                        .stream()
                        .map(application -> new ApplicationResponse(
                                application.getId(),
                                application.getJob().getId(),
                                application.getJob().getTitle(),
                                application.getJob().getCompany().getName(),
                                application.getCandidate().getId(),
                                application.getCandidate().getFirstName()
                                        + " "
                                        + application.getCandidate().getLastName(),
                                application.getCandidate().getEmail(),
                                application.getStatus(),
                                application.getCvFileName(),
                                application.getAppliedAt()
                                ))
                        .toList();

        return ResponseEntity.ok(applications);
    }

    @PatchMapping("/applications/{applicationId}/status")
public ResponseEntity<ApplicationResponse> updateApplicationStatus(
        @PathVariable Long applicationId,
        @Valid @RequestBody UpdateApplicationStatusRequest request,
        @AuthenticationPrincipal Jwt jwt) {

    String email = jwt.getSubject();

    User recruiter = userService.getUserByEmail(email)
            .orElseThrow(() ->
                    new IllegalArgumentException("User not found"));

    Application application =
            applicationService.updateApplicationStatus(
                    applicationId,
                    request.getStatus(),
                    recruiter
            );

    ApplicationResponse response =
        new ApplicationResponse(
                application.getId(),
                application.getJob().getId(),
                application.getJob().getTitle(),
                application.getJob().getCompany().getName(),
                application.getCandidate().getId(),
                application.getCandidate().getFirstName()
                        + " "
                        + application.getCandidate().getLastName(),
                application.getCandidate().getEmail(),
                application.getStatus(),
                application.getCvFileName(),
                application.getAppliedAt()
        );

    return ResponseEntity.ok(response);
}

@GetMapping("/applications/{applicationId}/cv")
public ResponseEntity<Resource> getApplicationCv(
        @PathVariable Long applicationId,
        @AuthenticationPrincipal Jwt jwt) {

    String email = jwt.getSubject();

    User recruiter = userService.getUserByEmail(email)
            .orElseThrow(() ->
                    new IllegalArgumentException("User not found"));

    Application application =
            applicationService.getApplicationById(applicationId);

    Resource resource =
            applicationService.getApplicationCv(
                    applicationId,
                    recruiter
            );

    return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header(
                    HttpHeaders.CONTENT_DISPOSITION,
                    "inline; filename=\"" +
                            application.getCvFileName() +
                            "\""
            )
            .body(resource);
}
}