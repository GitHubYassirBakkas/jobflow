package com.yassir.jobflow.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Entity
@Table(
        name = "application_status_history",
        indexes = {
                @Index(
                        name = "idx_application_status_history_application_changed_at_id",
                        columnList = "application_id, changed_at ASC, id ASC"
                )
        }
)
public class ApplicationStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApplicationStatus status;

    @Column(nullable = false)
    private LocalDateTime changedAt;

    private Instant changedAtInstant;

    @ManyToOne
    @JoinColumn(name = "changed_by")
    private User changedBy;

    @Enumerated(EnumType.STRING)
    private Role changedByRole;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ApplicationStatusHistoryEventType eventType;

    @PrePersist
    protected void onCreate() {
        if (changedAt == null && changedAtInstant != null) {
            changedAt = LocalDateTime.ofInstant(
                    changedAtInstant,
                    ZoneOffset.UTC
            );
        }
    }

    public Long getId() {
        return id;
    }

    public Application getApplication() {
        return application;
    }

    public void setApplication(Application application) {
        this.application = application;
    }

    public ApplicationStatus getStatus() {
        return status;
    }

    public void setStatus(ApplicationStatus status) {
        this.status = status;
    }

    public LocalDateTime getChangedAt() {
        return changedAt;
    }

    public void setChangedAt(LocalDateTime changedAt) {
        this.changedAt = changedAt;
    }

    public Instant getChangedAtInstant() {
        return changedAtInstant;
    }

    public void setChangedAtInstant(Instant changedAtInstant) {
        this.changedAtInstant = changedAtInstant;
    }

    public User getChangedBy() {
        return changedBy;
    }

    public void setChangedBy(User changedBy) {
        this.changedBy = changedBy;
    }

    public Role getChangedByRole() {
        return changedByRole;
    }

    public void setChangedByRole(Role changedByRole) {
        this.changedByRole = changedByRole;
    }

    public ApplicationStatusHistoryEventType getEventType() {
        return eventType;
    }

    public void setEventType(ApplicationStatusHistoryEventType eventType) {
        this.eventType = eventType;
    }
}
