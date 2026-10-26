package com.yassir.jobflow.dto;

import com.yassir.jobflow.entity.InterviewStatus;
import jakarta.validation.constraints.NotNull;

public class UpdateInterviewStatusRequest {

    @NotNull(message = "Interview status is required")
    private InterviewStatus status;

    public InterviewStatus getStatus() {
        return status;
    }

    public void setStatus(InterviewStatus status) {
        this.status = status;
    }
}