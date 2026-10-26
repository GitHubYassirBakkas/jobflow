package com.yassir.jobflow.repository;

import com.yassir.jobflow.entity.SavedJob;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface SavedJobRepository extends JpaRepository<SavedJob, Long> {

    Optional<SavedJob> findByCandidateIdAndJobId(
            Long candidateId,
            Long jobId
    );

    boolean existsByCandidateIdAndJobId(
            Long candidateId,
            Long jobId
    );

    long countByCandidateIdAndJobActiveTrue(Long candidateId);

    @Transactional
    @Modifying
    @Query("""
            DELETE FROM SavedJob savedJob
            WHERE savedJob.candidate.id = :candidateId
              AND savedJob.job.id = :jobId
            """)
    int deleteByCandidateIdAndJobId(
            @Param("candidateId") Long candidateId,
            @Param("jobId") Long jobId
    );

    @EntityGraph(attributePaths = {"job", "job.company", "job.createdBy"})
    Page<SavedJob> findByCandidateIdAndJobActiveTrueOrderBySavedAtDescIdDesc(
            Long candidateId,
            Pageable pageable
    );

    @Query("""
            SELECT savedJob.job.id
            FROM SavedJob savedJob
            WHERE savedJob.candidate.id = :candidateId
              AND savedJob.job.active = true
            ORDER BY savedJob.savedAt DESC, savedJob.id DESC
            """)
    List<Long> findActiveSavedJobIdsByCandidateId(
            @Param("candidateId") Long candidateId
    );
}
