package com.yassir.jobflow;

import com.yassir.jobflow.entity.Role;
import com.yassir.jobflow.entity.User;
import com.yassir.jobflow.service.InterviewService;
import com.yassir.jobflow.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityAccessIntegrationTest extends PostgresContainerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private InterviewService interviewService;

    @Test
    void protectedEndpointWithoutAuthenticationReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void candidateCannotAccessAdminEndpoint() throws Exception {
        mockMvc.perform(get("/api/admin/users")
                        .with(jwtFor(Role.CANDIDATE, "candidate@example.com", 1L)))
                .andExpect(status().isForbidden());
    }

    @Test
    void recruiterCannotAccessAdminEndpoint() throws Exception {
        mockMvc.perform(get("/api/admin/users")
                        .with(jwtFor(Role.RECRUITER, "recruiter@example.com", 2L)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanPassSecurityForAdminEndpoint() throws Exception {
        when(userService.getAllUsers()).thenReturn(List.of());

        mockMvc.perform(get("/api/admin/users")
                        .with(jwtFor(Role.ADMIN, "admin@example.com", 3L)))
                .andExpect(status().isOk());
    }

    @Test
    void candidateCannotAccessRecruiterEndpoint() throws Exception {
        mockMvc.perform(get("/api/recruiter/interviews")
                        .with(jwtFor(Role.CANDIDATE, "candidate@example.com", 1L)))
                .andExpect(status().isForbidden());
    }

    @Test
    void recruiterCanPassSecurityForRecruiterEndpoint() throws Exception {
        User recruiter = new User();
        recruiter.setEmail("recruiter@example.com");
        recruiter.setFirstName("Recruiter");
        recruiter.setLastName("User");
        recruiter.setRole(Role.RECRUITER);

        when(userService.getUserByEmail("recruiter@example.com"))
                .thenReturn(Optional.of(recruiter));
        when(interviewService.getInterviewsForRecruiter(any(User.class)))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/recruiter/interviews")
                        .with(jwtFor(Role.RECRUITER, "recruiter@example.com", 2L)))
                .andExpect(status().isOk());
    }

    @Test
    void loginEndpointDoesNotRequireJwt() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerEndpointDoesNotRequireJwt() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    private static RequestPostProcessor jwtFor(
            Role role,
            String subject,
            Long userId) {

        return jwt()
                .jwt(jwt -> jwt
                        .subject(subject)
                        .claim("role", role.name())
                        .claim("userId", userId))
                .authorities(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }
}
