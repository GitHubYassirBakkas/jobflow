package com.yassir.jobflow.dto;

import com.yassir.jobflow.entity.InterviewType;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public class ScheduleInterviewRequest {

    @NotNull(message = "Interview date is required")
    @Future(message = "Interview date must be in the future")
    private LocalDateTime scheduledAt;

    @NotNull(message = "Interview type is required")
    private InterviewType type;

    private String meetingLink;

    private String location;

    @Size(max = 2000, message = "Notes cannot exceed 2000 characters")
    private String notes;

    public LocalDateTime getScheduledAt() {
        return scheduledAt;
    }

    public void setScheduledAt(LocalDateTime scheduledAt) {
        this.scheduledAt = scheduledAt;
    }

    public InterviewType getType() {
        return type;
    }

    public void setType(InterviewType type) {
        this.type = type;
    }

    public String getMeetingLink() {
        return meetingLink;
    }

    public void setMeetingLink(String meetingLink) {
        this.meetingLink = meetingLink;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}