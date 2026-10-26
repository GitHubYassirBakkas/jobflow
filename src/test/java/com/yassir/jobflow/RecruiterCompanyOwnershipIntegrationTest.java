package com.yassir.jobflow;

import com.yassir.jobflow.entity.Company;
import com.yassir.jobflow.entity.RecruiterProfile;
import com.yassir.jobflow.entity.Role;
import com.yassir.jobflow.entity.User;
import com.yassir.jobflow.repository.CompanyRepository;
import com.yassir.jobflow.repository.RecruiterProfileRepository;
import com.yassir.jobflow.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RecruiterCompanyOwnershipIntegrationTest
        extends PostgresContainerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private RecruiterProfileRepository recruiterProfileRepository;

   

    @Test
    void recruiterCanCreateProfileForCompanyTheyCreated()
            throws Exception {

        User recruiter = saveRecruiter("owner@example.com");

        long companyId = createCompanyAs(recruiter);

        mockMvc.perform(post("/api/recruiter/profile")
                        .with(jwtFor(recruiter))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "companyId": %d,
                                  "jobTitle": "Recruiter"
                                }
                                """.formatted(companyId)))
                .andExpect(status().isOk());

        Optional<RecruiterProfile> profile =
                recruiterProfileRepository.findByUserId(recruiter.getId());

        assertThat(profile).isPresent();

        Company savedCompany =
                companyRepository.findById(companyId).orElseThrow();

        assertThat(savedCompany.getCreatedBy()).isNotNull();
        assertThat(savedCompany.getCreatedBy().getId())
                .isEqualTo(recruiter.getId());
    }

    @Test
    void recruiterCannotJoinCompanyCreatedByAnotherRecruiter()
            throws Exception {

        User owner = saveRecruiter("company-owner@example.com");
        User attacker = saveRecruiter("other-recruiter@example.com");

        long companyId = createCompanyAs(owner);

        mockMvc.perform(post("/api/recruiter/profile")
                        .with(jwtFor(attacker))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "companyId": %d,
                                  "jobTitle": "Recruiter"
                                }
                                """.formatted(companyId)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message")
                        .value("You are not allowed to join this company"));

        assertThat(recruiterProfileRepository
                .findByUserId(attacker.getId()))
                .isEmpty();
    }

    @Test
    void recruiterCannotJoinLegacyCompanyWithoutCreator()
            throws Exception {

        User recruiter = saveRecruiter("legacy-recruiter@example.com");

        Company legacyCompany = new Company();
        legacyCompany.setName("Legacy Company");

        Company savedCompany =
                companyRepository.saveAndFlush(legacyCompany);

        mockMvc.perform(post("/api/recruiter/profile")
                        .with(jwtFor(recruiter))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "companyId": %d,
                                  "jobTitle": "Recruiter"
                                }
                                """.formatted(savedCompany.getId())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message")
                        .value("You are not allowed to join this company"));

        assertThat(recruiterProfileRepository
                .findByUserId(recruiter.getId()))
                .isEmpty();
    }

    private User saveRecruiter(String email) {

        User user = new User();
        user.setFirstName("Test");
        user.setLastName("Recruiter");
        user.setEmail(email);
        user.setPassword("test-only-password");
        user.setRole(Role.RECRUITER);

        return userRepository.saveAndFlush(user);
    }

    private long createCompanyAs(User recruiter) throws Exception {

    String companyName = "Test Company " + recruiter.getEmail();

    mockMvc.perform(post("/api/companies")
                    .with(jwtFor(recruiter))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                              "name": "%s",
                              "description": "Integration test company",
                              "website": "https://example.com",
                              "location": "Meknes"
                            }
                            """.formatted(companyName)))
            .andExpect(status().isOk());

    Company savedCompany = companyRepository.findAll()
            .stream()
            .filter(company -> companyName.equals(company.getName()))
            .findFirst()
            .orElseThrow(() ->
                    new AssertionError("Created company was not found"));

    assertThat(savedCompany.getCreatedBy()).isNotNull();
    assertThat(savedCompany.getCreatedBy().getId())
            .isEqualTo(recruiter.getId());

    return savedCompany.getId();
}

    private static RequestPostProcessor jwtFor(User user) {

        return jwt()
                .jwt(token -> token
                        .subject(user.getEmail())
                        .claim("role", user.getRole().name())
                        .claim("userId", user.getId()))
                .authorities(new SimpleGrantedAuthority(
                        "ROLE_" + user.getRole().name()
                ));
    }
}