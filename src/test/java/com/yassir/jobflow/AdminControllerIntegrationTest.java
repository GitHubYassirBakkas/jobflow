package com.yassir.jobflow;

import com.yassir.jobflow.entity.Application;
import com.yassir.jobflow.entity.ApplicationStatus;
import com.yassir.jobflow.entity.Company;
import com.yassir.jobflow.entity.EmploymentType;
import com.yassir.jobflow.entity.Interview;
import com.yassir.jobflow.entity.InterviewStatus;
import com.yassir.jobflow.entity.InterviewType;
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
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import com.yassir.jobflow.entity.CandidateProfile;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminControllerIntegrationTest extends PostgresContainerTest {

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
    @AfterEach
    void cleanDatabase() {
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

    @Test
    void unauthenticatedAdminGetPutAndPatchEndpointsReturnUnauthorized()
            throws Exception {

        mockMvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(put("/api/admin/companies/{companyId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(companyJson()))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(patch("/api/admin/jobs/{jobId}/active", 1L)
                        .param("active", "false"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void candidateCannotAccessRepresentativeAdminGetPutAndPatchEndpoints()
            throws Exception {

        User candidate = saveUser(Role.CANDIDATE, true);

        mockMvc.perform(get("/api/admin/dashboard")
                        .with(jwtFor(candidate)))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/admin/companies/{companyId}", 1L)
                        .with(jwtFor(candidate))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(companyJson()))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/admin/jobs/{jobId}/active", 1L)
                        .with(jwtFor(candidate))
                        .param("active", "false"))
                .andExpect(status().isForbidden());
    }

    @Test
    void recruiterCannotAccessRepresentativeAdminGetPutAndPatchEndpoints()
            throws Exception {

        User recruiter = saveUser(Role.RECRUITER, true);

        mockMvc.perform(get("/api/admin/dashboard")
                        .with(jwtFor(recruiter)))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/admin/companies/{companyId}", 1L)
                        .with(jwtFor(recruiter))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(companyJson()))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/admin/jobs/{jobId}/active", 1L)
                        .with(jwtFor(recruiter))
                        .param("active", "false"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminDashboardReturnsAllStatisticsIncludingDisabledUsers()
            throws Exception {

        Fixture fixture = createFixture();

        mockMvc.perform(get("/api/admin/dashboard")
                        .with(jwtFor(fixture.admin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").value(5))
                .andExpect(jsonPath("$.totalCandidates").value(2))
                .andExpect(jsonPath("$.totalRecruiters").value(2))
                .andExpect(jsonPath("$.totalAdmins").value(1))
                .andExpect(jsonPath("$.totalCompanies").value(1))
                .andExpect(jsonPath("$.totalJobs").value(1))
                .andExpect(jsonPath("$.totalApplications").value(1));
    }

    @Test
    void adminUsersListReturnsSafeUserDtos()
            throws Exception {

        Fixture fixture = createFixture();

        MvcResult result = mockMvc.perform(get("/api/admin/users")
                        .with(jwtFor(fixture.admin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem(fixture.admin().getId().intValue())))
                .andExpect(jsonPath("$[*].id", hasItem(fixture.disabledCandidate().getId().intValue())))
                .andExpect(jsonPath("$[*].role", hasItem("ADMIN")))
                .andExpect(jsonPath("$[*].role", hasItem("CANDIDATE")))
                .andExpect(jsonPath("$[*].enabled", hasItem(false)))
                .andExpect(jsonPath("$[*].createdAt").isNotEmpty())
                .andReturn();

        assertThat(result.getResponse().getContentAsString())
                .doesNotContain("password")
                .doesNotContain("test-only-password-hash");
    }

    @Test
    void adminCompaniesListReturnsCompanyResponseShape()
            throws Exception {

        Fixture fixture = createFixture();

        mockMvc.perform(get("/api/admin/companies")
                        .with(jwtFor(fixture.admin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem(fixture.company().getId().intValue())))
                .andExpect(jsonPath("$[*].name", hasItem(fixture.company().getName())))
                .andExpect(jsonPath("$[*].description", hasItem(fixture.company().getDescription())))
                .andExpect(jsonPath("$[*].website", hasItem(fixture.company().getWebsite())))
                .andExpect(jsonPath("$[*].location", hasItem(fixture.company().getLocation())))
                .andExpect(jsonPath("$[*].createdAt").isNotEmpty());
    }

    @Test
    void adminJobsListReturnsJobResponseShape()
            throws Exception {

        Fixture fixture = createFixture();

        mockMvc.perform(get("/api/admin/jobs")
                        .with(jwtFor(fixture.admin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem(fixture.job().getId().intValue())))
                .andExpect(jsonPath("$[*].title", hasItem(fixture.job().getTitle())))
                .andExpect(jsonPath("$[*].employmentType", hasItem("FULL_TIME")))
                .andExpect(jsonPath("$[*].active", hasItem(true)))
                .andExpect(jsonPath("$[*].companyId", hasItem(fixture.company().getId().intValue())))
                .andExpect(jsonPath("$[*].companyName", hasItem(fixture.company().getName())))
                .andExpect(jsonPath("$[*].createdById", hasItem(fixture.recruiter().getId().intValue())))
                .andExpect(jsonPath("$[*].createdAt").isNotEmpty());
    }

    @Test
    void adminApplicationsListReturnsSafeApplicationResponseShape()
            throws Exception {

        Fixture fixture = createFixture();

        MvcResult result = mockMvc.perform(get("/api/admin/applications")
                        .with(jwtFor(fixture.admin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem(fixture.application().getId().intValue())))
                .andExpect(jsonPath("$[*].jobId", hasItem(fixture.job().getId().intValue())))
                .andExpect(jsonPath("$[*].companyName", hasItem(fixture.company().getName())))
                .andExpect(jsonPath("$[*].candidateId", hasItem(fixture.candidate().getId().intValue())))
                .andExpect(jsonPath("$[*].status", hasItem("APPLIED")))
                .andExpect(jsonPath("$[*].cvFileName", hasItem("candidate-cv.pdf")))
                .andExpect(jsonPath("$[*].appliedAt").isNotEmpty())
                .andReturn();

        assertThat(result.getResponse().getContentAsString())
                .doesNotContain("cvFilePath")
                .doesNotContain("test-only-cv-storage-path");
    }

    @Test
    void adminInterviewsListReturnsInterviewResponseShape()
            throws Exception {

        Fixture fixture = createFixture();

        mockMvc.perform(get("/api/admin/interviews")
                        .with(jwtFor(fixture.admin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem(fixture.interview().getId().intValue())))
                .andExpect(jsonPath("$[*].applicationId", hasItem(fixture.application().getId().intValue())))
                .andExpect(jsonPath("$[*].jobId", hasItem(fixture.job().getId().intValue())))
                .andExpect(jsonPath("$[*].candidateId", hasItem(fixture.candidate().getId().intValue())))
                .andExpect(jsonPath("$[*].type", hasItem("ONLINE")))
                .andExpect(jsonPath("$[*].status", hasItem("SCHEDULED")))
                .andExpect(jsonPath("$[*].scheduledAt").isNotEmpty());
    }
@Test
void adminCanDisableAndReenableCandidateAccount() throws Exception {
    Fixture fixture = createFixture();
    Long candidateId = fixture.candidate().getId();

    // Disable candidate account
    mockMvc.perform(patch("/api/admin/users/{userId}/enabled", candidateId)
                    .with(jwtFor(fixture.admin()))
                    .param("enabled", "false"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(candidateId.intValue()))
            .andExpect(jsonPath("$.enabled").value(false));

    User disabledCandidate = userRepository.findById(candidateId)
            .orElseThrow();

    assertThat(disabledCandidate.isEnabled()).isFalse();

    // Re-enable candidate account
    mockMvc.perform(patch("/api/admin/users/{userId}/enabled", candidateId)
                    .with(jwtFor(fixture.admin()))
                    .param("enabled", "true"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.enabled").value(true));

    User enabledCandidate = userRepository.findById(candidateId)
            .orElseThrow();

    assertThat(enabledCandidate.isEnabled()).isTrue();
}

@Test
void adminCannotDisableOwnAccountWhenAnotherAdminIsActive()
        throws Exception {
    Fixture fixture = createFixture();

    // A second active admin allows us to exercise the self-disable check.
    saveUser(Role.ADMIN, true);

    mockMvc.perform(patch(
                    "/api/admin/users/{userId}/enabled",
                    fixture.admin().getId())
                    .with(jwtFor(fixture.admin()))
                    .param("enabled", "false"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message")
                    .value("You cannot disable your own account"));

    User admin = userRepository.findById(fixture.admin().getId())
            .orElseThrow();

    assertThat(admin.isEnabled()).isTrue();
}

@Test
void adminCannotDisableTheOnlyActiveAdmin() throws Exception {
    Fixture fixture = createFixture();

    // createFixture creates exactly one active admin.
    mockMvc.perform(patch(
                    "/api/admin/users/{userId}/enabled",
                    fixture.admin().getId())
                    .with(jwtFor(fixture.admin()))
                    .param("enabled", "false"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message")
                    .value("You cannot disable the last active admin"));

    User admin = userRepository.findById(fixture.admin().getId())
            .orElseThrow();

    assertThat(admin.isEnabled()).isTrue();
}

@Test
void adminCompanyUpdateRejectsDescriptionLongerThan2000Characters()
        throws Exception {

    Fixture fixture = createFixture();
    String oversizedDescription = "a".repeat(2001);

    String requestBody = """
            {
              "name": "Updated company",
              "description": "%s",
              "website": "https://example.com",
              "location": "Remote"
            }
            """.formatted(oversizedDescription);

    mockMvc.perform(put(
                    "/api/admin/companies/{companyId}",
                    fixture.company().getId())
                    .with(jwtFor(fixture.admin()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.description")
                    .value("Description must not exceed 2000 characters"));
}

@Test
void adminJobUpdateRejectsDescriptionLongerThan5000Characters()
        throws Exception {

    Fixture fixture = createFixture();
    String oversizedDescription = "a".repeat(5001);

    String requestBody = """
            {
              "title": "Updated job",
              "description": "%s",
              "location": "Remote",
              "employmentType": "FULL_TIME"
            }
            """.formatted(oversizedDescription);

    mockMvc.perform(put(
                    "/api/admin/jobs/{jobId}",
                    fixture.job().getId())
                    .with(jwtFor(fixture.admin()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.description")
                    .value("Description must not exceed 5000 characters"));
}

@Test
void adminCompanyUpdateRejectsJavascriptWebsite() throws Exception {
    Fixture fixture = createFixture();

    String requestBody = """
            {
              "name": "Updated company",
              "description": "Updated description",
              "website": "javascript:alert(1)",
              "location": "Remote"
            }
            """;

    mockMvc.perform(put(
                    "/api/admin/companies/{companyId}",
                    fixture.company().getId())
                    .with(jwtFor(fixture.admin()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.website")
                    .value("Website must be a valid HTTP or HTTPS URL"));
}

    @Test
    void adminCompanyUpdateRejectsIncompleteHttpsWebsite()            throws Exception {

        Fixture fixture = createFixture();

        String requestBody = """
                {
                  "name": "Updated company",
                  "description": "Valid description",
                  "website": "https://",
                  "location": "Remote"
                }
                """;

        mockMvc.perform(put(
                        "/api/admin/companies/{companyId}",
                        fixture.company().getId())
                        .with(jwtFor(fixture.admin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.website")
                        .value("Website must be a valid HTTP or HTTPS URL"));
    }

@Test
void adminCompanyUpdateRejectsNameLongerThan255Characters()
        throws Exception {

    Fixture fixture = createFixture();
    String oversizedName = "a".repeat(256);

    String requestBody = """
            {
              "name": "%s",
              "description": "Valid description",
              "website": "https://example.com",
              "location": "Remote"
            }
            """.formatted(oversizedName);

    mockMvc.perform(put(
                    "/api/admin/companies/{companyId}",
                    fixture.company().getId())
                    .with(jwtFor(fixture.admin()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.name")
                    .value("Company name must not exceed 255 characters"));
}

@Test
void adminCompanyUpdateRejectsLocationLongerThan255Characters()
        throws Exception {

    Fixture fixture = createFixture();
    String oversizedLocation = "a".repeat(256);

    String requestBody = """
            {
              "name": "Updated company",
              "description": "Valid description",
              "website": "https://example.com",
              "location": "%s"
            }
            """.formatted(oversizedLocation);

    mockMvc.perform(put(
                    "/api/admin/companies/{companyId}",
                    fixture.company().getId())
                    .with(jwtFor(fixture.admin()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.location")
                    .value("Location must not exceed 255 characters"));
}

@Test
void adminJobUpdateRejectsTitleLongerThan255Characters()
        throws Exception {

    Fixture fixture = createFixture();
    String oversizedTitle = "a".repeat(256);

    String requestBody = """
            {
              "title": "%s",
              "description": "Valid description",
              "location": "Remote",
              "employmentType": "FULL_TIME"
            }
            """.formatted(oversizedTitle);

    mockMvc.perform(put(
                    "/api/admin/jobs/{jobId}",
                    fixture.job().getId())
                    .with(jwtFor(fixture.admin()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title")
                    .value("Job title must not exceed 255 characters"));
}

@Test
void adminJobUpdateRejectsLocationLongerThan255Characters()
        throws Exception {

    Fixture fixture = createFixture();
    String oversizedLocation = "a".repeat(256);

    String requestBody = """
            {
              "title": "Updated job",
              "description": "Valid description",
              "location": "%s",
              "employmentType": "FULL_TIME"
            }
            """.formatted(oversizedLocation);

    mockMvc.perform(put(
                    "/api/admin/jobs/{jobId}",
                    fixture.job().getId())
                    .with(jwtFor(fixture.admin()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.location")
                    .value("Job location must not exceed 255 characters"));
}

@Test
void everyAdminEndpointRejectsUnauthenticatedAndNonAdminUsers()
        throws Exception {

    Fixture fixture = createFixture();

    assertAdminEndpointsDenied(
            fixture,
            null,
            status().isUnauthorized()
    );

    assertAdminEndpointsDenied(
            fixture,
            jwtFor(fixture.candidate()),
            status().isForbidden()
    );

    assertAdminEndpointsDenied(
            fixture,
            jwtFor(fixture.recruiter()),
            status().isForbidden()
    );
}

private void assertAdminEndpointsDenied(
        Fixture fixture,
        RequestPostProcessor authentication,
        ResultMatcher expectedStatus
) throws Exception {

    expectAdminEndpointDenied(
            get("/api/admin/dashboard"),
            authentication,
            expectedStatus
    );

    expectAdminEndpointDenied(
            get("/api/admin/users"),
            authentication,
            expectedStatus
    );

    expectAdminEndpointDenied(
            patch(
                    "/api/admin/users/{userId}/enabled",
                    fixture.candidate().getId()
            ).param("enabled", "false"),
            authentication,
            expectedStatus
    );

    expectAdminEndpointDenied(
            get("/api/admin/companies"),
            authentication,
            expectedStatus
    );

    expectAdminEndpointDenied(
            put(
                    "/api/admin/companies/{companyId}",
                    fixture.company().getId()
            )
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(companyJson()),
            authentication,
            expectedStatus
    );

    expectAdminEndpointDenied(
            get("/api/admin/jobs"),
            authentication,
            expectedStatus
    );

    expectAdminEndpointDenied(
            patch(
                    "/api/admin/jobs/{jobId}/active",
                    fixture.job().getId()
            ).param("active", "false"),
            authentication,
            expectedStatus
    );

    expectAdminEndpointDenied(
            put(
                    "/api/admin/jobs/{jobId}",
                    fixture.job().getId()
            )
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                              "title": "Updated test job",
                              "description": "Valid description",
                              "location": "Remote",
                              "employmentType": "FULL_TIME"
                            }
                            """),
            authentication,
            expectedStatus
    );

    expectAdminEndpointDenied(
            get(
                    "/api/admin/candidates/{userId}",
                    fixture.candidate().getId()
            ),
            authentication,
            expectedStatus
    );

    expectAdminEndpointDenied(
            get(
                    "/api/admin/candidates/{userId}/cv",
                    fixture.candidate().getId()
            ),
            authentication,
            expectedStatus
    );

    expectAdminEndpointDenied(
            get("/api/admin/applications"),
            authentication,
            expectedStatus
    );

    expectAdminEndpointDenied(
            patch(
                    "/api/admin/applications/{applicationId}/status",
                    fixture.application().getId()
            ).param("status", "APPLIED"),
            authentication,
            expectedStatus
    );

    expectAdminEndpointDenied(
            get(
                    "/api/admin/applications/{applicationId}/cv",
                    fixture.application().getId()
            ),
            authentication,
            expectedStatus
    );

    expectAdminEndpointDenied(
            get("/api/admin/interviews"),
            authentication,
            expectedStatus
    );

    expectAdminEndpointDenied(
            patch(
                    "/api/admin/interviews/{interviewId}/status",
                    fixture.interview().getId()
            ).param("status", "SCHEDULED"),
            authentication,
            expectedStatus
    );
}

private void expectAdminEndpointDenied(
        MockHttpServletRequestBuilder request,
        RequestPostProcessor authentication,
        ResultMatcher expectedStatus
) throws Exception {

    if (authentication != null) {
        request.with(authentication);
    }

    mockMvc.perform(request)
            .andExpect(expectedStatus);
}

@Test
void adminCanUpdateCompanyWithValidData() throws Exception {
    Fixture fixture = createFixture();

    String requestBody = """
            {
              "name": "Updated OpenTech",
              "description": "Updated company description",
              "website": "https://updated.example.com/careers",
              "location": "Casablanca"
            }
            """;

    mockMvc.perform(put(
                    "/api/admin/companies/{companyId}",
                    fixture.company().getId())
                    .with(jwtFor(fixture.admin()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Updated OpenTech"))
            .andExpect(jsonPath("$.description")
                    .value("Updated company description"))
            .andExpect(jsonPath("$.website")
                    .value("https://updated.example.com/careers"))
            .andExpect(jsonPath("$.location").value("Casablanca"));

    Company updatedCompany = companyRepository
            .findById(fixture.company().getId())
            .orElseThrow();

    assertThat(updatedCompany.getName()).isEqualTo("Updated OpenTech");
    assertThat(updatedCompany.getDescription())
            .isEqualTo("Updated company description");
    assertThat(updatedCompany.getWebsite())
            .isEqualTo("https://updated.example.com/careers");
    assertThat(updatedCompany.getLocation()).isEqualTo("Casablanca");
}

@Test
void adminCanUpdateJobWithValidData() throws Exception {
    Fixture fixture = createFixture();

    String requestBody = """
            {
              "title": "Updated Software Engineer",
              "description": "Updated job description",
              "location": "Casablanca",
              "employmentType": "FULL_TIME"
            }
            """;

    mockMvc.perform(put(
                    "/api/admin/jobs/{jobId}",
                    fixture.job().getId())
                    .with(jwtFor(fixture.admin()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title")
                    .value("Updated Software Engineer"))
            .andExpect(jsonPath("$.description")
                    .value("Updated job description"))
            .andExpect(jsonPath("$.location").value("Casablanca"))
            .andExpect(jsonPath("$.employmentType").value("FULL_TIME"));

    Job updatedJob = jobRepository.findById(fixture.job().getId())
            .orElseThrow();

    assertThat(updatedJob.getTitle())
            .isEqualTo("Updated Software Engineer");
    assertThat(updatedJob.getDescription())
            .isEqualTo("Updated job description");
    assertThat(updatedJob.getLocation()).isEqualTo("Casablanca");
    assertThat(updatedJob.getEmploymentType())
            .isEqualTo(EmploymentType.FULL_TIME);
}

@Test
void adminCanUpdateApplicationStatusAndRecordsHistory() throws Exception {
    Fixture fixture = createFixture();

    mockMvc.perform(patch(
                    "/api/admin/applications/{applicationId}/status",
                    fixture.application().getId())
                    .param("status", "REVIEWING")
                    .with(jwtFor(fixture.admin())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id")
                    .value(fixture.application().getId()))
            .andExpect(jsonPath("$.status").value("REVIEWING"));

    Application updatedApplication = applicationRepository
            .findById(fixture.application().getId())
            .orElseThrow();

    assertThat(updatedApplication.getStatus())
            .isEqualTo(ApplicationStatus.REVIEWING);

    var history = statusHistoryRepository
            .findByApplicationIdOrderByChangedAtAscIdAsc(
                    fixture.application().getId());

    assertThat(history)
            .anySatisfy(event -> {
                assertThat(event.getStatus())
                        .isEqualTo(ApplicationStatus.REVIEWING);
                assertThat(event.getChangedBy().getId())
                        .isEqualTo(fixture.admin().getId());
                assertThat(event.getEventType().name())
                        .isEqualTo("TRANSITION");
            });
}

@Test
void adminCanUpdateInterviewStatus() throws Exception {
    Fixture fixture = createFixture();

    mockMvc.perform(patch(
                    "/api/admin/interviews/{interviewId}/status",
                    fixture.interview().getId())
                    .param("status", "COMPLETED")
                    .with(jwtFor(fixture.admin())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id")
                    .value(fixture.interview().getId()))
            .andExpect(jsonPath("$.status").value("COMPLETED"));

    Interview updatedInterview = interviewRepository
            .findById(fixture.interview().getId())
            .orElseThrow();

    assertThat(updatedInterview.getStatus())
            .isEqualTo(InterviewStatus.COMPLETED);
}

@Test
void adminCannotSetApplicationStatusToWithdrawn() throws Exception {
    Fixture fixture = createFixture();

    mockMvc.perform(patch(
                    "/api/admin/applications/{applicationId}/status",
                    fixture.application().getId())
                    .param("status", "WITHDRAWN")
                    .with(jwtFor(fixture.admin())))
            .andExpect(status().isBadRequest());

    Application unchangedApplication = applicationRepository
            .findById(fixture.application().getId())
            .orElseThrow();

    assertThat(unchangedApplication.getStatus())
            .isEqualTo(ApplicationStatus.APPLIED);

    var history = statusHistoryRepository
            .findByApplicationIdOrderByChangedAtAscIdAsc(
                    fixture.application().getId());

    assertThat(history)
            .noneMatch(event ->
                    event.getStatus() == ApplicationStatus.WITHDRAWN);
}

@Test
void adminCannotReactivateWithdrawnApplication() throws Exception {
    Fixture fixture = createFixture();

    Application application = applicationRepository
            .findById(fixture.application().getId())
            .orElseThrow();

    application.setStatus(ApplicationStatus.WITHDRAWN);
    applicationRepository.saveAndFlush(application);

    mockMvc.perform(patch(
                    "/api/admin/applications/{applicationId}/status",
                    fixture.application().getId())
                    .param("status", "REVIEWING")
                    .with(jwtFor(fixture.admin())))
            .andExpect(status().isConflict());

    Application unchangedApplication = applicationRepository
            .findById(fixture.application().getId())
            .orElseThrow();

    assertThat(unchangedApplication.getStatus())
            .isEqualTo(ApplicationStatus.WITHDRAWN);

    var history = statusHistoryRepository
            .findByApplicationIdOrderByChangedAtAscIdAsc(
                    fixture.application().getId());

    assertThat(history)
            .noneMatch(event ->
                    event.getStatus() == ApplicationStatus.REVIEWING);
}

@Test
void adminCanDownloadApplicationCvAsPdf() throws Exception {
    Fixture fixture = createFixture();

    byte[] pdfBytes = "%PDF-1.4\nJobFlow test CV\n"
            .getBytes(java.nio.charset.StandardCharsets.UTF_8);

    java.nio.file.Path cvPath =
            java.nio.file.Files.createTempFile(
                    "jobflow-admin-cv-", ".pdf");

    try {
        java.nio.file.Files.write(cvPath, pdfBytes);

        Application application = applicationRepository
                .findById(fixture.application().getId())
                .orElseThrow();

        application.setCvFileName("candidate-resume.pdf");
        application.setCvFilePath(cvPath.toString());
        applicationRepository.saveAndFlush(application);

        mockMvc.perform(get(
                        "/api/admin/applications/{applicationId}/cv",
                        fixture.application().getId())
                        .with(jwtFor(fixture.admin())))
                .andExpect(status().isOk())
                .andExpect(content()
                        .contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"candidate-resume.pdf\""))
                .andExpect(content().bytes(pdfBytes));
    } finally {
        java.nio.file.Files.deleteIfExists(cvPath);
    }
}

@Test
void adminGetsBadRequestWhenApplicationCvFileIsMissing()
        throws Exception {
    Fixture fixture = createFixture();

    java.nio.file.Path missingCvPath =
            java.nio.file.Files.createTempFile(
                    "jobflow-missing-cv-", ".pdf");

    java.nio.file.Files.deleteIfExists(missingCvPath);

    Application application = applicationRepository
            .findById(fixture.application().getId())
            .orElseThrow();

    application.setCvFileName("missing-resume.pdf");
    application.setCvFilePath(missingCvPath.toString());
    applicationRepository.saveAndFlush(application);

    mockMvc.perform(get(
                    "/api/admin/applications/{applicationId}/cv",
                    fixture.application().getId())
                    .with(jwtFor(fixture.admin())))
            .andExpect(status().isBadRequest());
}

@Test
void adminGetsNotFoundWhenApplicationCvDoesNotExist()
        throws Exception {
    Fixture fixture = createFixture();

    mockMvc.perform(get(
                    "/api/admin/applications/{applicationId}/cv",
                    Long.MAX_VALUE)
                    .with(jwtFor(fixture.admin())))
            .andExpect(status().isNotFound());
}

@Test
void adminCanDownloadCandidateProfileCvAsPdf() throws Exception {
    Fixture fixture = createFixture();

    byte[] pdfBytes = "%PDF-1.4\nJobFlow candidate profile CV\n"
            .getBytes(java.nio.charset.StandardCharsets.UTF_8);

    java.nio.file.Path cvPath =
            java.nio.file.Files.createTempFile(
                    "jobflow-candidate-cv-", ".pdf");

    try {
        java.nio.file.Files.write(cvPath, pdfBytes);

        CandidateProfile profile = new CandidateProfile();
        profile.setUser(fixture.candidate());
        profile.setCvFileName("candidate-profile-resume.pdf");
        profile.setCvFilePath(cvPath.toString());

        candidateProfileRepository.saveAndFlush(profile);

        mockMvc.perform(get(
                        "/api/admin/candidates/{userId}/cv",
                        fixture.candidate().getId())
                        .with(jwtFor(fixture.admin())))
                .andExpect(status().isOk())
                .andExpect(content()
                        .contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"candidate-profile-resume.pdf\""))
                .andExpect(content().bytes(pdfBytes));
    } finally {
        java.nio.file.Files.deleteIfExists(cvPath);
    }
}

@Test
void adminGetsBadRequestWhenCandidateProfileHasNoCv()
        throws Exception {
    Fixture fixture = createFixture();

    CandidateProfile profile = new CandidateProfile();
    profile.setUser(fixture.candidate());

    candidateProfileRepository.saveAndFlush(profile);

    mockMvc.perform(get(
                    "/api/admin/candidates/{userId}/cv",
                    fixture.candidate().getId())
                    .with(jwtFor(fixture.admin())))
            .andExpect(status().isBadRequest());
}

    private Fixture createFixture() {
        User admin = saveUser(Role.ADMIN, true);
        User candidate = saveUser(Role.CANDIDATE, true);
        User disabledCandidate = saveUser(Role.CANDIDATE, false);
        User recruiter = saveUser(Role.RECRUITER, true);
        saveUser(Role.RECRUITER, false);

        Company company = saveCompany();
        Job job = saveJob(company, recruiter);
        Application application = saveApplication(candidate, job);
        Interview interview = saveInterview(application);

        return new Fixture(
                admin,
                candidate,
                disabledCandidate,
                recruiter,
                company,
                job,
                application,
                interview
        );
    }

    private User saveUser(Role role, boolean enabled) {
        String unique = UUID.randomUUID().toString();

        User user = new User();
        user.setFirstName("Admin");
        user.setLastName(role.name());
        user.setEmail(role.name().toLowerCase() + "-" + unique + "@example.com");
        user.setPassword("test-only-password-hash");
        user.setRole(role);
        user.setEnabled(enabled);

        return userRepository.saveAndFlush(user);
    }

    private Company saveCompany() {
        Company company = new Company();
        company.setName("Admin Test Company " + UUID.randomUUID());
        company.setDescription("Admin list contract fixture");
        company.setWebsite("https://example.com/admin-test");
        company.setLocation("Remote");

        return companyRepository.saveAndFlush(company);
    }

    private Job saveJob(Company company, User recruiter) {
        Job job = new Job();
        job.setTitle("Admin Test Engineer " + UUID.randomUUID());
        job.setDescription("Admin job list contract fixture");
        job.setLocation("Remote");
        job.setEmploymentType(EmploymentType.FULL_TIME);
        job.setCompany(company);
        job.setCreatedBy(recruiter);
        job.setActive(true);

        return jobRepository.saveAndFlush(job);
    }

    private Application saveApplication(User candidate, Job job) {
        Application application = new Application();
        application.setCandidate(candidate);
        application.setJob(job);
        application.setStatus(ApplicationStatus.APPLIED);
        application.setCvFileName("candidate-cv.pdf");
        application.setCvFilePath("test-only-cv-storage-path");

        return applicationRepository.saveAndFlush(application);
    }

    private Interview saveInterview(Application application) {
        Interview interview = new Interview();
        interview.setApplication(application);
        interview.setScheduledAt(LocalDateTime.of(2030, 1, 1, 9, 0));
        interview.setType(InterviewType.ONLINE);
        interview.setMeetingLink("https://example.com/admin-test-interview");
        interview.setNotes("Admin interview list contract fixture");
        interview.setStatus(InterviewStatus.SCHEDULED);

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

    private static String companyJson() {
        return """
                {
                  "name": "Updated company",
                  "description": "Updated description",
                  "website": "https://example.com",
                  "location": "Remote"
                }
                """;
    }

    private record Fixture(
            User admin,
            User candidate,
            User disabledCandidate,
            User recruiter,
            Company company,
            Job job,
            Application application,
            Interview interview) {
    }
}
