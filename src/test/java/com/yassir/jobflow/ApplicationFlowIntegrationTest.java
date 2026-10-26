package com.yassir.jobflow;

import com.yassir.jobflow.entity.Application;
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
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApplicationFlowIntegrationTest extends PostgresContainerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoSpyBean
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
        deleteApplicationFlowData();
    }

    @AfterEach
    void cleanAfterTest() throws IOException {
        deleteApplicationCvFiles();
        deleteApplicationFlowData();
    }

    @Test
    void candidateSuccessfullyAppliesToActiveJob() throws Exception {
        TestData data = createTestData(true);
        saveCandidateProfileWithCv(data.candidate(), "profile-cv.pdf", "profile cv");

        mockMvc.perform(post("/api/jobs/{jobId}/apply", data.job().getId())
                        .with(jwtFor(data.candidate())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.jobId").value(data.job().getId()))
                .andExpect(jsonPath("$.jobTitle").value(data.job().getTitle()))
                .andExpect(jsonPath("$.companyName").value(data.company().getName()))
                .andExpect(jsonPath("$.candidateId").value(data.candidate().getId()))
                .andExpect(jsonPath("$.candidateEmail").value(data.candidate().getEmail()))
                .andExpect(jsonPath("$.status").value("APPLIED"))
                .andExpect(jsonPath("$.cvFileName").value("profile-cv.pdf"))
                .andExpect(jsonPath("$.appliedAt").isString());

        Application application = onlyApplication();

        assertThat(application.getCandidate().getId())
                .isEqualTo(data.candidate().getId());
        assertThat(application.getJob().getId()).isEqualTo(data.job().getId());
        assertThat(application.getCvFilePath()).isNotBlank();
        assertThat(Files.exists(Paths.get(application.getCvFilePath()))).isTrue();
    }

    @Test
    void inactiveJobApplicationIsRejected() throws Exception {
        TestData data = createTestData(false);
        saveCandidateProfileWithCv(data.candidate(), "profile-cv.pdf", "profile cv");

        mockMvc.perform(post("/api/jobs/{jobId}/apply", data.job().getId())
                        .with(jwtFor(data.candidate())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("This job is no longer active"));

        assertThat(applicationRepository.findAll()).isEmpty();
    }

    @Test
    void duplicateApplicationIsRejected() throws Exception {
        TestData data = createTestData(true);
        saveCandidateProfileWithCv(data.candidate(), "profile-cv.pdf", "profile cv");

        mockMvc.perform(post("/api/jobs/{jobId}/apply", data.job().getId())
                        .with(jwtFor(data.candidate())))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/jobs/{jobId}/apply", data.job().getId())
                        .with(jwtFor(data.candidate())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("You have already applied to this job"));

        assertThat(applicationRepository.findAll()).hasSize(1);
    }

    @Test
    void concurrentDuplicateApplicationReturnsFriendlyError()
            throws Exception {

        TestData data = createTestData(true);
        saveCandidateProfileWithCv(data.candidate(), "profile-cv.pdf", "profile cv");

        CyclicBarrier preSaveBarrier = new CyclicBarrier(2);

        doAnswer(invocation -> {
            preSaveBarrier.await(5, TimeUnit.SECONDS);

            return false;
        }).when(applicationRepository)
                .existsByCandidateIdAndJobId(
                        data.candidate().getId(),
                        data.job().getId()
                );

        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<MvcResult> firstRequest = executor.submit(() ->
                    mockMvc.perform(post(
                                    "/api/jobs/{jobId}/apply",
                                    data.job().getId()
                            )
                                    .with(jwtFor(data.candidate())))
                            .andReturn());

            Future<MvcResult> secondRequest = executor.submit(() ->
                    mockMvc.perform(post(
                                    "/api/jobs/{jobId}/apply",
                                    data.job().getId()
                            )
                                    .with(jwtFor(data.candidate())))
                            .andReturn());

            List<MvcResult> results = List.of(
                    firstRequest.get(15, TimeUnit.SECONDS),
                    secondRequest.get(15, TimeUnit.SECONDS)
            );

            assertThat(results)
                    .extracting(result -> result.getResponse().getStatus())
                    .containsExactlyInAnyOrder(200, 400);

            MvcResult duplicateResult = results.stream()
                    .filter(result -> result.getResponse().getStatus() == 400)
                    .findFirst()
                    .orElseThrow();

            assertThat(duplicateResult.getResponse().getContentAsString())
                    .contains("You have already applied to this job");
            assertThat(applicationRepository.findAll()).hasSize(1);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void missingRequiredCandidateProfileIsRejected() throws Exception {
        TestData data = createTestData(true);

        mockMvc.perform(post("/api/jobs/{jobId}/apply", data.job().getId())
                        .with(jwtFor(data.candidate())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message")
                        .value("Candidate profile not found"));

        assertThat(applicationRepository.findAll()).isEmpty();
    }

    @Test
    void missingRequiredCvIsRejected() throws Exception {
        TestData data = createTestData(true);
        saveCandidateProfile(data.candidate(), null, null);

        mockMvc.perform(post("/api/jobs/{jobId}/apply", data.job().getId())
                        .with(jwtFor(data.candidate())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("You must upload a CV before applying"));

        assertThat(applicationRepository.findAll()).isEmpty();
    }

    @Test
    void savedProfileCvIsSnapshottedIndependentlyFromProfileChanges()
            throws Exception {

        TestData data = createTestData(true);
        CandidateProfile profile = saveCandidateProfileWithCv(
                data.candidate(),
                "profile-original.pdf",
                "original profile cv"
        );

        mockMvc.perform(post("/api/jobs/{jobId}/apply", data.job().getId())
                        .with(jwtFor(data.candidate())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cvFileName")
                        .value("profile-original.pdf"));

        Application application = onlyApplication();
        Path applicationCvPath = Paths.get(application.getCvFilePath());

        assertThat(application.getCvFileName()).isEqualTo("profile-original.pdf");
        assertThat(applicationCvPath).isNotEqualTo(Path.of(profile.getCvFilePath()));
        assertThat(Files.readString(applicationCvPath))
                .contains("original profile cv");

        Path updatedProfileCv = createPdfFile(
                "profile-updated.pdf",
                "updated profile cv"
        );
        profile.setCvFileName("profile-updated.pdf");
        profile.setCvFilePath(updatedProfileCv.toString());
        candidateProfileRepository.saveAndFlush(profile);

        Application reloaded = applicationRepository
                .findById(application.getId())
                .orElseThrow();

        assertThat(reloaded.getCvFileName()).isEqualTo("profile-original.pdf");
        assertThat(reloaded.getCvFilePath()).isEqualTo(applicationCvPath.toString());
        assertThat(Files.readString(applicationCvPath))
                .contains("original profile cv")
                .doesNotContain("updated profile cv");
    }

    @Test
    void customUploadedPdfIsSnapshottedIndependentlyFromProfileChanges()
            throws Exception {

        TestData data = createTestData(true);
        CandidateProfile profile = saveCandidateProfile(data.candidate(), null, null);

        mockMvc.perform(multipart("/api/jobs/{jobId}/apply", data.job().getId())
                        .file(validPdfFile("custom-cv.pdf", "custom cv"))
                        .with(jwtFor(data.candidate())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cvFileName").value("custom-cv.pdf"));

        Application application = onlyApplication();
        Path applicationCvPath = Paths.get(application.getCvFilePath());

        assertThat(application.getCvFileName()).isEqualTo("custom-cv.pdf");
        assertThat(Files.readString(applicationCvPath)).contains("custom cv");

        Path updatedProfileCv = createPdfFile(
                "profile-added-after-apply.pdf",
                "new profile cv"
        );
        profile.setCvFileName("profile-added-after-apply.pdf");
        profile.setCvFilePath(updatedProfileCv.toString());
        candidateProfileRepository.saveAndFlush(profile);

        Application reloaded = applicationRepository
                .findById(application.getId())
                .orElseThrow();

        assertThat(reloaded.getCvFileName()).isEqualTo("custom-cv.pdf");
        assertThat(reloaded.getCvFilePath()).isEqualTo(applicationCvPath.toString());
        assertThat(Files.readString(applicationCvPath))
                .contains("custom cv")
                .doesNotContain("new profile cv");
    }

    @Test
    void applicationBelongsToAuthenticatedCandidate() throws Exception {
        TestData data = createTestData(true);
        User otherCandidate = saveUser(Role.CANDIDATE);

        saveCandidateProfileWithCv(data.candidate(), "candidate-a.pdf", "candidate a");
        saveCandidateProfileWithCv(otherCandidate, "candidate-b.pdf", "candidate b");

        mockMvc.perform(post("/api/jobs/{jobId}/apply", data.job().getId())
                        .with(jwtFor(data.candidate())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.candidateId").value(data.candidate().getId()))
                .andExpect(jsonPath("$.candidateEmail")
                        .value(data.candidate().getEmail()));

        Application application = onlyApplication();

        assertThat(application.getCandidate().getId())
                .isEqualTo(data.candidate().getId());

        mockMvc.perform(get("/api/applications/me")
                        .with(jwtFor(data.candidate())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].candidateId")
                        .value(data.candidate().getId()));

        mockMvc.perform(get("/api/applications/me")
                        .with(jwtFor(otherCandidate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    void unauthenticatedApplyIsDenied() throws Exception {
        TestData data = createTestData(true);

        mockMvc.perform(post("/api/jobs/{jobId}/apply", data.job().getId()))
                .andExpect(status().isUnauthorized());

        assertThat(applicationRepository.findAll()).isEmpty();
    }

    @Test
    void recruiterAndAdminCannotUseCandidateApplyApi() throws Exception {
        TestData data = createTestData(true);
        User admin = saveUser(Role.ADMIN);

        mockMvc.perform(post("/api/jobs/{jobId}/apply", data.job().getId())
                        .with(jwtFor(data.recruiter())))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/jobs/{jobId}/apply", data.job().getId())
                        .with(jwtFor(admin)))
                .andExpect(status().isForbidden());

        assertThat(applicationRepository.findAll()).isEmpty();
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

    private void deleteApplicationFlowData() {
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

    private TestData createTestData(boolean activeJob) {
        User candidate = saveUser(Role.CANDIDATE);
        User recruiter = saveUser(Role.RECRUITER);
        Company company = saveCompany();

        saveRecruiterProfile(recruiter, company);

        Job job = saveJob(company, recruiter, activeJob);

        return new TestData(candidate, recruiter, company, job);
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
        company.setName("Application Flow Company " + UUID.randomUUID());
        company.setDescription("Application flow integration test company");
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

    private Job saveJob(Company company, User recruiter, boolean active) {
        Job job = new Job();
        job.setTitle("Application Flow Engineer " + UUID.randomUUID());
        job.setDescription("Build application flow tests");
        job.setLocation("Remote");
        job.setEmploymentType(EmploymentType.FULL_TIME);
        job.setCompany(company);
        job.setCreatedBy(recruiter);
        job.setActive(active);

        return jobRepository.saveAndFlush(job);
    }

    private CandidateProfile saveCandidateProfileWithCv(
            User candidate,
            String cvFileName,
            String cvContent) throws IOException {

        Path cvPath = createPdfFile(cvFileName, cvContent);

        return saveCandidateProfile(candidate, cvFileName, cvPath.toString());
    }

    private CandidateProfile saveCandidateProfile(
            User candidate,
            String cvFileName,
            String cvFilePath) {

        CandidateProfile profile = new CandidateProfile();
        profile.setUser(candidate);
        profile.setPhone("555-0100");
        profile.setLocation("Remote");
        profile.setBio("Application flow test candidate");
        profile.setSkills("Java, Spring");
        profile.setCvFileName(cvFileName);
        profile.setCvFilePath(cvFilePath);

        return candidateProfileRepository.saveAndFlush(profile);
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
            User recruiter,
            Company company,
            Job job) {
    }
}
