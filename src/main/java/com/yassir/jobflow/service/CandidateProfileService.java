package com.yassir.jobflow.service;

import com.yassir.jobflow.dto.CreateCandidateProfileRequest;
import com.yassir.jobflow.dto.CandidateProfileResponse;
import com.yassir.jobflow.dto.UpdateCandidateProfileRequest;
import com.yassir.jobflow.entity.CandidateProfile;
import com.yassir.jobflow.entity.User;
import com.yassir.jobflow.repository.CandidateProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;

import java.net.MalformedURLException;
import com.yassir.jobflow.exception.ResourceNotFoundException;

@Service
public class CandidateProfileService {

    private static final Logger logger =
            LoggerFactory.getLogger(CandidateProfileService.class);

    private final CandidateProfileRepository candidateProfileRepository;
    private final PdfFileValidator pdfFileValidator;

    public CandidateProfileService(
            CandidateProfileRepository candidateProfileRepository,
            PdfFileValidator pdfFileValidator) {

        this.candidateProfileRepository = candidateProfileRepository;
        this.pdfFileValidator = pdfFileValidator;
    }

    public CandidateProfile createProfile(
            User user,
            CreateCandidateProfileRequest request) {

        if (candidateProfileRepository.findByUserId(user.getId()).isPresent()) {
            throw new IllegalArgumentException(
                    "Candidate profile already exists"
            );
        }

        CandidateProfile profile = new CandidateProfile();

        profile.setUser(user);
        applyProfileFields(
                profile,
                request.getPhone(),
                request.getLocation(),
                request.getBio(),
                request.getSkills()
        );

        return candidateProfileRepository.save(profile);
    }

    public CandidateProfile upsertProfile(
            User user,
            UpdateCandidateProfileRequest request) {

        CandidateProfile profile =
                candidateProfileRepository.findByUserId(user.getId())
                        .orElseGet(() -> {
                            CandidateProfile newProfile =
                                    new CandidateProfile();
                            newProfile.setUser(user);
                            return newProfile;
                        });

        applyProfileFields(
                profile,
                request.getPhone(),
                request.getLocation(),
                request.getBio(),
                request.getSkills()
        );

        return candidateProfileRepository.save(profile);
    }

    private void applyProfileFields(
            CandidateProfile profile,
            String phone,
            String location,
            String bio,
            String skills) {

        profile.setPhone(phone);
        profile.setLocation(location);
        profile.setBio(bio);
        profile.setSkills(skills);
    }

    public CandidateProfileResponse toResponse(CandidateProfile profile) {

        User user = profile.getUser();

        return new CandidateProfileResponse(
                profile.getId(),
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                profile.getPhone(),
                profile.getLocation(),
                profile.getBio(),
                profile.getSkills(),
                profile.getCvFileName(),
                profile.getCreatedAt(),
                calculateProfileCompleteness(profile)
        );
    }

    private int calculateProfileCompleteness(CandidateProfile profile) {

        int completeness = 0;

        if (hasText(profile.getPhone())) {
            completeness += 20;
        }

        if (hasText(profile.getLocation())) {
            completeness += 20;
        }

        if (hasText(profile.getBio())) {
            completeness += 20;
        }

        if (hasText(profile.getSkills())) {
            completeness += 20;
        }

        if (hasText(profile.getCvFileName())
                && hasText(profile.getCvFilePath())) {
            completeness += 20;
        }

        return completeness;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    public CandidateProfile getProfileByUserId(Long userId) {
    return candidateProfileRepository.findByUserId(userId)
            .orElseThrow(() ->
                    new ResourceNotFoundException(
                            "Candidate profile not found"
                    ));
}

public CandidateProfile uploadCv(Long userId, MultipartFile file) {

    pdfFileValidator.validatePdf(file);

    CandidateProfile profile = getProfileByUserId(userId);

    Path newFilePath = null;
    String oldCvFileName = profile.getCvFileName();
    String oldCvFilePath = profile.getCvFilePath();

    try {
        Path uploadDirectory = Paths.get("uploads", "cvs");
        Files.createDirectories(uploadDirectory);

        String originalFileName =
                pdfFileValidator.getSafeOriginalFilename(file);
        String storedFileName =
                java.util.UUID.randomUUID() + ".pdf";

        newFilePath = uploadDirectory.resolve(storedFileName);

        Files.copy(
                file.getInputStream(),
                newFilePath,
                StandardCopyOption.REPLACE_EXISTING
        );

        profile.setCvFileName(originalFileName);
        profile.setCvFilePath(newFilePath.toString());

        CandidateProfile savedProfile =
                candidateProfileRepository.save(profile);

        deleteProfileCvQuietly(oldCvFilePath, newFilePath);

        return savedProfile;

    } catch (Exception e) {

        profile.setCvFileName(oldCvFileName);
        profile.setCvFilePath(oldCvFilePath);

        if (newFilePath != null) {
            try {
                Files.deleteIfExists(newFilePath);
            } catch (IOException cleanupException) {
                e.addSuppressed(cleanupException);
            }
        }

        if (e instanceof IllegalArgumentException) {
            throw (IllegalArgumentException) e;
        }

        throw new RuntimeException(
                "Failed to store CV file",
                e
        );
    }
}

public void deleteCv(Long userId) {

    CandidateProfile profile = getProfileByUserId(userId);

    String oldCvFilePath = profile.getCvFilePath();

    if (!hasText(profile.getCvFileName())
            && !hasText(profile.getCvFilePath())) {
        return;
    }

    profile.setCvFileName(null);
    profile.setCvFilePath(null);

    candidateProfileRepository.save(profile);

    deleteProfileCvQuietly(oldCvFilePath, null);
}

private void deleteProfileCvQuietly(
        String cvFilePath,
        Path replacementCvPath) {

    if (!hasText(cvFilePath)) {
        return;
    }

    Path oldPath = Paths.get(cvFilePath);

    if (replacementCvPath != null && oldPath.equals(replacementCvPath)) {
        return;
    }

    try {
        Files.deleteIfExists(oldPath);
    } catch (IOException e) {
        logger.warn("Failed to delete profile CV file after metadata update", e);
    }
}


public Resource getCvFile(Long userId) {

    CandidateProfile profile = getProfileByUserId(userId);

    if (profile.getCvFilePath() == null ||
            profile.getCvFilePath().isBlank()) {
        throw new IllegalArgumentException("Candidate has no CV");
    }

    try {
        Path path = Paths.get(profile.getCvFilePath());

        Resource resource = new UrlResource(path.toUri());

        if (!resource.exists() || !resource.isReadable()) {
            throw new IllegalArgumentException("CV file not found");
        }

        return resource;

    } catch (MalformedURLException e) {
        throw new RuntimeException("Could not read CV file", e);
    }
}

}
