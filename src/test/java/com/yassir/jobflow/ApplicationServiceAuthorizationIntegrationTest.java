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
import com.yassir.jobflow.service.ApplicationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class ApplicationServiceAuthorizationIntegrationTest extends PostgresContainerTest {

    @Autowired
    private ApplicationService applicationService;

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

    @Autowired
    private InterviewRepository interviewRepository;

    @TempDir
    private Path tempDirectory;

    @BeforeEach
    void cleanDatabase() {
        interviewRepository.deleteAll();
        statusHistoryRepository.deleteAll();
        applicationRepository.deleteAll();
        candidateProfileRepository.deleteAll();
        jobRepository.deleteAll();
        recruiterProfileRepository.deleteAll();
        companyRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void candidateApplicationsAreScopedToTheRequestedCandidate() {
        TestData data = createTestData();

        List<Application> candidateAApplications =
                applicationService.getApplicationsByCandidate(
                        data.candidateA().getId()
                );

        List<Application> candidateBApplications =
                applicationService.getApplicationsByCandidate(
                        data.candidateB().getId()
                );

        assertThat(candidateAApplications)
                .extracting(Application::getId)
                .containsExactly(data.applicationA().getId());

        assertThat(candidateBApplications)
                .extracting(Application::getId)
                .doesNotContain(data.applicationA().getId());
    }

    @Test
    void recruiterCanViewApplicationsForOwnCompanyJob() {
        TestData data = createTestData();

        List<Application> applications =
                applicationService.getApplicationsByJob(
                        data.jobA().getId(),
                        data.recruiterA()
                );

        assertThat(applications)
                .extracting(Application::getId)
                .containsExactly(data.applicationA().getId());
    }

    @Test
    void recruiterCannotViewApplicationsForAnotherCompanyJob() {
        TestData data = createTestData();

        assertThatThrownBy(() ->
                applicationService.getApplicationsByJob(
                        data.jobA().getId(),
                        data.recruiterB()
                ))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("You are not allowed to view applications for this job");
    }

    @Test
    void recruiterCanUpdateApplicationStatusForOwnCompanyApplication() {
        TestData data = createTestData();

        Application updated =
                applicationService.updateApplicationStatus(
                        data.applicationA().getId(),
                        ApplicationStatus.REVIEWING,
                        data.recruiterA()
                );

        assertThat(updated.getStatus())
                .isEqualTo(ApplicationStatus.REVIEWING);
    }

    @Test
    void recruiterCannotAssignWithdrawnStatus() {
        TestData data = createTestData();

        assertThatThrownBy(() ->
                applicationService.updateApplicationStatus(
                        data.applicationA().getId(),
                        ApplicationStatus.WITHDRAWN,
                        data.recruiterA()
                ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("WITHDRAWN status can only be set by candidate withdrawal");

        assertThat(applicationRepository.findById(data.applicationA().getId()))
                .get()
                .extracting(Application::getStatus)
                .isEqualTo(ApplicationStatus.APPLIED);
    }

    @Test
    void adminCannotAssignWithdrawnStatus() {
        TestData data = createTestData();
        User admin = saveUser(Role.ADMIN);

        assertThatThrownBy(() ->
                applicationService.updateApplicationStatusForAdmin(
                        data.applicationA().getId(),
                        ApplicationStatus.WITHDRAWN,
                        admin
                ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("WITHDRAWN status can only be set by candidate withdrawal");

        assertThat(applicationRepository.findById(data.applicationA().getId()))
                .get()
                .extracting(Application::getStatus)
                .isEqualTo(ApplicationStatus.APPLIED);
    }

    @Test
    void recruiterCannotUpdateApplicationStatusForAnotherCompanyApplication() {
        TestData data = createTestData();

        assertThatThrownBy(() ->
                applicationService.updateApplicationStatus(
                        data.applicationA().getId(),
                        ApplicationStatus.REVIEWING,
                        data.recruiterB()
                ))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("You are not allowed to update this application");

        assertThat(applicationRepository.findById(data.applicationA().getId()))
                .get()
                .extracting(Application::getStatus)
                .isEqualTo(ApplicationStatus.APPLIED);
    }

    @Test
    void recruiterCanAccessCvForOwnCompanyApplication() throws IOException {
        TestData data = createTestDataWithReadableApplicationCv();

        Resource resource =
                applicationService.getApplicationCv(
                        data.applicationA().getId(),
                        data.recruiterA()
                );

        assertThat(resource.exists()).isTrue();
        assertThat(resource.isReadable()).isTrue();
    }

    @Test
    void recruiterCannotAccessCvForAnotherCompanyApplication() throws IOException {
        TestData data = createTestDataWithReadableApplicationCv();

        assertThatThrownBy(() ->
                applicationService.getApplicationCv(
                        data.applicationA().getId(),
                        data.recruiterB()
                ))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("You are not allowed to view this CV");
    }

    @Test
    void missingApplicationAndForbiddenApplicationRemainDifferentFailures() {
        TestData data = createTestData();
        Long missingApplicationId = data.applicationA().getId() + 10_000L;

        assertThatThrownBy(() ->
                applicationService.updateApplicationStatus(
                        missingApplicationId,
                        ApplicationStatus.REVIEWING,
                        data.recruiterA()
                ))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Application not found");

        assertThatThrownBy(() ->
                applicationService.updateApplicationStatus(
                        data.applicationA().getId(),
                        ApplicationStatus.REVIEWING,
                        data.recruiterB()
                ))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("You are not allowed to update this application");
    }

    private TestData createTestDataWithReadableApplicationCv()
            throws IOException {

        TestData data = createTestData();
        Path applicationCv = tempDirectory.resolve("application-cv.pdf");
        Files.writeString(applicationCv, "%PDF-1.4 test fixture");

        data.applicationA().setCvFileName("application-cv.pdf");
        data.applicationA().setCvFilePath(applicationCv.toString());
        Application savedApplication =
                applicationRepository.saveAndFlush(data.applicationA());

        return new TestData(
                data.candidateA(),
                data.candidateB(),
                data.recruiterA(),
                data.recruiterB(),
                data.jobA(),
                savedApplication
        );
    }

    private TestData createTestData() {
        Company companyA = saveCompany("Company A");
        Company companyB = saveCompany("Company B");

        User candidateA = saveUser(Role.CANDIDATE);
User candidateB = saveUser(Role.CANDIDATE);
User recruiterA = saveUser(Role.RECRUITER);
User recruiterB = saveUser(Role.RECRUITER);

// Give each test company its trusted creator.
companyA.setCreatedBy(recruiterA);
companyB.setCreatedBy(recruiterB);

companyRepository.saveAndFlush(companyA);
companyRepository.saveAndFlush(companyB);

saveCandidateProfile(candidateA);
saveCandidateProfile(candidateB);
saveRecruiterProfile(recruiterA, companyA);
saveRecruiterProfile(recruiterB, companyB);
        Job jobA = saveJob(companyA, recruiterA);
        Application applicationA =
                saveApplication(candidateA, jobA, "candidate-a-cv.pdf");

        return new TestData(
                candidateA,
                candidateB,
                recruiterA,
                recruiterB,
                jobA,
                applicationA
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

    private Job saveJob(Company company, User recruiter) {
        Job job = new Job();
        job.setTitle("Backend Engineer");
        job.setDescription("Build Spring services");
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
            String cvFileName) {

        Application application = new Application();
        application.setCandidate(candidate);
        application.setJob(job);
        application.setStatus(ApplicationStatus.APPLIED);
        application.setCvFileName(cvFileName);

        return applicationRepository.saveAndFlush(application);
    }

    private record TestData(
            User candidateA,
            User candidateB,
            User recruiterA,
            User recruiterB,
            Job jobA,
            Application applicationA) {
    }
}
