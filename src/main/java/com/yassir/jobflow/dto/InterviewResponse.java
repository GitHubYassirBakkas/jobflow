package com.yassir.jobflow.dto;

import com.yassir.jobflow.entity.InterviewStatus;
import com.yassir.jobflow.entity.InterviewType;

import java.time.LocalDateTime;

public class InterviewResponse {

    private Long id;
    private Long applicationId;
    private Long jobId;
    private String jobTitle;
    private Long candidateId;
    private String candidateName;
    private String candidateEmail;
    private LocalDateTime scheduledAt;
    private InterviewType type;
    private String meetingLink;
    private String location;
    private String notes;
    private InterviewStatus status;

    public InterviewResponse(
            Long id,
            Long applicationId,
            Long jobId,
            String jobTitle,
            Long candidateId,
            String candidateName,
            String candidateEmail,
            LocalDateTime scheduledAt,
            InterviewType type,
            String meetingLink,
            String location,
            String notes,
            InterviewStatus status) {

        this.id = id;
        this.applicationId = applicationId;
        this.jobId = jobId;
        this.jobTitle = jobTitle;
        this.candidateId = candidateId;
        this.candidateName = candidateName;
        this.candidateEmail = candidateEmail;
        this.scheduledAt = scheduledAt;
        this.type = type;
        this.meetingLink = meetingLink;
        this.location = location;
        this.notes = notes;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public Long getApplicationId() {
        return applicationId;
    }

    public Long getJobId() {
        return jobId;
    }

    public String getJobTitle() {
        return jobTitle;
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

    public LocalDateTime getScheduledAt() {
        return scheduledAt;
    }

    public InterviewType getType() {
        return type;
    }

    public String getMeetingLink() {
        return meetingLink;
    }

    public String getLocation() {
        return location;
    }

    public String getNotes() {
        return notes;
    }

    public InterviewStatus getStatus() {
        return status;
    }
}