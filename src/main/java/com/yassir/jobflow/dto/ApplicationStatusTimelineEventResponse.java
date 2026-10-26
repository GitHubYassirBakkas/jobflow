package com.yassir.jobflow.dto;

import com.yassir.jobflow.entity.ApplicationStatus;
import com.yassir.jobflow.entity.ApplicationStatusHistoryEventType;

import java.time.Instant;
import java.time.LocalDateTime;

public class ApplicationStatusTimelineEventResponse {

    private final ApplicationStatus status;

    private final String changedAt;

    private final ApplicationStatusHistoryEventType eventType;

    public ApplicationStatusTimelineEventResponse(
            ApplicationStatus status,
            LocalDateTime changedAt,
            Instant changedAtInstant,
            ApplicationStatusHistoryEventType eventType) {

        this.status = status;
        this.changedAt = formatChangedAt(changedAt, changedAtInstant);
        this.eventType = eventType;
    }

    private static String formatChangedAt(
            LocalDateTime changedAt,
            Instant changedAtInstant) {

        if (changedAtInstant != null) {
            return changedAtInstant.toString();
        }

        return changedAt != null ? changedAt.toString() : null;
    }

    public ApplicationStatus getStatus() {
        return status;
    }

    public String getChangedAt() {
        return changedAt;
    }

    public ApplicationStatusHistoryEventType getEventType() {
        return eventType;
    }
}
