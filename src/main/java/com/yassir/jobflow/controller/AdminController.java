package com.yassir.jobflow.controller;

import com.yassir.jobflow.dto.AdminDashboardResponse;
import com.yassir.jobflow.dto.CompanyResponse;
import com.yassir.jobflow.dto.JobResponse;
import com.yassir.jobflow.dto.UserResponse;
import com.yassir.jobflow.entity.Company;
import com.yassir.jobflow.entity.Job;
import com.yassir.jobflow.entity.User;
import com.yassir.jobflow.service.AdminService;
import com.yassir.jobflow.service.CompanyService;
import com.yassir.jobflow.service.JobService;
import com.yassir.jobflow.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.yassir.jobflow.dto.CandidateProfileResponse;
import com.yassir.jobflow.entity.CandidateProfile;
import com.yassir.jobflow.service.CandidateProfileService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.List;
import com.yassir.jobflow.dto.ApplicationResponse;
import com.yassir.jobflow.entity.Application;
import com.yassir.jobflow.service.ApplicationService;
import com.yassir.jobflow.dto.InterviewResponse;
import com.yassir.jobflow.entity.Interview;
import com.yassir.jobflow.service.InterviewService;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.yassir.jobflow.dto.UpdateCompanyRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import com.yassir.jobflow.dto.UpdateJobRequest;
import com.yassir.jobflow.entity.ApplicationStatus;
import com.yassir.jobflow.entity.InterviewStatus;



@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;
    private final UserService userService;
    private final CompanyService companyService;
    private final JobService jobService;
    private final CandidateProfileService candidateProfileService;
    private final ApplicationService applicationService;
    private final InterviewService interviewService;

    public AdminController(
        AdminService adminService,
        UserService userService,
        CompanyService companyService,
        JobService jobService,
        CandidateProfileService candidateProfileService,
        ApplicationService applicationService,
        InterviewService interviewService) {

    this.adminService = adminService;
    this.userService = userService;
    this.companyService = companyService;
    this.jobService = jobService;
    this.candidateProfileService = candidateProfileService;
    this.applicationService = applicationService;
    this.interviewService = interviewService;
}

    @GetMapping("/dashboard")
    public ResponseEntity<AdminDashboardResponse> getDashboard() {

        AdminDashboardResponse response =
                adminService.getDashboardStats();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserResponse>> getUsers() {

        List<UserResponse> users = userService.getAllUsers()
                .stream()
                .map(this::toUserResponse)
                .toList();

        return ResponseEntity.ok(users);
    }

    @GetMapping("/companies")
    public ResponseEntity<List<CompanyResponse>> getCompanies() {

        List<CompanyResponse> companies =
                companyService.getAllCompanies()
                        .stream()
                        .map(this::toCompanyResponse)
                        .toList();

        return ResponseEntity.ok(companies);
    }

    @GetMapping("/jobs")
    public ResponseEntity<List<JobResponse>> getJobs() {

        List<JobResponse> jobs = jobService.getAllJobs()
                .stream()
                .map(this::toJobResponse)
                .toList();

        return ResponseEntity.ok(jobs);
    }

    private UserResponse toUserResponse(User user) {

        return new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getRole(),
                user.isEnabled(),
                user.getCreatedAt()
        );
    }

    private CompanyResponse toCompanyResponse(Company company) {

        return new CompanyResponse(
                company.getId(),
                company.getName(),
                company.getDescription(),
                company.getWebsite(),
                company.getLocation(),
                company.getCreatedAt()
        );
    }

    private JobResponse toJobResponse(Job job) {

        return new JobResponse(
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
    }

    @GetMapping("/candidates/{userId}")
public ResponseEntity<CandidateProfileResponse> getCandidateProfile(
        @PathVariable Long userId) {

    CandidateProfile profile =
            candidateProfileService.getProfileByUserId(userId);

    return ResponseEntity.ok(candidateProfileService.toResponse(profile));
}

@GetMapping("/candidates/{userId}/cv")
public ResponseEntity<Resource> getCandidateCv(
        @PathVariable Long userId) {

    CandidateProfile profile =
            candidateProfileService.getProfileByUserId(userId);

    Resource resource =
            candidateProfileService.getCvFile(userId);

    return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header(
                    HttpHeaders.CONTENT_DISPOSITION,
                    "inline; filename=\"" + profile.getCvFileName() + "\""
            )
            .body(resource);
}

@GetMapping("/applications")
public ResponseEntity<List<ApplicationResponse>> getApplications() {

    List<ApplicationResponse> applications =
            applicationService.getAllApplications()
                    .stream()
                    .map(this::toApplicationResponse)
                    .toList();

    return ResponseEntity.ok(applications);
}

private ApplicationResponse toApplicationResponse(
        Application application) {

    User candidate = application.getCandidate();

    return new ApplicationResponse(
            application.getId(),
            application.getJob().getId(),
            application.getJob().getTitle(),
            application.getJob().getCompany().getName(),
            candidate.getId(),
            candidate.getFirstName()
                    + " "
                    + candidate.getLastName(),
            candidate.getEmail(),
            application.getStatus(),
            application.getCvFileName(),
            application.getAppliedAt()
    );
}

@GetMapping("/applications/{applicationId}/cv")
public ResponseEntity<Resource> getApplicationCv(
        @PathVariable Long applicationId) {

    Application application =
            applicationService.getApplicationById(applicationId);

    Resource resource =
            applicationService.getApplicationCvForAdmin(
                    applicationId
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

@GetMapping("/interviews")
public ResponseEntity<List<InterviewResponse>> getInterviews() {

    List<InterviewResponse> interviews =
            interviewService.getAllInterviews()
                    .stream()
                    .map(this::toInterviewResponse)
                    .toList();

    return ResponseEntity.ok(interviews);
}
private InterviewResponse toInterviewResponse(
        Interview interview) {

    Application application = interview.getApplication();
    User candidate = application.getCandidate();

    return new InterviewResponse(
            interview.getId(),
            application.getId(),
            application.getJob().getId(),
            application.getJob().getTitle(),
            candidate.getId(),
            candidate.getFirstName() + " " + candidate.getLastName(),
            candidate.getEmail(),
            interview.getScheduledAt(),
            interview.getType(),
            interview.getMeetingLink(),
            interview.getLocation(),
            interview.getNotes(),
            interview.getStatus()
    );
}

@PatchMapping("/users/{userId}/enabled")
public ResponseEntity<UserResponse> updateUserEnabled(
        @PathVariable Long userId,
        @RequestParam boolean enabled,
        @AuthenticationPrincipal Jwt jwt) {

    Long currentAdminId =
            ((Number) jwt.getClaim("userId")).longValue();

    User user = userService.updateUserEnabled(
            userId,
            enabled,
            currentAdminId
    );

    return ResponseEntity.ok(toUserResponse(user));
}

@PatchMapping("/jobs/{jobId}/active")
public ResponseEntity<JobResponse> updateJobActive(
        @PathVariable Long jobId,
        @RequestParam boolean active) {

    Job job = jobService.updateJobActive(jobId, active);

    return ResponseEntity.ok(toJobResponse(job));
}

@PutMapping("/companies/{companyId}")
public ResponseEntity<CompanyResponse> updateCompany(
        @PathVariable Long companyId,
        @Valid @RequestBody UpdateCompanyRequest request) {

    Company company =
            companyService.updateCompany(companyId, request);

    return ResponseEntity.ok(toCompanyResponse(company));
}

@PutMapping("/jobs/{jobId}")
public ResponseEntity<JobResponse> updateJob(
        @PathVariable Long jobId,
        @Valid @RequestBody UpdateJobRequest request) {

    Job job = jobService.updateJob(jobId, request);

    return ResponseEntity.ok(toJobResponse(job));
}

@PatchMapping("/applications/{applicationId}/status")
public ResponseEntity<ApplicationResponse> updateApplicationStatus(
        @PathVariable Long applicationId,
        @RequestParam ApplicationStatus status,
        @AuthenticationPrincipal Jwt jwt) {

    String email = jwt.getSubject();

    User admin = userService.getUserByEmail(email)
            .orElseThrow(() ->
                    new IllegalArgumentException("User not found"));

    Application application =
            applicationService.updateApplicationStatusForAdmin(
                    applicationId,
                    status,
                    admin
            );

    return ResponseEntity.ok(
            toApplicationResponse(application)
    );
}

@PatchMapping("/interviews/{interviewId}/status")
public ResponseEntity<InterviewResponse> updateInterviewStatus(
        @PathVariable Long interviewId,
        @RequestParam InterviewStatus status) {

    Interview interview =
            interviewService.updateInterviewStatusForAdmin(
                    interviewId,
                    status
            );

    return ResponseEntity.ok(
            toInterviewResponse(interview)
    );
}
}
