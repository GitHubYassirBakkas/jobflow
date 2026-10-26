package com.yassir.jobflow;

import com.yassir.jobflow.dto.ScheduleInterviewRequest;
import com.yassir.jobflow.entity.Application;
import com.yassir.jobflow.entity.ApplicationStatus;
import com.yassir.jobflow.entity.ApplicationStatusHistory;
import com.yassir.jobflow.entity.ApplicationStatusHistoryEventType;
import com.yassir.jobflow.entity.CandidateProfile;
import com.yassir.jobflow.entity.Company;
import com.yassir.jobflow.entity.EmploymentType;
import com.yassir.jobflow.entity.InterviewType;
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
import com.yassir.jobflow.service.ApplicationService;
import com.yassir.jobflow.service.InterviewService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;

@SpringBootTest
@ActiveProfiles("test")
class ApplicationStatusHistoryRecordingIntegrationTest
        extends PostgresContainerTest {

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private InterviewService interviewService;

    @MockitoSpyBean
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

    @TempDir
    private Path tempDirectory;

    @BeforeEach
    void cleanDatabase() throws IOException {
        deleteApplicationCvFiles();
        deleteStatusRecordingData();
    }

    @AfterEach
    void cleanAfterTest() throws IOException {
        reset(statusHistoryRepository);
        deleteApplicationCvFiles();
        deleteStatusRecordingData();
    }

    @Test
    void applyingToJobRecordsInitialAppliedBaselineEvent()
            throws Exception {

        TestData data = createTestData();
        saveCandidateProfileWithCv(data.candidate(), "profile-cv.pdf");

        Application application =
                applicationService.applyToJob(
                        data.job().getId(),
                        data.candidate(),
                        null
                );

        List<ApplicationStatusHistory> history = historyFor(application);

        assertThat(history).hasSize(1);
        assertThat(history.get(0).getStatus())
                .isEqualTo(ApplicationStatus.APPLIED);
        assertThat(history.get(0).getEventType())
                .isEqualTo(ApplicationStatusHistoryEventType.BASELINE);
        assertThat(history.get(0).getChangedBy().getId())
                .isEqualTo(data.candidate().getId());
        assertThat(history.get(0).getChangedByRole())
                .isEqualTo(Role.CANDIDATE);
        assertThat(history.get(0).getChangedAt()).isNotNull();
    }

    @Test
    void recruiterStatusChangeRecordsTransitionEvent() {
        TestData data = createTestData();
        Application application = saveApplication(
                data.candidate(),
                data.job(),
                ApplicationStatus.APPLIED
        );

        applicationService.updateApplicationStatus(
                application.getId(),
                ApplicationStatus.REVIEWING,
                data.recruiter()
        );

        List<ApplicationStatusHistory> history = historyFor(application);

        assertThat(history).hasSize(1);
        assertThat(history.get(0).getStatus())
                .isEqualTo(ApplicationStatus.REVIEWING);
        assertThat(history.get(0).getEventType())
                .isEqualTo(ApplicationStatusHistoryEventType.TRANSITION);
        assertThat(history.get(0).getChangedBy().getId())
                .isEqualTo(data.recruiter().getId());
        assertThat(history.get(0).getChangedByRole())
                .isEqualTo(Role.RECRUITER);
        assertThat(applicationRepository.findById(application.getId()))
                .get()
                .extracting(Application::getStatus)
                .isEqualTo(ApplicationStatus.REVIEWING);
    }

    @Test
    void adminStatusChangeRecordsTransitionEvent() {
        TestData data = createTestData();
        Application application = saveApplication(
                data.candidate(),
                data.job(),
                ApplicationStatus.APPLIED
        );

        applicationService.updateApplicationStatusForAdmin(
                application.getId(),
                ApplicationStatus.ACCEPTED,
                data.admin()
        );

        List<ApplicationStatusHistory> history = historyFor(application);

        assertThat(history).hasSize(1);
        assertThat(history.get(0).getStatus())
                .isEqualTo(ApplicationStatus.ACCEPTED);
        assertThat(history.get(0).getEventType())
                .isEqualTo(ApplicationStatusHistoryEventType.TRANSITION);
        assertThat(history.get(0).getChangedBy().getId())
                .isEqualTo(data.admin().getId());
        assertThat(history.get(0).getChangedByRole())
                .isEqualTo(Role.ADMIN);
    }

    @Test
    void schedulingInterviewRecordsInterviewTransitionEvent() {
        TestData data = createTestData();
        Application application = saveApplication(
                data.candidate(),
                data.job(),
                ApplicationStatus.REVIEWING
        );

        interviewService.scheduleInterview(
                application.getId(),
                scheduleInterviewRequest(),
                data.recruiter()
        );

        List<ApplicationStatusHistory> history = historyFor(application);

        assertThat(history).hasSize(1);
        assertThat(history.get(0).getStatus())
                .isEqualTo(ApplicationStatus.INTERVIEW);
        assertThat(history.get(0).getEventType())
                .isEqualTo(ApplicationStatusHistoryEventType.TRANSITION);
        assertThat(history.get(0).getChangedBy().getId())
                .isEqualTo(data.recruiter().getId());
        assertThat(history.get(0).getChangedByRole())
                .isEqualTo(Role.RECRUITER);
    }

    @Test
    void noOpStatusUpdateDoesNotRecordDuplicateEvent()
            throws Exception {

        TestData data = createTestData();
        saveCandidateProfileWithCv(data.candidate(), "profile-cv.pdf");
        Application application =
                applicationService.applyToJob(
                        data.job().getId(),
                        data.candidate(),
                        null
                );

        applicationService.updateApplicationStatus(
                application.getId(),
                ApplicationStatus.APPLIED,
                data.recruiter()
        );

        List<ApplicationStatusHistory> history = historyFor(application);

        assertThat(history).hasSize(1);
        assertThat(history.get(0).getStatus())
                .isEqualTo(ApplicationStatus.APPLIED);
        assertThat(history.get(0).getEventType())
                .isEqualTo(ApplicationStatusHistoryEventType.BASELINE);
    }

    @Test
    void multipleChangesAreReturnedInChronologicalOrder()
            throws Exception {

        TestData data = createTestData();
        saveCandidateProfileWithCv(data.candidate(), "profile-cv.pdf");
        Application application =
                applicationService.applyToJob(
                        data.job().getId(),
                        data.candidate(),
                        null
                );

        applicationService.updateApplicationStatus(
                application.getId(),
                ApplicationStatus.REVIEWING,
                data.recruiter()
        );
        applicationService.updateApplicationStatusForAdmin(
                application.getId(),
                ApplicationStatus.ACCEPTED,
                data.admin()
        );

        List<ApplicationStatusHistory> history = historyFor(application);

        assertThat(history)
                .extracting(ApplicationStatusHistory::getStatus)
                .containsExactly(
                        ApplicationStatus.APPLIED,
                        ApplicationStatus.REVIEWING,
                        ApplicationStatus.ACCEPTED
                );
        assertThat(history)
                .extracting(ApplicationStatusHistory::getEventType)
                .containsExactly(
                        ApplicationStatusHistoryEventType.BASELINE,
                        ApplicationStatusHistoryEventType.TRANSITION,
                        ApplicationStatusHistoryEventType.TRANSITION
                );
    }

    @Test
    void statusUpdateRollsBackWhenHistoryWriteFails() {
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
                applicationService.updateApplicationStatus(
                        application.getId(),
                        ApplicationStatus.REVIEWING,
                        data.recruiter()
                ))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("history write failed");

        assertThat(applicationRepository.findById(application.getId()))
                .get()
                .extracting(Application::getStatus)
                .isEqualTo(ApplicationStatus.APPLIED);
        assertThat(historyFor(application)).isEmpty();
    }

    private List<ApplicationStatusHistory> historyFor(Application application) {
        return statusHistoryRepository
                .findByApplicationIdOrderByChangedAtAscIdAsc(
                        application.getId()
                );
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

    private void deleteStatusRecordingData() {
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
        User recruiter = saveUser(Role.RECRUITER);
        User admin = saveUser(Role.ADMIN);
        Company company = saveCompany();

company.setCreatedBy(recruiter);
company = companyRepository.saveAndFlush(company);

saveRecruiterProfile(recruiter, company);

        Job job = saveJob(company, recruiter);

        return new TestData(candidate, recruiter, admin, company, job);
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
        company.setName("Status Recording Company " + UUID.randomUUID());
        company.setDescription("Status recording integration test company");
        company.setLocation("Remote");
        company.setWebsite("https://example.com");

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
        job.setTitle("Status Recording Role " + UUID.randomUUID());
        job.setDescription("Status recording test role");
        job.setLocation("Remote");
        job.setEmploymentType(EmploymentType.FULL_TIME);
        job.setCompany(company);
        job.setCreatedBy(recruiter);
        job.setActive(true);

        return jobRepository.saveAndFlush(job);
    }

    private CandidateProfile saveCandidateProfileWithCv(
            User candidate,
            String cvFileName) throws IOException {

        Path cvPath = tempDirectory.resolve(cvFileName);
        Files.writeString(
                cvPath,
                "%PDF-1.4 profile cv",
                StandardCharsets.UTF_8
        );

        CandidateProfile profile = new CandidateProfile();
        profile.setUser(candidate);
        profile.setPhone("555-0100");
        profile.setLocation("Remote");
        profile.setBio("Status recording test candidate");
        profile.setSkills("Java, Spring");
        profile.setCvFileName(cvFileName);
        profile.setCvFilePath(cvPath.toString());

        return candidateProfileRepository.saveAndFlush(profile);
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

    private ScheduleInterviewRequest scheduleInterviewRequest() {
        ScheduleInterviewRequest request = new ScheduleInterviewRequest();
        request.setScheduledAt(LocalDateTime.now().plusDays(3));
        request.setType(InterviewType.ONLINE);
        request.setMeetingLink("https://example.com/status-recording");
        request.setNotes("Status recording interview");
        return request;
    }

    private record TestData(
            User candidate,
            User recruiter,
            User admin,
            Company company,
            Job job) {
    }
}
