package com.yassir.jobflow;

import com.yassir.jobflow.entity.Company;
import com.yassir.jobflow.entity.EmploymentType;
import com.yassir.jobflow.entity.Job;
import com.yassir.jobflow.entity.Role;
import com.yassir.jobflow.entity.SavedJob;
import com.yassir.jobflow.entity.User;
import com.yassir.jobflow.repository.ApplicationRepository;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CandidateSavedJobControllerIntegrationTest extends PostgresContainerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SavedJobRepository savedJobRepository;

    @Autowired
    private InterviewRepository interviewRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

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

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        deleteSavedJobData();
    }

    @AfterEach
    void cleanAfterTest() {
        deleteSavedJobData();
    }

    @Test
    void candidateSavesAndReceivesCorrectResponse() throws Exception {
        TestData data = createTestData();

        mockMvc.perform(post("/api/candidate/saved-jobs/{jobId}",
                        data.activeJobA().getId())
                        .with(jwtFor(data.candidate())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.job.id").value(data.activeJobA().getId()))
                .andExpect(jsonPath("$.job.title").value(data.activeJobA().getTitle()))
                .andExpect(jsonPath("$.savedAt").isString());

        assertThat(savedJobRepository.existsByCandidateIdAndJobId(
                data.candidate().getId(),
                data.activeJobA().getId()
        )).isTrue();
    }

    @Test
    void duplicatePostRemainsIdempotent() throws Exception {
        TestData data = createTestData();

        mockMvc.perform(post("/api/candidate/saved-jobs/{jobId}",
                        data.activeJobA().getId())
                        .with(jwtFor(data.candidate())))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/candidate/saved-jobs/{jobId}",
                        data.activeJobA().getId())
                        .with(jwtFor(data.candidate())))
                .andExpect(status().isOk());

        assertThat(savedJobRepository.findAll()).hasSize(1);
    }

    @Test
    void candidateRetrievesOwnSavedList() throws Exception {
        TestData data = createTestData();
        SavedJob older = saveSavedJob(data.candidate(), data.activeJobA());
        SavedJob newer = saveSavedJob(data.candidate(), data.activeJobB());
        saveSavedJob(data.otherCandidate(), data.otherCandidateJob());

        setSavedAt(older, LocalDateTime.of(2026, 10, 1, 10, 0));
        setSavedAt(newer, LocalDateTime.of(2026, 10, 2, 10, 0));

        mockMvc.perform(get("/api/candidate/saved-jobs")
                        .param("page", "0")
                        .param("size", "10")
                        .with(jwtFor(data.candidate())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].id").value(newer.getId()))
                .andExpect(jsonPath("$.content[0].job.id").value(data.activeJobB().getId()))
                .andExpect(jsonPath("$.content[1].id").value(older.getId()))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10));
    }

    @Test
    void candidateRetrievesActiveSavedJobIds() throws Exception {
        TestData data = createTestData();
        SavedJob older = saveSavedJob(data.candidate(), data.activeJobA());
        SavedJob newer = saveSavedJob(data.candidate(), data.activeJobB());
        saveSavedJob(data.candidate(), data.inactiveJob());
        saveSavedJob(data.otherCandidate(), data.otherCandidateJob());

        setSavedAt(older, LocalDateTime.of(2026, 10, 1, 10, 0));
        setSavedAt(newer, LocalDateTime.of(2026, 10, 2, 10, 0));

        mockMvc.perform(get("/api/candidate/saved-jobs/job-ids")
                        .with(jwtFor(data.candidate())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0]").value(data.activeJobB().getId()))
                .andExpect(jsonPath("$[1]").value(data.activeJobA().getId()));
    }

    @Test
    void candidateDeletesOwnSavedJob() throws Exception {
        TestData data = createTestData();
        saveSavedJob(data.candidate(), data.activeJobA());
        saveSavedJob(data.otherCandidate(), data.activeJobA());

        mockMvc.perform(delete("/api/candidate/saved-jobs/{jobId}",
                        data.activeJobA().getId())
                        .with(jwtFor(data.candidate())))
                .andExpect(status().isNoContent());

        assertThat(savedJobRepository.existsByCandidateIdAndJobId(
                data.candidate().getId(),
                data.activeJobA().getId()
        )).isFalse();
        assertThat(savedJobRepository.existsByCandidateIdAndJobId(
                data.otherCandidate().getId(),
                data.activeJobA().getId()
        )).isTrue();
    }

    @Test
    void repeatedDeleteReturnsNoContent() throws Exception {
        TestData data = createTestData();

        mockMvc.perform(delete("/api/candidate/saved-jobs/{jobId}",
                        data.activeJobA().getId())
                        .with(jwtFor(data.candidate())))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/candidate/saved-jobs/{jobId}",
                        data.activeJobA().getId())
                        .with(jwtFor(data.candidate())))
                .andExpect(status().isNoContent());
    }

    @Test
    void inactiveJobCannotBeSaved() throws Exception {
        TestData data = createTestData();

        mockMvc.perform(post("/api/candidate/saved-jobs/{jobId}",
                        data.inactiveJob().getId())
                        .with(jwtFor(data.candidate())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("This job is no longer active"));

        assertThat(savedJobRepository.findAll()).isEmpty();
    }

    @Test
    void inactiveSavedJobIsHidden() throws Exception {
        TestData data = createTestData();
        saveSavedJob(data.candidate(), data.inactiveJob());

        mockMvc.perform(get("/api/candidate/saved-jobs")
                        .with(jwtFor(data.candidate())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements").value(0));
        mockMvc.perform(get("/api/candidate/saved-jobs/job-ids")
                        .with(jwtFor(data.candidate())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void savedJobsAreIsolatedByCandidate() throws Exception {
        TestData data = createTestData();
        saveSavedJob(data.candidate(), data.activeJobA());
        saveSavedJob(data.otherCandidate(), data.otherCandidateJob());

        mockMvc.perform(get("/api/candidate/saved-jobs")
                        .with(jwtFor(data.candidate())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].job.id")
                        .value(data.activeJobA().getId()));
    }

    @Test
    void recruiterAndAdminAreDenied() throws Exception {
        TestData data = createTestData();

        mockMvc.perform(get("/api/candidate/saved-jobs")
                        .with(jwtFor(data.recruiter())))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/candidate/saved-jobs/{jobId}",
                        data.activeJobA().getId())
                        .with(jwtFor(data.admin())))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedAccessIsDenied() throws Exception {
        mockMvc.perform(get("/api/candidate/saved-jobs"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidPaginationIsRejected() throws Exception {
        TestData data = createTestData();

        mockMvc.perform(get("/api/candidate/saved-jobs")
                        .param("page", "-1")
                        .with(jwtFor(data.candidate())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Page must be greater than or equal to 0"));
        mockMvc.perform(get("/api/candidate/saved-jobs")
                        .param("size", "0")
                        .with(jwtFor(data.candidate())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Size must be greater than or equal to 1"));
        mockMvc.perform(get("/api/candidate/saved-jobs")
                        .param("size", "51")
                        .with(jwtFor(data.candidate())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Size must not exceed 50"));
    }

    private void deleteSavedJobData() {
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

        Job activeJobA = saveJob(company, recruiter, true, "Saved API Role A");
        Job activeJobB = saveJob(company, recruiter, true, "Saved API Role B");
        Job inactiveJob = saveJob(company, recruiter, false, "Saved API Inactive Role");
        Job otherCandidateJob = saveJob(
                company,
                recruiter,
                true,
                "Saved API Other Candidate Role"
        );

        return new TestData(
                candidate,
                otherCandidate,
                recruiter,
                admin,
                activeJobA,
                activeJobB,
                inactiveJob,
                otherCandidateJob
        );
    }

    private User saveUser(Role role) {
        String unique = UUID.randomUUID().toString();

        User user = new User();
        user.setFirstName("Saved");
        user.setLastName(role.name());
        user.setEmail(role.name().toLowerCase() + "-" + unique + "@example.com");
        user.setPassword("not-used");
        user.setRole(role);
        user.setEnabled(true);

        return userRepository.saveAndFlush(user);
    }

    private Company saveCompany() {
        Company company = new Company();
        company.setName("Saved Jobs API Company " + UUID.randomUUID());
        company.setDescription("Saved jobs controller test company");
        company.setLocation("Remote");
        company.setWebsite("https://example.com");

        return companyRepository.saveAndFlush(company);
    }

    private Job saveJob(
            Company company,
            User recruiter,
            boolean active,
            String title) {

        Job job = new Job();
        job.setTitle(title + " " + UUID.randomUUID());
        job.setDescription("Saved jobs controller test role");
        job.setLocation("Remote");
        job.setEmploymentType(EmploymentType.FULL_TIME);
        job.setCompany(company);
        job.setCreatedBy(recruiter);
        job.setActive(active);

        return jobRepository.saveAndFlush(job);
    }

    private SavedJob saveSavedJob(User candidate, Job job) {
        SavedJob savedJob = new SavedJob();
        savedJob.setCandidate(candidate);
        savedJob.setJob(job);

        return savedJobRepository.saveAndFlush(savedJob);
    }

    private void setSavedAt(SavedJob savedJob, LocalDateTime savedAt) {
        jdbcTemplate.update(
                "UPDATE saved_jobs SET saved_at = ? WHERE id = ?",
                savedAt,
                savedJob.getId()
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
            User otherCandidate,
            User recruiter,
            User admin,
            Job activeJobA,
            Job activeJobB,
            Job inactiveJob,
            Job otherCandidateJob) {
    }
}
