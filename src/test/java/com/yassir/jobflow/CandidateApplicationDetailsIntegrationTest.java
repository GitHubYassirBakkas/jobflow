package com.yassir.jobflow;

import com.yassir.jobflow.entity.Application;
import com.yassir.jobflow.entity.ApplicationStatus;
import com.yassir.jobflow.entity.CandidateProfile;
import com.yassir.jobflow.entity.Company;
import com.yassir.jobflow.entity.EmploymentType;
import com.yassir.jobflow.entity.Job;
import com.yassir.jobflow.entity.RecruiterProfile;
import com.yassir.jobflow.entity.Role;
import com.yassir.jobflow.entity.User;
import com.yassir.jobflow.repository.ApplicationRepository;
import com.yassir.jobflow.repository.ApplicationStatusHistoryRepository;
import com.yassir.jobflow.repository.CandidateProfileRepository;
import com.yassir.jobflow.repository.CompanyRepository;
import com.yassir.jobflow.repository.InterviewRepository;
import com.yassir.jobflow.repository.JobRepository;
import com.yassir.jobflow.repository.RecruiterProfileRepository;
import com.yassir.jobflow.repository.SavedJobRepository;
import com.yassir.jobflow.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CandidateApplicationDetailsIntegrationTest extends PostgresContainerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private ApplicationStatusHistoryRepository statusHistoryRepository;

    @Autowired
    private SavedJobRepository savedJobRepository;

    @Autowired
    private InterviewRepository interviewRepository;

    @Autowired
    private CandidateProfileRepository candidateProfileRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private RecruiterProfileRepository recruiterProfileRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private UserRepository userRepository;

    @TempDir
    private Path tempDirectory;

    @BeforeEach
    void cleanDatabase() throws IOException {
        deleteApplicationCvFiles();
        deleteApplicationDetailsData();
    }

    @AfterEach
    void cleanAfterTest() throws IOException {
        deleteApplicationCvFiles();
        deleteApplicationDetailsData();
    }

    @Test
    void candidateAccessesOwnApplication() throws Exception {
        TestData data = createTestData();
        Application application = saveApplication(
                data.candidate(),
                data.job(),
                "submitted-cv.pdf",
                null
        );

        mockMvc.perform(get(
                                "/api/applications/me/{applicationId}",
                                application.getId()
                        )
                        .with(jwtFor(data.candidate())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(application.getId()))
                .andExpect(jsonPath("$.jobId").value(data.job().getId()))
                .andExpect(jsonPath("$.jobTitle").value(data.job().getTitle()))
                .andExpect(jsonPath("$.companyName").value(data.company().getName()))
                .andExpect(jsonPath("$.candidateId").value(data.candidate().getId()))
                .andExpect(jsonPath("$.candidateEmail").value(data.candidate().getEmail()))
                .andExpect(jsonPath("$.status").value("APPLIED"))
                .andExpect(jsonPath("$.cvFileName").value("submitted-cv.pdf"))
                .andExpect(jsonPath("$.appliedAt").isString());
    }

    @Test
    void candidateCannotAccessAnotherCandidatesApplication() throws Exception {
        TestData data = createTestData();
        Application application = saveApplication(
                data.otherCandidate(),
                data.job(),
                "other-cv.pdf",
                null
        );

        mockMvc.perform(get(
                                "/api/applications/me/{applicationId}",
                                application.getId()
                        )
                        .with(jwtFor(data.candidate())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Application not found"));
    }

    @Test
    void candidateGetsNoContentWhenNoApplicationExistsForJob() throws Exception {
        TestData data = createTestData();

        mockMvc.perform(get(
                                "/api/applications/me/jobs/{jobId}",
                                data.job().getId()
                        )
                        .with(jwtFor(data.candidate())))
                .andExpect(status().isNoContent());
    }

    @Test
    void candidateGetsOwnApplicationForJob() throws Exception {
        TestData data = createTestData();
        Application application = saveApplication(
                data.candidate(),
                data.job(),
                "submitted-cv.pdf",
                null
        );

        mockMvc.perform(get(
                                "/api/applications/me/jobs/{jobId}",
                                data.job().getId()
                        )
                        .with(jwtFor(data.candidate())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(application.getId()))
                .andExpect(jsonPath("$.status").value("APPLIED"));
    }

    @Test
    void candidateGetsOwnWithdrawnApplicationForJob() throws Exception {
        TestData data = createTestData();
        Application application = saveApplication(
                data.candidate(),
                data.job(),
                "submitted-cv.pdf",
                null
        );
        application.setStatus(ApplicationStatus.WITHDRAWN);
        applicationRepository.saveAndFlush(application);

        mockMvc.perform(get(
                                "/api/applications/me/jobs/{jobId}",
                                data.job().getId()
                        )
                        .with(jwtFor(data.candidate())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(application.getId()))
                .andExpect(jsonPath("$.status").value("WITHDRAWN"));
    }

    @Test
    void candidateCannotDiscoverAnotherCandidatesApplicationForJob() throws Exception {
        TestData data = createTestData();
        saveApplication(
                data.otherCandidate(),
                data.job(),
                "other-cv.pdf",
                null
        );

        mockMvc.perform(get(
                                "/api/applications/me/jobs/{jobId}",
                                data.job().getId()
                        )
                        .with(jwtFor(data.candidate())))
                .andExpect(status().isNoContent());
    }

    @Test
    void missingApplicationReturnsNotFound() throws Exception {
        TestData data = createTestData();

        mockMvc.perform(get("/api/applications/me/{applicationId}", 999_999L)
                        .with(jwtFor(data.candidate())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Application not found"));
    }

    @Test
    void candidateDownloadsOwnSubmittedCv() throws Exception {
        TestData data = createTestData();
        Path cvPath = createPdfFile("submitted-cv.pdf", "submitted cv");
        Application application = saveApplication(
                data.candidate(),
                data.job(),
                "submitted-cv.pdf",
                cvPath
        );

        mockMvc.perform(get(
                                "/api/applications/me/{applicationId}/cv",
                                application.getId()
                        )
                        .with(jwtFor(data.candidate())))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PDF))
                .andExpect(header().string(
                        HttpHeaders.CONTENT_DISPOSITION,
                        containsString("filename=\"submitted-cv.pdf\"")
                ))
                .andExpect(content().string(containsString("submitted cv")));
    }

    @Test
    void otherCandidateCannotDownloadApplicationCv() throws Exception {
        TestData data = createTestData();
        Path cvPath = createPdfFile("private-cv.pdf", "private cv");
        Application application = saveApplication(
                data.otherCandidate(),
                data.job(),
                "private-cv.pdf",
                cvPath
        );

        mockMvc.perform(get(
                                "/api/applications/me/{applicationId}/cv",
                                application.getId()
                        )
                        .with(jwtFor(data.candidate())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Application not found"));
    }

    @Test
    void downloadedCvRemainsOriginalSnapshotAfterProfileCvChanges()
            throws Exception {

        TestData data = createTestData();
        CandidateProfile profile = saveCandidateProfileWithCv(
                data.candidate(),
                "profile-original.pdf",
                "original profile cv"
        );

        mockMvc.perform(post("/api/jobs/{jobId}/apply", data.job().getId())
                        .with(jwtFor(data.candidate())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cvFileName").value("profile-original.pdf"));

        Application application = onlyApplication();
        Path updatedProfileCv = createPdfFile(
                "profile-updated.pdf",
                "updated profile cv"
        );
        profile.setCvFileName("profile-updated.pdf");
        profile.setCvFilePath(updatedProfileCv.toString());
        candidateProfileRepository.saveAndFlush(profile);

        mockMvc.perform(get(
                                "/api/applications/me/{applicationId}/cv",
                                application.getId()
                        )
                        .with(jwtFor(data.candidate())))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        HttpHeaders.CONTENT_DISPOSITION,
                        containsString("filename=\"profile-original.pdf\"")
                ))
                .andExpect(content().string(containsString("original profile cv")))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        containsString("updated profile cv")
                )));
    }

    @Test
    void unauthenticatedAccessIsDenied() throws Exception {
        TestData data = createTestData();
        Application application = saveApplication(
                data.candidate(),
                data.job(),
                "submitted-cv.pdf",
                null
        );

        mockMvc.perform(get(
                                "/api/applications/me/{applicationId}",
                                application.getId()
                        ))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get(
                                "/api/applications/me/{applicationId}/cv",
                                application.getId()
                        ))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void recruiterAndAdminCannotAccessCandidateOnlyEndpoints()
            throws Exception {

        TestData data = createTestData();
        Application application = saveApplication(
                data.candidate(),
                data.job(),
                "submitted-cv.pdf",
                null
        );

        mockMvc.perform(get(
                                "/api/applications/me/{applicationId}",
                                application.getId()
                        )
                        .with(jwtFor(data.recruiter())))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(
                                "/api/applications/me/{applicationId}/cv",
                                application.getId()
                        )
                        .with(jwtFor(data.recruiter())))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(
                                "/api/applications/me/{applicationId}",
                                application.getId()
                        )
                        .with(jwtFor(data.admin())))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(
                                "/api/applications/me/{applicationId}/cv",
                                application.getId()
                        )
                        .with(jwtFor(data.admin())))
                .andExpect(status().isForbidden());
    }

    private void deleteApplicationCvFiles() throws IOException {
        List<Application> applications = applicationRepository.findAll();

        for (Application application : applications) {
            String cvFilePath = application.getCvFilePath();

            if (cvFilePath != null && !cvFilePath.isBlank()) {
                Files.deleteIfExists(Paths.get(cvFilePath));
            }
        }
    }

    private void deleteApplicationDetailsData() {
        statusHistoryRepository.deleteAll();
        savedJobRepository.deleteAll();
        interviewRepository.deleteAll();
        applicationRepository.deleteAll();
        candidateProfileRepository.deleteAll();
        jobRepository.deleteAll();
        recruiterProfileRepository.deleteAll();
        companyRepository.deleteAll();
        userRepository.deleteAll();
    }

    private TestData createTestData() {
        User candidate = saveUser(Role.CANDIDATE);
        User otherCandidate = saveUser(Role.CANDIDATE);
        User recruiter = saveUser(Role.RECRUITER);
        User admin = saveUser(Role.ADMIN);
        Company company = saveCompany();

        saveRecruiterProfile(recruiter, company);

        Job job = saveJob(company, recruiter);

        return new TestData(
                candidate,
                otherCandidate,
                recruiter,
                admin,
                company,
                job
        );
    }

    private User saveUser(Role role) {
        String unique = UUID.randomUUID().toString();

        User user = new User();
        user.setFirstName("Application");
        user.setLastName(role.name());
        user.setEmail(role.name().toLowerCase() + "-" + unique + "@example.com");
        user.setPassword("not-used");
        user.setRole(role);
        user.setEnabled(true);

        return userRepository.saveAndFlush(user);
    }

    private Company saveCompany() {
        Company company = new Company();
        company.setName("Application Details Company " + UUID.randomUUID());
        company.setDescription("Application details integration test company");
        company.setWebsite("https://example.com");
        company.setLocation("Remote");

        return companyRepository.saveAndFlush(company);
    }

    private void saveRecruiterProfile(User recruiter, Company company) {
        RecruiterProfile profile = new RecruiterProfile();
        profile.setUser(recruiter);
        profile.setCompany(company);
        profile.setJobTitle("Recruiter");

        recruiterProfileRepository.saveAndFlush(profile);
    }

    private Job saveJob(Company company, User recruiter) {
        Job job = new Job();
        job.setTitle("Application Details Engineer " + UUID.randomUUID());
        job.setDescription("Build application details");
        job.setLocation("Remote");
        job.setEmploymentType(EmploymentType.FULL_TIME);
        job.setCompany(company);
        job.setCreatedBy(recruiter);
        job.setActive(true);

        return jobRepository.saveAndFlush(job);
    }

    private CandidateProfile saveCandidateProfileWithCv(
            User candidate,
            String cvFileName,
            String cvContent) throws IOException {

        Path cvPath = createPdfFile(cvFileName, cvContent);

        CandidateProfile profile = new CandidateProfile();
        profile.setUser(candidate);
        profile.setPhone("555-0100");
        profile.setLocation("Remote");
        profile.setBio("Application details test candidate");
        profile.setSkills("Java, Spring");
        profile.setCvFileName(cvFileName);
        profile.setCvFilePath(cvPath.toString());

        return candidateProfileRepository.saveAndFlush(profile);
    }

    private Application saveApplication(
            User candidate,
            Job job,
            String cvFileName,
            Path cvPath) {

        Application application = new Application();
        application.setCandidate(candidate);
        application.setJob(job);
        application.setStatus(ApplicationStatus.APPLIED);
        application.setCvFileName(cvFileName);

        if (cvPath != null) {
            application.setCvFilePath(cvPath.toString());
        }

        return applicationRepository.saveAndFlush(application);
    }

    private Path createPdfFile(String fileName, String content) throws IOException {
        Path file = tempDirectory.resolve(fileName);
        Files.writeString(
                file,
                "%PDF-1.4 " + content,
                StandardCharsets.UTF_8
        );

        return file;
    }

    private Application onlyApplication() {
        List<Application> applications = applicationRepository.findAll();

        assertThat(applications).hasSize(1);

        return applications.get(0);
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

    private record TestData(
            User candidate,
            User otherCandidate,
            User recruiter,
            User admin,
            Company company,
            Job job) {
    }
}
