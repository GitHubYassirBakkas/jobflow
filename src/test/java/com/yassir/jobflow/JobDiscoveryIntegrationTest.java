package com.yassir.jobflow;

import com.yassir.jobflow.dto.JobResponse;
import com.yassir.jobflow.dto.PageResponse;
import com.yassir.jobflow.entity.Company;
import com.yassir.jobflow.entity.EmploymentType;
import com.yassir.jobflow.entity.Job;
import com.yassir.jobflow.entity.Role;
import com.yassir.jobflow.entity.User;
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
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class JobDiscoveryIntegrationTest extends PostgresContainerTest {

    @Autowired
    private JobService jobService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

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
    void noFiltersReturnsActiveJobsOnly() {
        TestData data = createDiscoveryData();

        assertThat(jobService.getActiveJobs())
                .extracting(Job::getId)
                .contains(
                        data.javaBackend().getId(),
                        data.frontend().getId(),
                        data.paymentsContract().getId(),
                        data.designer().getId()
                )
                .doesNotContain(data.inactiveJavaArchitect().getId());
    }

    @Test
    void titleSearchIsCaseInsensitiveAndPartial() {
        TestData data = createDiscoveryData();

        List<Job> jobs = jobService.getActiveJobs(
                "BACK",
                null,
                null
        );

        assertThat(jobs)
                .extracting(Job::getId)
                .containsExactly(data.javaBackend().getId());
    }

    @Test
    void descriptionSearchIsCaseInsensitiveAndPartial() {
        TestData data = createDiscoveryData();

        List<Job> jobs = jobService.getActiveJobs(
                "PAY",
                null,
                null
        );

        assertThat(jobs)
                .extracting(Job::getId)
                .containsExactly(data.paymentsContract().getId());
    }

    @Test
    void companyNameSearchIsCaseInsensitiveAndPartial() {
        TestData data = createDiscoveryData();

        List<Job> jobs = jobService.getActiveJobs(
                "atlas",
                null,
                null
        );

        assertThat(jobs)
                .extracting(Job::getId)
                .containsExactly(data.frontend().getId());
    }

    @Test
    void locationFilterIsCaseInsensitiveAndPartial() {
        TestData data = createDiscoveryData();

        List<Job> jobs = jobService.getActiveJobs(
                null,
                "MEK",
                null
        );

        assertThat(jobs)
                .extracting(Job::getId)
                .containsExactly(
                        data.paymentsContract().getId(),
                        data.javaBackend().getId()
                );
    }

    @Test
    void employmentTypeFilterMatchesExactEnum() {
        TestData data = createDiscoveryData();

        List<Job> jobs = jobService.getActiveJobs(
                null,
                null,
                EmploymentType.PART_TIME
        );

        assertThat(jobs)
                .extracting(Job::getId)
                .containsExactly(data.frontend().getId());
    }

    @Test
    void searchAndLocationCombineWithAndSemantics() {
        TestData data = createDiscoveryData();

        List<Job> jobs = jobService.getActiveJobs(
                "java",
                "meknes",
                null
        );

        assertThat(jobs)
                .extracting(Job::getId)
                .containsExactly(data.javaBackend().getId());
    }

    @Test
    void searchAndEmploymentTypeCombineWithAndSemantics() {
        TestData data = createDiscoveryData();

        List<Job> jobs = jobService.getActiveJobs(
                "java",
                null,
                EmploymentType.FULL_TIME
        );

        assertThat(jobs)
                .extracting(Job::getId)
                .containsExactly(data.javaBackend().getId());
    }

    @Test
    void allFiltersCombineWithAndSemantics() {
        TestData data = createDiscoveryData();

        List<Job> jobs = jobService.getActiveJobs(
                "java",
                "meknes",
                EmploymentType.FULL_TIME
        );

        assertThat(jobs)
                .extracting(Job::getId)
                .containsExactly(data.javaBackend().getId());
    }

    @Test
    void blankSearchBehavesAsAbsent() {
        TestData data = createDiscoveryData();

        assertThat(jobService.getActiveJobs("   ", null, null))
                .extracting(Job::getId)
                .contains(
                        data.javaBackend().getId(),
                        data.frontend().getId(),
                        data.paymentsContract().getId(),
                        data.designer().getId()
                )
                .doesNotContain(data.inactiveJavaArchitect().getId());
    }

    @Test
    void blankLocationBehavesAsAbsent() {
        TestData data = createDiscoveryData();

        assertThat(jobService.getActiveJobs(null, "   ", null))
                .extracting(Job::getId)
                .contains(
                        data.javaBackend().getId(),
                        data.frontend().getId(),
                        data.paymentsContract().getId(),
                        data.designer().getId()
                )
                .doesNotContain(data.inactiveJavaArchitect().getId());
    }

    @Test
    void noMatchingJobsReturnsEmptyList() {
        createDiscoveryData();

        assertThat(jobService.getActiveJobs("golang", null, null))
                .isEmpty();
    }

    @Test
    void matchingInactiveJobIsExcluded() {
        TestData data = createDiscoveryData();

        assertThat(jobService.getActiveJobs("architect", null, null))
                .extracting(Job::getId)
                .doesNotContain(data.inactiveJavaArchitect().getId())
                .isEmpty();
    }

    @Test
    void activeJobsAreOrderedByCreatedAtDescendingThenIdDescending() {
        User recruiter = saveUser(Role.RECRUITER);
        Company company = saveCompany("Ordering Company");

        Job older = saveJob(
                "Older Role",
                "First role",
                "Remote",
                EmploymentType.FULL_TIME,
                company,
                recruiter,
                true
        );
        Job sameTimestampLowerId = saveJob(
                "Same Timestamp Lower Id",
                "Second role",
                "Remote",
                EmploymentType.FULL_TIME,
                company,
                recruiter,
                true
        );
        Job sameTimestampHigherId = saveJob(
                "Same Timestamp Higher Id",
                "Third role",
                "Remote",
                EmploymentType.FULL_TIME,
                company,
                recruiter,
                true
        );

        LocalDateTime olderCreatedAt = LocalDateTime.of(2026, 1, 1, 10, 0);
        LocalDateTime newerCreatedAt = LocalDateTime.of(2026, 1, 2, 10, 0);

        setCreatedAt(older, olderCreatedAt);
        setCreatedAt(sameTimestampLowerId, newerCreatedAt);
        setCreatedAt(sameTimestampHigherId, newerCreatedAt);

        assertThat(jobService.getActiveJobs())
                .extracting(Job::getId)
                .containsExactly(
                        sameTimestampHigherId.getId(),
                        sameTimestampLowerId.getId(),
                        older.getId()
                );
    }

    @Test
    void invalidEmploymentTypeReturnsControlledBadRequest() throws Exception {
        mockMvc.perform(get("/api/jobs")
                        .param("employmentType", "REMOTE_ONLY")
                        .with(jwtFor(Role.CANDIDATE)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Invalid value for parameter: employmentType"));
    }

    @Test
    void defaultPaginationUsesPageZeroAndSizeTen() {
        PaginationData data = createPaginationData();

        PageResponse<JobResponse> response =
                jobService.getActiveJobPage(
                        null,
                        null,
                        null
                );

        assertThat(response.getPage()).isEqualTo(0);
        assertThat(response.getSize()).isEqualTo(10);
        assertThat(response.getTotalElements()).isEqualTo(12);
        assertThat(response.getTotalPages()).isEqualTo(2);
        assertThat(response.isFirst()).isTrue();
        assertThat(response.isLast()).isFalse();
        assertThat(response.getContent()).hasSize(10);
        assertThat(response.getContent())
                .extracting(JobResponse::getId)
                .containsExactlyElementsOf(
                        data.expectedActiveIds().subList(0, 10)
                );
    }

    @Test
    void customPageSizeReturnsFirstPageContent() {
        PaginationData data = createPaginationData();

        PageResponse<JobResponse> response =
                jobService.getActiveJobPage(
                        null,
                        null,
                        null,
                        0,
                        5
                );

        assertThat(response.getPage()).isEqualTo(0);
        assertThat(response.getSize()).isEqualTo(5);
        assertThat(response.getTotalElements()).isEqualTo(12);
        assertThat(response.getTotalPages()).isEqualTo(3);
        assertThat(response.isFirst()).isTrue();
        assertThat(response.isLast()).isFalse();
        assertThat(response.getContent())
                .extracting(JobResponse::getId)
                .containsExactlyElementsOf(
                        data.expectedActiveIds().subList(0, 5)
                );
    }

    @Test
    void secondPageReturnsNextContentAndMetadata() {
        PaginationData data = createPaginationData();

        PageResponse<JobResponse> response =
                jobService.getActiveJobPage(
                        null,
                        null,
                        null,
                        1,
                        5
                );

        assertThat(response.getPage()).isEqualTo(1);
        assertThat(response.getSize()).isEqualTo(5);
        assertThat(response.getTotalElements()).isEqualTo(12);
        assertThat(response.getTotalPages()).isEqualTo(3);
        assertThat(response.isFirst()).isFalse();
        assertThat(response.isLast()).isFalse();
        assertThat(response.getContent())
                .extracting(JobResponse::getId)
                .containsExactlyElementsOf(
                        data.expectedActiveIds().subList(5, 10)
                );
    }

    @Test
    void pageBeyondLastReturnsEmptyContentWithMetadata() {
        createPaginationData();

        PageResponse<JobResponse> response =
                jobService.getActiveJobPage(
                        null,
                        null,
                        null,
                        5,
                        5
                );

        assertThat(response.getPage()).isEqualTo(5);
        assertThat(response.getSize()).isEqualTo(5);
        assertThat(response.getTotalElements()).isEqualTo(12);
        assertThat(response.getTotalPages()).isEqualTo(3);
        assertThat(response.isFirst()).isFalse();
        assertThat(response.isLast()).isTrue();
        assertThat(response.getContent()).isEmpty();
    }

    @Test
    void paginationOrderingUsesCreatedAtDescendingThenIdDescending() {
        User recruiter = saveUser(Role.RECRUITER);
        Company company = saveCompany("Page Ordering Company");

        Job older = saveJob(
                "Older Page Role",
                "First role",
                "Remote",
                EmploymentType.FULL_TIME,
                company,
                recruiter,
                true
        );
        Job sameTimestampLowerId = saveJob(
                "Same Timestamp Page Lower Id",
                "Second role",
                "Remote",
                EmploymentType.FULL_TIME,
                company,
                recruiter,
                true
        );
        Job sameTimestampHigherId = saveJob(
                "Same Timestamp Page Higher Id",
                "Third role",
                "Remote",
                EmploymentType.FULL_TIME,
                company,
                recruiter,
                true
        );

        LocalDateTime olderCreatedAt = LocalDateTime.of(2026, 2, 1, 10, 0);
        LocalDateTime newerCreatedAt = LocalDateTime.of(2026, 2, 2, 10, 0);

        setCreatedAt(older, olderCreatedAt);
        setCreatedAt(sameTimestampLowerId, newerCreatedAt);
        setCreatedAt(sameTimestampHigherId, newerCreatedAt);

        PageResponse<JobResponse> response =
                jobService.getActiveJobPage(
                        null,
                        null,
                        null,
                        0,
                        3
                );

        assertThat(response.getContent())
                .extracting(JobResponse::getId)
                .containsExactly(
                        sameTimestampHigherId.getId(),
                        sameTimestampLowerId.getId(),
                        older.getId()
                );
    }

    @Test
    void inactiveJobsAreExcludedFromPaginationContentAndMetadata() {
        PaginationData data = createPaginationData();

        PageResponse<JobResponse> response =
                jobService.getActiveJobPage(
                        "java",
                        "meknes",
                        EmploymentType.FULL_TIME,
                        0,
                        2
                );

        assertThat(response.getTotalElements()).isEqualTo(4);
        assertThat(response.getTotalPages()).isEqualTo(2);
        assertThat(response.getContent())
                .extracting(JobResponse::getId)
                .doesNotContainAnyElementsOf(data.inactiveIds());
    }

    @Test
    void paginationAndSearchUseSameDiscoverySemantics() {
        PaginationData data = createPaginationData();

        PageResponse<JobResponse> response =
                jobService.getActiveJobPage(
                        "java",
                        null,
                        null,
                        0,
                        4
                );

        assertThat(response.getTotalElements()).isEqualTo(6);
        assertThat(response.getTotalPages()).isEqualTo(2);
        assertThat(response.getContent())
                .extracting(JobResponse::getId)
                .containsExactlyElementsOf(
                        data.javaActiveIds().subList(0, 4)
                );
    }

    @Test
    void paginationAndLocationUseSameDiscoverySemantics() {
        PaginationData data = createPaginationData();

        PageResponse<JobResponse> response =
                jobService.getActiveJobPage(
                        null,
                        "meknes",
                        null,
                        0,
                        5
                );

        assertThat(response.getTotalElements()).isEqualTo(8);
        assertThat(response.getTotalPages()).isEqualTo(2);
        assertThat(response.getContent())
                .extracting(JobResponse::getId)
                .containsExactlyElementsOf(
                        data.meknesActiveIds().subList(0, 5)
                );
    }

    @Test
    void paginationAndEmploymentTypeUseSameDiscoverySemantics() {
        PaginationData data = createPaginationData();

        PageResponse<JobResponse> response =
                jobService.getActiveJobPage(
                        null,
                        null,
                        EmploymentType.FULL_TIME,
                        0,
                        3
                );

        assertThat(response.getTotalElements()).isEqualTo(4);
        assertThat(response.getTotalPages()).isEqualTo(2);
        assertThat(response.getContent())
                .extracting(JobResponse::getId)
                .containsExactlyElementsOf(
                        data.fullTimeActiveIds().subList(0, 3)
                );
    }

    @Test
    void paginationAndAllFiltersUseSameDiscoverySemantics() {
        PaginationData data = createPaginationData();

        PageResponse<JobResponse> response =
                jobService.getActiveJobPage(
                        "java",
                        "meknes",
                        EmploymentType.FULL_TIME,
                        0,
                        2
                );

        assertThat(response.getTotalElements()).isEqualTo(4);
        assertThat(response.getTotalPages()).isEqualTo(2);
        assertThat(response.getContent())
                .extracting(JobResponse::getId)
                .containsExactlyElementsOf(
                        data.fullTimeActiveIds().subList(0, 2)
                );
    }

    @Test
    void negativePageReturnsControlledBadRequest() throws Exception {
        mockMvc.perform(get("/api/jobs")
                        .param("page", "-1")
                        .with(jwtFor(Role.CANDIDATE)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Page must be greater than or equal to 0"));
    }

    @Test
    void zeroSizeReturnsControlledBadRequest() throws Exception {
        mockMvc.perform(get("/api/jobs")
                        .param("size", "0")
                        .with(jwtFor(Role.CANDIDATE)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Size must be greater than or equal to 1"));
    }

    @Test
    void sizeAboveMaximumReturnsControlledBadRequest() throws Exception {
        mockMvc.perform(get("/api/jobs")
                        .param("size", "51")
                        .with(jwtFor(Role.CANDIDATE)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Size must not exceed 50"));
    }

    private TestData createDiscoveryData() {
        User recruiter = saveUser(Role.RECRUITER);
        Company atlasJavaLabs = saveCompany("Atlas Java Labs");
        Company meknesTalent = saveCompany("Meknes Talent");
        Company northstarCareers = saveCompany("Northstar Careers");

        Job javaBackend = saveJob(
                "Java Backend Developer",
                "Build Spring APIs",
                "Meknes, Morocco",
                EmploymentType.FULL_TIME,
                northstarCareers,
                recruiter,
                true
        );
        Job frontend = saveJob(
                "Frontend Engineer",
                "Build React user interfaces",
                "Casablanca",
                EmploymentType.PART_TIME,
                atlasJavaLabs,
                recruiter,
                true
        );
        Job paymentsContract = saveJob(
                "Platform Analyst",
                "Own payments integrations",
                "Meknes Remote",
                EmploymentType.CONTRACT,
                meknesTalent,
                recruiter,
                true
        );
        Job designer = saveJob(
                "Product Designer",
                "Design hiring workflows",
                "Rabat",
                EmploymentType.FREELANCE,
                northstarCareers,
                recruiter,
                true
        );
        Job inactiveJavaArchitect = saveJob(
                "Java Backend Architect",
                "Lead payments platform work",
                "Meknes",
                EmploymentType.FULL_TIME,
                atlasJavaLabs,
                recruiter,
                false
        );

        setCreatedAt(javaBackend, LocalDateTime.of(2026, 1, 1, 9, 0));
        setCreatedAt(frontend, LocalDateTime.of(2026, 1, 2, 9, 0));
        setCreatedAt(paymentsContract, LocalDateTime.of(2026, 1, 3, 9, 0));
        setCreatedAt(designer, LocalDateTime.of(2026, 1, 4, 9, 0));
        setCreatedAt(inactiveJavaArchitect, LocalDateTime.of(2026, 1, 5, 9, 0));

        return new TestData(
                javaBackend,
                frontend,
                paymentsContract,
                designer,
                inactiveJavaArchitect
        );
    }

    private PaginationData createPaginationData() {
        User recruiter = saveUser(Role.RECRUITER);
        Company company = saveCompany("Pagination Company");

        List<Job> activeJobs = new ArrayList<>();
        List<Job> inactiveJobs = new ArrayList<>();

        LocalDateTime baseCreatedAt = LocalDateTime.of(2026, 3, 1, 9, 0);

        for (int index = 1; index <= 12; index++) {
            boolean javaRole = index <= 6;
            String location = index <= 8 ? "Meknes, Morocco" : "Rabat";
            EmploymentType employmentType =
                    index <= 4
                            ? EmploymentType.FULL_TIME
                            : index <= 8
                                    ? EmploymentType.PART_TIME
                                    : EmploymentType.CONTRACT;

            Job job = saveJob(
                    javaRole
                            ? "Java Engineer " + index
                            : "Product Role " + index,
                    "Pagination role " + index,
                    location,
                    employmentType,
                    company,
                    recruiter,
                    true
            );

            setCreatedAt(job, baseCreatedAt.plusDays(index));
            activeJobs.add(job);
        }

        for (int index = 1; index <= 3; index++) {
            Job inactiveJob = saveJob(
                    "Java Inactive Role " + index,
                    "Inactive pagination role",
                    "Meknes",
                    EmploymentType.FULL_TIME,
                    company,
                    recruiter,
                    false
            );

            setCreatedAt(inactiveJob, baseCreatedAt.plusDays(20 + index));
            inactiveJobs.add(inactiveJob);
        }

        List<Job> expectedActiveJobs = new ArrayList<>(activeJobs);
        Collections.reverse(expectedActiveJobs);

        List<Long> expectedActiveIds =
                expectedActiveJobs.stream()
                        .map(Job::getId)
                        .toList();

        List<Long> javaActiveIds =
                expectedActiveJobs.stream()
                        .filter(job -> job.getTitle().contains("Java"))
                        .map(Job::getId)
                        .toList();

        List<Long> meknesActiveIds =
                expectedActiveJobs.stream()
                        .filter(job -> job.getLocation().contains("Meknes"))
                        .map(Job::getId)
                        .toList();

        List<Long> fullTimeActiveIds =
                expectedActiveJobs.stream()
                        .filter(job ->
                                job.getEmploymentType()
                                        == EmploymentType.FULL_TIME)
                        .map(Job::getId)
                        .toList();

        List<Long> inactiveIds =
                inactiveJobs.stream()
                        .map(Job::getId)
                        .toList();

        return new PaginationData(
                expectedActiveIds,
                javaActiveIds,
                meknesActiveIds,
                fullTimeActiveIds,
                inactiveIds
        );
    }

    private User saveUser(Role role) {
        String unique = UUID.randomUUID().toString();

        User user = new User();
        user.setFirstName("Discovery");
        user.setLastName(role.name());
        user.setEmail(role.name().toLowerCase() + "-" + unique + "@example.com");
        user.setPassword("not-used");
        user.setRole(role);
        user.setEnabled(true);

        return userRepository.saveAndFlush(user);
    }

    private Company saveCompany(String name) {
        Company company = new Company();
        company.setName(name);
        company.setDescription("Discovery test company");
        company.setLocation("Morocco");
        company.setWebsite("https://example.com");

        return companyRepository.saveAndFlush(company);
    }

    private Job saveJob(
            String title,
            String description,
            String location,
            EmploymentType employmentType,
            Company company,
            User recruiter,
            boolean active) {

        Job job = new Job();
        job.setTitle(title);
        job.setDescription(description);
        job.setLocation(location);
        job.setEmploymentType(employmentType);
        job.setCompany(company);
        job.setCreatedBy(recruiter);
        job.setActive(active);

        return jobRepository.saveAndFlush(job);
    }

    private void setCreatedAt(Job job, LocalDateTime createdAt) {
        jdbcTemplate.update(
                "UPDATE jobs SET created_at = ? WHERE id = ?",
                createdAt,
                job.getId()
        );
    }

    private static RequestPostProcessor jwtFor(Role role) {
        return jwt()
                .jwt(jwt -> jwt
                        .subject(role.name().toLowerCase() + "@example.com")
                        .claim("role", role.name())
                        .claim("userId", 1L))
                .authorities(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    private record TestData(
            Job javaBackend,
            Job frontend,
            Job paymentsContract,
            Job designer,
            Job inactiveJavaArchitect) {
    }

    private record PaginationData(
            List<Long> expectedActiveIds,
            List<Long> javaActiveIds,
            List<Long> meknesActiveIds,
            List<Long> fullTimeActiveIds,
            List<Long> inactiveIds) {
    }
}
