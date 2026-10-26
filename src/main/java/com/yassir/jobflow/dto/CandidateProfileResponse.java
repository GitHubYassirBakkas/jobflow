package com.yassir.jobflow.dto;

import java.time.LocalDateTime;

public class CandidateProfileResponse {

    private Long id;
    private Long userId;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String location;
    private String bio;
    private String skills;
    private LocalDateTime createdAt;
    private String cvFileName;
    private int profileCompleteness;

   public CandidateProfileResponse(
        Long id,
        Long userId,
        String firstName,
        String lastName,
        String email,
        String phone,
        String location,
        String bio,
        String skills,
        String cvFileName,
        LocalDateTime createdAt,
        int profileCompleteness) {

    this.id = id;
    this.userId = userId;
    this.firstName = firstName;
    this.lastName = lastName;
    this.email = email;
    this.phone = phone;
    this.location = location;
    this.bio = bio;
    this.skills = skills;
    this.cvFileName = cvFileName;
    this.createdAt = createdAt;
    this.profileCompleteness = profileCompleteness;
}

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getLocation() {
        return location;
    }

    public String getBio() {
        return bio;
    }

    public String getSkills() {
        return skills;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getCvFileName() {
        return cvFileName;
    }

    public int getProfileCompleteness() {
        return profileCompleteness;
    }

}
