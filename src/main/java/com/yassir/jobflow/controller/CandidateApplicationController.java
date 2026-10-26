package com.yassir.jobflow.controller;

import com.yassir.jobflow.dto.ApplicationResponse;
import com.yassir.jobflow.dto.ApplicationStatusTimelineEventResponse;
import com.yassir.jobflow.dto.PageResponse;
import com.yassir.jobflow.entity.Application;
import com.yassir.jobflow.entity.User;
import com.yassir.jobflow.service.ApplicationService;
import com.yassir.jobflow.service.UserService;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/applications")
public class CandidateApplicationController {

    private final ApplicationService applicationService;
    private final UserService userService;

    public CandidateApplicationController(
            ApplicationService applicationService,
            UserService userService) {

        this.applicationService = applicationService;
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<PageResponse<ApplicationResponse>> getMyApplications(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        User candidate = getAuthenticatedUser(jwt);

        return ResponseEntity.ok(
                applicationService.getApplicationsByCandidate(
                        candidate.getId(),
                        page,
                        size
                )
        );
    }

    @GetMapping("/me/{applicationId}")
    public ResponseEntity<ApplicationResponse> getMyApplication(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long applicationId) {

        User candidate = getAuthenticatedUser(jwt);

        return ResponseEntity.ok(
                applicationService.getApplicationResponseForCandidate(
                        applicationId,
                        candidate.getId()
                )
        );
    }

    @GetMapping("/me/jobs/{jobId}")
    public ResponseEntity<ApplicationResponse> getMyApplicationForJob(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long jobId) {

        User candidate = getAuthenticatedUser(jwt);

        return applicationService
                .getApplicationResponseForCandidateAndJob(
                        candidate.getId(),
                        jobId
                )
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PatchMapping("/me/{applicationId}/withdraw")
    public ResponseEntity<ApplicationResponse> withdrawMyApplication(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long applicationId) {

        User candidate = getAuthenticatedUser(jwt);

        Application application =
                applicationService.withdrawApplication(
                        applicationId,
                        candidate
                );

        return ResponseEntity.ok(
                applicationService.toApplicationResponse(application)
        );
    }

    @GetMapping("/me/{applicationId}/timeline")
    public ResponseEntity<List<ApplicationStatusTimelineEventResponse>>
            getMyApplicationTimeline(
                    @AuthenticationPrincipal Jwt jwt,
                    @PathVariable Long applicationId) {

        User candidate = getAuthenticatedUser(jwt);

        return ResponseEntity.ok(
                applicationService.getApplicationTimelineForCandidate(
                        applicationId,
                        candidate.getId()
                )
        );
    }

    @GetMapping("/me/{applicationId}/cv")
    public ResponseEntity<Resource> getMyApplicationCv(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long applicationId) {

        User candidate = getAuthenticatedUser(jwt);

        Application application =
                applicationService.getApplicationByIdForCandidate(
                        applicationId,
                        candidate.getId()
                );

        Resource resource =
                applicationService.getApplicationCvForCandidate(
                        applicationId,
                        candidate.getId()
                );

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\""
                                + application.getCvFileName()
                                + "\""
                )
                .body(resource);
    }

    private User getAuthenticatedUser(Jwt jwt) {
        String email = jwt.getSubject();

        return userService.getUserByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found"));
    }
}
