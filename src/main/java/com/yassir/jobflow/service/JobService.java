package com.yassir.jobflow.service;

import com.yassir.jobflow.dto.CreateJobRequest;
import com.yassir.jobflow.dto.JobDetailsResponse;
import com.yassir.jobflow.dto.JobResponse;
import com.yassir.jobflow.dto.PageResponse;
import com.yassir.jobflow.entity.Company;
import com.yassir.jobflow.entity.EmploymentType;
import com.yassir.jobflow.entity.Job;
import com.yassir.jobflow.entity.RecruiterProfile;
import com.yassir.jobflow.entity.Role;
import com.yassir.jobflow.entity.User;
import com.yassir.jobflow.repository.JobRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import com.yassir.jobflow.dto.UpdateJobRequest;
import com.yassir.jobflow.exception.ResourceNotFoundException;

import java.util.List;

@Service
public class JobService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_SIZE = 50;

    private final JobRepository jobRepository;
    private final RecruiterProfileService recruiterProfileService;

    public JobService(
            JobRepository jobRepository,
            RecruiterProfileService recruiterProfileService) {

        this.jobRepository = jobRepository;
        this.recruiterProfileService = recruiterProfileService;
    }

    public Job updateJob(
        Long jobId,
        UpdateJobRequest request) {

    Job job = jobRepository.findById(jobId)
            .orElseThrow(() ->
                    new ResourceNotFoundException("Job not found")
            );

    job.setTitle(request.getTitle());
    job.setDescription(request.getDescription());
    job.setLocation(request.getLocation());
    job.setEmploymentType(request.getEmploymentType());

    return jobRepository.save(job);
}

    public Job createJob(CreateJobRequest request, User recruiter) {

        RecruiterProfile profile =
                recruiterProfileService.getProfileByUserId(recruiter.getId());

        Job job = new Job();

        job.setTitle(request.getTitle());
        job.setDescription(request.getDescription());
        job.setLocation(request.getLocation());
        job.setEmploymentType(request.getEmploymentType());

        job.setCompany(profile.getCompany());
        job.setCreatedBy(recruiter);

        return jobRepository.save(job);
    }

    public List<Job> getActiveJobs() {
        return getActiveJobs(null, null, null);
    }

    public List<Job> getActiveJobs(
            String search,
            String location,
            EmploymentType employmentType) {

        return jobRepository.findActiveJobsForDiscovery(
                toLikePatternOrNull(search),
                toLikePatternOrNull(location),
                employmentType,
                Pageable.unpaged()
        ).getContent();
    }

    public PageResponse<JobResponse> getActiveJobPage(
            String search,
            String location,
            EmploymentType employmentType,
            int page,
            int size) {

        validatePageRequest(page, size);

        Page<Job> jobs = jobRepository.findActiveJobsForDiscovery(
                toLikePatternOrNull(search),
                toLikePatternOrNull(location),
                employmentType,
                PageRequest.of(page, size)
        );

        return PageResponse.from(jobs, this::toJobResponse);
    }

    public PageResponse<JobResponse> getActiveJobPage(
            String search,
            String location,
            EmploymentType employmentType) {

        return getActiveJobPage(
                search,
                location,
                employmentType,
                DEFAULT_PAGE,
                DEFAULT_SIZE
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

    private String toLikePatternOrNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        return "%" + value.trim().toLowerCase() + "%";
    }

    public JobResponse toJobResponse(Job job) {
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

    public JobDetailsResponse toJobDetailsResponse(Job job) {
        Company company = job.getCompany();
        User createdBy = job.getCreatedBy();

        return new JobDetailsResponse(
                job.getId(),
                job.getTitle(),
                job.getDescription(),
                job.getLocation(),
                job.getEmploymentType(),
                job.isActive(),
                job.getCreatedAt(),
                job.getUpdatedAt(),
                company.getId(),
                company.getName(),
                company.getDescription(),
                company.getWebsite(),
                company.getLocation(),
                createdBy.getId(),
                createdBy.getFirstName()
                        + " "
                        + createdBy.getLastName()
        );
    }

    public Job getJobById(Long id) {
        return jobRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Job not found"));
    }

    public Job getJobByIdForUser(Long id, User user) {

        Job job = getJobById(id);

        if (user.getRole() == Role.ADMIN || job.isActive()) {
            return job;
        }

        if (user.getRole() == Role.RECRUITER
                && recruiterOwnsJobCompany(user, job)) {

            return job;
        }

        throw new ResourceNotFoundException("Job not found");
    }

    private boolean recruiterOwnsJobCompany(User recruiter, Job job) {

        RecruiterProfile profile;

        try {
            profile = recruiterProfileService.getProfileByUserId(
                    recruiter.getId()
            );
        } catch (ResourceNotFoundException exception) {
            return false;
        }

        return job.getCompany()
                .getId()
                .equals(profile.getCompany().getId());
    }

    public List<Job> getAllJobs() {
        return jobRepository.findAll();
    }


public Job updateJobActive(Long jobId, boolean active) {

    Job job = jobRepository.findById(jobId)
            .orElseThrow(() ->
                    new ResourceNotFoundException("Job not found")
            );

    job.setActive(active);

    return jobRepository.save(job);
}
}
