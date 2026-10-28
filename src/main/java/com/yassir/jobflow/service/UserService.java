package com.yassir.jobflow.service;

import com.yassir.jobflow.entity.Role;
import com.yassir.jobflow.dto.RegisterRequest;
import com.yassir.jobflow.entity.User;
import com.yassir.jobflow.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.yassir.jobflow.dto.LoginRequest;

import com.yassir.jobflow.exception.ResourceNotFoundException;
import com.yassir.jobflow.exception.BusinessConflictException;

import org.springframework.dao.DataIntegrityViolationException;
import org.hibernate.exception.ConstraintViolationException;

import java.util.List;
import java.util.Optional;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public boolean emailExists(String email) {
        return userRepository.existsByEmail(email);
    }

    public User registerUser(RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
        throw new BusinessConflictException("Email is already in use");
    }

        User user = new User();

        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.CANDIDATE);

        try {
    return userRepository.saveAndFlush(user);
} catch (DataIntegrityViolationException exception) {
    if (isEmailUniqueConstraintViolation(exception)) {
        throw new BusinessConflictException("Email is already in use");
    }

    throw exception;
}
    }

    private boolean isEmailUniqueConstraintViolation(Throwable exception) {
    Throwable cause = exception;

    while (cause != null) {
        if (cause instanceof ConstraintViolationException violationException) {
            return "uq_users_email".equalsIgnoreCase(
                    violationException.getConstraintName()
            );
        }

        cause = cause.getCause();
    }

    return false;
}

    public User loginUser(LoginRequest request) {

    User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() ->
                    new IllegalArgumentException("Invalid email or password")
            );

            if (!user.isEnabled()) {
                throw new IllegalArgumentException(
                        "This account has been disabled"
                );
            }

    if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
        throw new IllegalArgumentException("Invalid email or password");
    }

    return user;
}

@Transactional
public User updateUserEnabled(
        Long userId,
        boolean enabled,
        Long currentAdminId) {

    List<User> lockedEnabledAdmins = List.of();

    if (!enabled) {
        lockedEnabledAdmins =
                userRepository.findEnabledUsersByRoleForUpdate(Role.ADMIN);
    }

    User user = userRepository.findById(userId)
            .orElseThrow(() ->
                    new ResourceNotFoundException("User not found")
            );

    if (user.getRole() == Role.ADMIN
            && !enabled
            && lockedEnabledAdmins.size() <= 1) {

        throw new IllegalArgumentException(
                "You cannot disable the last active admin"
        );
    }

    if (user.getId().equals(currentAdminId) && !enabled) {
        throw new IllegalArgumentException(
                "You cannot disable your own account"
        );
    }

    user.setEnabled(enabled);

    return userRepository.save(user);
}
}