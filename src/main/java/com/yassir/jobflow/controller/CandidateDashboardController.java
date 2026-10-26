package com.yassir.jobflow.controller;

import com.yassir.jobflow.dto.CandidateDashboardResponse;
import com.yassir.jobflow.entity.User;
import com.yassir.jobflow.service.CandidateDashboardService;
import com.yassir.jobflow.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/candidate/dashboard")
public class CandidateDashboardController {

    private final CandidateDashboardService candidateDashboardService;
    private final UserService userService;

    public CandidateDashboardController(
            CandidateDashboardService candidateDashboardService,
            UserService userService) {

        this.candidateDashboardService = candidateDashboardService;
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<CandidateDashboardResponse> getDashboard(
            @AuthenticationPrincipal Jwt jwt) {

        User candidate = getAuthenticatedUser(jwt);

        return ResponseEntity.ok(
                candidateDashboardService.getDashboard(candidate)
        );
    }

    private User getAuthenticatedUser(Jwt jwt) {
        String email = jwt.getSubject();

        return userService.getUserByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found"));
    }
}
