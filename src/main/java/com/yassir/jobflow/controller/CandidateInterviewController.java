package com.yassir.jobflow.controller;

import com.yassir.jobflow.dto.InterviewResponse;
import com.yassir.jobflow.entity.Interview;
import com.yassir.jobflow.entity.User;
import com.yassir.jobflow.service.InterviewService;
import com.yassir.jobflow.service.UserService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/candidate/interviews")
public class CandidateInterviewController {

    private final InterviewService interviewService;
    private final UserService userService;

    public CandidateInterviewController(
            InterviewService interviewService,
            UserService userService) {

        this.interviewService = interviewService;
        this.userService = userService;
    }

    @GetMapping
public ResponseEntity<List<InterviewResponse>> getCandidateInterviews(
        @AuthenticationPrincipal Jwt jwt) {

    String email = jwt.getSubject();

    User candidate = userService.getUserByEmail(email)
            .orElseThrow(() ->
                    new IllegalArgumentException("User not found"));

    List<InterviewResponse> responses =
            interviewService.getInterviewsForCandidate(candidate)
                    .stream()
                    .map(interview -> new InterviewResponse(
                            interview.getId(),
                            interview.getApplication().getId(),
                            interview.getApplication().getJob().getId(),
                            interview.getApplication().getJob().getTitle(),
                            interview.getApplication().getCandidate().getId(),
                            interview.getApplication().getCandidate().getFirstName()
                                    + " "
                                    + interview.getApplication().getCandidate().getLastName(),
                            interview.getApplication().getCandidate().getEmail(),
                            interview.getScheduledAt(),
                            interview.getType(),
                            interview.getMeetingLink(),
                            interview.getLocation(),
                            interview.getNotes(),
                            interview.getStatus()
                    ))
                    .toList();

    return ResponseEntity.ok(responses);
}

    @GetMapping("/applications/{applicationId}")
    public ResponseEntity<InterviewResponse> getInterview(
            @PathVariable Long applicationId,
            @AuthenticationPrincipal Jwt jwt) {

        String email = jwt.getSubject();

        User candidate = userService.getUserByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found"));

        Interview interview =
                interviewService.getInterviewByApplicationForCandidate(
                        applicationId,
                        candidate
                );

        InterviewResponse response =
                new InterviewResponse(
                        interview.getId(),
                        interview.getApplication().getId(),
                        interview.getApplication().getJob().getId(),
                        interview.getApplication().getJob().getTitle(),
                        interview.getApplication().getCandidate().getId(),
                        interview.getApplication().getCandidate().getFirstName()
                                + " "
                                + interview.getApplication().getCandidate().getLastName(),
                        interview.getApplication().getCandidate().getEmail(),
                        interview.getScheduledAt(),
                        interview.getType(),
                        interview.getMeetingLink(),
                        interview.getLocation(),
                        interview.getNotes(),
                        interview.getStatus()
                );

        return ResponseEntity.ok(response);
    }
}