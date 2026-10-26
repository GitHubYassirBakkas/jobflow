package com.yassir.jobflow.service;

import com.yassir.jobflow.dto.PageResponse;
import com.yassir.jobflow.dto.SavedJobResponse;
import com.yassir.jobflow.entity.Job;
import com.yassir.jobflow.entity.Role;
import com.yassir.jobflow.entity.SavedJob;
import com.yassir.jobflow.entity.User;
import com.yassir.jobflow.exception.ForbiddenException;
import com.yassir.jobflow.exception.ResourceNotFoundException;
import com.yassir.jobflow.repository.JobRepository;
import com.yassir.jobflow.repository.SavedJobRepository;
import com.yassir.jobflow.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SavedJobService {

    private static final int MAX_SIZE = 50;

    private final SavedJobRepository savedJobRepository;
    private final UserRepository userRepository;
    private final JobRepository jobRepository;
    private final JobService jobService;

    public SavedJobService(
            SavedJobRepository savedJobRepository,
            UserRepository userRepository,
            JobRepository jobRepository,
            JobService jobService) {

        this.savedJobRepository = savedJobRepository;
        this.userRepository = userRepository;
        this.jobRepository = jobRepository;
        this.jobService = jobService;
    }

    public SavedJobResponse saveJob(Long candidateId, Long jobId) {
        User candidate = getCandidate(candidateId);
        Job job = getActiveJob(jobId);

        return savedJobRepository
                .findByCandidateIdAndJobId(candidate.getId(), job.getId())
                .map(this::toResponse)
                .orElseGet(() -> createSavedJob(candidate, job));
    }

    private SavedJobResponse createSavedJob(User candidate, Job job) {
        SavedJob savedJob = new SavedJob();
        savedJob.setCandidate(candidate);
        savedJob.setJob(job);

        try {
            return toResponse(savedJobRepository.saveAndFlush(savedJob));
        } catch (DataIntegrityViolationException exception) {
            return savedJobRepository
                    .findByCandidateIdAndJobId(candidate.getId(), job.getId())
                    .map(this::toResponse)
                    .orElseThrow(() -> exception);
        }
    }

    public void unsaveJob(Long candidateId, Long jobId) {
        getCandidate(candidateId);

        savedJobRepository.deleteByCandidateIdAndJobId(candidateId, jobId);
    }

    @Transactional(readOnly = true)
    public PageResponse<SavedJobResponse> getSavedJobs(
            Long candidateId,
            int page,
            int size) {

        getCandidate(candidateId);
        validatePageRequest(page, size);

        Page<SavedJob> savedJobs =
                savedJobRepository
                        .findByCandidateIdAndJobActiveTrueOrderBySavedAtDescIdDesc(
                                candidateId,
                                PageRequest.of(page, size)
                        );

        return PageResponse.from(savedJobs, this::toResponse);
    }

    @Transactional(readOnly = true)
    public List<Long> getActiveSavedJobIds(Long candidateId) {
        getCandidate(candidateId);

        return savedJobRepository.findActiveSavedJobIdsByCandidateId(candidateId);
    }

    private User getCandidate(Long candidateId) {
        User candidate = userRepository.findById(candidateId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Candidate not found"));

        if (candidate.getRole() != Role.CANDIDATE) {
            throw new ForbiddenException("User is not a candidate");
        }

        return candidate;
    }

    private Job getActiveJob(Long jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Job not found"));

        if (!job.isActive()) {
            throw new IllegalArgumentException("This job is no longer active");
        }

        return job;
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

    private SavedJobResponse toResponse(SavedJob savedJob) {
        return new SavedJobResponse(
                savedJob.getId(),
                jobService.toJobResponse(savedJob.getJob()),
                savedJob.getSavedAt()
        );
    }
}
