package com.yassir.jobflow.repository;

import com.yassir.jobflow.entity.User;
import com.yassir.jobflow.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    long countByRole(Role role);
    
    long countByRoleAndEnabledTrue(Role role);
}