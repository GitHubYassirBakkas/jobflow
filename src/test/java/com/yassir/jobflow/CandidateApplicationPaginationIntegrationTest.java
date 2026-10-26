package com.yassir.jobflow;

import com.yassir.jobflow.entity.Application;
import com.yassir.jobflow.entity.ApplicationStatus;
import com.yassir.jobflow.entity.Company;
import com.yassir.jobflow.entity.EmploymentType;
import com.yassir.jobflow.entity.Job;
import com.yassir.jobflow.entity.RecruiterProfile;
import com.yassir.jobflow.entity.Role;
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

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CandidateApplicationPaginationIntegrationTest extends PostgresContainerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ApplicationRepository applicationRepository;

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

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        deleteApplicationData();
    }

    @AfterEach
    void cleanAfterTest() {
        deleteApplicationData();
    }

    @Test
    void defaultPaginationReturnsFirstTenApplications() throws Exception {
        TestData data = createTestData();
        LocalDateTime baseTime = LocalDateTime.of(2026, 1, 1, 12, 0);
        Application[] applications = new Application[12];

        for (int index = 0; index < applications.length; index++) {
            applications[index] = saveApplication(
                    data.candidate(),
                    saveJob(data.company(), data.recruiter(), "Default Page " + index),
                    baseTime.plusMinutes(index)
            );
        }

        mockMvc.perform(get("/api/applications/me")
                        .with(jwtFor(data.candidate())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(10)))
                .andExpect(jsonPath("$.content[0].id")
                        .value(applications[11].getId()))
                .andExpect(jsonPath("$.content[9].id")
                        .value(applications[2].getId()))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(12))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(false));
    }

    @Test
    void explicitPageAndSizeAreApplied() throws Exception {
        TestData data = createTestData();
        LocalDateTime baseTime = LocalDateTime.of(2026, 1, 2, 12, 0);
        Application[] applications = new Application[5];

        for (int index = 0; index < applications.length; index++) {
            applications[index] = saveApplication(
                    data.candidate(),
                    saveJob(data.company(), data.recruiter(), "Explicit Page " + index),
                    baseTime.plusMinutes(index)
            );
        }

        mockMvc.perform(get("/api/applications/me")
                        .param("page", "1")
                        .param("size", "2")
                        .with(jwtFor(data.candidate())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].id")
                        .value(applications[2].getId()))
                .andExpect(jsonPath("$.content[1].id")
                        .value(applications[1].getId()))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.first").value(false))
                .andExpect(jsonPath("$.last").value(false));
    }

    @Test
    void applicationsAreSortedNewestFirst() throws Exception {
        TestData data = createTestData();
        LocalDateTime baseTime = LocalDateTime.of(2026, 1, 3, 12, 0);

        Application middle = saveApplication(
                data.candidate(),
                saveJob(data.company(), data.recruiter(), "Middle"),
                baseTime.plusHours(1)
        );
        Application newest = saveApplication(
                data.candidate(),
                saveJob(data.company(), data.recruiter(), "Newest"),
                baseTime.plusHours(2)
        );
        Application oldest = saveApplication(
                data.candidate(),
                saveJob(data.company(), data.recruiter(), "Oldest"),
                baseTime
        );

        mockMvc.perform(get("/api/applications/me")
                        .with(jwtFor(data.candidate())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.content[0].id").value(newest.getId()))
                .andExpect(jsonPath("$.content[1].id").value(middle.getId()))
                .andExpect(jsonPath("$.content[2].id").value(oldest.getId()));
    }

    @Test
    void applicationsWithSameTimestampAreSortedByIdDescending()
            throws Exception {

        TestData data = createTestData();
        LocalDateTime sameTime = LocalDateTime.of(2026, 1, 4, 12, 0);
        Application first = saveApplication(
                data.candidate(),
                saveJob(data.company(), data.recruiter(), "Tie One"),
                sameTime
        );
        Application second = saveApplication(
                data.candidate(),
                saveJob(data.company(), data.recruiter(), "Tie Two"),
                sameTime
        );
        Application third = saveApplication(
                data.candidate(),
                saveJob(data.company(), data.recruiter(), "Tie Three"),
                sameTime
        );

        mockMvc.perform(get("/api/applications/me")
                        .with(jwtFor(data.candidate())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.content[0].id").value(third.getId()))
                .andExpect(jsonPath("$.content[1].id").value(second.getId()))
                .andExpect(jsonPath("$.content[2].id").value(first.getId()));
    }

    @Test
    void applicationsAreIsolatedByCandidate() throws Exception {
        TestData data = createTestData();
        User otherCandidate = saveUser(Role.CANDIDATE);
        Application ownApplication = saveApplication(
                data.candidate(),
                saveJob(data.company(), data.recruiter(), "Own"),
                LocalDateTime.of(2026, 1, 5, 12, 0)
        );
        saveApplication(
                otherCandidate,
                saveJob(data.company(), data.recruiter(), "Other Candidate"),
                LocalDateTime.of(2026, 1, 5, 13, 0)
        );

        mockMvc.perform(get("/api/applications/me")
                        .with(jwtFor(data.candidate())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id")
                        .value(ownApplication.getId()))
                .andExpect(jsonPath("$.content[0].candidateId")
                        .value(data.candidate().getId()))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void emptyResultsReturnEmptyPage() throws Exception {
        TestData data = createTestData();

        mockMvc.perform(get("/api/applications/me")
                        .with(jwtFor(data.candidate())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(true));
    }

    @Test
    void invalidPaginationParametersAreRejected() throws Exception {
        TestData data = createTestData();

        mockMvc.perform(get("/api/applications/me")
                        .param("page", "-1")
                        .with(jwtFor(data.candidate())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Page must be greater than or equal to 0"));
        mockMvc.perform(get("/api/applications/me")
                        .param("size", "0")
                        .with(jwtFor(data.candidate())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Size must be greater than or equal to 1"));
        mockMvc.perform(get("/api/applications/me")
                        .param("size", "51")
                        .with(jwtFor(data.candidate())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Size must not exceed 50"));
    }

    @Test
    void unauthenticatedAndUnauthorizedRolesAreDenied() throws Exception {
        TestData data = createTestData();
        User admin = saveUser(Role.ADMIN);

        mockMvc.perform(get("/api/applications/me"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/applications/me")
                        .with(jwtFor(data.recruiter())))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/applications/me")
                        .with(jwtFor(admin)))
                .andExpect(status().isForbidden());
    }

    private void deleteApplicationData() {
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
        User recruiter = saveUser(Role.RECRUITER);
        Company company = saveCompany();

        saveRecruiterProfile(recruiter, company);

        return new TestData(candidate, recruiter, company);
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
        company.setName("Candidate Applications Company " + UUID.randomUUID());
        company.setDescription("Candidate applications pagination test company");
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

    private Job saveJob(Company company, User recruiter, String title) {
        Job job = new Job();
        job.setTitle(title + " " + UUID.randomUUID());
        job.setDescription("Candidate applications pagination test role");
        job.setLocation("Remote");
        job.setEmploymentType(EmploymentType.FULL_TIME);
        job.setCompany(company);
        job.setCreatedBy(recruiter);
        job.setActive(true);

        return jobRepository.saveAndFlush(job);
    }

    private Application saveApplication(
            User candidate,
            Job job,
            LocalDateTime appliedAt) {

        Application application = new Application();
        application.setCandidate(candidate);
        application.setJob(job);
        application.setStatus(ApplicationStatus.APPLIED);
        application.setCvFileName("candidate-cv.pdf");

        Application savedApplication =
                applicationRepository.saveAndFlush(application);

        jdbcTemplate.update(
                "UPDATE applications SET applied_at = ?, updated_at = ? WHERE id = ?",
                appliedAt,
                appliedAt,
                savedApplication.getId()
        );

        return savedApplication;
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
            Company company) {
    }
}
