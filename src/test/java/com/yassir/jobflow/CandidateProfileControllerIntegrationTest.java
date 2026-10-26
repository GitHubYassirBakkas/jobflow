package com.yassir.jobflow;

import com.yassir.jobflow.entity.Application;
import com.yassir.jobflow.entity.CandidateProfile;
import com.yassir.jobflow.entity.Company;
import com.yassir.jobflow.entity.EmploymentType;
import com.yassir.jobflow.entity.Job;
import com.yassir.jobflow.entity.Role;
import com.yassir.jobflow.entity.User;
import com.yassir.jobflow.repository.ApplicationRepository;
import com.yassir.jobflow.repository.ApplicationStatusHistoryRepository;
import com.yassir.jobflow.repository.CandidateProfileRepository;
import com.yassir.jobflow.repository.CompanyRepository;
import com.yassir.jobflow.repository.InterviewRepository;
import com.yassir.jobflow.repository.JobRepository;
import com.yassir.jobflow.repository.RecruiterProfileRepository;
import com.yassir.jobflow.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CandidateProfileControllerIntegrationTest extends PostgresContainerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CandidateProfileRepository candidateProfileRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private InterviewRepository interviewRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private ApplicationStatusHistoryRepository statusHistoryRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private RecruiterProfileRepository recruiterProfileRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @TempDir
    private Path tempDirectory;

    private final List<Path> uploadedCvFiles = new ArrayList<>();
    private final List<Path> applicationCvFiles = new ArrayList<>();

    @BeforeEach
    void cleanDatabase() {
        statusHistoryRepository.deleteAll();
        interviewRepository.deleteAll();
        applicationRepository.deleteAll();
        candidateProfileRepository.deleteAll();
        jobRepository.deleteAll();
        recruiterProfileRepository.deleteAll();
        companyRepository.deleteAll();
        userRepository.deleteAll();
    }

    @AfterEach
    void cleanUploadedCvFiles() throws IOException {
        for (Path uploadedCvFile : uploadedCvFiles) {
            Files.deleteIfExists(uploadedCvFile);
        }

        for (Path applicationCvFile : applicationCvFiles) {
            Files.deleteIfExists(applicationCvFile);
        }

        uploadedCvFiles.clear();
        applicationCvFiles.clear();
    }

    @Test
    void candidateWithNoProfileCanPutAndCreateOwnProfile() throws Exception {
        User candidate = saveUser(Role.CANDIDATE);

        mockMvc.perform(put("/api/candidate/profile")
                        .with(jwtFor(candidate))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(profileJson(
                                "555-1000",
                                "Casablanca",
                                "Backend candidate",
                                "Java, Spring"
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(candidate.getId()))
                .andExpect(jsonPath("$.phone").value("555-1000"))
                .andExpect(jsonPath("$.location").value("Casablanca"))
                .andExpect(jsonPath("$.bio").value("Backend candidate"))
                .andExpect(jsonPath("$.skills").value("Java, Spring"));

        CandidateProfile savedProfile =
                candidateProfileRepository.findByUserId(candidate.getId())
                        .orElseThrow();

        assertThat(savedProfile.getUser().getId()).isEqualTo(candidate.getId());
    }

    @Test
    void candidateWithExistingProfileCanPutAndUpdateIt() throws Exception {
        User candidate = saveUser(Role.CANDIDATE);
        saveProfile(candidate, "111", "Old City", "Old bio", "Old skills");

        mockMvc.perform(put("/api/candidate/profile")
                        .with(jwtFor(candidate))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(profileJson(
                                "222",
                                "New City",
                                "New bio",
                                "New skills"
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phone").value("222"))
                .andExpect(jsonPath("$.location").value("New City"))
                .andExpect(jsonPath("$.bio").value("New bio"))
                .andExpect(jsonPath("$.skills").value("New skills"));
    }

    @Test
    void putPreservesSameCandidateProfileId() throws Exception {
        User candidate = saveUser(Role.CANDIDATE);
        CandidateProfile existingProfile =
                saveProfile(candidate, "111", "Old City", "Old bio", "Old skills");

        mockMvc.perform(put("/api/candidate/profile")
                        .with(jwtFor(candidate))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(profileJson(
                                "222",
                                "New City",
                                "New bio",
                                "New skills"
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(existingProfile.getId()));

        CandidateProfile updatedProfile =
                candidateProfileRepository.findByUserId(candidate.getId())
                        .orElseThrow();

        assertThat(updatedProfile.getId()).isEqualTo(existingProfile.getId());
    }

    @Test
    void putPreservesExistingCvMetadataAndPath() throws Exception {
        User candidate = saveUser(Role.CANDIDATE);
        CandidateProfile existingProfile =
                saveProfile(candidate, "111", "Old City", "Old bio", "Old skills");
        existingProfile.setCvFileName("original-cv.pdf");
        existingProfile.setCvFilePath("uploads/cvs/original-cv.pdf");
        candidateProfileRepository.saveAndFlush(existingProfile);

        mockMvc.perform(put("/api/candidate/profile")
                        .with(jwtFor(candidate))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(profileJson(
                                "222",
                                "New City",
                                "New bio",
                                "New skills"
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cvFileName").value("original-cv.pdf"));

        CandidateProfile updatedProfile =
                candidateProfileRepository.findByUserId(candidate.getId())
                        .orElseThrow();

        assertThat(updatedProfile.getCvFileName()).isEqualTo("original-cv.pdf");
        assertThat(updatedProfile.getCvFilePath())
                .isEqualTo("uploads/cvs/original-cv.pdf");
    }

    @Test
    void putDoesNotCreateSecondProfileRow() throws Exception {
        User candidate = saveUser(Role.CANDIDATE);
        saveProfile(candidate, "111", "Old City", "Old bio", "Old skills");

        mockMvc.perform(put("/api/candidate/profile")
                        .with(jwtFor(candidate))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(profileJson(
                                "222",
                                "New City",
                                "New bio",
                                "New skills"
                        )))
                .andExpect(status().isOk());

        assertThat(candidateProfileRepository.findAll()).hasSize(1);
    }

    @Test
    void requestCannotAssignProfileToAnotherUser() throws Exception {
        User candidate = saveUser(Role.CANDIDATE);
        User otherCandidate = saveUser(Role.CANDIDATE);

        String requestWithForbiddenOwnershipFields = """
                {
                  "userId": %d,
                  "candidateProfileId": 999,
                  "email": "%s",
                  "role": "ADMIN",
                  "cvFileName": "hacked.pdf",
                  "cvFilePath": "uploads/cvs/hacked.pdf",
                  "phone": "555-2000",
                  "location": "Rabat",
                  "bio": "Own profile",
                  "skills": "Spring Security"
                }
                """.formatted(otherCandidate.getId(), otherCandidate.getEmail());

        mockMvc.perform(put("/api/candidate/profile")
                        .with(jwtFor(candidate))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestWithForbiddenOwnershipFields))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(candidate.getId()))
                .andExpect(jsonPath("$.email").value(candidate.getEmail()));

        assertThat(candidateProfileRepository.findByUserId(candidate.getId()))
                .get()
                .satisfies(profile -> {
                    assertThat(profile.getCvFileName()).isNull();
                    assertThat(profile.getCvFilePath()).isNull();
                });
        assertThat(candidateProfileRepository.findByUserId(otherCandidate.getId()))
                .isEmpty();
    }

    @Test
    void postDuplicateProfileReturnsControlledBadRequest() throws Exception {
        User candidate = saveUser(Role.CANDIDATE);
        saveProfile(candidate, "111", "Old City", "Old bio", "Old skills");

        mockMvc.perform(post("/api/candidate/profile")
                        .with(jwtFor(candidate))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(profileJson(
                                "222",
                                "New City",
                                "New bio",
                                "New skills"
                        )))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Candidate profile already exists"));

        assertThat(candidateProfileRepository.findAll()).hasSize(1);
    }

    @Test
    void bioLongerThan2000CharactersIsRejected() throws Exception {
        User candidate = saveUser(Role.CANDIDATE);

        mockMvc.perform(put("/api/candidate/profile")
                        .with(jwtFor(candidate))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(profileJson(
                                "555-1000",
                                "Casablanca",
                                "a".repeat(2001),
                                "Java"
                        )))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.bio")
                        .value("Bio must not exceed 2000 characters"));
    }

    @Test
    void skillsLongerThan1000CharactersIsRejected() throws Exception {
        User candidate = saveUser(Role.CANDIDATE);

        mockMvc.perform(put("/api/candidate/profile")
                        .with(jwtFor(candidate))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(profileJson(
                                "555-1000",
                                "Casablanca",
                                "Bio",
                                "a".repeat(1001)
                        )))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.skills")
                        .value("Skills must not exceed 1000 characters"));
    }

    @Test
    void unauthenticatedPutReturnsUnauthorized() throws Exception {
        mockMvc.perform(put("/api/candidate/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(profileJson(
                                "555-1000",
                                "Casablanca",
                                "Bio",
                                "Java"
                        )))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void recruiterPutCandidateProfileReturnsForbidden() throws Exception {
        User recruiter = saveUser(Role.RECRUITER);

        mockMvc.perform(put("/api/candidate/profile")
                        .with(jwtFor(recruiter))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(profileJson(
                                "555-1000",
                                "Casablanca",
                                "Bio",
                                "Java"
                        )))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminPutCandidateProfileReturnsForbidden() throws Exception {
        User admin = saveUser(Role.ADMIN);

        mockMvc.perform(put("/api/candidate/profile")
                        .with(jwtFor(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(profileJson(
                                "555-1000",
                                "Casablanca",
                                "Bio",
                                "Java"
                        )))
                .andExpect(status().isForbidden());
    }

    @Test
    void emptyOptionalProfileFieldsWithoutCvReturnsZeroCompleteness()
            throws Exception {

        User candidate = saveUser(Role.CANDIDATE);

        mockMvc.perform(post("/api/candidate/profile")
                        .with(jwtFor(candidate))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(profileJson("", "", "", "")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profileCompleteness").value(0));
    }

    @Test
    void phoneOnlyReturnsTwentyCompleteness() throws Exception {
        User candidate = saveUser(Role.CANDIDATE);

        mockMvc.perform(post("/api/candidate/profile")
                        .with(jwtFor(candidate))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(profileJson("555-1000", "", "", "")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profileCompleteness").value(20));
    }

    @Test
    void phoneAndLocationReturnFortyCompleteness() throws Exception {
        User candidate = saveUser(Role.CANDIDATE);

        mockMvc.perform(post("/api/candidate/profile")
                        .with(jwtFor(candidate))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(profileJson(
                                "555-1000",
                                "Casablanca",
                                "",
                                ""
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profileCompleteness").value(40));
    }

    @Test
    void phoneLocationAndBioReturnSixtyCompleteness() throws Exception {
        User candidate = saveUser(Role.CANDIDATE);

        mockMvc.perform(post("/api/candidate/profile")
                        .with(jwtFor(candidate))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(profileJson(
                                "555-1000",
                                "Casablanca",
                                "Java developer",
                                ""
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profileCompleteness").value(60));
    }

    @Test
    void allTextFieldsWithoutCvReturnEightyCompleteness() throws Exception {
        User candidate = saveUser(Role.CANDIDATE);

        mockMvc.perform(post("/api/candidate/profile")
                        .with(jwtFor(candidate))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(profileJson(
                                "555-1000",
                                "Casablanca",
                                "Java developer",
                                "Java, Spring Boot"
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profileCompleteness").value(80));
    }

    @Test
    void allFiveComponentsReturnCompleteProfileAfterCvUpload()
            throws Exception {

        User candidate = saveUser(Role.CANDIDATE);

        mockMvc.perform(post("/api/candidate/profile")
                        .with(jwtFor(candidate))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(profileJson(
                                "555-1000",
                                "Casablanca",
                                "Java developer",
                                "Java, Spring Boot"
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profileCompleteness").value(80));

        mockMvc.perform(multipart("/api/candidate/profile/cv")
                        .file(validPdfFile())
                        .with(jwtFor(candidate)))
                .andExpect(status().isOk());

        CandidateProfile profile =
                candidateProfileRepository.findByUserId(candidate.getId())
                        .orElseThrow();
        uploadedCvFiles.add(Path.of(profile.getCvFilePath()));

        mockMvc.perform(get("/api/candidate/profile/me")
                        .with(jwtFor(candidate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profileCompleteness").value(100));
    }

    @Test
    void whitespaceOnlyTextFieldDoesNotCountTowardCompleteness()
            throws Exception {

        User candidate = saveUser(Role.CANDIDATE);

        mockMvc.perform(post("/api/candidate/profile")
                        .with(jwtFor(candidate))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(profileJson("   ", "Meknes", "", "")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profileCompleteness").value(20));
    }

    @Test
    void putRecalculatesCompletenessAfterProfileFieldsChange()
            throws Exception {

        User candidate = saveUser(Role.CANDIDATE);
        saveProfile(
                candidate,
                "555-1000",
                "Casablanca",
                "Java developer",
                "Java"
        );

        mockMvc.perform(put("/api/candidate/profile")
                        .with(jwtFor(candidate))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(profileJson("555-2000", "", "", "")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profileCompleteness").value(20));
    }

    @Test
    void cvMetadataContributesExactlyTwentyCompleteness() throws Exception {
        User candidate = saveUser(Role.CANDIDATE);
        CandidateProfile profile = saveProfile(candidate, "", "", "", "");
        profile.setCvFileName("candidate-cv.pdf");
        profile.setCvFilePath("uploads/cvs/candidate-cv.pdf");
        candidateProfileRepository.saveAndFlush(profile);

        mockMvc.perform(get("/api/candidate/profile/me")
                        .with(jwtFor(candidate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profileCompleteness").value(20));
    }

    @Test
    void getMyProfileReturnsProfileCompleteness() throws Exception {
        User candidate = saveUser(Role.CANDIDATE);
        saveProfile(candidate, "555-1000", "", "", "");

        mockMvc.perform(get("/api/candidate/profile/me")
                        .with(jwtFor(candidate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profileCompleteness").value(20));
    }

    @Test
    void postCreateReturnsProfileCompleteness() throws Exception {
        User candidate = saveUser(Role.CANDIDATE);

        mockMvc.perform(post("/api/candidate/profile")
                        .with(jwtFor(candidate))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(profileJson("", "", "", "Java")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profileCompleteness").value(20));
    }

    @Test
    void putUpsertReturnsProfileCompleteness() throws Exception {
        User candidate = saveUser(Role.CANDIDATE);

        mockMvc.perform(put("/api/candidate/profile")
                        .with(jwtFor(candidate))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(profileJson(
                                "555-1000",
                                "Casablanca",
                                "Java developer",
                                ""
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profileCompleteness").value(60));
    }

    @Test
    void adminCandidateProfileResponseReturnsProfileCompleteness()
            throws Exception {

        User admin = saveUser(Role.ADMIN);
        User candidate = saveUser(Role.CANDIDATE);
        saveProfile(
                candidate,
                "555-1000",
                "Casablanca",
                "Java developer",
                "Java"
        );

        mockMvc.perform(get("/api/admin/candidates/{userId}", candidate.getId())
                        .with(jwtFor(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profileCompleteness").value(80));
    }

    @Test
    void firstValidProfileCvUploadSucceeds() throws Exception {
        User candidate = saveUser(Role.CANDIDATE);
        saveProfile(candidate, "555-1000", "Casablanca", "Bio", "Java");

        mockMvc.perform(multipart("/api/candidate/profile/cv")
                        .file(validPdfFile("first-cv.pdf", "first cv"))
                        .with(jwtFor(candidate)))
                .andExpect(status().isOk());

        CandidateProfile profile =
                candidateProfileRepository.findByUserId(candidate.getId())
                        .orElseThrow();
        Path cvPath = Path.of(profile.getCvFilePath());
        uploadedCvFiles.add(cvPath);

        assertThat(profile.getCvFileName()).isEqualTo("first-cv.pdf");
        assertThat(Files.exists(cvPath)).isTrue();
        assertThat(Files.readString(cvPath)).contains("first cv");
    }

    @Test
    void successfulProfileCvReplacementUpdatesMetadataAndDeletesOldFile()
            throws Exception {

        User candidate = saveUser(Role.CANDIDATE);
        saveProfile(candidate, "555-1000", "Casablanca", "Bio", "Java");

        uploadProfileCv(candidate, "old-cv.pdf", "old cv");
        CandidateProfile oldProfile =
                candidateProfileRepository.findByUserId(candidate.getId())
                        .orElseThrow();
        Long profileId = oldProfile.getId();
        Path oldPath = Path.of(oldProfile.getCvFilePath());

        uploadProfileCv(candidate, "new-cv.pdf", "new cv");

        CandidateProfile newProfile =
                candidateProfileRepository.findByUserId(candidate.getId())
                        .orElseThrow();
        Path newPath = Path.of(newProfile.getCvFilePath());
        uploadedCvFiles.add(newPath);

        assertThat(newProfile.getId()).isEqualTo(profileId);
        assertThat(newProfile.getCvFileName()).isEqualTo("new-cv.pdf");
        assertThat(newProfile.getPhone()).isEqualTo("555-1000");
        assertThat(newProfile.getLocation()).isEqualTo("Casablanca");
        assertThat(newProfile.getBio()).isEqualTo("Bio");
        assertThat(newProfile.getSkills()).isEqualTo("Java");
        assertThat(Files.exists(newPath)).isTrue();
        assertThat(Files.readString(newPath)).contains("new cv");
        assertThat(Files.exists(oldPath)).isFalse();
    }

    @Test
    void invalidProfileCvReplacementPreservesPreviousMetadataAndFile()
            throws Exception {

        User candidate = saveUser(Role.CANDIDATE);
        saveProfile(candidate, "555-1000", "Casablanca", "Bio", "Java");

        uploadProfileCv(candidate, "old-cv.pdf", "old cv");
        CandidateProfile oldProfile =
                candidateProfileRepository.findByUserId(candidate.getId())
                        .orElseThrow();
        String oldFileName = oldProfile.getCvFileName();
        String oldFilePath = oldProfile.getCvFilePath();
        Path oldPath = Path.of(oldFilePath);
        uploadedCvFiles.add(oldPath);

        mockMvc.perform(multipart("/api/candidate/profile/cv")
                        .file(invalidTextFile())
                        .with(jwtFor(candidate)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Only PDF files are allowed"));

        CandidateProfile unchangedProfile =
                candidateProfileRepository.findByUserId(candidate.getId())
                        .orElseThrow();

        assertThat(unchangedProfile.getCvFileName()).isEqualTo(oldFileName);
        assertThat(unchangedProfile.getCvFilePath()).isEqualTo(oldFilePath);
        assertThat(Files.exists(oldPath)).isTrue();
        assertThat(Files.readString(oldPath)).contains("old cv");
    }

    @Test
    void missingPhysicalProfileCvReturnsControlledError()
            throws Exception {

        User candidate = saveUser(Role.CANDIDATE);
        CandidateProfile profile =
                saveProfile(candidate, "555-1000", "Casablanca", "Bio", "Java");
        profile.setCvFileName("missing-cv.pdf");
        profile.setCvFilePath(tempDirectory.resolve("missing-cv.pdf").toString());
        candidateProfileRepository.saveAndFlush(profile);

        mockMvc.perform(get("/api/candidate/profile/cv")
                        .with(jwtFor(candidate)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("CV file not found"));
    }

    @Test
    void deleteCurrentProfileCvClearsMetadataAndRemovesPhysicalFile()
            throws Exception {

        User candidate = saveUser(Role.CANDIDATE);
        saveProfile(candidate, "555-1000", "Casablanca", "Bio", "Java");
        uploadProfileCv(candidate, "candidate-cv.pdf", "profile cv");

        CandidateProfile profileBeforeDelete =
                candidateProfileRepository.findByUserId(candidate.getId())
                        .orElseThrow();
        Path cvPath = Path.of(profileBeforeDelete.getCvFilePath());

        mockMvc.perform(delete("/api/candidate/profile/cv")
                        .with(jwtFor(candidate)))
                .andExpect(status().isNoContent());

        CandidateProfile profileAfterDelete =
                candidateProfileRepository.findByUserId(candidate.getId())
                        .orElseThrow();

        assertThat(profileAfterDelete.getCvFileName()).isNull();
        assertThat(profileAfterDelete.getCvFilePath()).isNull();
        assertThat(Files.exists(cvPath)).isFalse();
    }

    @Test
    void deleteCurrentProfileCvRecalculatesCompleteness()
            throws Exception {

        User candidate = saveUser(Role.CANDIDATE);
        saveProfile(
                candidate,
                "555-1000",
                "Casablanca",
                "Java developer",
                "Java"
        );
        uploadProfileCv(candidate, "candidate-cv.pdf", "profile cv");

        CandidateProfile profileBeforeDelete =
                candidateProfileRepository.findByUserId(candidate.getId())
                        .orElseThrow();
        uploadedCvFiles.add(Path.of(profileBeforeDelete.getCvFilePath()));

        mockMvc.perform(get("/api/candidate/profile/me")
                        .with(jwtFor(candidate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profileCompleteness").value(100));

        mockMvc.perform(delete("/api/candidate/profile/cv")
                        .with(jwtFor(candidate)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/candidate/profile/me")
                        .with(jwtFor(candidate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profileCompleteness").value(80));
    }

    @Test
    void deleteWhenCandidateHasNoProfileCvIsIdempotent()
            throws Exception {

        User candidate = saveUser(Role.CANDIDATE);
        saveProfile(candidate, "555-1000", "Casablanca", "Bio", "Java");

        mockMvc.perform(delete("/api/candidate/profile/cv")
                        .with(jwtFor(candidate)))
                .andExpect(status().isNoContent());

        CandidateProfile profile =
                candidateProfileRepository.findByUserId(candidate.getId())
                        .orElseThrow();

        assertThat(profile.getCvFileName()).isNull();
        assertThat(profile.getCvFilePath()).isNull();
    }

    @Test
    void unauthenticatedDeleteProfileCvReturnsUnauthorized()
            throws Exception {

        mockMvc.perform(delete("/api/candidate/profile/cv"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void recruiterDeleteProfileCvReturnsForbidden() throws Exception {
        User recruiter = saveUser(Role.RECRUITER);

        mockMvc.perform(delete("/api/candidate/profile/cv")
                        .with(jwtFor(recruiter)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminDeleteProfileCvReturnsForbidden() throws Exception {
        User admin = saveUser(Role.ADMIN);

        mockMvc.perform(delete("/api/candidate/profile/cv")
                        .with(jwtFor(admin)))
                .andExpect(status().isForbidden());
    }

    @Test
    void profileCvReplacementKeepsNewCvWhenOldFileCleanupFails()
            throws Exception {

        User candidate = saveUser(Role.CANDIDATE);
        CandidateProfile profile =
                saveProfile(candidate, "555-1000", "Casablanca", "Bio", "Java");
        Path undeletableOldPath = tempDirectory.resolve("old-cv-directory");
        Files.createDirectories(undeletableOldPath);
        Files.writeString(undeletableOldPath.resolve("child.txt"), "not empty");
        profile.setCvFileName("old-cv.pdf");
        profile.setCvFilePath(undeletableOldPath.toString());
        candidateProfileRepository.saveAndFlush(profile);

        mockMvc.perform(multipart("/api/candidate/profile/cv")
                        .file(validPdfFile("new-cv.pdf", "new cv"))
                        .with(jwtFor(candidate)))
                .andExpect(status().isOk());

        CandidateProfile updatedProfile =
                candidateProfileRepository.findByUserId(candidate.getId())
                        .orElseThrow();
        Path newPath = Path.of(updatedProfile.getCvFilePath());
        uploadedCvFiles.add(newPath);

        assertThat(updatedProfile.getCvFileName()).isEqualTo("new-cv.pdf");
        assertThat(Files.exists(newPath)).isTrue();
        assertThat(Files.readString(newPath)).contains("new cv");
        assertThat(Files.exists(undeletableOldPath)).isTrue();
    }

    @Test
    void applicationSnapshotSurvivesProfileCvReplacementAndDeletion()
            throws Exception {

        User candidate = saveUser(Role.CANDIDATE);
        Company company = saveCompany();
        Job job = saveJob(company, candidate);
        saveProfile(
                candidate,
                "555-1000",
                "Casablanca",
                "Java developer",
                "Java"
        );

        uploadProfileCv(candidate, "profile-a.pdf", "profile cv A");
        CandidateProfile profileA =
                candidateProfileRepository.findByUserId(candidate.getId())
                        .orElseThrow();
        uploadedCvFiles.add(Path.of(profileA.getCvFilePath()));

        mockMvc.perform(post("/api/jobs/{jobId}/apply", job.getId())
                        .with(jwtFor(candidate)))
                .andExpect(status().isOk());

        Application application = applicationRepository.findAll().get(0);
        Path applicationCvPath = Path.of(application.getCvFilePath());
        applicationCvFiles.add(applicationCvPath);

        assertThat(Files.exists(applicationCvPath)).isTrue();
        assertThat(Files.readString(applicationCvPath)).contains("profile cv A");

        uploadProfileCv(candidate, "profile-b.pdf", "profile cv B");
        CandidateProfile profileB =
                candidateProfileRepository.findByUserId(candidate.getId())
                        .orElseThrow();
        uploadedCvFiles.add(Path.of(profileB.getCvFilePath()));

        assertThat(Files.readString(Path.of(profileB.getCvFilePath())))
                .contains("profile cv B");
        assertThat(Files.exists(applicationCvPath)).isTrue();
        assertThat(Files.readString(applicationCvPath)).contains("profile cv A");

        mockMvc.perform(delete("/api/candidate/profile/cv")
                        .with(jwtFor(candidate)))
                .andExpect(status().isNoContent());

        assertThat(Files.exists(applicationCvPath)).isTrue();
        assertThat(Files.readString(applicationCvPath)).contains("profile cv A");
    }

    private User saveUser(Role role) {
        String unique = UUID.randomUUID().toString();

        User user = new User();
        user.setFirstName("Profile");
        user.setLastName(role.name());
        user.setEmail(role.name().toLowerCase() + "-" + unique + "@example.com");
        user.setPassword("not-used");
        user.setRole(role);
        user.setEnabled(true);

        return userRepository.saveAndFlush(user);
    }

    private CandidateProfile saveProfile(
            User candidate,
            String phone,
            String location,
            String bio,
            String skills) {

        CandidateProfile profile = new CandidateProfile();
        profile.setUser(candidate);
        profile.setPhone(phone);
        profile.setLocation(location);
        profile.setBio(bio);
        profile.setSkills(skills);

        return candidateProfileRepository.saveAndFlush(profile);
    }

    private Company saveCompany() {
        Company company = new Company();
        company.setName("Company " + UUID.randomUUID());
        company.setDescription("Test company");
        company.setLocation("Remote");
        company.setWebsite("https://example.com");

        return companyRepository.saveAndFlush(company);
    }

    private Job saveJob(Company company, User createdBy) {
        Job job = new Job();
        job.setTitle("Backend Engineer");
        job.setDescription("Build Spring services");
        job.setLocation("Remote");
        job.setEmploymentType(EmploymentType.FULL_TIME);
        job.setCompany(company);
        job.setCreatedBy(createdBy);
        job.setActive(true);

        return jobRepository.saveAndFlush(job);
    }

    private static RequestPostProcessor jwtFor(User user) {
        return jwt()
                .jwt(jwt -> jwt
                        .subject(user.getEmail())
                        .claim("role", user.getRole().name())
                        .claim("userId", user.getId()))
                .authorities(
                        new SimpleGrantedAuthority(
                                "ROLE_" + user.getRole().name()
                        )
                );
    }

    private static String profileJson(
            String phone,
            String location,
            String bio,
            String skills) {

        return """
                {
                  "phone": "%s",
                  "location": "%s",
                  "bio": "%s",
                  "skills": "%s"
                }
                """.formatted(phone, location, bio, skills);
    }

    private void uploadProfileCv(User candidate, String fileName, String content)
            throws Exception {

        mockMvc.perform(multipart("/api/candidate/profile/cv")
                        .file(validPdfFile(fileName, content))
                        .with(jwtFor(candidate)))
                .andExpect(status().isOk());
    }

    private static MockMultipartFile validPdfFile() {
        return validPdfFile("candidate-cv.pdf", "test fixture");
    }

    private static MockMultipartFile validPdfFile(
            String fileName,
            String content) {

        return new MockMultipartFile(
                "file",
                fileName,
                MediaType.APPLICATION_PDF_VALUE,
                ("%PDF-1.4 " + content).getBytes(StandardCharsets.UTF_8)
        );
    }

    private static MockMultipartFile invalidTextFile() {
        return new MockMultipartFile(
                "file",
                "not-a-pdf.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "not a pdf".getBytes(StandardCharsets.UTF_8)
        );
    }

}
