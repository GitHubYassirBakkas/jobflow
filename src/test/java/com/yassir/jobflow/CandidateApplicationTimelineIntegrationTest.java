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

import java.time.Instant;
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
class CandidateApplicationTimelineIntegrationTest
        extends PostgresContainerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ApplicationStatusHistoryRepository statusHistoryRepository;

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

    @BeforeEach
    void cleanDatabase() {
        deleteTimelineData();
    }

    @AfterEach
    void cleanAfterTest() {
        deleteTimelineData();
    }

    @Test
    void candidateAccessesOwnTimeline() throws Exception {
        TestData data = createTestData();
        Application application = saveApplication(
                data.candidate(),
                data.job(),
                ApplicationStatus.REVIEWING
        );
        LocalDateTime appliedAt = LocalDateTime.of(2026, 10, 10, 9, 0);

        saveHistory(
                application,
                ApplicationStatus.APPLIED,
                appliedAt,
                ApplicationStatusHistoryEventType.BASELINE,
                data.candidate()
        );

        mockMvc.perform(get(
                                "/api/applications/me/{applicationId}/timeline",
                                application.getId()
                        )
                        .with(jwtFor(data.candidate())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].status").value("APPLIED"))
                .andExpect(jsonPath("$[0].changedAt").isString())
                .andExpect(jsonPath("$[0].eventType").value("BASELINE"));
    }

    @Test
    void timelineSerializesNewUtcInstantsAndLegacyLocalTimestamps()
            throws Exception {

        TestData data = createTestData();
        Application application = saveApplication(
                data.candidate(),
                data.job(),
                ApplicationStatus.REVIEWING
        );
        LocalDateTime legacyChangedAt =
                LocalDateTime.of(2026, 10, 10, 9, 0);
        LocalDateTime utcChangedAt =
                LocalDateTime.of(2026, 10, 13, 20, 11, 21, 34614000);
        Instant changedAtInstant =
                Instant.parse("2026-10-13T20:11:21.034614Z");

        saveHistory(
                application,
                ApplicationStatus.APPLIED,
                legacyChangedAt,
                ApplicationStatusHistoryEventType.BASELINE,
                data.candidate()
        );
        saveHistory(
                application,
                ApplicationStatus.REVIEWING,
                utcChangedAt,
                changedAtInstant,
                ApplicationStatusHistoryEventType.TRANSITION,
                data.recruiter()
        );

        mockMvc.perform(get(
                                "/api/applications/me/{applicationId}/timeline",
                                application.getId()
                        )
                        .with(jwtFor(data.candidate())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].status").value("APPLIED"))
                .andExpect(jsonPath("$[0].changedAt")
                        .value("2026-10-10T09:00"))
                .andExpect(jsonPath("$[1].status").value("REVIEWING"))
                .andExpect(jsonPath("$[1].changedAt")
                        .value("2026-10-13T20:11:21.034614Z"));
    }

    @Test
    void timelineEventsAreOrderedByChangedAtThenIdAscending()
            throws Exception {

        TestData data = createTestData();
        Application application = saveApplication(
                data.candidate(),
                data.job(),
                ApplicationStatus.INTERVIEW
        );
        LocalDateTime first = LocalDateTime.of(2026, 10, 10, 9, 0);
        LocalDateTime second = LocalDateTime.of(2026, 10, 10, 10, 0);

        saveHistory(
                application,
                ApplicationStatus.REVIEWING,
                second,
                ApplicationStatusHistoryEventType.TRANSITION,
                data.recruiter()
        );
        saveHistory(
                application,
                ApplicationStatus.APPLIED,
                first,
                ApplicationStatusHistoryEventType.BASELINE,
                data.candidate()
        );
        saveHistory(
                application,
                ApplicationStatus.INTERVIEW,
                second,
                ApplicationStatusHistoryEventType.TRANSITION,
                data.recruiter()
        );

        mockMvc.perform(get(
                                "/api/applications/me/{applicationId}/timeline",
                                application.getId()
                        )
                        .with(jwtFor(data.candidate())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].status").value("APPLIED"))
                .andExpect(jsonPath("$[1].status").value("REVIEWING"))
                .andExpect(jsonPath("$[2].status").value("INTERVIEW"));
    }

    @Test
    void baselineAndTransitionEventTypesArePreserved()
            throws Exception {

        TestData data = createTestData();
        Application application = saveApplication(
                data.candidate(),
                data.job(),
                ApplicationStatus.ACCEPTED
        );

        saveHistory(
                application,
                ApplicationStatus.APPLIED,
                LocalDateTime.of(2026, 10, 10, 9, 0),
                ApplicationStatusHistoryEventType.BASELINE,
                data.candidate()
        );
        saveHistory(
                application,
                ApplicationStatus.ACCEPTED,
                LocalDateTime.of(2026, 10, 10, 10, 0),
                ApplicationStatusHistoryEventType.TRANSITION,
                data.admin()
        );

        mockMvc.perform(get(
                                "/api/applications/me/{applicationId}/timeline",
                                application.getId()
                        )
                        .with(jwtFor(data.candidate())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].eventType").value("BASELINE"))
                .andExpect(jsonPath("$[1].eventType").value("TRANSITION"));
    }

    @Test
    void candidateCannotAccessAnotherCandidatesTimeline()
            throws Exception {

        TestData data = createTestData();
        Application application = saveApplication(
                data.otherCandidate(),
                data.job(),
                ApplicationStatus.APPLIED
        );
        saveHistory(
                application,
                ApplicationStatus.APPLIED,
                LocalDateTime.of(2026, 10, 10, 9, 0),
                ApplicationStatusHistoryEventType.BASELINE,
                data.otherCandidate()
        );

        mockMvc.perform(get(
                                "/api/applications/me/{applicationId}/timeline",
                                application.getId()
                        )
                        .with(jwtFor(data.candidate())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Application not found"));
    }

    @Test
    void missingApplicationReturnsNotFound() throws Exception {
        TestData data = createTestData();

        mockMvc.perform(get(
                                "/api/applications/me/{applicationId}/timeline",
                                999_999L
                        )
                        .with(jwtFor(data.candidate())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Application not found"));
    }

    @Test
    void unauthenticatedRequestIsDenied() throws Exception {
        TestData data = createTestData();
        Application application = saveApplication(
                data.candidate(),
                data.job(),
                ApplicationStatus.APPLIED
        );

        mockMvc.perform(get(
                                "/api/applications/me/{applicationId}/timeline",
                                application.getId()
                        ))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void recruiterAndAdminAccessIsDenied() throws Exception {
        TestData data = createTestData();
        Application application = saveApplication(
                data.candidate(),
                data.job(),
                ApplicationStatus.APPLIED
        );

        mockMvc.perform(get(
                                "/api/applications/me/{applicationId}/timeline",
                                application.getId()
                        )
                        .with(jwtFor(data.recruiter())))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(
                                "/api/applications/me/{applicationId}/timeline",
                                application.getId()
                        )
                        .with(jwtFor(data.admin())))
                .andExpect(status().isForbidden());
    }

    @Test
    void emptyHistoryReturnsEmptyList() throws Exception {
        TestData data = createTestData();
        Application application = saveApplication(
                data.candidate(),
                data.job(),
                ApplicationStatus.APPLIED
        );

        mockMvc.perform(get(
                                "/api/applications/me/{applicationId}/timeline",
                                application.getId()
                        )
                        .with(jwtFor(data.candidate())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void actorIdentitiesAreNotExposed() throws Exception {
        TestData data = createTestData();
        Application application = saveApplication(
                data.candidate(),
                data.job(),
                ApplicationStatus.REVIEWING
        );
        saveHistory(
                application,
                ApplicationStatus.REVIEWING,
                LocalDateTime.of(2026, 10, 10, 9, 0),
                ApplicationStatusHistoryEventType.TRANSITION,
                data.recruiter()
        );

        mockMvc.perform(get(
                                "/api/applications/me/{applicationId}/timeline",
                                application.getId()
                        )
                        .with(jwtFor(data.candidate())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("REVIEWING"))
                .andExpect(jsonPath("$[0].changedBy").doesNotExist())
                .andExpect(jsonPath("$[0].changedByRole").doesNotExist())
                .andExpect(jsonPath("$[0].id").doesNotExist())
                .andExpect(jsonPath("$[0].application").doesNotExist());
    }

    private void deleteTimelineData() {
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
        user.setFirstName("Timeline");
        user.setLastName(role.name());
        user.setEmail(role.name().toLowerCase() + "-" + unique + "@example.com");
        user.setPassword("not-used");
        user.setRole(role);
        user.setEnabled(true);

        return userRepository.saveAndFlush(user);
    }

    private Company saveCompany() {
        Company company = new Company();
        company.setName("Timeline Company " + UUID.randomUUID());
        company.setDescription("Application timeline integration test company");
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
        job.setTitle("Timeline Engineer " + UUID.randomUUID());
        job.setDescription("Build application timelines");
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
        application.setCvFileName("timeline-cv.pdf");

        return applicationRepository.saveAndFlush(application);
    }

    private ApplicationStatusHistory saveHistory(
            Application application,
            ApplicationStatus status,
            LocalDateTime changedAt,
            ApplicationStatusHistoryEventType eventType,
            User changedBy) {

        return saveHistory(
                application,
                status,
                changedAt,
                null,
                eventType,
                changedBy
        );
    }

    private ApplicationStatusHistory saveHistory(
            Application application,
            ApplicationStatus status,
            LocalDateTime changedAt,
            Instant changedAtInstant,
            ApplicationStatusHistoryEventType eventType,
            User changedBy) {

        ApplicationStatusHistory history = new ApplicationStatusHistory();
        history.setApplication(application);
        history.setStatus(status);
        history.setChangedAt(changedAt);
        history.setChangedAtInstant(changedAtInstant);
        history.setEventType(eventType);

        if (changedBy != null) {
            history.setChangedBy(changedBy);
            history.setChangedByRole(changedBy.getRole());
        }

        return statusHistoryRepository.saveAndFlush(history);
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
