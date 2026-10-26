package com.yassir.jobflow.dto;

import com.yassir.jobflow.entity.ApplicationStatus;

import java.time.LocalDateTime;

public class ApplicationResponse {

    private Long id;

    private Long jobId;

    private String jobTitle;

    private String companyName;

    private Long candidateId;

    private String candidateName;

    private String candidateEmail;

    private ApplicationStatus status;

    private String cvFileName;

    private LocalDateTime appliedAt;

    public ApplicationResponse(
            Long id,
            Long jobId,
            String jobTitle,
            String companyName,
            Long candidateId,
            String candidateName,
            String candidateEmail,
            ApplicationStatus status,
            String cvFileName,
            LocalDateTime appliedAt) {

        this.id = id;
        this.jobId = jobId;
        this.jobTitle = jobTitle;
        this.companyName = companyName;
        this.candidateId = candidateId;
        this.candidateName = candidateName;
        this.candidateEmail = candidateEmail;
        this.status = status;
        this.cvFileName = cvFileName;
        this.appliedAt = appliedAt;
    }

    public Long getId() {
        return id;
    }

    public Long getJobId() {
        return jobId;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public String getCompanyName() {
        return companyName;
    }

    public Long getCandidateId() {
        return candidateId;
    }

    public String getCandidateName() {
        return candidateName;
    }

    public String getCandidateEmail() {
        return candidateEmail;
    }

    public ApplicationStatus getStatus() {
        return status;
    }

    public String getCvFileName() {
        return cvFileName;
    }

    public LocalDateTime getAppliedAt() {
        return appliedAt;
    }
}