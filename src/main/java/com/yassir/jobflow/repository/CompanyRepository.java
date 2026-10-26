package com.yassir.jobflow.repository;

import com.yassir.jobflow.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyRepository extends JpaRepository<Company, Long> {
}