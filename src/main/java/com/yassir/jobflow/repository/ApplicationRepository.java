package com.yassir.jobflow.repository;

import com.yassir.jobflow.entity.Application;
import com.yassir.jobflow.entity.ApplicationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ApplicationRepository
        extends JpaRepository<Application, Long> {

    boolean existsByCandidateIdAndJobId(
            Long candidateId,
            Long jobId
    );

    @EntityGraph(attributePaths = {"candidate", "job", "job.company"})
    Optional<Application> findByCandidateIdAndJobId(
            Long candidateId,
            Long jobId
    );

    List<Application> findByCandidateId(Long candidateId);

    long countByCandidateId(Long candidateId);

    long countByCandidateIdAndStatusIn(
            Long candidateId,
            Collection<ApplicationStatus> statuses
    );

    long countByCandidateIdAndStatus(
            Long candidateId,
            ApplicationStatus status
    );

    @EntityGraph(attributePaths = {"candidate", "job", "job.company"})
    Page<Application> findByCandidateIdOrderByAppliedAtDescIdDesc(
            Long candidateId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"candidate", "job", "job.company"})
    Optional<Application> findByIdAndCandidateId(Long id, Long candidateId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"candidate", "job", "job.company"})
    Optional<Application> findWithLockById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"candidate", "job", "job.company"})
    Optional<Application> findWithLockByIdAndCandidateId(
            Long id,
            Long candidateId
    );

    List<Application> findByJobId(Long jobId);
}
