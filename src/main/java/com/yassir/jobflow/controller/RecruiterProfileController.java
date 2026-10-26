package com.yassir.jobflow.controller;

import com.yassir.jobflow.dto.CreateRecruiterProfileRequest;
import com.yassir.jobflow.dto.RecruiterProfileResponse;
import com.yassir.jobflow.entity.Company;
import com.yassir.jobflow.entity.RecruiterProfile;
import com.yassir.jobflow.entity.User;
import com.yassir.jobflow.service.CompanyService;
import com.yassir.jobflow.service.RecruiterProfileService;
import com.yassir.jobflow.service.UserService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/recruiter/profile")
public class RecruiterProfileController {

    private final RecruiterProfileService recruiterProfileService;
    private final UserService userService;
    private final CompanyService companyService;

    public RecruiterProfileController(
            RecruiterProfileService recruiterProfileService,
            UserService userService,
            CompanyService companyService) {

        this.recruiterProfileService = recruiterProfileService;
        this.userService = userService;
        this.companyService = companyService;
    }

    @PostMapping
    public ResponseEntity<RecruiterProfileResponse> createProfile(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateRecruiterProfileRequest request) {

        String email = jwt.getSubject();

        User user = userService.getUserByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found"));

        Company company =
                companyService.getCompanyById(request.getCompanyId());

        RecruiterProfile profile =
                recruiterProfileService.createProfile(
                        user,
                        company,
                        request.getJobTitle()
                );

        RecruiterProfileResponse response =
                new RecruiterProfileResponse(
                        profile.getId(),
                        user.getId(),
                        user.getFirstName(),
                        user.getLastName(),
                        user.getEmail(),
                        profile.getJobTitle(),
                        company.getId(),
                        company.getName()
                );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
public ResponseEntity<RecruiterProfileResponse> getMyProfile(
        @AuthenticationPrincipal Jwt jwt) {

    String email = jwt.getSubject();

    User user = userService.getUserByEmail(email)
            .orElseThrow(() ->
                    new IllegalArgumentException("User not found"));

    RecruiterProfile profile =
            recruiterProfileService.getProfileByUserId(user.getId());

    Company company = profile.getCompany();

    RecruiterProfileResponse response =
            new RecruiterProfileResponse(
                    profile.getId(),
                    user.getId(),
                    user.getFirstName(),
                    user.getLastName(),
                    user.getEmail(),
                    profile.getJobTitle(),
                    company.getId(),
                    company.getName()
            );

    return ResponseEntity.ok(response);
}
}