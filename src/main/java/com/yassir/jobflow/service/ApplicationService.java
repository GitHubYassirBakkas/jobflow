package com.yassir.jobflow.service;

import com.yassir.jobflow.dto.ApplicationResponse;
import com.yassir.jobflow.dto.ApplicationStatusTimelineEventResponse;
import com.yassir.jobflow.dto.PageResponse;
import com.yassir.jobflow.entity.Application;
import com.yassir.jobflow.entity.ApplicationStatus;
import com.yassir.jobflow.entity.ApplicationStatusHistory;
import com.yassir.jobflow.entity.ApplicationStatusHistoryEventType;
import com.yassir.jobflow.entity.CandidateProfile;
import com.yassir.jobflow.entity.Job;
import com.yassir.jobflow.entity.Role;
import com.yassir.jobflow.entity.User;
import com.yassir.jobflow.entity.RecruiterProfile;
import com.yassir.jobflow.repository.ApplicationRepository;
import com.yassir.jobflow.repository.ApplicationStatusHistoryRepository;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;

import java.net.MalformedURLException;
import org.springframework.web.multipart.MultipartFile;

import com.yassir.jobflow.exception.ResourceNotFoundException;
import com.yassir.jobflow.exception.ForbiddenException;
import com.yassir.jobflow.exception.BusinessConflictException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
public class ApplicationService {

    private static final String DUPLICATE_APPLICATION_MESSAGE =
            "You have already applied to this job";
    private static final String APPLICATION_UNIQUE_CONSTRAINT =
            "uq_applications_candidate_job";
    private static final String APPLICATION_NOT_FOUND_MESSAGE =
            "Application not found";
    private static final String WITHDRAWAL_CONFLICT_MESSAGE =
            "Application cannot be withdrawn from its current status";
    private static final String WITHDRAWN_REACTIVATION_MESSAGE =
            "Withdrawn applications cannot be reactivated";
    private static final int MAX_SIZE = 50;

    private final ApplicationRepository applicationRepository;
    private final ApplicationStatusHistoryRepository statusHistoryRepository;
    private final JobService jobService;
    private final RecruiterProfileService recruiterProfileService;
    private final CandidateProfileService candidateProfileService;
    private final PdfFileValidator pdfFileValidator;
    private final Clock clock;

    public ApplicationService(
            ApplicationRepository applicationRepository,
            ApplicationStatusHistoryRepository statusHistoryRepository,
            JobService jobService,
            RecruiterProfileService recruiterProfileService,
            CandidateProfileService candidateProfileService,
            PdfFileValidator pdfFileValidator,
            Clock clock) {

        this.applicationRepository = applicationRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.jobService = jobService;
        this.recruiterProfileService = recruiterProfileService;
        this.candidateProfileService = candidateProfileService;
        this.pdfFileValidator = pdfFileValidator;
        this.clock = clock;
    }

   @Transactional
   public Application applyToJob(
        Long jobId,
        User candidate,
        MultipartFile customCv) {

    Job job = jobService.getJobById(jobId);

    if (!job.isActive()) {
        throw new IllegalArgumentException(
                "This job is no longer active"
        );
    }

    if (applicationRepository.existsByCandidateIdAndJobId(
            candidate.getId(),
            job.getId())) {

        throw new IllegalArgumentException(
                DUPLICATE_APPLICATION_MESSAGE
        );
    }

    CandidateProfile candidateProfile =
            candidateProfileService.getProfileByUserId(
                    candidate.getId()
            );

    boolean hasCustomCv = customCv != null;

    Path sourceCv = null;
    String cvFileName;

    /*
     * CASE 1:
     * Candidate uploaded a different CV
     * specifically for this application.
     */
    if (hasCustomCv) {

        pdfFileValidator.validatePdf(customCv);
        cvFileName =
                pdfFileValidator.getSafeOriginalFilename(customCv);

    } else {

        /*
         * CASE 2:
         * No custom CV was uploaded.
         * Use the CV saved in Candidate Profile.
         */
        if (candidateProfile.getCvFilePath() == null
                || candidateProfile.getCvFilePath().isBlank()) {

            throw new IllegalArgumentException(
                    "You must upload a CV before applying"
            );
        }

        sourceCv = Paths.get(
                candidateProfile.getCvFilePath()
        );

        if (!Files.exists(sourceCv)) {
            throw new IllegalArgumentException(
                    "Candidate CV file not found"
            );
        }

        cvFileName = candidateProfile.getCvFileName();
    }

    Path applicationCvPath = null;
    boolean applicationCvCreated = false;

    try {

        Path applicationCvDirectory =
                Paths.get(
                        "uploads",
                        "applications",
                        "cvs"
                ).normalize();

        Files.createDirectories(applicationCvDirectory);

        applicationCvPath =
                createApplicationCvPath(applicationCvDirectory);

        if (hasCustomCv) {

            try (var inputStream = customCv.getInputStream();
                 var outputStream = Files.newOutputStream(
                         applicationCvPath,
                         StandardOpenOption.WRITE,
                         StandardOpenOption.CREATE_NEW)) {

                applicationCvCreated = true;
                inputStream.transferTo(outputStream);
            }

        } else {

            try (var inputStream = Files.newInputStream(sourceCv);
                 var outputStream = Files.newOutputStream(
                         applicationCvPath,
                         StandardOpenOption.WRITE,
                         StandardOpenOption.CREATE_NEW)) {

                applicationCvCreated = true;
                inputStream.transferTo(outputStream);
            }
        }

        Application application = new Application();

        application.setCandidate(candidate);
        application.setJob(job);
        application.setStatus(ApplicationStatus.APPLIED);

        application.setCvFileName(cvFileName);

        application.setCvFilePath(
                applicationCvPath.toString()
        );

        Application savedApplication =
                applicationRepository.saveAndFlush(application);

        recordStatusHistory(
                savedApplication,
                ApplicationStatus.APPLIED,
                candidate,
                candidate.getRole(),
                ApplicationStatusHistoryEventType.BASELINE
        );

        return savedApplication;

    } catch (IOException e) {

        deleteApplicationCvQuietly(
                applicationCvPath,
                applicationCvCreated,
                e
        );

        throw new RuntimeException(
                "Failed to store CV for application",
                e
        );

    } catch (DataIntegrityViolationException e) {

        deleteApplicationCvQuietly(
                applicationCvPath,
                applicationCvCreated,
                e
        );

        if (isDuplicateApplicationConstraintViolation(e)) {
            throw new IllegalArgumentException(
                    DUPLICATE_APPLICATION_MESSAGE
            );
        }

        throw e;

    } catch (RuntimeException e) {

        deleteApplicationCvQuietly(
                applicationCvPath,
                applicationCvCreated,
                e
        );
        throw e;
    }
}

private boolean isDuplicateApplicationConstraintViolation(
        DataIntegrityViolationException exception) {

    Throwable current = exception;

    while (current != null) {
        if (current instanceof org.hibernate.exception.ConstraintViolationException
                constraintViolationException
                && APPLICATION_UNIQUE_CONSTRAINT.equalsIgnoreCase(
                        constraintViolationException.getConstraintName())) {

            return true;
        }

        String message = current.getMessage();

        if (message != null
                && message
                        .toLowerCase()
                        .contains(APPLICATION_UNIQUE_CONSTRAINT)) {

            return true;
        }

        current = current.getCause();
    }

    return false;
}

private Path createApplicationCvPath(Path applicationCvDirectory) {

    String storedFileName =
            UUID.randomUUID() + ".pdf";

    Path applicationCvPath =
            applicationCvDirectory
                    .resolve(storedFileName)
                    .normalize();

    if (!applicationCvDirectory.equals(applicationCvPath.getParent())) {
        throw new IllegalStateException("Invalid CV storage path");
    }

    return applicationCvPath;
}

private void deleteApplicationCvQuietly(
        Path applicationCvPath,
        boolean applicationCvCreated,
        Exception originalException) {

    if (applicationCvPath == null || !applicationCvCreated) {
        return;
    }

    try {
        Files.deleteIfExists(applicationCvPath);
    } catch (IOException cleanupException) {
        originalException.addSuppressed(cleanupException);
    }
}

    public List<Application> getApplicationsByCandidate(
            Long candidateId) {

        return applicationRepository
                .findByCandidateId(candidateId);
    }

    public PageResponse<ApplicationResponse> getApplicationsByCandidate(
            Long candidateId,
            int page,
            int size) {

        validatePageRequest(page, size);

        Page<Application> applications =
                applicationRepository
                        .findByCandidateIdOrderByAppliedAtDescIdDesc(
                                candidateId,
                                PageRequest.of(page, size)
                        );

        return PageResponse.from(applications, this::toApplicationResponse);
    }

    public ApplicationResponse toApplicationResponse(Application application) {
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

    private void validatePageRequest(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException(
                    "Page must be greater than or equal to 0"
            );
        }

        if (size < 1) {
            throw new IllegalArgumentException(
                    "Size must be greater than or equal to 1"
            );
        }

        if (size > MAX_SIZE) {
            throw new IllegalArgumentException(
                    "Size must not exceed " + MAX_SIZE
            );
        }
    }

    public List<Application> getApplicationsByJob(
            Long jobId,
            User recruiter) {

        Job job = jobService.getJobById(jobId);

        RecruiterProfile profile =
                recruiterProfileService
                        .getProfileByUserId(
                                recruiter.getId()
                        );

        if (!job.getCompany().getId()
                .equals(profile.getCompany().getId())) {

            throw new ForbiddenException(
                    "You are not allowed to view applications for this job"
            );
        }

        return applicationRepository
                .findByJobId(jobId);
    }

    @Transactional
    public Application updateApplicationStatus(
            Long applicationId,
            ApplicationStatus newStatus,
            User recruiter) {

        validateAssignableStatus(newStatus);

        Application application =
                applicationRepository
                        .findWithLockById(applicationId)
                        .orElseThrow(() ->
                               new ResourceNotFoundException(APPLICATION_NOT_FOUND_MESSAGE)   );

        RecruiterProfile profile =
                recruiterProfileService
                        .getProfileByUserId(
                                recruiter.getId()
                        );

        if (!application.getJob()
                .getCompany()
                .getId()
                .equals(profile.getCompany().getId())) {

                        throw new ForbiddenException(
                        "You are not allowed to update this application"
                );
        }

        updateApplicationStatusIfChanged(application, newStatus, recruiter);

        return application;
    }

    public List<Application> getAllApplications() {
        return applicationRepository.findAll();
    }

    public Application getApplicationById(
            Long applicationId) {

        return applicationRepository
                .findById(applicationId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(APPLICATION_NOT_FOUND_MESSAGE));
    }

    @Transactional
    public Application getApplicationByIdForUpdate(
            Long applicationId) {

        return applicationRepository
                .findWithLockById(applicationId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(APPLICATION_NOT_FOUND_MESSAGE));
    }

    public Application getApplicationByIdForCandidate(
            Long applicationId,
            Long candidateId) {

        return applicationRepository
                .findByIdAndCandidateId(applicationId, candidateId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(APPLICATION_NOT_FOUND_MESSAGE));
    }

    @Transactional
    public Application withdrawApplication(
            Long applicationId,
            User candidate) {

        Application application =
                applicationRepository
                        .findWithLockByIdAndCandidateId(
                                applicationId,
                                candidate.getId()
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        APPLICATION_NOT_FOUND_MESSAGE
                                ));

        validateWithdrawableStatus(application.getStatus());

        updateApplicationStatusIfChanged(
                application,
                ApplicationStatus.WITHDRAWN,
                candidate
        );

        return application;
    }

    public ApplicationResponse getApplicationResponseForCandidate(
            Long applicationId,
            Long candidateId) {

        return toApplicationResponse(
                getApplicationByIdForCandidate(applicationId, candidateId)
        );
    }

    @Transactional(readOnly = true)
    public Optional<ApplicationResponse>
            getApplicationResponseForCandidateAndJob(
                    Long candidateId,
                    Long jobId) {

        return applicationRepository
                .findByCandidateIdAndJobId(candidateId, jobId)
                .map(this::toApplicationResponse);
    }

    @Transactional(readOnly = true)
    public List<ApplicationStatusTimelineEventResponse>
            getApplicationTimelineForCandidate(
                    Long applicationId,
                    Long candidateId) {

        getApplicationByIdForCandidate(applicationId, candidateId);

        return statusHistoryRepository
                .findByApplicationIdOrderByChangedAtAscIdAsc(applicationId)
                .stream()
                .map(history ->
                        new ApplicationStatusTimelineEventResponse(
                                history.getStatus(),
                                history.getChangedAt(),
                                history.getChangedAtInstant(),
                                history.getEventType()
                        )
                )
                .toList();
    }

    public Resource getApplicationCv(
        Long applicationId,
        User recruiter) {

    Application application =
            getApplicationById(applicationId);

    RecruiterProfile profile =
            recruiterProfileService
                    .getProfileByUserId(recruiter.getId());

    if (!application.getJob()
            .getCompany()
            .getId()
            .equals(profile.getCompany().getId())) {

        throw new ForbiddenException(
                "You are not allowed to view this CV"
        );
    }

    return getApplicationCvResource(application);
}

public Resource getApplicationCvForAdmin(Long applicationId) {

    Application application =
            getApplicationById(applicationId);

    return getApplicationCvResource(application);
}

public Resource getApplicationCvForCandidate(
        Long applicationId,
        Long candidateId) {

    Application application =
            getApplicationByIdForCandidate(applicationId, candidateId);

    return getApplicationCvResource(application);
}

private Resource getApplicationCvResource(Application application) {

    if (application.getCvFilePath() == null
            || application.getCvFilePath().isBlank()) {

        throw new IllegalArgumentException(
                "This application has no CV"
        );
    }

    try {

        Path cvPath =
                Paths.get(application.getCvFilePath());

        Resource resource =
                new UrlResource(cvPath.toUri());

        if (!resource.exists()
                || !resource.isReadable()) {

            throw new IllegalArgumentException(
                    "Application CV file not found"
            );
        }

        return resource;

    } catch (MalformedURLException e) {

        throw new RuntimeException(
                "Could not read application CV",
                e
        );
    }
}

@Transactional
public Application updateApplicationStatusForAdmin(
        Long applicationId,
        ApplicationStatus newStatus,
        User admin) {

    validateAssignableStatus(newStatus);

    Application application =
            applicationRepository.findWithLockById(applicationId)
                    .orElseThrow(() ->
                            new ResourceNotFoundException(APPLICATION_NOT_FOUND_MESSAGE)
                    );

    updateApplicationStatusIfChanged(application, newStatus, admin);

    return application;
}

private void validateAssignableStatus(ApplicationStatus newStatus) {
    if (newStatus == ApplicationStatus.WITHDRAWN) {
        throw new IllegalArgumentException(
                "WITHDRAWN status can only be set by candidate withdrawal"
        );
    }
}

@Transactional
void updateApplicationStatusIfChanged(
        Application application,
        ApplicationStatus newStatus,
        User actor) {

    if (application.getStatus() == newStatus) {
        return;
    }

    if (application.getStatus() == ApplicationStatus.WITHDRAWN) {
        throw new BusinessConflictException(WITHDRAWN_REACTIVATION_MESSAGE);
    }

    application.setStatus(newStatus);

    recordStatusHistory(
            application,
            newStatus,
            actor,
            actor != null ? actor.getRole() : null,
            ApplicationStatusHistoryEventType.TRANSITION
    );
}

private void recordStatusHistory(
        Application application,
        ApplicationStatus status,
        User changedBy,
        Role changedByRole,
        ApplicationStatusHistoryEventType eventType) {

    ApplicationStatusHistory history =
            new ApplicationStatusHistory();
    Instant changedAtInstant = clock.instant();

    history.setApplication(application);
    history.setStatus(status);
    history.setChangedAtInstant(changedAtInstant);
    history.setChangedAt(
            LocalDateTime.ofInstant(changedAtInstant, ZoneOffset.UTC)
    );
    history.setChangedBy(changedBy);
    history.setChangedByRole(changedByRole);
    history.setEventType(eventType);

    statusHistoryRepository.saveAndFlush(history);
}

private void validateWithdrawableStatus(ApplicationStatus status) {
    if (status != ApplicationStatus.APPLIED
            && status != ApplicationStatus.REVIEWING
            && status != ApplicationStatus.INTERVIEW) {

        throw new BusinessConflictException(WITHDRAWAL_CONFLICT_MESSAGE);
    }
}
}
