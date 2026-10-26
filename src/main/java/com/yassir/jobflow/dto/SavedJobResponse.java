package com.yassir.jobflow.dto;

import java.time.LocalDateTime;

public class SavedJobResponse {

    private Long id;

    private JobResponse job;

    private LocalDateTime savedAt;

    public SavedJobResponse(
            Long id,
            JobResponse job,
            LocalDateTime savedAt) {

        this.id = id;
        this.job = job;
        this.savedAt = savedAt;
    }

    public Long getId() {
        return id;
    }

    public JobResponse getJob() {
        return job;
    }

    public LocalDateTime getSavedAt() {
        return savedAt;
    }
}
