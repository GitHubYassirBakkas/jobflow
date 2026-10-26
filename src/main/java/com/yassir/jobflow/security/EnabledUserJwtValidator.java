package com.yassir.jobflow.security;

import com.yassir.jobflow.entity.User;
import com.yassir.jobflow.repository.UserRepository;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;


@Component
public class EnabledUserJwtValidator implements OAuth2TokenValidator<Jwt> {

    private final UserRepository userRepository;

    public EnabledUserJwtValidator(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {

        Long userId = jwt.getClaim("userId");

        if (userId == null) {
            return failure("JWT does not contain userId");
        }

        User user = userRepository.findById(userId).orElse(null);

        if (user == null) {
            return failure("User no longer exists");
        }

        if (!user.isEnabled()) {
            return failure("User account is disabled");
        }

        return OAuth2TokenValidatorResult.success();
    }

    private OAuth2TokenValidatorResult failure(String message) {

        OAuth2Error error = new OAuth2Error(
                "invalid_token",
                message,
                null
        );

        return OAuth2TokenValidatorResult.failure(error);
    }
}