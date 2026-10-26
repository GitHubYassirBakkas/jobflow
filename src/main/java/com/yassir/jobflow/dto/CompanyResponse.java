package com.yassir.jobflow.dto;

import java.time.LocalDateTime;

public class CompanyResponse {

    private Long id;
    private String name;
    private String description;
    private String website;
    private String location;
    private LocalDateTime createdAt;

    public CompanyResponse(
            Long id,
            String name,
            String description,
            String website,
            String location,
            LocalDateTime createdAt) {

        this.id = id;
        this.name = name;
        this.description = description;
        this.website = website;
        this.location = location;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getWebsite() {
        return website;
    }

    public String getLocation() {
        return location;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}