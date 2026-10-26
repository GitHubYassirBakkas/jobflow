package com.yassir.jobflow.controller;

import com.yassir.jobflow.dto.PageResponse;
import com.yassir.jobflow.dto.SavedJobResponse;
import com.yassir.jobflow.entity.User;
import com.yassir.jobflow.service.SavedJobService;
import com.yassir.jobflow.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/candidate/saved-jobs")
public class CandidateSavedJobController {

    private final SavedJobService savedJobService;
    private final UserService userService;

    public CandidateSavedJobController(
            SavedJobService savedJobService,
            UserService userService) {

        this.savedJobService = savedJobService;
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<PageResponse<SavedJobResponse>> getSavedJobs(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        User candidate = getAuthenticatedUser(jwt);

        return ResponseEntity.ok(
                savedJobService.getSavedJobs(candidate.getId(), page, size)
        );
    }

    @GetMapping("/job-ids")
    public ResponseEntity<List<Long>> getActiveSavedJobIds(
            @AuthenticationPrincipal Jwt jwt) {

        User candidate = getAuthenticatedUser(jwt);

        return ResponseEntity.ok(
                savedJobService.getActiveSavedJobIds(candidate.getId())
        );
    }

    @PostMapping("/{jobId}")
    public ResponseEntity<SavedJobResponse> saveJob(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long jobId) {

        User candidate = getAuthenticatedUser(jwt);

        return ResponseEntity.ok(
                savedJobService.saveJob(candidate.getId(), jobId)
        );
    }

    @DeleteMapping("/{jobId}")
    public ResponseEntity<Void> unsaveJob(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long jobId) {

        User candidate = getAuthenticatedUser(jwt);

        savedJobService.unsaveJob(candidate.getId(), jobId);

        return ResponseEntity.noContent().build();
    }

    private User getAuthenticatedUser(Jwt jwt) {
        return userService.getUserByEmail(jwt.getSubject())
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found"));
    }
}
