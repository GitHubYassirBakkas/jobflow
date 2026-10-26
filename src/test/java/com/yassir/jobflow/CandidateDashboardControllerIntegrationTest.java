package com.yassir.jobflow;

import com.yassir.jobflow.entity.Application;
import com.yassir.jobflow.entity.ApplicationStatus;
import com.yassir.jobflow.entity.Company;
import com.yassir.jobflow.entity.EmploymentType;
import com.yassir.jobflow.entity.Interview;
import com.yassir.jobflow.entity.InterviewStatus;
import com.yassir.jobflow.entity.InterviewType;
import com.yassir.jobflow.entity.Job;
import com.yassir.jobflow.entity.RecruiterProfile;
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

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CandidateDashboardControllerIntegrationTest extends PostgresContainerTest {

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
        deleteDashboardData();
    }

    @AfterEach
    void cleanAfterTest() {
        deleteDashboardData();
    }

    @Test
    void emptyDashboardReturnsZeroCountsAndEmptyPreviews() throws Exception {
        TestData data = createTestData();

        mockMvc.perform(get("/api/candidate/dashboard")
                        .with(jwtFor(data.candidate())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalApplications").value(0))
                .andExpect(jsonPath("$.activeApplications").value(0))
                .andExpect(jsonPath("$.withdrawnApplications").value(0))
                .andExpect(jsonPath("$.upcomingInterviews").value(0))
                .andExpect(jsonPath("$.savedJobs").value(0))
                .andExpect(jsonPath("$.recentApplications", hasSize(0)))
                .andExpect(jsonPath("$.nextInterviews", hasSize(0)));
    }

    @Test
    void dashboardReturnsApplicationCountsSavedJobsAndRecentApplications()
            throws Exception {

        TestData data = createTestData();
        LocalDateTime baseTime = LocalDateTime.of(2026, 2, 1, 12, 0);

        Application oldestActive = saveApplication(
                data.candidate(),
                saveJob(data.company(), data.recruiter(), true, "Old Active"),
                ApplicationStatus.APPLIED,
                baseTime
        );
        saveApplication(
                data.candidate(),
                saveJob(data.company(), data.recruiter(), true, "Reviewing"),
                ApplicationStatus.REVIEWING,
                baseTime.plusDays(1)
        );
        saveApplication(
                data.candidate(),
                saveJob(data.company(), data.recruiter(), true, "Interview"),
                ApplicationStatus.INTERVIEW,
                baseTime.plusDays(2)
        );
        Application accepted = saveApplication(
                data.candidate(),
                saveJob(data.company(), data.recruiter(), true, "Accepted"),
                ApplicationStatus.ACCEPTED,
                baseTime.plusDays(3)
        );
        Application rejected = saveApplication(
                data.candidate(),
                saveJob(data.company(), data.recruiter(), true, "Rejected"),
                ApplicationStatus.REJECTED,
                baseTime.plusDays(4)
        );
        Application withdrawn = saveApplication(
                data.candidate(),
                saveJob(data.company(), data.recruiter(), true, "Withdrawn"),
                ApplicationStatus.WITHDRAWN,
                baseTime.plusDays(5)
        );
        Application sameTimestampOlder = saveApplication(
                data.candidate(),
                saveJob(data.company(), data.recruiter(), true, "Same Older"),
                ApplicationStatus.APPLIED,
                baseTime.plusDays(6)
        );
        Application sameTimestampNewer = saveApplication(
                data.candidate(),
                saveJob(data.company(), data.recruiter(), true, "Same Newer"),
                ApplicationStatus.REVIEWING,
                baseTime.plusDays(6)
        );

        saveApplication(
                data.otherCandidate(),
                saveJob(data.company(), data.recruiter(), true, "Other Candidate"),
                ApplicationStatus.APPLIED,
                baseTime.plusDays(7)
        );

        saveSavedJob(
                data.candidate(),
                saveJob(data.company(), data.recruiter(), true, "Saved One")
        );
        saveSavedJob(
                data.candidate(),
                saveJob(data.company(), data.recruiter(), true, "Saved Two")
        );
        saveSavedJob(
                data.candidate(),
                saveJob(data.company(), data.recruiter(), false, "Inactive Saved")
        );
        saveSavedJob(
                data.otherCandidate(),
                saveJob(data.company(), data.recruiter(), true, "Other Saved")
        );

        mockMvc.perform(get("/api/candidate/dashboard")
                        .with(jwtFor(data.candidate())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalApplications").value(8))
                .andExpect(jsonPath("$.activeApplications").value(5))
                .andExpect(jsonPath("$.withdrawnApplications").value(1))
                .andExpect(jsonPath("$.savedJobs").value(2))
                .andExpect(jsonPath("$.recentApplications", hasSize(5)))
                .andExpect(jsonPath("$.recentApplications[0].id")
                        .value(sameTimestampNewer.getId()))
                .andExpect(jsonPath("$.recentApplications[1].id")
                        .value(sameTimestampOlder.getId()))
                .andExpect(jsonPath("$.recentApplications[2].id")
                        .value(withdrawn.getId()))
                .andExpect(jsonPath("$.recentApplications[3].id")
                        .value(rejected.getId()))
                .andExpect(jsonPath("$.recentApplications[4].id")
                        .value(accepted.getId()))
                .andExpect(jsonPath("$.recentApplications[0].candidateId")
                        .value(data.candidate().getId()))
                .andExpect(jsonPath("$.recentApplications[0].cvFileName")
                        .value("candidate-cv.pdf"))
                .andExpect(jsonPath("$.recentApplications[0].cvFilePath")
                        .doesNotExist())
                .andExpect(jsonPath("$.recentApplications[5]").doesNotExist())
                .andExpect(jsonPath("$.recentApplications[?(@.id == %s)]",
                        oldestActive.getId()).doesNotExist());
    }

    @Test
    void dashboardReturnsUpcomingInterviewCountAndNextInterviews()
            throws Exception {

        TestData data = createTestData();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime appliedAt = LocalDateTime.of(2026, 3, 1, 12, 0);

        Application withdrawnApplication = saveApplication(
                data.candidate(),
                saveJob(data.company(), data.recruiter(), true, "Withdrawn Interview"),
                ApplicationStatus.WITHDRAWN,
                appliedAt
        );
        Application firstActiveApplication = saveApplication(
                data.candidate(),
                saveJob(data.company(), data.recruiter(), true, "First Future"),
                ApplicationStatus.INTERVIEW,
                appliedAt.plusMinutes(1)
        );
        Application secondActiveApplication = saveApplication(
                data.candidate(),
                saveJob(data.company(), data.recruiter(), true, "Second Future"),
                ApplicationStatus.INTERVIEW,
                appliedAt.plusMinutes(2)
        );
        Application fourthFutureApplication = saveApplication(
                data.candidate(),
                saveJob(data.company(), data.recruiter(), true, "Fourth Future"),
                ApplicationStatus.INTERVIEW,
                appliedAt.plusMinutes(3)
        );
        Application completedApplication = saveApplication(
                data.candidate(),
                saveJob(data.company(), data.recruiter(), true, "Completed Future"),
                ApplicationStatus.INTERVIEW,
                appliedAt.plusMinutes(4)
        );
        Application cancelledApplication = saveApplication(
                data.candidate(),
                saveJob(data.company(), data.recruiter(), true, "Cancelled Future"),
                ApplicationStatus.INTERVIEW,
                appliedAt.plusMinutes(5)
        );
        Application pastApplication = saveApplication(
                data.candidate(),
                saveJob(data.company(), data.recruiter(), true, "Past Future"),
                ApplicationStatus.INTERVIEW,
                appliedAt.plusMinutes(6)
        );
        Application otherCandidateApplication = saveApplication(
                data.otherCandidate(),
                saveJob(data.company(), data.recruiter(), true, "Other Interview"),
                ApplicationStatus.INTERVIEW,
                appliedAt.plusMinutes(7)
        );

        Interview firstUpcoming = saveInterview(
                withdrawnApplication,
                now.plusDays(1),
                InterviewStatus.SCHEDULED
        );
        Interview secondUpcoming = saveInterview(
                firstActiveApplication,
                now.plusDays(2),
                InterviewStatus.SCHEDULED
        );
        Interview thirdUpcoming = saveInterview(
                secondActiveApplication,
                now.plusDays(3),
                InterviewStatus.SCHEDULED
        );
        saveInterview(
                fourthFutureApplication,
                now.plusDays(4),
                InterviewStatus.SCHEDULED
        );
        saveInterview(
                completedApplication,
                now.plusDays(1),
                InterviewStatus.COMPLETED
        );
        saveInterview(
                cancelledApplication,
                now.plusDays(1),
                InterviewStatus.CANCELLED
        );
        saveInterview(
                pastApplication,
                now.minusDays(1),
                InterviewStatus.SCHEDULED
        );
        saveInterview(
                otherCandidateApplication,
                now.plusHours(1),
                InterviewStatus.SCHEDULED
        );

        mockMvc.perform(get("/api/candidate/dashboard")
                        .with(jwtFor(data.candidate())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.upcomingInterviews").value(4))
                .andExpect(jsonPath("$.nextInterviews", hasSize(3)))
                .andExpect(jsonPath("$.nextInterviews[0].id")
                        .value(firstUpcoming.getId()))
                .andExpect(jsonPath("$.nextInterviews[0].applicationId")
                        .value(withdrawnApplication.getId()))
                .andExpect(jsonPath("$.nextInterviews[0].status")
                        .value("SCHEDULED"))
                .andExpect(jsonPath("$.nextInterviews[1].id")
                        .value(secondUpcoming.getId()))
                .andExpect(jsonPath("$.nextInterviews[2].id")
                        .value(thirdUpcoming.getId()))
                .andExpect(jsonPath("$.nextInterviews[3]").doesNotExist());
    }

    @Test
    void unauthenticatedRecruiterAndAdminRequestsAreDenied() throws Exception {
        TestData data = createTestData();

        mockMvc.perform(get("/api/candidate/dashboard"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/candidate/dashboard")
                        .with(jwtFor(data.recruiter())))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/candidate/dashboard")
                        .with(jwtFor(data.admin())))
                .andExpect(status().isForbidden());
    }

    private void deleteDashboardData() {
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

        return new TestData(candidate, otherCandidate, recruiter, admin, company);
    }

    private User saveUser(Role role) {
        String unique = UUID.randomUUID().toString();

        User user = new User();
        user.setFirstName("Dashboard");
        user.setLastName(role.name());
        user.setEmail(role.name().toLowerCase() + "-" + unique + "@example.com");
        user.setPassword("not-used");
        user.setRole(role);
        user.setEnabled(true);

        return userRepository.saveAndFlush(user);
    }

    private Company saveCompany() {
        Company company = new Company();
        company.setName("Dashboard Company " + UUID.randomUUID());
        company.setDescription("Candidate dashboard test company");
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

    private Job saveJob(
            Company company,
            User recruiter,
            boolean active,
            String title) {

        Job job = new Job();
        job.setTitle(title + " " + UUID.randomUUID());
        job.setDescription("Candidate dashboard test role");
        job.setLocation("Remote");
        job.setEmploymentType(EmploymentType.FULL_TIME);
        job.setCompany(company);
        job.setCreatedBy(recruiter);
        job.setActive(active);

        return jobRepository.saveAndFlush(job);
    }

    private Application saveApplication(
            User candidate,
            Job job,
            ApplicationStatus status,
            LocalDateTime appliedAt) {

        Application application = new Application();
        application.setCandidate(candidate);
        application.setJob(job);
        application.setStatus(status);
        application.setCvFileName("candidate-cv.pdf");
        application.setCvFilePath("uploads/applications/cvs/private.pdf");

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

    private SavedJob saveSavedJob(User candidate, Job job) {
        SavedJob savedJob = new SavedJob();
        savedJob.setCandidate(candidate);
        savedJob.setJob(job);

        return savedJobRepository.saveAndFlush(savedJob);
    }

    private Interview saveInterview(
            Application application,
            LocalDateTime scheduledAt,
            InterviewStatus status) {

        Interview interview = new Interview();
        interview.setApplication(application);
        interview.setScheduledAt(scheduledAt);
        interview.setType(InterviewType.ONLINE);
        interview.setMeetingLink("https://example.com/interview");
        interview.setNotes("Dashboard interview notes");
        interview.setStatus(status);

        return interviewRepository.saveAndFlush(interview);
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
            Company company) {
    }
}
