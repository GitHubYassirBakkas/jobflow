package com.yassir.jobflow;

import com.yassir.jobflow.entity.Company;
import com.yassir.jobflow.entity.EmploymentType;
import com.yassir.jobflow.entity.Job;
import com.yassir.jobflow.entity.RecruiterProfile;
import com.yassir.jobflow.entity.Role;
import com.yassir.jobflow.entity.User;
import com.yassir.jobflow.repository.ApplicationRepository;
import com.yassir.jobflow.repository.CandidateProfileRepository;
import com.yassir.jobflow.repository.CompanyRepository;
import com.yassir.jobflow.repository.InterviewRepository;
import com.yassir.jobflow.repository.JobRepository;
import com.yassir.jobflow.repository.RecruiterProfileRepository;
import com.yassir.jobflow.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class JobDetailsIntegrationTest extends PostgresContainerTest {

    @Autowired
    private MockMvc mockMvc;

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
    void candidateGetsActiveJobDetailWithCompanyFields() throws Exception {
        TestData data = createTestData();

        mockMvc.perform(get("/api/jobs/{id}", data.activeJobA().getId())
                        .with(jwtFor(data.candidate())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(data.activeJobA().getId()))
                .andExpect(jsonPath("$.title").value("Backend Engineer"))
                .andExpect(jsonPath("$.description").value("Build Spring services"))
                .andExpect(jsonPath("$.location").value("Meknes"))
                .andExpect(jsonPath("$.employmentType").value("FULL_TIME"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.createdAt").isString())
                .andExpect(jsonPath("$.updatedAt").isString())
                .andExpect(jsonPath("$.companyId").value(data.companyA().getId()))
                .andExpect(jsonPath("$.companyName").value(data.companyA().getName()))
                .andExpect(jsonPath("$.companyDescription").value("Company A description"))
                .andExpect(jsonPath("$.companyWebsite").value("https://company-a.example"))
                .andExpect(jsonPath("$.companyLocation").value("Casablanca"))
                .andExpect(jsonPath("$.createdById").value(data.recruiterA().getId()))
                .andExpect(jsonPath("$.createdByName").value("Recruiter A"));
    }

    @Test
    void nullableCompanyOptionalFieldsAreHandledInJobDetail() throws Exception {
        User candidate = saveUser(Role.CANDIDATE, "Candidate", "User");
        User recruiter = saveUser(Role.RECRUITER, "Recruiter", "User");
        Company company = saveCompany("Nullable Company", null, null, null);
        saveRecruiterProfile(recruiter, company);
        Job job = saveJob(company, recruiter, true);

        mockMvc.perform(get("/api/jobs/{id}", job.getId())
                        .with(jwtFor(candidate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companyId").value(company.getId()))
                .andExpect(jsonPath("$.companyName").value(company.getName()))
                .andExpect(jsonPath("$.companyDescription").value(nullValue()))
                .andExpect(jsonPath("$.companyWebsite").value(nullValue()))
                .andExpect(jsonPath("$.companyLocation").value(nullValue()));
    }

    @Test
    void candidateCannotAccessInactiveJobDetail() throws Exception {
        TestData data = createTestData();

        mockMvc.perform(get("/api/jobs/{id}", data.inactiveJobA().getId())
                        .with(jwtFor(data.candidate())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Job not found"));
    }

    @Test
    void recruiterCanAccessActiveJobDetail() throws Exception {
        TestData data = createTestData();

        mockMvc.perform(get("/api/jobs/{id}", data.activeJobA().getId())
                        .with(jwtFor(data.recruiterB())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(data.activeJobA().getId()));
    }

    @Test
    void owningRecruiterCanAccessOwnCompanyInactiveJobDetail() throws Exception {
        TestData data = createTestData();

        mockMvc.perform(get("/api/jobs/{id}", data.inactiveJobA().getId())
                        .with(jwtFor(data.recruiterA())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(data.inactiveJobA().getId()))
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void otherRecruiterCannotAccessInactiveJobDetail() throws Exception {
        TestData data = createTestData();

        mockMvc.perform(get("/api/jobs/{id}", data.inactiveJobA().getId())
                        .with(jwtFor(data.recruiterB())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Job not found"));
    }

    @Test
    void adminCanAccessInactiveJobDetail() throws Exception {
        TestData data = createTestData();

        mockMvc.perform(get("/api/jobs/{id}", data.inactiveJobA().getId())
                        .with(jwtFor(data.admin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(data.inactiveJobA().getId()))
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void missingJobAndHiddenInactiveJobKeepSameNotFoundSemantics() throws Exception {
        TestData data = createTestData();
        long missingJobId = data.inactiveJobA().getId() + 10_000L;

        mockMvc.perform(get("/api/jobs/{id}", missingJobId)
                        .with(jwtFor(data.candidate())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Job not found"));

        mockMvc.perform(get("/api/jobs/{id}", data.inactiveJobA().getId())
                        .with(jwtFor(data.candidate())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Job not found"));
    }

    @Test
    void discoveryEndpointRemainsPaginatedJobResponse() throws Exception {
        TestData data = createTestData();

        mockMvc.perform(get("/api/jobs")
                        .with(jwtFor(data.candidate())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].companyName").exists())
                .andExpect(jsonPath("$.content[0].companyDescription").doesNotExist())
                .andExpect(jsonPath("$.content[0].updatedAt").doesNotExist())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(true));
    }

    private TestData createTestData() {
        Company companyA = saveCompany(
                "Company A",
                "Company A description",
                "https://company-a.example",
                "Casablanca"
        );
        Company companyB = saveCompany(
                "Company B",
                "Company B description",
                "https://company-b.example",
                "Rabat"
        );

        User candidate = saveUser(Role.CANDIDATE, "Candidate", "User");
        User recruiterA = saveUser(Role.RECRUITER, "Recruiter", "A");
        User recruiterB = saveUser(Role.RECRUITER, "Recruiter", "B");
        User admin = saveUser(Role.ADMIN, "Admin", "User");

       // Assign the trusted creator of each company.
       companyA.setCreatedBy(recruiterA);
       companyB.setCreatedBy(recruiterB);

       companyA = companyRepository.saveAndFlush(companyA);
       companyB = companyRepository.saveAndFlush(companyB);

       saveRecruiterProfile(recruiterA, companyA);
       saveRecruiterProfile(recruiterB, companyB);

        Job activeJobA = saveJob(companyA, recruiterA, true);
        Job inactiveJobA = saveJob(companyA, recruiterA, false);
        saveJob(companyB, recruiterB, true);

        return new TestData(
                companyA,
                candidate,
                recruiterA,
                recruiterB,
                admin,
                activeJobA,
                inactiveJobA
        );
    }

    private User saveUser(Role role, String firstName, String lastName) {
        String unique = UUID.randomUUID().toString();

        User user = new User();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(role.name().toLowerCase() + "-" + unique + "@example.com");
        user.setPassword("not-used");
        user.setRole(role);
        user.setEnabled(true);

        return userRepository.saveAndFlush(user);
    }

    private Company saveCompany(
            String name,
            String description,
            String website,
            String location) {

        Company company = new Company();
        company.setName(name + " " + UUID.randomUUID());
        company.setDescription(description);
        company.setWebsite(website);
        company.setLocation(location);

        return companyRepository.saveAndFlush(company);
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
        job.setLocation("Meknes");
        job.setEmploymentType(EmploymentType.FULL_TIME);
        job.setCompany(company);
        job.setCreatedBy(recruiter);
        job.setActive(active);

        return jobRepository.saveAndFlush(job);
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
            Company companyA,
            User candidate,
            User recruiterA,
            User recruiterB,
            User admin,
            Job activeJobA,
            Job inactiveJobA) {
    }
}
