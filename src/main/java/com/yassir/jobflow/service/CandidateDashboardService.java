package com.yassir.jobflow.service;

import com.yassir.jobflow.dto.ApplicationResponse;
import com.yassir.jobflow.dto.CandidateDashboardResponse;
import com.yassir.jobflow.dto.InterviewResponse;
import com.yassir.jobflow.entity.Application;
import com.yassir.jobflow.entity.ApplicationStatus;
import com.yassir.jobflow.entity.Interview;
import com.yassir.jobflow.entity.InterviewStatus;
import com.yassir.jobflow.entity.User;
import com.yassir.jobflow.repository.ApplicationRepository;
import com.yassir.jobflow.repository.InterviewRepository;
import com.yassir.jobflow.repository.SavedJobRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CandidateDashboardService {

    private static final int RECENT_APPLICATION_LIMIT = 5;
    private static final int NEXT_INTERVIEW_LIMIT = 3;
    private static final List<ApplicationStatus> ACTIVE_APPLICATION_STATUSES =
            List.of(
                    ApplicationStatus.APPLIED,
                    ApplicationStatus.REVIEWING,
                    ApplicationStatus.INTERVIEW
            );

    private final ApplicationRepository applicationRepository;
    private final SavedJobRepository savedJobRepository;
    private final InterviewRepository interviewRepository;
    private final ApplicationService applicationService;

    public CandidateDashboardService(
            ApplicationRepository applicationRepository,
            SavedJobRepository savedJobRepository,
            InterviewRepository interviewRepository,
            ApplicationService applicationService) {

        this.applicationRepository = applicationRepository;
        this.savedJobRepository = savedJobRepository;
        this.interviewRepository = interviewRepository;
        this.applicationService = applicationService;
    }

    @Transactional(readOnly = true)
    public CandidateDashboardResponse getDashboard(User candidate) {
        Long candidateId = candidate.getId();
        LocalDateTime now = LocalDateTime.now();

        long totalApplications =
                applicationRepository.countByCandidateId(candidateId);

        long activeApplications =
                applicationRepository.countByCandidateIdAndStatusIn(
                        candidateId,
                        ACTIVE_APPLICATION_STATUSES
                );

        long withdrawnApplications =
                applicationRepository.countByCandidateIdAndStatus(
                        candidateId,
                        ApplicationStatus.WITHDRAWN
                );

        long upcomingInterviews =
                interviewRepository
                        .countByApplicationCandidateIdAndStatusAndScheduledAtAfter(
                                candidateId,
                                InterviewStatus.SCHEDULED,
                                now
                        );

        long savedJobs =
                savedJobRepository.countByCandidateIdAndJobActiveTrue(candidateId);

        List<ApplicationResponse> recentApplications =
                applicationRepository
                        .findByCandidateIdOrderByAppliedAtDescIdDesc(
                                candidateId,
                                PageRequest.of(0, RECENT_APPLICATION_LIMIT)
                        )
                        .getContent()
                        .stream()
                        .map(applicationService::toApplicationResponse)
                        .toList();

        List<InterviewResponse> nextInterviews =
                interviewRepository
                        .findByApplicationCandidateIdAndStatusAndScheduledAtAfterOrderByScheduledAtAscIdAsc(
                                candidateId,
                                InterviewStatus.SCHEDULED,
                                now,
                                PageRequest.of(0, NEXT_INTERVIEW_LIMIT)
                        )
                        .getContent()
                        .stream()
                        .map(this::toInterviewResponse)
                        .toList();

        return new CandidateDashboardResponse(
                totalApplications,
                activeApplications,
                withdrawnApplications,
                upcomingInterviews,
                savedJobs,
                recentApplications,
                nextInterviews
        );
    }

    private InterviewResponse toInterviewResponse(Interview interview) {
        Application application = interview.getApplication();
        User candidate = application.getCandidate();

        return new InterviewResponse(
                interview.getId(),
                application.getId(),
                application.getJob().getId(),
                application.getJob().getTitle(),
                candidate.getId(),
                candidate.getFirstName()
                        + " "
                        + candidate.getLastName(),
                candidate.getEmail(),
                interview.getScheduledAt(),
                interview.getType(),
                interview.getMeetingLink(),
                interview.getLocation(),
                interview.getNotes(),
                interview.getStatus()
        );
    }
}
