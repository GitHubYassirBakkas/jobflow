package com.yassir.jobflow.service;

import com.yassir.jobflow.dto.RegisterRequest;
import com.yassir.jobflow.entity.User;
import com.yassir.jobflow.exception.BusinessConflictException;
import com.yassir.jobflow.repository.UserRepository;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceRegistrationTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void mapsEmailUniqueConstraintViolationToConflict() {
        RegisterRequest request = createRequest();

        when(userRepository.existsByEmail(request.getEmail()))
                .thenReturn(false);

        when(passwordEncoder.encode(request.getPassword()))
                .thenReturn("encoded-password");

        ConstraintViolationException cause =
                org.mockito.Mockito.mock(
                        ConstraintViolationException.class);

        when(cause.getConstraintName())
                .thenReturn("uq_users_email");

        DataIntegrityViolationException databaseException =
                new DataIntegrityViolationException(
                        "Unique constraint violation",
                        cause);

        when(userRepository.saveAndFlush(any(User.class)))
                .thenThrow(databaseException);

        assertThatThrownBy(() -> userService.registerUser(request))
                .isInstanceOf(BusinessConflictException.class)
                .hasMessage("Email is already in use");
    }

    @Test
    void rethrowsUnrelatedDatabaseConstraintViolation() {
        RegisterRequest request = createRequest();

        when(userRepository.existsByEmail(request.getEmail()))
                .thenReturn(false);

        when(passwordEncoder.encode(request.getPassword()))
                .thenReturn("encoded-password");

        ConstraintViolationException cause =
                org.mockito.Mockito.mock(
                        ConstraintViolationException.class);

        when(cause.getConstraintName())
                .thenReturn("uq_some_other_constraint");

        DataIntegrityViolationException databaseException =
                new DataIntegrityViolationException(
                        "Unrelated database constraint violation",
                        cause);

        when(userRepository.saveAndFlush(any(User.class)))
                .thenThrow(databaseException);

        assertThatThrownBy(() -> userService.registerUser(request))
                .isSameAs(databaseException);
    }

    private RegisterRequest createRequest() {
        RegisterRequest request = new RegisterRequest();
        request.setFirstName("Test");
        request.setLastName("User");
        request.setEmail("race-test@example.com");
        request.setPassword("ValidPassword123!");
        return request;
    }
}