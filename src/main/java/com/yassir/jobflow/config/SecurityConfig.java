package com.yassir.jobflow.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidators;
import com.yassir.jobflow.security.EnabledUserJwtValidator;

import java.util.List;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

       http
        .csrf(csrf -> csrf.disable())
        .cors(cors -> {})
        .sessionManagement(session ->
    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
)
        .authorizeHttpRequests(auth -> auth
        .requestMatchers("/api/auth/register").permitAll()
        .requestMatchers("/api/auth/login").permitAll()
        .requestMatchers("/api/admin/**").hasRole("ADMIN")
        .requestMatchers("/api/recruiter/**").hasRole("RECRUITER")
        .requestMatchers("/api/companies/**").hasRole("RECRUITER")
        .requestMatchers("/api/recruiter/profile/**").hasRole("RECRUITER")
        .requestMatchers(HttpMethod.POST, "/api/jobs").hasRole("RECRUITER")
        .requestMatchers("/api/candidate/dashboard/**").hasRole("CANDIDATE")
        .requestMatchers("/api/candidate/profile/**").hasRole("CANDIDATE")
        .requestMatchers("/api/candidate/interviews/**").hasRole("CANDIDATE")
        .requestMatchers("/api/candidate/saved-jobs/**").hasRole("CANDIDATE")
        .requestMatchers(HttpMethod.POST, "/api/jobs/*/apply").hasRole("CANDIDATE")
        .requestMatchers("/api/applications/**").hasRole("CANDIDATE")
        

        .anyRequest().authenticated()
    )
        .oauth2ResourceServer(oauth2 -> oauth2
        .jwt(jwt -> jwt
            .jwtAuthenticationConverter(jwtAuthenticationConverter())
        )
    );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JwtEncoder jwtEncoder(
            @Value("${jwt.secret}") String secret) {

        SecretKeySpec secretKey = new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
        );

        return new NimbusJwtEncoder(
                new ImmutableSecret<>(secretKey)
        );
    }

    @Bean
public JwtDecoder jwtDecoder(
        @Value("${jwt.secret}") String secret,
        EnabledUserJwtValidator enabledUserJwtValidator) {

    SecretKeySpec secretKey = new SecretKeySpec(
            secret.getBytes(StandardCharsets.UTF_8),
            "HmacSHA256"
    );

    NimbusJwtDecoder decoder = NimbusJwtDecoder
            .withSecretKey(secretKey)
            .macAlgorithm(MacAlgorithm.HS256)
            .build();

    OAuth2TokenValidator<Jwt> defaultValidators =
            JwtValidators.createDefault();

    OAuth2TokenValidator<Jwt> validators =
            new DelegatingOAuth2TokenValidator<>(
                    defaultValidators,
                    enabledUserJwtValidator
            );

    decoder.setJwtValidator(validators);

    return decoder;
}

    @Bean
public JwtAuthenticationConverter jwtAuthenticationConverter() {

    JwtGrantedAuthoritiesConverter authoritiesConverter =
            new JwtGrantedAuthoritiesConverter();

    authoritiesConverter.setAuthoritiesClaimName("role");
    authoritiesConverter.setAuthorityPrefix("ROLE_");

    JwtAuthenticationConverter authenticationConverter =
            new JwtAuthenticationConverter();

    authenticationConverter.setJwtGrantedAuthoritiesConverter(
            authoritiesConverter
    );

    return authenticationConverter;
}

@Bean
public CorsConfigurationSource corsConfigurationSource() {

    CorsConfiguration configuration = new CorsConfiguration();

    configuration.setAllowedOrigins(List.of(
            "http://localhost:5173",
            "http://127.0.0.1:5173"
    ));

    configuration.setAllowedMethods(List.of(
            "GET",
            "POST",
            "PUT",
            "PATCH",
            "DELETE",
            "OPTIONS"
    ));

    configuration.setAllowedHeaders(List.of(
            "Authorization",
            "Content-Type"
    ));

    CorsConfigurationSource source =
            new UrlBasedCorsConfigurationSource();

    ((UrlBasedCorsConfigurationSource) source)
            .registerCorsConfiguration("/**", configuration);

    return source;
}
}
