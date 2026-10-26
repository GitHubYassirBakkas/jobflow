package com.yassir.jobflow.repository;

import com.yassir.jobflow.entity.EmploymentType;
import com.yassir.jobflow.entity.Job;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface JobRepository extends JpaRepository<Job, Long> {

    List<Job> findByCompanyId(Long companyId);

    List<Job> findByActiveTrue();

    @Query(
            value = """
            SELECT j
            FROM Job j
            JOIN j.company c
            WHERE j.active = true
              AND (
                    :search IS NULL
                    OR LOWER(j.title) LIKE :search
                    OR LOWER(j.description) LIKE :search
                    OR LOWER(c.name) LIKE :search
              )
              AND (
                    :location IS NULL
                    OR LOWER(j.location) LIKE :location
              )
              AND (
                    :employmentType IS NULL
                    OR j.employmentType = :employmentType
              )
            ORDER BY j.createdAt DESC, j.id DESC
            """,
            countQuery = """
            SELECT COUNT(j)
            FROM Job j
            JOIN j.company c
            WHERE j.active = true
              AND (
                    :search IS NULL
                    OR LOWER(j.title) LIKE :search
                    OR LOWER(j.description) LIKE :search
                    OR LOWER(c.name) LIKE :search
              )
              AND (
                    :location IS NULL
                    OR LOWER(j.location) LIKE :location
              )
              AND (
                    :employmentType IS NULL
                    OR j.employmentType = :employmentType
              )
            """)
    Page<Job> findActiveJobsForDiscovery(
            @Param("search") String search,
            @Param("location") String location,
            @Param("employmentType") EmploymentType employmentType,
            Pageable pageable);
}
