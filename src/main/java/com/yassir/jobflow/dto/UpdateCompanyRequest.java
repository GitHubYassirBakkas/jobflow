package com.yassir.jobflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class UpdateCompanyRequest {

    @NotBlank(message = "Company name is required")
    @Size(
        max = 255,
        message = "Company name must not exceed 255 characters"
    )
    private String name;

    @Size(
        max = 2000,
        message = "Description must not exceed 2000 characters"
    )
    private String description;

    @Pattern(
        regexp = "(?i)^(https?://(?:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\\.)+[a-z]{2,63}(?::\\d{1,5})?(?:[/?#][^\\s]*)?)?$",
        message = "Website must be a valid HTTP or HTTPS URL"
    )
    private String website;

    @NotBlank(message = "Location is required")
    @Size(
        max = 255,
        message = "Location must not exceed 255 characters"
    )
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