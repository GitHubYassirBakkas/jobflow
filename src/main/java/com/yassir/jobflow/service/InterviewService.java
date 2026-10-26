package com.yassir.jobflow.service;


import com.yassir.jobflow.repository.InterviewRepository;
import org.springframework.stereotype.Service;
import com.yassir.jobflow.dto.ScheduleInterviewRequest;
import com.yassir.jobflow.entity.Application;
import com.yassir.jobflow.entity.ApplicationStatus;
import com.yassir.jobflow.entity.Interview;
import com.yassir.jobflow.entity.InterviewStatus;
import com.yassir.jobflow.entity.RecruiterProfile;
import com.yassir.jobflow.entity.User;
import org.springframework.transaction.annotation.Transactional;
import com.yassir.jobflow.exception.ResourceNotFoundException;
import com.yassir.jobflow.exception.ForbiddenException;
import com.yassir.jobflow.exception.BusinessConflictException;
import java.util.List;

@Service
public class InterviewService {

    private final InterviewRepository interviewRepository;
    private final ApplicationService applicationService;
    private final RecruiterProfileService recruiterProfileService;

    public InterviewService(
            InterviewRepository interviewRepository,
            ApplicationService applicationService,
            RecruiterProfileService recruiterProfileService) {

        this.interviewRepository = interviewRepository;
        this.applicationService = applicationService;
        this.recruiterProfileService = recruiterProfileService;
    }
    @Transactional
    public Interview scheduleInterview(
        Long applicationId,
        ScheduleInterviewRequest request,
        User recruiter) {

    Application application =
            applicationService.getApplicationByIdForUpdate(applicationId);

    RecruiterProfile profile =
            recruiterProfileService.getProfileByUserId(recruiter.getId());

    if (!application.getJob().getCompany().getId()
            .equals(profile.getCompany().getId())) {

        throw new ForbiddenException(
                "You are not allowed to schedule an interview for this application"
        );
    }

    if (interviewRepository.existsByApplicationId(applicationId)) {
        throw new IllegalArgumentException(
                "An interview already exists for this application"
        );
    }

    if (application.getStatus() == ApplicationStatus.WITHDRAWN) {
        throw new BusinessConflictException(
                "Cannot schedule an interview for a withdrawn application"
        );
    }

    Interview interview = new Interview();

    interview.setApplication(application);
    interview.setScheduledAt(request.getScheduledAt());
    interview.setType(request.getType());
    interview.setMeetingLink(request.getMeetingLink());
    interview.setLocation(request.getLocation());
    interview.setNotes(request.getNotes());
    interview.setStatus(InterviewStatus.SCHEDULED);

    applicationService.updateApplicationStatusIfChanged(
            application,
            ApplicationStatus.INTERVIEW,
            recruiter
    );

    return interviewRepository.save(interview);
}

public Interview getInterviewByApplicationForCandidate(
        Long applicationId,
        User candidate) {

    Application application =
            applicationService.getApplicationById(applicationId);

    if (!application.getCandidate().getId()
            .equals(candidate.getId())) {

        throw new ForbiddenException(
                "You are not allowed to view this interview"
        );
    }

    return interviewRepository.findByApplicationId(applicationId)
        .orElseThrow(() ->
                new ResourceNotFoundException("Interview not found"));
}

public Interview updateInterviewStatus(
        Long interviewId,
        InterviewStatus newStatus,
        User recruiter) {

    Interview interview = interviewRepository.findById(interviewId)
        .orElseThrow(() ->
                new ResourceNotFoundException("Interview not found"));

    RecruiterProfile profile =
            recruiterProfileService.getProfileByUserId(recruiter.getId());

    if (!interview.getApplication()
            .getJob()
            .getCompany()
            .getId()
            .equals(profile.getCompany().getId())) {

        throw new ForbiddenException(
                "You are not allowed to update this interview");
    }

    interview.setStatus(newStatus);

    return interviewRepository.save(interview);
}

public List<Interview> getInterviewsForRecruiter(User recruiter) {

    RecruiterProfile profile =
            recruiterProfileService.getProfileByUserId(recruiter.getId());

    Long companyId = profile.getCompany().getId();

    return interviewRepository.findByApplicationJobCompanyId(companyId);
}

public List<Interview> getInterviewsForCandidate(User candidate) {

    return interviewRepository
            .findByApplicationCandidateId(candidate.getId());
}

public Interview updateInterviewStatusForAdmin(
        Long interviewId,
        InterviewStatus newStatus) {

   Interview interview = interviewRepository.findById(interviewId)
        .orElseThrow(() ->
                new ResourceNotFoundException("Interview not found")
        );

    interview.setStatus(newStatus);

    return interviewRepository.save(interview);
}

public List<Interview> getAllInterviews() {
    return interviewRepository.findAll();
}
}
