package com.yassir.jobflow;

import com.yassir.jobflow.dto.ScheduleInterviewRequest;
import com.yassir.jobflow.entity.Application;
import com.yassir.jobflow.entity.ApplicationStatus;
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
import com.yassir.jobflow.exception.ForbiddenException;
import com.yassir.jobflow.exception.ResourceNotFoundException;
import com.yassir.jobflow.repository.ApplicationRepository;
import com.yassir.jobflow.repository.ApplicationStatusHistoryRepository;
import com.yassir.jobflow.repository.CandidateProfileRepository;
import com.yassir.jobflow.repository.CompanyRepository;
import com.yassir.jobflow.repository.InterviewRepository;
import com.yassir.jobflow.repository.JobRepository;
import com.yassir.jobflow.repository.RecruiterProfileRepository;
import com.yassir.jobflow.repository.UserRepository;
import com.yassir.jobflow.service.InterviewService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class InterviewServiceAuthorizationIntegrationTest extends PostgresContainerTest {

    @Autowired
    private InterviewService interviewService;

    @Autowired
    private InterviewRepository interviewRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private ApplicationStatusHistoryRepository statusHistoryRepository;

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

    @Test
    void candidateCanAccessOwnInterview() {
        TestData data = createTestData();

        Interview detail =
                interviewService.getInterviewByApplicationForCandidate(
                        data.applicationA().getId(),
                        data.candidateA()
                );
        List<Interview> candidateInterviews =
                interviewService.getInterviewsForCandidate(data.candidateA());

        assertThat(detail.getId()).isEqualTo(data.interviewA().getId());
        assertThat(candidateInterviews)
                .extracting(Interview::getId)
                .containsExactly(data.interviewA().getId());
    }

    @Test
    void otherCandidateCannotAccessCandidateAInterview() {
        TestData data = createTestData();

        assertThatThrownBy(() ->
                interviewService.getInterviewByApplicationForCandidate(
                        data.applicationA().getId(),
                        data.candidateB()
                ))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("You are not allowed to view this interview");

        assertThat(interviewService.getInterviewsForCandidate(data.candidateB()))
                .extracting(Interview::getId)
                .doesNotContain(data.interviewA().getId());
    }

    @Test
    void recruiterCanAccessOwnCompanyInterviews() {
        TestData data = createTestData();

        List<Interview> interviews =
                interviewService.getInterviewsForRecruiter(data.recruiterA());

        assertThat(interviews)
                .extracting(Interview::getId)
                .containsExactly(data.interviewA().getId());
    }

    @Test
    void otherRecruiterCannotAccessOwnCompanyInterviewInScopedList() {
        TestData data = createTestData();

        assertThat(interviewService.getInterviewsForRecruiter(data.recruiterB()))
                .extracting(Interview::getId)
                .doesNotContain(data.interviewA().getId());
    }

    @Test
    void recruiterCanScheduleInterviewForOwnCompanyApplication() {
        TestData data = createTestData();

        Interview interview =
                interviewService.scheduleInterview(
                        data.unscheduledApplicationA().getId(),
                        scheduleInterviewRequest(),
                        data.recruiterA()
                );

        assertThat(interview.getStatus()).isEqualTo(InterviewStatus.SCHEDULED);
        assertThat(interview.getApplication().getId())
                .isEqualTo(data.unscheduledApplicationA().getId());
        assertThat(applicationRepository.findById(
                data.unscheduledApplicationA().getId()))
                .get()
                .extracting(Application::getStatus)
                .isEqualTo(ApplicationStatus.INTERVIEW);
    }

    @Test
    void otherRecruiterCannotScheduleInterviewForCompanyAApplication() {
        TestData data = createTestData();

        assertThatThrownBy(() ->
                interviewService.scheduleInterview(
                        data.unscheduledApplicationA().getId(),
                        scheduleInterviewRequest(),
                        data.recruiterB()
                ))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage(
                        "You are not allowed to schedule an interview for this application"
                );
    }

    @Test
    void recruiterCannotScheduleInterviewForWithdrawnApplication() {
        TestData data = createTestData();
        data.unscheduledApplicationA().setStatus(ApplicationStatus.WITHDRAWN);
        applicationRepository.saveAndFlush(data.unscheduledApplicationA());

        assertThatThrownBy(() ->
                interviewService.scheduleInterview(
                        data.unscheduledApplicationA().getId(),
                        scheduleInterviewRequest(),
                        data.recruiterA()
                ))
                .isInstanceOf(BusinessConflictException.class)
                .hasMessage("Cannot schedule an interview for a withdrawn application");

        assertThat(applicationRepository.findById(
                data.unscheduledApplicationA().getId()))
                .get()
                .extracting(Application::getStatus)
                .isEqualTo(ApplicationStatus.WITHDRAWN);
    }

    @Test
    void recruiterCanUpdateInterviewStatusForOwnCompanyInterview() {
        TestData data = createTestData();

        Interview updated =
                interviewService.updateInterviewStatus(
                        data.interviewA().getId(),
                        InterviewStatus.COMPLETED,
                        data.recruiterA()
                );

        assertThat(updated.getStatus()).isEqualTo(InterviewStatus.COMPLETED);
    }

    @Test
    void otherRecruiterCannotUpdateInterviewStatusForCompanyAInterview() {
        TestData data = createTestData();

        assertThatThrownBy(() ->
                interviewService.updateInterviewStatus(
                        data.interviewA().getId(),
                        InterviewStatus.COMPLETED,
                        data.recruiterB()
                ))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("You are not allowed to update this interview");

        assertThat(interviewRepository.findById(data.interviewA().getId()))
                .get()
                .extracting(Interview::getStatus)
                .isEqualTo(InterviewStatus.SCHEDULED);
    }

    @Test
    void missingInterviewAndForbiddenInterviewRemainDifferentFailures() {
        TestData data = createTestData();
        Long missingInterviewId = data.interviewA().getId() + 10_000L;

        assertThatThrownBy(() ->
                interviewService.updateInterviewStatus(
                        missingInterviewId,
                        InterviewStatus.COMPLETED,
                        data.recruiterA()
                ))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Interview not found");

        assertThatThrownBy(() ->
                interviewService.updateInterviewStatus(
                        data.interviewA().getId(),
                        InterviewStatus.COMPLETED,
                        data.recruiterB()
                ))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("You are not allowed to update this interview");
    }

    private TestData createTestData() {
        Company companyA = saveCompany("Company A");
        Company companyB = saveCompany("Company B");

        User candidateA = saveUser(Role.CANDIDATE);
        User candidateB = saveUser(Role.CANDIDATE);
        User recruiterA = saveUser(Role.RECRUITER);
        User recruiterB = saveUser(Role.RECRUITER);

        companyA.setCreatedBy(recruiterA);
        companyB.setCreatedBy(recruiterB);

        companyA = companyRepository.saveAndFlush(companyA);
        companyB = companyRepository.saveAndFlush(companyB);

       saveCandidateProfile(candidateA);
       saveCandidateProfile(candidateB);
       saveRecruiterProfile(recruiterA, companyA);
       saveRecruiterProfile(recruiterB, companyB);

        Job jobA = saveJob(companyA, recruiterA, true);
        Job jobB = saveJob(companyB, recruiterB, true);
        Application applicationA = saveApplication(candidateA, jobA);
        saveApplication(candidateB, jobB);
        Application unscheduledApplicationA = saveApplication(candidateB, jobA);
        Interview interviewA = saveInterview(applicationA);

        return new TestData(
                candidateA,
                candidateB,
                recruiterA,
                recruiterB,
                applicationA,
                unscheduledApplicationA,
                interviewA
        );
    }

    private User saveUser(Role role) {
        String unique = UUID.randomUUID().toString();

        User user = new User();
        user.setFirstName("Test");
        user.setLastName(role.name());
        user.setEmail(role.name().toLowerCase() + "-" + unique + "@example.com");
        user.setPassword("not-used");
        user.setRole(role);
        user.setEnabled(true);

        return userRepository.saveAndFlush(user);
    }

    private Company saveCompany(String name) {
        Company company = new Company();
        company.setName(name + " " + UUID.randomUUID());
        company.setDescription("Test company");
        company.setLocation("Remote");
        company.setWebsite("https://example.com");

        return companyRepository.saveAndFlush(company);
    }

    private void saveCandidateProfile(User candidate) {
        CandidateProfile profile = new CandidateProfile();
        profile.setUser(candidate);
        profile.setPhone("555-0100");
        profile.setLocation("Remote");
        profile.setBio("Test candidate");
        profile.setSkills("Java");

        candidateProfileRepository.saveAndFlush(profile);
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
        job.setTitle("Backend Engineer");
        job.setDescription("Build Spring services");
        job.setLocation("Remote");
        job.setEmploymentType(EmploymentType.FULL_TIME);
        job.setCompany(company);
        job.setCreatedBy(recruiter);
        job.setActive(active);

        return jobRepository.saveAndFlush(job);
    }

    private Application saveApplication(User candidate, Job job) {
        Application application = new Application();
        application.setCandidate(candidate);
        application.setJob(job);
        application.setStatus(ApplicationStatus.APPLIED);
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
        request.setNotes("Follow up");
        return request;
    }

    private record TestData(
            User candidateA,
            User candidateB,
            User recruiterA,
            User recruiterB,
            Application applicationA,
            Application unscheduledApplicationA,
            Interview interviewA) {
    }
}
