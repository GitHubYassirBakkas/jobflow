package com.yassir.jobflow;

import com.yassir.jobflow.dto.ScheduleInterviewRequest;
import com.yassir.jobflow.entity.Application;
import com.yassir.jobflow.entity.ApplicationStatus;
import com.yassir.jobflow.entity.ApplicationStatusHistory;
import com.yassir.jobflow.entity.ApplicationStatusHistoryEventType;
import com.yassir.jobflow.entity.CandidateProfile;
import com.yassir.jobflow.entity.Company;
import com.yassir.jobflow.entity.EmploymentType;
import com.yassir.jobflow.entity.Interview;
import com.yassir.jobflow.entity.InterviewStatus;
import com.yassir.jobflow.entity.InterviewType;
import com.yassir.jobflow.entity.Job;
import com.yassir.jobflow.entity.RecruiterProfile;
import com.yassir.jobflow.entity.Role;
import com.yassir.jobflow.entity.User;
import com.yassir.jobflow.exception.BusinessConflictException;
import com.yassir.jobflow.exception.ResourceNotFoundException;
import com.yassir.jobflow.repository.ApplicationRepository;
import com.yassir.jobflow.repository.ApplicationStatusHistoryRepository;
import com.yassir.jobflow.repository.CandidateProfileRepository;
import com.yassir.jobflow.repository.CompanyRepository;
import com.yassir.jobflow.repository.InterviewRepository;
import com.yassir.jobflow.repository.JobRepository;
import com.yassir.jobflow.repository.RecruiterProfileRepository;
import com.yassir.jobflow.repository.UserRepository;
import com.yassir.jobflow.service.ApplicationService;
import com.yassir.jobflow.service.InterviewService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class ApplicationWithdrawalServiceIntegrationTest extends PostgresContainerTest {

    private static final String WITHDRAWAL_CONFLICT_MESSAGE =
            "Application cannot be withdrawn from its current status";
    private static final Instant FIXED_HISTORY_INSTANT =
            Instant.parse("2026-10-13T20:11:21Z");

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private InterviewService interviewService;

    @MockitoSpyBean
    private ApplicationStatusHistoryRepository statusHistoryRepository;

    @MockitoBean
    private Clock clock;

    @Autowired
    private InterviewRepository interviewRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private CandidateProfileRepository candidateProfileRepository;

    @Autowired
    private RecruiterProfileRepository recruiterProfileRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private UserRepository userRepository;

    @TempDir
    private Path tempDirectory;

    @BeforeEach
    void cleanDatabase() {
        when(clock.instant()).thenReturn(FIXED_HISTORY_INSTANT);
        deleteWithdrawalData();
    }

    @AfterEach
    void cleanAfterTest() {
        reset(statusHistoryRepository);
        deleteWithdrawalData();
    }

    @Test
    void withdrawsAppliedApplication() {
        TestData data = createTestData();
        Application application = saveApplication(
                data.candidate(),
                data.job(),
                ApplicationStatus.APPLIED
        );

        Application withdrawn =
                applicationService.withdrawApplication(
                        application.getId(),
                        data.candidate()
                );

        assertThat(withdrawn.getStatus()).isEqualTo(ApplicationStatus.WITHDRAWN);
        assertThat(applicationRepository.findById(application.getId()))
                .get()
                .extracting(Application::getStatus)
                .isEqualTo(ApplicationStatus.WITHDRAWN);
    }

    @Test
    void withdrawsReviewingApplication() {
        TestData data = createTestData();
        Application application = saveApplication(
                data.candidate(),
                data.job(),
                ApplicationStatus.REVIEWING
        );

        applicationService.withdrawApplication(
                application.getId(),
                data.candidate()
        );

        assertThat(applicationRepository.findById(application.getId()))
                .get()
                .extracting(Application::getStatus)
                .isEqualTo(ApplicationStatus.WITHDRAWN);
    }

    @Test
    void withdrawsInterviewApplication() {
        TestData data = createTestData();
        Application application = saveApplication(
                data.candidate(),
                data.job(),
                ApplicationStatus.INTERVIEW
        );

        applicationService.withdrawApplication(
                application.getId(),
                data.candidate()
        );

        assertThat(applicationRepository.findById(application.getId()))
                .get()
                .extracting(Application::getStatus)
                .isEqualTo(ApplicationStatus.WITHDRAWN);
    }

    @Test
    void rejectsAcceptedApplicationWithdrawal() {
        assertWithdrawalRejectedFromStatus(ApplicationStatus.ACCEPTED);
    }

    @Test
    void rejectsRejectedApplicationWithdrawal() {
        assertWithdrawalRejectedFromStatus(ApplicationStatus.REJECTED);
    }

    @Test
    void rejectsAlreadyWithdrawnApplicationWithdrawal() {
        assertWithdrawalRejectedFromStatus(ApplicationStatus.WITHDRAWN);
    }

    @Test
    void otherCandidateReceivesNotFoundAndApplicationIsUnchanged() {
        TestData data = createTestData();
        User otherCandidate = saveUser(Role.CANDIDATE);
        Application application = saveApplication(
                data.candidate(),
                data.job(),
                ApplicationStatus.APPLIED
        );

        assertThatThrownBy(() ->
                applicationService.withdrawApplication(
                        application.getId(),
                        otherCandidate
                ))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Application not found");

        assertThat(applicationRepository.findById(application.getId()))
                .get()
                .extracting(Application::getStatus)
                .isEqualTo(ApplicationStatus.APPLIED);
        assertThat(historyFor(application)).isEmpty();
    }

    @Test
    void missingApplicationReceivesSameNotFoundBehavior() {
        TestData data = createTestData();

        assertThatThrownBy(() ->
                applicationService.withdrawApplication(
                        999_999L,
                        data.candidate()
                ))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Application not found");
    }

    @Test
    void withdrawalRecordsExactlyOneCandidateTransition() {
        TestData data = createTestData();
        Application application = saveApplication(
                data.candidate(),
                data.job(),
                ApplicationStatus.REVIEWING
        );

        applicationService.withdrawApplication(
                application.getId(),
                data.candidate()
        );

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
        assertThat(history.get(0).getChangedAtInstant())
                .isEqualTo(FIXED_HISTORY_INSTANT);
        assertThat(history.get(0).getChangedAt())
                .isEqualTo(LocalDateTime.ofInstant(
                        FIXED_HISTORY_INSTANT,
                        ZoneOffset.UTC
                ));
    }

    @Test
    void withdrawalPreservesCvSnapshot() throws IOException {
        TestData data = createTestData();
        Path cvPath = tempDirectory.resolve("submitted-cv.pdf");
        Files.writeString(cvPath, "%PDF-1.4 submitted cv");
        Application application = saveApplication(
                data.candidate(),
                data.job(),
                ApplicationStatus.APPLIED
        );
        application.setCvFileName("submitted-cv.pdf");
        application.setCvFilePath(cvPath.toString());
        applicationRepository.saveAndFlush(application);

        applicationService.withdrawApplication(
                application.getId(),
                data.candidate()
        );

        Application reloaded =
                applicationRepository.findById(application.getId()).orElseThrow();

        assertThat(reloaded.getCvFileName()).isEqualTo("submitted-cv.pdf");
        assertThat(reloaded.getCvFilePath()).isEqualTo(cvPath.toString());
        assertThat(Files.exists(cvPath)).isTrue();
    }

    @Test
    void withdrawalPreservesInterviewRecords() {
        TestData data = createTestData();
        Application application = saveApplication(
                data.candidate(),
                data.job(),
                ApplicationStatus.INTERVIEW
        );
        Interview interview = saveInterview(application);

        applicationService.withdrawApplication(
                application.getId(),
                data.candidate()
        );

        assertThat(interviewRepository.findById(interview.getId()))
                .get()
                .extracting(Interview::getStatus)
                .isEqualTo(InterviewStatus.SCHEDULED);
        assertThat(interviewRepository.findByApplicationId(application.getId()))
                .isPresent();
    }

    @Test
    void withdrawalDoesNotEnableReapplicationToSameJob() {
        TestData data = createTestData();
        Application application = saveApplication(
                data.candidate(),
                data.job(),
                ApplicationStatus.APPLIED
        );

        applicationService.withdrawApplication(
                application.getId(),
                data.candidate()
        );

        assertThatThrownBy(() ->
                applicationService.applyToJob(
                        data.job().getId(),
                        data.candidate(),
                        null
                ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("You have already applied to this job");
        assertThat(applicationRepository.findAll()).hasSize(1);
    }

    @Test
    void withdrawalRollsBackStatusWhenHistoryWriteFails() {
        TestData data = createTestData();
        Application application = saveApplication(
                data.candidate(),
                data.job(),
                ApplicationStatus.APPLIED
        );

        doThrow(new RuntimeException("history write failed"))
                .when(statusHistoryRepository)
                .saveAndFlush(any(ApplicationStatusHistory.class));

        assertThatThrownBy(() ->
                applicationService.withdrawApplication(
                        application.getId(),
                        data.candidate()
                ))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("history write failed");

        assertThat(applicationRepository.findById(application.getId()))
                .get()
                .extracting(Application::getStatus)
                .isEqualTo(ApplicationStatus.APPLIED);
        assertThat(historyFor(application)).isEmpty();
    }

    @Test
    void concurrentDuplicateWithdrawalsCreateOnlyOneTransition()
            throws Exception {

        TestData data = createTestData();
        Application application = saveApplication(
                data.candidate(),
                data.job(),
                ApplicationStatus.APPLIED
        );
        CountDownLatch startGate = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Callable<String> withdrawal = () -> {
                startGate.await(5, TimeUnit.SECONDS);

                try {
                    applicationService.withdrawApplication(
                            application.getId(),
                            data.candidate()
                    );
                    return "success";
                } catch (BusinessConflictException exception) {
                    return "conflict";
                }
            };

            Future<String> first = executor.submit(withdrawal);
            Future<String> second = executor.submit(withdrawal);
            startGate.countDown();

            assertThat(List.of(
                    first.get(15, TimeUnit.SECONDS),
                    second.get(15, TimeUnit.SECONDS)
            ))
                    .containsExactlyInAnyOrder("success", "conflict");
        } finally {
            executor.shutdownNow();
        }

        assertThat(applicationRepository.findById(application.getId()))
                .get()
                .extracting(Application::getStatus)
                .isEqualTo(ApplicationStatus.WITHDRAWN);
        assertThat(historyFor(application))
                .extracting(ApplicationStatusHistory::getStatus)
                .containsExactly(ApplicationStatus.WITHDRAWN);
    }

    @Test
    void concurrentRecruiterStatusUpdateCannotOverwriteWithdrawal()
            throws Exception {

        TestData data = createTestData();
        Application application = saveApplication(
                data.candidate(),
                data.job(),
                ApplicationStatus.APPLIED
        );
        CountDownLatch startGate = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<String> withdrawal = executor.submit(() -> {
                startGate.await(5, TimeUnit.SECONDS);
                applicationService.withdrawApplication(
                        application.getId(),
                        data.candidate()
                );
                return "withdrawn";
            });
            Future<String> recruiterUpdate = executor.submit(() -> {
                startGate.await(5, TimeUnit.SECONDS);

                try {
                    applicationService.updateApplicationStatus(
                            application.getId(),
                            ApplicationStatus.REVIEWING,
                            data.recruiter()
                    );
                    return "updated";
                } catch (BusinessConflictException exception) {
                    return "conflict";
                }
            });
            startGate.countDown();

            assertThat(List.of(
                    withdrawal.get(15, TimeUnit.SECONDS),
                    recruiterUpdate.get(15, TimeUnit.SECONDS)
            ))
                    .contains("withdrawn");
        } finally {
            executor.shutdownNow();
        }

        assertFinalStatusAndLastHistory(application, ApplicationStatus.WITHDRAWN);
    }

    @Test
    void concurrentInterviewSchedulingCannotReactivateWithdrawal()
            throws Exception {

        TestData data = createTestData();
        Application application = saveApplication(
                data.candidate(),
                data.job(),
                ApplicationStatus.REVIEWING
        );
        CountDownLatch startGate = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<String> withdrawal = executor.submit(() -> {
                startGate.await(5, TimeUnit.SECONDS);
                applicationService.withdrawApplication(
                        application.getId(),
                        data.candidate()
                );
                return "withdrawn";
            });
            Future<String> scheduling = executor.submit(() -> {
                startGate.await(5, TimeUnit.SECONDS);

                try {
                    interviewService.scheduleInterview(
                            application.getId(),
                            scheduleInterviewRequest(),
                            data.recruiter()
                    );
                    return "scheduled";
                } catch (BusinessConflictException exception) {
                    return "conflict";
                }
            });
            startGate.countDown();

            assertThat(List.of(
                    withdrawal.get(15, TimeUnit.SECONDS),
                    scheduling.get(15, TimeUnit.SECONDS)
            ))
                    .contains("withdrawn");
        } finally {
            executor.shutdownNow();
        }

        assertFinalStatusAndLastHistory(application, ApplicationStatus.WITHDRAWN);
        interviewRepository.findByApplicationId(application.getId())
                .ifPresent(interview ->
                        assertThat(interview.getStatus())
                                .isEqualTo(InterviewStatus.SCHEDULED)
                );
    }

    @Test
    void withdrawnApplicationCannotBeReactivated() {
        TestData data = createTestData();
        Application application = saveApplication(
                data.candidate(),
                data.job(),
                ApplicationStatus.WITHDRAWN
        );

        assertThatThrownBy(() ->
                applicationService.updateApplicationStatus(
                        application.getId(),
                        ApplicationStatus.REVIEWING,
                        data.recruiter()
                ))
                .isInstanceOf(BusinessConflictException.class)
                .hasMessage("Withdrawn applications cannot be reactivated");

        assertThatThrownBy(() ->
                interviewService.scheduleInterview(
                        application.getId(),
                        scheduleInterviewRequest(),
                        data.recruiter()
                ))
                .isInstanceOf(BusinessConflictException.class)
                .hasMessage("Cannot schedule an interview for a withdrawn application");
    }

    private void assertWithdrawalRejectedFromStatus(ApplicationStatus status) {
        TestData data = createTestData();
        Application application = saveApplication(
                data.candidate(),
                data.job(),
                status
        );

        assertThatThrownBy(() ->
                applicationService.withdrawApplication(
                        application.getId(),
                        data.candidate()
                ))
                .isInstanceOf(BusinessConflictException.class)
                .hasMessage(WITHDRAWAL_CONFLICT_MESSAGE);

        assertThat(applicationRepository.findById(application.getId()))
                .get()
                .extracting(Application::getStatus)
                .isEqualTo(status);
        assertThat(historyFor(application)).isEmpty();
    }

    private void assertFinalStatusAndLastHistory(
            Application application,
            ApplicationStatus status) {

        assertThat(applicationRepository.findById(application.getId()))
                .get()
                .extracting(Application::getStatus)
                .isEqualTo(status);
        assertThat(historyFor(application))
                .last()
                .extracting(ApplicationStatusHistory::getStatus)
                .isEqualTo(status);
    }

    private List<ApplicationStatusHistory> historyFor(Application application) {
        return statusHistoryRepository
                .findByApplicationIdOrderByChangedAtAscIdAsc(
                        application.getId()
                );
    }

    private void deleteWithdrawalData() {
        statusHistoryRepository.deleteAll();
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

        company.setCreatedBy(recruiter);
        company = companyRepository.saveAndFlush(company);

        saveCandidateProfile(candidate);
        saveRecruiterProfile(recruiter, company);
        Job job = saveJob(company, recruiter);

        return new TestData(candidate, recruiter, company, job);
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
        company.setName("Withdrawal Company " + UUID.randomUUID());
        company.setDescription("Withdrawal service test company");
        company.setLocation("Remote");
        company.setWebsite("https://example.com");

        return companyRepository.saveAndFlush(company);
    }

    private void saveCandidateProfile(User candidate) {
        CandidateProfile profile = new CandidateProfile();
        profile.setUser(candidate);
        profile.setPhone("555-0100");
        profile.setLocation("Remote");
        profile.setBio("Withdrawal service test candidate");
        profile.setSkills("Java, Spring");

        candidateProfileRepository.saveAndFlush(profile);
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
        job.setTitle("Withdrawal Role " + UUID.randomUUID());
        job.setDescription("Withdrawal service test role");
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

    private Interview saveInterview(Application application) {
        Interview interview = new Interview();
        interview.setApplication(application);
        interview.setScheduledAt(LocalDateTime.now().plusDays(2));
        interview.setType(InterviewType.ONLINE);
        interview.setMeetingLink("https://example.com/meeting");
        interview.setNotes("Initial interview");
        interview.setStatus(InterviewStatus.SCHEDULED);

        return interviewRepository.saveAndFlush(interview);
    }

    private ScheduleInterviewRequest scheduleInterviewRequest() {
        ScheduleInterviewRequest request = new ScheduleInterviewRequest();
        request.setScheduledAt(LocalDateTime.now().plusDays(3));
        request.setType(InterviewType.ONLINE);
        request.setMeetingLink("https://example.com/new-meeting");
        request.setNotes("Withdrawal service interview");
        return request;
    }

    private record TestData(
            User candidate,
            User recruiter,
            Company company,
            Job job) {
    }
}
