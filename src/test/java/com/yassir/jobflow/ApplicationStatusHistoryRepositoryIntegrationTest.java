package com.yassir.jobflow;

import com.yassir.jobflow.entity.Application;
import com.yassir.jobflow.entity.ApplicationStatus;
import com.yassir.jobflow.entity.ApplicationStatusHistory;
import com.yassir.jobflow.entity.ApplicationStatusHistoryEventType;
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
import com.yassir.jobflow.repository.SavedJobRepository;
import com.yassir.jobflow.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class ApplicationStatusHistoryRepositoryIntegrationTest
        extends PostgresContainerTest {

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

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        deleteStatusHistoryData();
    }

    @AfterEach
    void cleanAfterTest() {
        deleteStatusHistoryData();
    }

    @Test
    void savesAndLoadsStatusHistoryMapping() {
        TestData data = createTestData();
        Application application = saveApplication(
                data.candidate(),
                data.job(),
                ApplicationStatus.APPLIED
        );
        LocalDateTime changedAt = LocalDateTime.of(2026, 10, 10, 9, 30);

        ApplicationStatusHistory history = new ApplicationStatusHistory();
        history.setApplication(application);
        history.setStatus(ApplicationStatus.REVIEWING);
        history.setChangedAt(changedAt);
        history.setChangedBy(data.recruiter());
        history.setChangedByRole(Role.RECRUITER);
        history.setEventType(ApplicationStatusHistoryEventType.TRANSITION);

        ApplicationStatusHistory saved =
                statusHistoryRepository.saveAndFlush(history);

        ApplicationStatusHistory reloaded =
                statusHistoryRepository.findById(saved.getId()).orElseThrow();

        assertThat(reloaded.getApplication().getId())
                .isEqualTo(application.getId());
        assertThat(reloaded.getStatus())
                .isEqualTo(ApplicationStatus.REVIEWING);
        assertThat(reloaded.getChangedAt())
                .isEqualTo(changedAt);
        assertThat(reloaded.getChangedAtInstant()).isNull();
        assertThat(reloaded.getChangedBy().getId())
                .isEqualTo(data.recruiter().getId());
        assertThat(reloaded.getChangedByRole())
                .isEqualTo(Role.RECRUITER);
        assertThat(reloaded.getEventType())
                .isEqualTo(ApplicationStatusHistoryEventType.TRANSITION);
    }

    @Test
    void savesAndLoadsUtcInstantWhenPresent() {
        TestData data = createTestData();
        Application application = saveApplication(
                data.candidate(),
                data.job(),
                ApplicationStatus.APPLIED
        );
        Instant changedAtInstant =
                Instant.parse("2026-10-13T20:11:21.034614Z");
        LocalDateTime changedAt =
                LocalDateTime.of(2026, 10, 13, 20, 11, 21, 34614000);

        ApplicationStatusHistory history = new ApplicationStatusHistory();
        history.setApplication(application);
        history.setStatus(ApplicationStatus.REVIEWING);
        history.setChangedAt(changedAt);
        history.setChangedAtInstant(changedAtInstant);
        history.setChangedBy(data.recruiter());
        history.setChangedByRole(Role.RECRUITER);
        history.setEventType(ApplicationStatusHistoryEventType.TRANSITION);

        ApplicationStatusHistory saved =
                statusHistoryRepository.saveAndFlush(history);

        ApplicationStatusHistory reloaded =
                statusHistoryRepository.findById(saved.getId()).orElseThrow();

        assertThat(reloaded.getChangedAt()).isEqualTo(changedAt);
        assertThat(reloaded.getChangedAtInstant()).isEqualTo(changedAtInstant);
    }

    @Test
    void retrievesHistoryByApplicationOrderedByChangedAtThenIdAscending() {
        TestData data = createTestData();
        Application application = saveApplication(
                data.candidate(),
                data.job(),
                ApplicationStatus.APPLIED
        );
        Application otherApplication = saveApplication(
                data.otherCandidate(),
                data.job(),
                ApplicationStatus.APPLIED
        );
        LocalDateTime olderTime = LocalDateTime.of(2026, 10, 10, 9, 0);
        LocalDateTime newerTime = LocalDateTime.of(2026, 10, 10, 10, 0);

        ApplicationStatusHistory older = saveHistory(
                application,
                ApplicationStatus.APPLIED,
                olderTime,
                ApplicationStatusHistoryEventType.BASELINE
        );
        ApplicationStatusHistory sameTimeLowerId = saveHistory(
                application,
                ApplicationStatus.REVIEWING,
                newerTime,
                ApplicationStatusHistoryEventType.TRANSITION
        );
        ApplicationStatusHistory sameTimeHigherId = saveHistory(
                application,
                ApplicationStatus.INTERVIEW,
                newerTime,
                ApplicationStatusHistoryEventType.TRANSITION
        );
        saveHistory(
                otherApplication,
                ApplicationStatus.APPLIED,
                olderTime.minusHours(1),
                ApplicationStatusHistoryEventType.BASELINE
        );

        List<ApplicationStatusHistory> history =
                statusHistoryRepository
                        .findByApplicationIdOrderByChangedAtAscIdAsc(
                                application.getId()
                        );

        assertThat(history)
                .extracting(ApplicationStatusHistory::getId)
                .containsExactly(
                        older.getId(),
                        sameTimeLowerId.getId(),
                        sameTimeHigherId.getId()
                );
    }

    @Test
    void foreignKeyRejectsHistoryForMissingApplication() {
        assertThatThrownBy(() ->
                jdbcTemplate.update(
                        """
                        INSERT INTO application_status_history (
                            application_id,
                            status,
                            changed_at,
                            event_type
                        )
                        VALUES (?, ?, ?, ?)
                        """,
                        999_999L,
                        "APPLIED",
                        LocalDateTime.now(),
                        "BASELINE"
                ))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void existingApplicationDataRemainsIntactWhenHistoryExists() {
        TestData data = createTestData();
        Application application = saveApplication(
                data.candidate(),
                data.job(),
                ApplicationStatus.ACCEPTED
        );

        saveHistory(
                application,
                ApplicationStatus.ACCEPTED,
                LocalDateTime.of(2026, 10, 10, 12, 0),
                ApplicationStatusHistoryEventType.BASELINE
        );

        Application reloaded =
                applicationRepository.findById(application.getId())
                        .orElseThrow();

        assertThat(reloaded.getStatus())
                .isEqualTo(ApplicationStatus.ACCEPTED);
        assertThat(reloaded.getCandidate().getId())
                .isEqualTo(data.candidate().getId());
        assertThat(reloaded.getJob().getId())
                .isEqualTo(data.job().getId());
    }

    @Test
    void applicationAndHistoryConstraintsAcceptWithdrawnStatus() {
        TestData data = createTestData();
        Application application = saveApplication(
                data.candidate(),
                data.job(),
                ApplicationStatus.WITHDRAWN
        );

        ApplicationStatusHistory history = saveHistory(
                application,
                ApplicationStatus.WITHDRAWN,
                LocalDateTime.of(2026, 10, 12, 9, 0),
                ApplicationStatusHistoryEventType.TRANSITION
        );

        assertThat(applicationRepository.findById(application.getId()))
                .get()
                .extracting(Application::getStatus)
                .isEqualTo(ApplicationStatus.WITHDRAWN);
        assertThat(statusHistoryRepository.findById(history.getId()))
                .get()
                .extracting(ApplicationStatusHistory::getStatus)
                .isEqualTo(ApplicationStatus.WITHDRAWN);
    }

    private void deleteStatusHistoryData() {
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
        Company company = saveCompany();
        Job job = saveJob(company, recruiter);

        return new TestData(candidate, otherCandidate, recruiter, company, job);
    }

    private User saveUser(Role role) {
        String unique = UUID.randomUUID().toString();

        User user = new User();
        user.setFirstName("Status");
        user.setLastName(role.name());
        user.setEmail(role.name().toLowerCase() + "-" + unique + "@example.com");
        user.setPassword("not-used");
        user.setRole(role);
        user.setEnabled(true);

        return userRepository.saveAndFlush(user);
    }

    private Company saveCompany() {
        Company company = new Company();
        company.setName("Status History Company " + UUID.randomUUID());
        company.setDescription("Status history repository test company");
        company.setLocation("Remote");
        company.setWebsite("https://example.com");

        return companyRepository.saveAndFlush(company);
    }

    private Job saveJob(Company company, User recruiter) {
        Job job = new Job();
        job.setTitle("Status History Role " + UUID.randomUUID());
        job.setDescription("Status history repository test role");
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
        application.setCvFileName("candidate-cv.pdf");

        return applicationRepository.saveAndFlush(application);
    }

    private ApplicationStatusHistory saveHistory(
            Application application,
            ApplicationStatus status,
            LocalDateTime changedAt,
            ApplicationStatusHistoryEventType eventType) {

        ApplicationStatusHistory history = new ApplicationStatusHistory();
        history.setApplication(application);
        history.setStatus(status);
        history.setChangedAt(changedAt);
        history.setEventType(eventType);

        return statusHistoryRepository.saveAndFlush(history);
    }

    private record TestData(
            User candidate,
            User otherCandidate,
            User recruiter,
            Company company,
            Job job) {
    }
}
