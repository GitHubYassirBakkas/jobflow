package com.yassir.jobflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

public class UpdateCompanyRequest {

    @NotBlank(message = "Company name is required")
    private String name;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;
    
    @Pattern(
        regexp = "(?i)^(https?://\\S+)?$",
        message = "Website must be an HTTP or HTTPS URL"
    )
    private String website;
    
    @NotBlank(message = "Location is required")
    private String location;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }
}