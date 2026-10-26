package com.yassir.jobflow.controller;

import com.yassir.jobflow.dto.CandidateProfileResponse;
import com.yassir.jobflow.dto.CreateCandidateProfileRequest;
import com.yassir.jobflow.dto.UpdateCandidateProfileRequest;
import com.yassir.jobflow.entity.CandidateProfile;
import com.yassir.jobflow.entity.User;
import com.yassir.jobflow.service.CandidateProfileService;
import com.yassir.jobflow.service.UserService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

@RestController
@RequestMapping("/api/candidate/profile")
public class CandidateProfileController {

    private final CandidateProfileService candidateProfileService;
    private final UserService userService;

    public CandidateProfileController(
            CandidateProfileService candidateProfileService,
            UserService userService) {

        this.candidateProfileService = candidateProfileService;
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<CandidateProfileResponse> createProfile(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateCandidateProfileRequest request) {

        User user = getAuthenticatedUser(jwt);

        CandidateProfile profile =
                candidateProfileService.createProfile(user, request);

        CandidateProfileResponse response =
                candidateProfileService.toResponse(profile);

        return ResponseEntity.ok(response);
    }

    @PutMapping
    public ResponseEntity<CandidateProfileResponse> upsertProfile(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateCandidateProfileRequest request) {

        User user = getAuthenticatedUser(jwt);

        CandidateProfile profile =
                candidateProfileService.upsertProfile(user, request);

        return ResponseEntity.ok(candidateProfileService.toResponse(profile));
    }

    @GetMapping("/me")
public ResponseEntity<CandidateProfileResponse> getMyProfile(
        @AuthenticationPrincipal Jwt jwt) {

    User user = getAuthenticatedUser(jwt);

    CandidateProfile profile =
            candidateProfileService.getProfileByUserId(user.getId());

    CandidateProfileResponse response =
            candidateProfileService.toResponse(profile);

    return ResponseEntity.ok(response);
}

@PostMapping("/cv")
public ResponseEntity<String> uploadCv(
        @AuthenticationPrincipal Jwt jwt,
        @RequestParam("file") MultipartFile file) {

    User user = getAuthenticatedUser(jwt);

    CandidateProfile profile =
            candidateProfileService.uploadCv(user.getId(), file);

    return ResponseEntity.ok(
            "CV uploaded successfully: " + profile.getCvFileName()
    );
}

@GetMapping("/cv")
public ResponseEntity<Resource> getMyCv(
        @AuthenticationPrincipal Jwt jwt) {

    User user = getAuthenticatedUser(jwt);

    CandidateProfile profile =
            candidateProfileService.getProfileByUserId(user.getId());

    Resource resource =
            candidateProfileService.getCvFile(user.getId());

    return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header(
                    HttpHeaders.CONTENT_DISPOSITION,
                    "inline; filename=\"" + profile.getCvFileName() + "\""
            )
            .body(resource);
}

@DeleteMapping("/cv")
public ResponseEntity<Void> deleteMyCv(
        @AuthenticationPrincipal Jwt jwt) {

    User user = getAuthenticatedUser(jwt);

    candidateProfileService.deleteCv(user.getId());

    return ResponseEntity.noContent().build();
}

private User getAuthenticatedUser(Jwt jwt) {

    String email = jwt.getSubject();

    return userService.getUserByEmail(email)
            .orElseThrow(() ->
                    new IllegalArgumentException("User not found"));
}

}
