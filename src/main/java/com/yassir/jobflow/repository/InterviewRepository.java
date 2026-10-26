package com.yassir.jobflow.repository;

import com.yassir.jobflow.entity.Interview;
import com.yassir.jobflow.entity.InterviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

import java.time.LocalDateTime;
import java.util.Optional;

public interface InterviewRepository
        extends JpaRepository<Interview, Long> {

    Optional<Interview> findByApplicationId(Long applicationId);

    boolean existsByApplicationId(Long applicationId);

    List<Interview> findByApplicationJobCompanyId(Long companyId);

    List<Interview> findByApplicationCandidateId(Long candidateId);

    long countByApplicationCandidateIdAndStatusAndScheduledAtAfter(
            Long candidateId,
            InterviewStatus status,
            LocalDateTime scheduledAt
    );

    @EntityGraph(attributePaths = {
            "application",
            "application.job",
            "application.candidate"
    })
    Page<Interview>
            findByApplicationCandidateIdAndStatusAndScheduledAtAfterOrderByScheduledAtAscIdAsc(
                    Long candidateId,
                    InterviewStatus status,
                    LocalDateTime scheduledAt,
                    Pageable pageable
            );
}
