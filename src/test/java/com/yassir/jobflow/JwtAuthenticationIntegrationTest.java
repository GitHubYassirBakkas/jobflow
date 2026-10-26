package com.yassir.jobflow;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.yassir.jobflow.entity.Role;
import com.yassir.jobflow.entity.User;
import com.yassir.jobflow.repository.UserRepository;
import com.yassir.jobflow.service.JwtService;
import com.yassir.jobflow.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class JwtAuthenticationIntegrationTest extends PostgresContainerTest {

    private static final String PROTECTED_ADMIN_ENDPOINT = "/api/admin/users";
    private static final String WRONG_TEST_SECRET =
            "jobflow-test-wrong-secret-key-32-plus-bytes-only-for-tests";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private UserService userService;

    @Test
    void malformedJwtReturnsUnauthorized() throws Exception {
        mockMvc.perform(get(PROTECTED_ADMIN_ENDPOINT)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void jwtSignedWithWrongSecretReturnsUnauthorized() throws Exception {
        User admin = saveUser(Role.ADMIN, true);
        String token = generateTokenWithWrongSecret(admin);

        mockMvc.perform(get(PROTECTED_ADMIN_ENDPOINT)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void expiredJwtReturnsUnauthorized() throws Exception {
        User admin = saveUser(Role.ADMIN, true);
        String token = generateExpiredToken(admin);

        mockMvc.perform(get(PROTECTED_ADMIN_ENDPOINT)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void validJwtForEnabledUserPassesAuthentication() throws Exception {
        User admin = saveUser(Role.ADMIN, true);
        String token = jwtService.generateToken(admin);

        when(userService.getAllUsers()).thenReturn(List.of(admin));

        mockMvc.perform(get(PROTECTED_ADMIN_ENDPOINT)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk());
    }

    @Test
    void validJwtForDisabledUserReturnsUnauthorized() throws Exception {
        User admin = saveUser(Role.ADMIN, true);
        String token = jwtService.generateToken(admin);

        admin.setEnabled(false);
        userRepository.saveAndFlush(admin);

        mockMvc.perform(get(PROTECTED_ADMIN_ENDPOINT)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isUnauthorized());
    }

@Test
void jwtWithoutUserIdReturnsUnauthorized() throws Exception {
    User admin = saveUser(Role.ADMIN, true);
    Instant now = Instant.now();

    JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer("jobflow")
            .issuedAt(now.minusSeconds(1))
            .expiresAt(now.plus(1, ChronoUnit.HOURS))
            .subject(admin.getEmail())
            .claim("role", admin.getRole().name())
            .build();

    JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();

    String token = jwtEncoder.encode(
            JwtEncoderParameters.from(header, claims)
    ).getTokenValue();

    mockMvc.perform(get(PROTECTED_ADMIN_ENDPOINT)
                    .header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isUnauthorized());
}

@Test
void validJwtForDeletedUserReturnsUnauthorized() throws Exception {
    User admin = saveUser(Role.ADMIN, true);
    String token = jwtService.generateToken(admin);

    userRepository.delete(admin);
    userRepository.flush();

    mockMvc.perform(get(PROTECTED_ADMIN_ENDPOINT)
                    .header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isUnauthorized());
}

    private User saveUser(Role role, boolean enabled) {
        User user = new User();
        user.setFirstName("Jwt");
        user.setLastName("Tester");
        user.setEmail("jwt-" + UUID.randomUUID() + "@example.com");
        user.setPassword("not-used-by-jwt-tests");
        user.setRole(role);
        user.setEnabled(enabled);

        return userRepository.saveAndFlush(user);
    }

    private String generateExpiredToken(User user) {
        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("jobflow")
                .issuedAt(now.minus(2, ChronoUnit.HOURS))
                .expiresAt(now.minus(1, ChronoUnit.HOURS))
                .subject(user.getEmail())
                .claim("role", user.getRole().name())
                .claim("userId", user.getId())
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();

        return jwtEncoder.encode(
                JwtEncoderParameters.from(header, claims)
        ).getTokenValue();
    }

    private String generateTokenWithWrongSecret(User user) {
        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("jobflow")
                .issuedAt(now)
                .expiresAt(now.plus(1, ChronoUnit.HOURS))
                .subject(user.getEmail())
                .claim("role", user.getRole().name())
                .claim("userId", user.getId())
                .build();

        return encodeWithSecret(claims, WRONG_TEST_SECRET);
    }

    private String encodeWithSecret(JwtClaimsSet claims, String secret) {
        SecretKeySpec secretKey = new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
        );

        NimbusJwtEncoder encoder = new NimbusJwtEncoder(
                new ImmutableSecret<>(secretKey)
        );

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();

        return encoder.encode(
                JwtEncoderParameters.from(header, claims)
        ).getTokenValue();
    }


    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
