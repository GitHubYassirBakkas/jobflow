package com.yassir.jobflow;

import com.yassir.jobflow.entity.Application;
import com.yassir.jobflow.entity.CandidateProfile;
import com.yassir.jobflow.entity.Company;
import com.yassir.jobflow.entity.EmploymentType;
import com.yassir.jobflow.entity.Job;
import com.yassir.jobflow.entity.RecruiterProfile;
import com.yassir.jobflow.entity.Role;
import com.yassir.jobflow.entity.User;
import com.yassir.jobflow.exception.ResourceNotFoundException;
import com.yassir.jobflow.repository.ApplicationRepository;
import com.yassir.jobflow.repository.CandidateProfileRepository;
import com.yassir.jobflow.repository.CompanyRepository;
import com.yassir.jobflow.repository.InterviewRepository;
import com.yassir.jobflow.repository.JobRepository;
import com.yassir.jobflow.repository.RecruiterProfileRepository;
import com.yassir.jobflow.repository.UserRepository;
import com.yassir.jobflow.service.JobService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class JobServiceAuthorizationIntegrationTest extends PostgresContainerTest {

    @Autowired
    private JobService jobService;

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

    @BeforeEach
    void cleanDatabase() {
        interviewRepository.deleteAll();
        applicationRepository.deleteAll();
        candidateProfileRepository.deleteAll();
        jobRepository.deleteAll();
        recruiterProfileRepository.deleteAll();
        companyRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void activeJobCanBeRetrievedThroughRoleAwareFlow() {
        TestData data = createTestData();

        Job job = jobService.getJobByIdForUser(
                data.activeJobA().getId(),
                data.candidate()
        );

        assertThat(job.getId()).isEqualTo(data.activeJobA().getId());
        assertThat(job.isActive()).isTrue();
    }

    @Test
    void candidateCannotAccessInactiveJob() {
        TestData data = createTestData();

        assertThatThrownBy(() ->
                jobService.getJobByIdForUser(
                        data.inactiveJobA().getId(),
                        data.candidate()
                ))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Job not found");
    }

    @Test
    void owningRecruiterCanAccessOwnInactiveJob() {
        TestData data = createTestData();

        Job job = jobService.getJobByIdForUser(
                data.inactiveJobA().getId(),
                data.recruiterA()
        );

        assertThat(job.getId()).isEqualTo(data.inactiveJobA().getId());
        assertThat(job.isActive()).isFalse();
    }

    @Test
    void otherRecruiterCannotAccessCompanyAInactiveJob() {
        TestData data = createTestData();

        assertThatThrownBy(() ->
                jobService.getJobByIdForUser(
                        data.inactiveJobA().getId(),
                        data.recruiterB()
                ))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Job not found");
    }

    @Test
    void adminCanAccessInactiveJob() {
        TestData data = createTestData();

        Job job = jobService.getJobByIdForUser(
                data.inactiveJobA().getId(),
                data.admin()
        );

        assertThat(job.getId()).isEqualTo(data.inactiveJobA().getId());
        assertThat(job.isActive()).isFalse();
    }

    @Test
    void activeJobListingDoesNotExposeInactiveJobs() {
        TestData data = createTestData();

        assertThat(jobService.getActiveJobs())
                .extracting(Job::getId)
                .contains(data.activeJobA().getId())
                .doesNotContain(data.inactiveJobA().getId());
    }

    @Test
    void missingJobAndHiddenInactiveJobUseSameNotFoundSemantics() {
        TestData data = createTestData();
        Long missingJobId = data.inactiveJobA().getId() + 10_000L;

        assertThatThrownBy(() ->
                jobService.getJobByIdForUser(
                        missingJobId,
                        data.candidate()
                ))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Job not found");

        assertThatThrownBy(() ->
                jobService.getJobByIdForUser(
                        data.inactiveJobA().getId(),
                        data.candidate()
                ))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Job not found");
    }

    private TestData createTestData() {
        Company companyA = saveCompany("Company A");
        Company companyB = saveCompany("Company B");

        User candidate = saveUser(Role.CANDIDATE);
        User recruiterA = saveUser(Role.RECRUITER);
        User recruiterB = saveUser(Role.RECRUITER);
        User admin = saveUser(Role.ADMIN);

       // Assign the trusted creator of each company.
       companyA.setCreatedBy(recruiterA);
       companyB.setCreatedBy(recruiterB);

       companyA = companyRepository.saveAndFlush(companyA);
       companyB = companyRepository.saveAndFlush(companyB);

      saveCandidateProfile(candidate);
      saveRecruiterProfile(recruiterA, companyA);
      saveRecruiterProfile(recruiterB, companyB);

        Job activeJobA = saveJob(companyA, recruiterA, true);
        Job inactiveJobA = saveJob(companyA, recruiterA, false);
        saveJob(companyB, recruiterB, false);

        return new TestData(
                candidate,
                recruiterA,
                recruiterB,
                admin,
                activeJobA,
                inactiveJobA
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

    private record TestData(
            User candidate,
            User recruiterA,
            User recruiterB,
            User admin,
            Job activeJobA,
            Job inactiveJobA) {
    }
}
