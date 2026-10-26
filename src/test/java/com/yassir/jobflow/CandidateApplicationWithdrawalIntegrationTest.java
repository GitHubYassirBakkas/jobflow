package com.yassir.jobflow;

import com.yassir.jobflow.entity.Application;
import com.yassir.jobflow.entity.ApplicationStatus;
import com.yassir.jobflow.entity.ApplicationStatusHistory;
import com.yassir.jobflow.entity.ApplicationStatusHistoryEventType;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CandidateApplicationWithdrawalIntegrationTest
        extends PostgresContainerTest {

    private static final String WITHDRAWAL_CONFLICT_MESSAGE =
            "Application cannot be withdrawn from its current status";

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

    @BeforeEach
    void cleanDatabase() {
        deleteWithdrawalData();
    }

    @AfterEach
    void cleanAfterTest() {
        deleteWithdrawalData();
    }

    @Test
    void candidateWithdrawsOwnAppliedApplication() throws Exception {
        assertSuccessfulWithdrawal(ApplicationStatus.APPLIED);
    }

    @Test
    void candidateWithdrawsOwnReviewingApplication() throws Exception {
        assertSuccessfulWithdrawal(ApplicationStatus.REVIEWING);
    }

    @Test
    void candidateWithdrawsOwnInterviewApplication() throws Exception {
        assertSuccessfulWithdrawal(ApplicationStatus.INTERVIEW);
    }

    @Test
    void missingApplicationReturnsNotFoundAndCreatesNoHistory()
            throws Exception {

        TestData data = createTestData();

        mockMvc.perform(patch(
                                "/api/applications/me/{applicationId}/withdraw",
                                999_999L
                        )
                        .with(jwtFor(data.candidate())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Application not found"));

        assertThat(statusHistoryRepository.findAll()).isEmpty();
    }

    @Test
    void otherCandidatesApplicationReturnsNotFoundAndCreatesNoHistory()
            throws Exception {

        TestData data = createTestData();
        Application application = saveApplication(
                data.otherCandidate(),
                data.job(),
                ApplicationStatus.APPLIED
        );

        mockMvc.perform(patch(
                                "/api/applications/me/{applicationId}/withdraw",
                                application.getId()
                        )
                        .with(jwtFor(data.candidate())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Application not found"));

        assertThat(applicationRepository.findById(application.getId()))
                .get()
                .extracting(Application::getStatus)
                .isEqualTo(ApplicationStatus.APPLIED);
        assertThat(statusHistoryRepository.findAll()).isEmpty();
    }

    @Test
    void acceptedApplicationReturnsConflictAndCreatesNoWithdrawalHistory()
            throws Exception {

        assertConflictDoesNotCreateHistory(ApplicationStatus.ACCEPTED);
    }

    @Test
    void rejectedApplicationReturnsConflictAndCreatesNoWithdrawalHistory()
            throws Exception {

        assertConflictDoesNotCreateHistory(ApplicationStatus.REJECTED);
    }

    @Test
    void alreadyWithdrawnApplicationReturnsConflictAndCreatesNoNewHistory()
            throws Exception {

        assertConflictDoesNotCreateHistory(ApplicationStatus.WITHDRAWN);
    }

    @Test
    void unauthenticatedRequestIsDeniedAndCreatesNoHistory() throws Exception {
        TestData data = createTestData();
        Application application = saveApplication(
                data.candidate(),
                data.job(),
                ApplicationStatus.APPLIED
        );

        mockMvc.perform(patch(
                                "/api/applications/me/{applicationId}/withdraw",
                                application.getId()
                        ))
                .andExpect(status().isUnauthorized());

        assertThat(applicationRepository.findById(application.getId()))
                .get()
                .extracting(Application::getStatus)
                .isEqualTo(ApplicationStatus.APPLIED);
        assertThat(statusHistoryRepository.findAll()).isEmpty();
    }

    @Test
    void recruiterAndAdminRequestsAreDeniedAndCreateNoHistory()
            throws Exception {

        TestData data = createTestData();
        Application application = saveApplication(
                data.candidate(),
                data.job(),
                ApplicationStatus.APPLIED
        );

        mockMvc.perform(patch(
                                "/api/applications/me/{applicationId}/withdraw",
                                application.getId()
                        )
                        .with(jwtFor(data.recruiter())))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch(
                                "/api/applications/me/{applicationId}/withdraw",
                                application.getId()
                        )
                        .with(jwtFor(data.admin())))
                .andExpect(status().isForbidden());

        assertThat(applicationRepository.findById(application.getId()))
                .get()
                .extracting(Application::getStatus)
                .isEqualTo(ApplicationStatus.APPLIED);
        assertThat(statusHistoryRepository.findAll()).isEmpty();
    }

    @Test
    void timelineContainsOneWithdrawalTransitionAfterSuccessfulRequest()
            throws Exception {

        TestData data = createTestData();
        Application application = saveApplication(
                data.candidate(),
                data.job(),
                ApplicationStatus.REVIEWING
        );

        mockMvc.perform(patch(
                                "/api/applications/me/{applicationId}/withdraw",
                                application.getId()
                        )
                        .with(jwtFor(data.candidate())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WITHDRAWN"));

        mockMvc.perform(get(
                                "/api/applications/me/{applicationId}/timeline",
                                application.getId()
                        )
                        .with(jwtFor(data.candidate())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].status").value("WITHDRAWN"))
                .andExpect(jsonPath("$[0].eventType").value("TRANSITION"))
                .andExpect(jsonPath("$[0].changedAt").isString());
    }

    private void assertSuccessfulWithdrawal(ApplicationStatus originalStatus)
            throws Exception {

        TestData data = createTestData();
        Application application = saveApplication(
                data.candidate(),
                data.job(),
                originalStatus
        );

        mockMvc.perform(patch(
                                "/api/applications/me/{applicationId}/withdraw",
                                application.getId()
                        )
                        .with(jwtFor(data.candidate())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(application.getId()))
                .andExpect(jsonPath("$.jobId").value(data.job().getId()))
                .andExpect(jsonPath("$.jobTitle").value(data.job().getTitle()))
                .andExpect(jsonPath("$.companyName").value(data.company().getName()))
                .andExpect(jsonPath("$.candidateId").value(data.candidate().getId()))
                .andExpect(jsonPath("$.candidateEmail")
                        .value(data.candidate().getEmail()))
                .andExpect(jsonPath("$.status").value("WITHDRAWN"))
                .andExpect(jsonPath("$.cvFileName").value("withdrawal-cv.pdf"))
                .andExpect(jsonPath("$.appliedAt").isString());

        assertThat(applicationRepository.findById(application.getId()))
                .get()
                .extracting(Application::getStatus)
                .isEqualTo(ApplicationStatus.WITHDRAWN);

        List<ApplicationStatusHistory> history = historyFor(application);

        assertThat(history).hasSize(1);
        assertThat(history.get(0).getStatus())
                .isEqualTo(ApplicationStatus.WITHDRAWN);
        assertThat(history.get(0).getEventType())
                .isEqualTo(ApplicationStatusHistoryEventType.TRANSITION);
        assertThat(history.get(0).getChangedBy().getId())
                .isEqualTo(data.candidate().getId());
        assertThat(history.get(0).getChangedByRole())
                .isEqualTo(Role.CANDIDATE);
        assertThat(history.get(0).getChangedAt()).isNotNull();
    }

    private void assertConflictDoesNotCreateHistory(
            ApplicationStatus originalStatus) throws Exception {

        TestData data = createTestData();
        Application application = saveApplication(
                data.candidate(),
                data.job(),
                originalStatus
        );

        mockMvc.perform(patch(
                                "/api/applications/me/{applicationId}/withdraw",
                                application.getId()
                        )
                        .with(jwtFor(data.candidate())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value(WITHDRAWAL_CONFLICT_MESSAGE));

        assertThat(applicationRepository.findById(application.getId()))
                .get()
                .extracting(Application::getStatus)
                .isEqualTo(originalStatus);
        assertThat(historyFor(application)).isEmpty();
    }

    private List<ApplicationStatusHistory> historyFor(Application application) {
        return statusHistoryRepository
                .findByApplicationIdOrderByChangedAtAscIdAsc(
                        application.getId()
                );
    }

    private void deleteWithdrawalData() {
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
        user.setFirstName("Withdrawal");
        user.setLastName(role.name());
        user.setEmail(role.name().toLowerCase() + "-" + unique + "@example.com");
        user.setPassword("not-used");
        user.setRole(role);
        user.setEnabled(true);

        return userRepository.saveAndFlush(user);
    }

    private Company saveCompany() {
        Company company = new Company();
        company.setName("Withdrawal API Company " + UUID.randomUUID());
        company.setDescription("Application withdrawal API test company");
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
        job.setTitle("Withdrawal API Engineer " + UUID.randomUUID());
        job.setDescription("Build withdrawal API");
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
            ApplicationStatus status) {

        Application application = new Application();
        application.setCandidate(candidate);
        application.setJob(job);
        application.setStatus(status);
        application.setCvFileName("withdrawal-cv.pdf");

        return applicationRepository.saveAndFlush(application);
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
