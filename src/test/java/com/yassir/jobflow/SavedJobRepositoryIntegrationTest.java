package com.yassir.jobflow;

import com.yassir.jobflow.entity.Company;
import com.yassir.jobflow.entity.EmploymentType;
import com.yassir.jobflow.entity.Job;
import com.yassir.jobflow.entity.Role;
import com.yassir.jobflow.entity.SavedJob;
import com.yassir.jobflow.entity.User;
import com.yassir.jobflow.repository.ApplicationRepository;
import com.yassir.jobflow.repository.CandidateProfileRepository;
import com.yassir.jobflow.repository.CompanyRepository;
import com.yassir.jobflow.repository.InterviewRepository;
import com.yassir.jobflow.repository.JobRepository;
import com.yassir.jobflow.repository.RecruiterProfileRepository;
import com.yassir.jobflow.repository.SavedJobRepository;
import com.yassir.jobflow.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class SavedJobRepositoryIntegrationTest extends PostgresContainerTest {

    @Autowired
    private SavedJobRepository savedJobRepository;

    @Autowired
    private InterviewRepository interviewRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private CandidateProfileRepository candidateProfileRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private RecruiterProfileRepository recruiterProfileRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        deleteSavedJobData();
    }

    @AfterEach
    void cleanSavedJobs() {
        savedJobRepository.deleteAll();
    }

    @Test
    void findsAndChecksSavedJobByCandidateAndJob() {
        TestData data = createTestData();

        SavedJob savedJob = saveSavedJob(data.candidate(), data.activeJobA());
        saveSavedJob(data.otherCandidate(), data.activeJobA());

        assertThat(savedJobRepository.findByCandidateIdAndJobId(
                data.candidate().getId(),
                data.activeJobA().getId()
        ))
                .isPresent()
                .get()
                .extracting(SavedJob::getId)
                .isEqualTo(savedJob.getId());

        assertThat(savedJobRepository.existsByCandidateIdAndJobId(
                data.candidate().getId(),
                data.activeJobA().getId()
        )).isTrue();

        assertThat(savedJobRepository.findByCandidateIdAndJobId(
                data.candidate().getId(),
                data.otherCandidateJob().getId()
        )).isEmpty();
    }

    @Test
    void deletesOnlyTheCandidateScopedSavedJob() {
        TestData data = createTestData();

        saveSavedJob(data.candidate(), data.activeJobA());
        saveSavedJob(data.otherCandidate(), data.activeJobA());

        int deleted = savedJobRepository.deleteByCandidateIdAndJobId(
                data.candidate().getId(),
                data.activeJobA().getId()
        );

        assertThat(deleted).isEqualTo(1);
        assertThat(savedJobRepository.existsByCandidateIdAndJobId(
                data.candidate().getId(),
                data.activeJobA().getId()
        )).isFalse();
        assertThat(savedJobRepository.existsByCandidateIdAndJobId(
                data.otherCandidate().getId(),
                data.activeJobA().getId()
        )).isTrue();
    }

    @Test
    void pagesActiveSavedJobsByCandidateOrderedBySavedAtThenIdDescending() {
        TestData data = createTestData();
        LocalDateTime olderSavedAt = LocalDateTime.of(2026, 10, 1, 10, 0);
        LocalDateTime newerSavedAt = LocalDateTime.of(2026, 10, 2, 10, 0);

        SavedJob older = saveSavedJob(data.candidate(), data.activeJobA());
        SavedJob sameTimeLowerId = saveSavedJob(data.candidate(), data.activeJobB());
        SavedJob sameTimeHigherId = saveSavedJob(data.candidate(), data.activeJobC());
        saveSavedJob(data.candidate(), data.inactiveJob());
        saveSavedJob(data.otherCandidate(), data.otherCandidateJob());

        setSavedAt(older, olderSavedAt);
        setSavedAt(sameTimeLowerId, newerSavedAt);
        setSavedAt(sameTimeHigherId, newerSavedAt);

        Page<SavedJob> page = savedJobRepository
                .findByCandidateIdAndJobActiveTrueOrderBySavedAtDescIdDesc(
                        data.candidate().getId(),
                        PageRequest.of(0, 2)
                );

        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getTotalPages()).isEqualTo(2);
        assertThat(page.getContent())
                .extracting(SavedJob::getId)
                .containsExactly(
                        sameTimeHigherId.getId(),
                        sameTimeLowerId.getId()
                );
    }

    @Test
    void activeSavedJobIdsAreScopedToCandidateAndExcludeInactiveJobs() {
        TestData data = createTestData();
        LocalDateTime olderSavedAt = LocalDateTime.of(2026, 10, 1, 10, 0);
        LocalDateTime newerSavedAt = LocalDateTime.of(2026, 10, 2, 10, 0);

        SavedJob older = saveSavedJob(data.candidate(), data.activeJobA());
        SavedJob newer = saveSavedJob(data.candidate(), data.activeJobB());
        SavedJob inactive = saveSavedJob(data.candidate(), data.inactiveJob());
        saveSavedJob(data.otherCandidate(), data.otherCandidateJob());

        setSavedAt(older, olderSavedAt);
        setSavedAt(newer, newerSavedAt);
        setSavedAt(inactive, newerSavedAt.plusDays(1));

        List<Long> jobIds = savedJobRepository
                .findActiveSavedJobIdsByCandidateId(data.candidate().getId());

        assertThat(jobIds)
                .containsExactly(
                        data.activeJobB().getId(),
                        data.activeJobA().getId()
                )
                .doesNotContain(
                        data.inactiveJob().getId(),
                        data.otherCandidateJob().getId()
                );
    }

    private void deleteSavedJobData() {
        savedJobRepository.deleteAll();
        interviewRepository.deleteAll();
        applicationRepository.deleteAll();
        candidateProfileRepository.deleteAll();
        jobRepository.deleteAll();
        recruiterProfileRepository.deleteAll();
        companyRepository.deleteAll();
        userRepository.deleteAll();
    }

    private TestData createTestData() {
        User candidate = saveUser(Role.CANDIDATE);
        User otherCandidate = saveUser(Role.CANDIDATE);
        User recruiter = saveUser(Role.RECRUITER);
        Company company = saveCompany();

        Job activeJobA = saveJob(company, recruiter, true);
        Job activeJobB = saveJob(company, recruiter, true);
        Job activeJobC = saveJob(company, recruiter, true);
        Job inactiveJob = saveJob(company, recruiter, false);
        Job otherCandidateJob = saveJob(company, recruiter, true);

        return new TestData(
                candidate,
                otherCandidate,
                activeJobA,
                activeJobB,
                activeJobC,
                inactiveJob,
                otherCandidateJob
        );
    }

    private User saveUser(Role role) {
        String unique = UUID.randomUUID().toString();

        User user = new User();
        user.setFirstName("Saved");
        user.setLastName(role.name());
        user.setEmail(role.name().toLowerCase() + "-" + unique + "@example.com");
        user.setPassword("not-used");
        user.setRole(role);
        user.setEnabled(true);

        return userRepository.saveAndFlush(user);
    }

    private Company saveCompany() {
        Company company = new Company();
        company.setName("Saved Jobs Company " + UUID.randomUUID());
        company.setDescription("Saved jobs test company");
        company.setLocation("Remote");
        company.setWebsite("https://example.com");

        return companyRepository.saveAndFlush(company);
    }

    private Job saveJob(
            Company company,
            User recruiter,
            boolean active) {

        Job job = new Job();
        job.setTitle("Saved Role " + UUID.randomUUID());
        job.setDescription("Saved jobs repository test role");
        job.setLocation("Remote");
        job.setEmploymentType(EmploymentType.FULL_TIME);
        job.setCompany(company);
        job.setCreatedBy(recruiter);
        job.setActive(active);

        return jobRepository.saveAndFlush(job);
    }

    private SavedJob saveSavedJob(User candidate, Job job) {
        SavedJob savedJob = new SavedJob();
        savedJob.setCandidate(candidate);
        savedJob.setJob(job);

        return savedJobRepository.saveAndFlush(savedJob);
    }

    private void setSavedAt(SavedJob savedJob, LocalDateTime savedAt) {
        jdbcTemplate.update(
                "UPDATE saved_jobs SET saved_at = ? WHERE id = ?",
                savedAt,
                savedJob.getId()
        );
    }

    private record TestData(
            User candidate,
            User otherCandidate,
            Job activeJobA,
            Job activeJobB,
            Job activeJobC,
            Job inactiveJob,
            Job otherCandidateJob) {
    }
}
