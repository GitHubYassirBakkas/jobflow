package com.yassir.jobflow.controller;

import com.yassir.jobflow.dto.CreateJobRequest;
import com.yassir.jobflow.dto.JobDetailsResponse;
import com.yassir.jobflow.dto.JobResponse;
import com.yassir.jobflow.dto.PageResponse;
import com.yassir.jobflow.entity.EmploymentType;
import com.yassir.jobflow.entity.Job;
import com.yassir.jobflow.entity.User;
import com.yassir.jobflow.service.JobService;
import com.yassir.jobflow.service.UserService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

    private final JobService jobService;
    private final UserService userService;

    public JobController(
            JobService jobService,
            UserService userService) {

        this.jobService = jobService;
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<JobResponse> createJob(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateJobRequest request) {

        String email = jwt.getSubject();

        User recruiter = userService.getUserByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found"));

        Job job = jobService.createJob(request, recruiter);

        JobResponse response = new JobResponse(
                job.getId(),
                job.getTitle(),
                job.getDescription(),
                job.getLocation(),
                job.getEmploymentType(),
                job.isActive(),
                job.getCompany().getId(),
                job.getCompany().getName(),
                job.getCreatedBy().getId(),
                job.getCreatedBy().getFirstName()
                        + " "
                        + job.getCreatedBy().getLastName(),
                job.getCreatedAt()
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<PageResponse<JobResponse>> getActiveJobs(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) EmploymentType employmentType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        PageResponse<JobResponse> jobs =
                jobService.getActiveJobPage(
                        search,
                        location,
                        employmentType,
                        page,
                        size
                );

        return ResponseEntity.ok(jobs);
    }

@GetMapping("/{id}")
public ResponseEntity<JobDetailsResponse> getJobById(
        @PathVariable Long id,
        @AuthenticationPrincipal Jwt jwt) {

    String email = jwt.getSubject();

    User user = userService.getUserByEmail(email)
            .orElseThrow(() ->
                    new IllegalArgumentException("User not found"));

    Job job = jobService.getJobByIdForUser(id, user);

    JobDetailsResponse response = jobService.toJobDetailsResponse(job);

    return ResponseEntity.ok(response);
}
}
