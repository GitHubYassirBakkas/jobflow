package com.yassir.jobflow;

import com.yassir.jobflow.dto.PageResponse;
import com.yassir.jobflow.dto.SavedJobResponse;
import com.yassir.jobflow.entity.Company;
import com.yassir.jobflow.entity.EmploymentType;
import com.yassir.jobflow.entity.Job;
import com.yassir.jobflow.entity.Role;
import com.yassir.jobflow.entity.SavedJob;
import com.yassir.jobflow.entity.User;
import com.yassir.jobflow.exception.ResourceNotFoundException;
import com.yassir.jobflow.repository.ApplicationRepository;
import com.yassir.jobflow.repository.CandidateProfileRepository;
import com.yassir.jobflow.repository.CompanyRepository;
import com.yassir.jobflow.repository.InterviewRepository;
import com.yassir.jobflow.repository.JobRepository;
import com.yassir.jobflow.repository.RecruiterProfileRepository;
import com.yassir.jobflow.repository.SavedJobRepository;
import com.yassir.jobflow.repository.UserRepository;
import com.yassir.jobflow.service.SavedJobService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class SavedJobServiceIntegrationTest extends PostgresContainerTest {

    @Autowired
    private SavedJobService savedJobService;

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
    void savesActiveJob() {
        TestData data = createTestData();

        SavedJobResponse response = savedJobService.saveJob(
                data.candidate().getId(),
                data.activeJobA().getId()
        );

        assertThat(response.getId()).isNotNull();
        assertThat(response.getJob().getId()).isEqualTo(data.activeJobA().getId());
        assertThat(response.getSavedAt()).isNotNull();
        assertThat(savedJobRepository.existsByCandidateIdAndJobId(
                data.candidate().getId(),
                data.activeJobA().getId()
        )).isTrue();
    }

    @Test
    void duplicateSaveIsIdempotent() {
        TestData data = createTestData();

        SavedJobResponse first = savedJobService.saveJob(
                data.candidate().getId(),
                data.activeJobA().getId()
        );
        SavedJobResponse second = savedJobService.saveJob(
                data.candidate().getId(),
                data.activeJobA().getId()
        );

        assertThat(second.getId()).isEqualTo(first.getId());
        assertThat(savedJobRepository.findAll()).hasSize(1);
    }

    @Test
    void concurrentDuplicateSavesCreateOneSavedJob() throws Exception {
        TestData data = createTestData();
        ExecutorService executor = Executors.newFixedThreadPool(4);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<SavedJobResponse>> futures = new ArrayList<>();

        try {
            for (int index = 0; index < 8; index++) {
                futures.add(executor.submit(() -> {
                    start.await(5, TimeUnit.SECONDS);

                    return savedJobService.saveJob(
                            data.candidate().getId(),
                            data.activeJobA().getId()
                    );
                }));
            }

            start.countDown();

            List<SavedJobResponse> responses = new ArrayList<>();

            for (Future<SavedJobResponse> future : futures) {
                responses.add(future.get(10, TimeUnit.SECONDS));
            }

            assertThat(responses)
                    .extracting(SavedJobResponse::getId)
                    .containsOnly(responses.getFirst().getId());
            assertThat(savedJobRepository.findAll()).hasSize(1);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void rejectsInactiveJob() {
        TestData data = createTestData();

        assertThatThrownBy(() ->
                savedJobService.saveJob(
                        data.candidate().getId(),
                        data.inactiveJob().getId()
                ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("This job is no longer active");

        assertThat(savedJobRepository.findAll()).isEmpty();
    }

    @Test
    void unsaveIsIdempotentAndCandidateScoped() {
        TestData data = createTestData();

        savedJobService.saveJob(data.candidate().getId(), data.activeJobA().getId());
        savedJobService.saveJob(data.otherCandidate().getId(), data.activeJobA().getId());

        savedJobService.unsaveJob(data.candidate().getId(), data.activeJobA().getId());
        savedJobService.unsaveJob(data.candidate().getId(), data.activeJobA().getId());

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
    void missingCandidateIsRejected() {
        TestData data = createTestData();
        Long missingCandidateId = data.candidate().getId() + 10_000L;

        assertThatThrownBy(() ->
                savedJobService.saveJob(missingCandidateId, data.activeJobA().getId()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Candidate not found");
    }

    @Test
    void savedJobsPageIsActiveOnlyCandidateScopedAndOrdered() {
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

        PageResponse<SavedJobResponse> page =
                savedJobService.getSavedJobs(data.candidate().getId(), 0, 2);

        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getTotalPages()).isEqualTo(2);
        assertThat(page.getContent())
                .extracting(SavedJobResponse::getId)
                .containsExactly(
                        sameTimeHigherId.getId(),
                        sameTimeLowerId.getId()
                );
        assertThat(page.getContent())
                .extracting(response -> response.getJob().getId())
                .doesNotContain(
                        data.inactiveJob().getId(),
                        data.otherCandidateJob().getId()
                );
    }

    @Test
    void savedJobsPageValidatesPagination() {
        TestData data = createTestData();

        assertThatThrownBy(() ->
                savedJobService.getSavedJobs(data.candidate().getId(), -1, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Page must be greater than or equal to 0");

        assertThatThrownBy(() ->
                savedJobService.getSavedJobs(data.candidate().getId(), 0, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Size must be greater than or equal to 1");

        assertThatThrownBy(() ->
                savedJobService.getSavedJobs(data.candidate().getId(), 0, 51))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Size must not exceed 50");
    }

    @Test
    void activeSavedJobIdsAreActiveOnlyAndCandidateScoped() {
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

        List<Long> jobIds =
                savedJobService.getActiveSavedJobIds(data.candidate().getId());

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
        company.setName("Saved Jobs Service Company " + UUID.randomUUID());
        company.setDescription("Saved jobs service test company");
        company.setLocation("Remote");
        company.setWebsite("https://example.com");

        return companyRepository.saveAndFlush(company);
    }

    private Job saveJob(
            Company company,
            User recruiter,
            boolean active) {

        Job job = new Job();
        job.setTitle("Saved Service Role " + UUID.randomUUID());
        job.setDescription("Saved jobs service test role");
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
