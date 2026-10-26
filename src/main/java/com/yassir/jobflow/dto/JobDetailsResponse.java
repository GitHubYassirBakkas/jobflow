package com.yassir.jobflow.dto;

import com.yassir.jobflow.entity.EmploymentType;

import java.time.LocalDateTime;

public class JobDetailsResponse {

    private Long id;
    private String title;
    private String description;
    private String location;
    private EmploymentType employmentType;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Long companyId;
    private String companyName;
    private String companyDescription;
    private String companyWebsite;
    private String companyLocation;

    private Long createdById;
    private String createdByName;

    public JobDetailsResponse(
            Long id,
            String title,
            String description,
            String location,
            EmploymentType employmentType,
            boolean active,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            Long companyId,
            String companyName,
            String companyDescription,
            String companyWebsite,
            String companyLocation,
            Long createdById,
            String createdByName) {

        this.id = id;
        this.title = title;
        this.description = description;
        this.location = location;
        this.employmentType = employmentType;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.companyId = companyId;
        this.companyName = companyName;
        this.companyDescription = companyDescription;
        this.companyWebsite = companyWebsite;
        this.companyLocation = companyLocation;
        this.createdById = createdById;
        this.createdByName = createdByName;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public String getCompanyName() {
        return companyName;
    }

    public String getCompanyDescription() {
        return companyDescription;
    }

    public String getCompanyWebsite() {
        return companyWebsite;
    }

    public String getCompanyLocation() {
        return companyLocation;
    }

    public Long getCreatedById() {
        return createdById;
    }

    public String getCreatedByName() {
        return createdByName;
    }
}
