package com.yassir.jobflow.dto;

import com.yassir.jobflow.entity.EmploymentType;
import java.time.LocalDateTime;

public class JobResponse {

    private Long id;
    private String title;
    private String description;
    private String location;
    private EmploymentType employmentType;
    private boolean active;

    private Long companyId;
    private String companyName;

    private Long createdById;
    private String createdByName;

    private LocalDateTime createdAt;

    public JobResponse(
            Long id,
            String title,
            String description,
            String location,
            EmploymentType employmentType,
            boolean active,
            Long companyId,
            String companyName,
            Long createdById,
            String createdByName,
            LocalDateTime createdAt) {

        this.id = id;
        this.title = title;
        this.description = description;
        this.location = location;
        this.employmentType = employmentType;
        this.active = active;
        this.companyId = companyId;
        this.companyName = companyName;
        this.createdById = createdById;
        this.createdByName = createdByName;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getLocation() {
        return location;
    }

    public EmploymentType getEmploymentType() {
        return employmentType;
    }

    public boolean isActive() {
        return active;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public String getCompanyName() {
        return companyName;
    }

    public Long getCreatedById() {
        return createdById;
    }

    public String getCreatedByName() {
        return createdByName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}