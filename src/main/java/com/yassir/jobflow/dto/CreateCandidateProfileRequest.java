package com.yassir.jobflow.dto;

import jakarta.validation.constraints.Size;

public class CreateCandidateProfileRequest {

    @Size(max = 255, message = "Phone must not exceed 255 characters")
    private String phone;

    @Size(max = 255, message = "Location must not exceed 255 characters")
    private String location;

    @Size(max = 2000, message = "Bio must not exceed 2000 characters")
    private String bio;

    @Size(max = 1000, message = "Skills must not exceed 1000 characters")
    private String skills;

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getSkills() {
        return skills;
    }

    public void setSkills(String skills) {
        this.skills = skills;
    }
}
