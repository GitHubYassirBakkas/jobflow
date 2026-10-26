package com.yassir.jobflow.controller;

import com.yassir.jobflow.dto.InterviewResponse;
import com.yassir.jobflow.dto.ScheduleInterviewRequest;
import com.yassir.jobflow.dto.UpdateInterviewStatusRequest;
import com.yassir.jobflow.entity.Interview;
import com.yassir.jobflow.entity.User;
import com.yassir.jobflow.service.InterviewService;
import com.yassir.jobflow.service.UserService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/recruiter/interviews")
public class InterviewController {

    private final InterviewService interviewService;
    private final UserService userService;

    public InterviewController(
            InterviewService interviewService,
            UserService userService) {

        this.interviewService = interviewService;
        this.userService = userService;
    }

    @PostMapping("/applications/{applicationId}")
    public ResponseEntity<InterviewResponse> scheduleInterview(
            @PathVariable Long applicationId,
            @Valid @RequestBody ScheduleInterviewRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        String email = jwt.getSubject();

        User recruiter = userService.getUserByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found"));

        Interview interview =
                interviewService.scheduleInterview(
                        applicationId,
                        request,
                        recruiter
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

    @PatchMapping("/{interviewId}/status")
public ResponseEntity<InterviewResponse> updateInterviewStatus(
        @PathVariable Long interviewId,
        @Valid @RequestBody UpdateInterviewStatusRequest request,
        @AuthenticationPrincipal Jwt jwt) {

    String email = jwt.getSubject();

    User recruiter = userService.getUserByEmail(email)
            .orElseThrow(() ->
                    new IllegalArgumentException("User not found"));

    Interview interview =
            interviewService.updateInterviewStatus(
                    interviewId,
                    request.getStatus(),
                    recruiter
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

@GetMapping
public ResponseEntity<List<InterviewResponse>> getRecruiterInterviews(
        @AuthenticationPrincipal Jwt jwt) {

    String email = jwt.getSubject();

    User recruiter = userService.getUserByEmail(email)
            .orElseThrow(() ->
                    new IllegalArgumentException("User not found"));

    List<InterviewResponse> responses =
            interviewService.getInterviewsForRecruiter(recruiter)
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
}