package org.patinanetwork.codebloom.common.db.repos.job;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.patinanetwork.codebloom.common.components.IncompleteQuestionRequeuer;
import org.patinanetwork.codebloom.common.db.models.job.Job;
import org.patinanetwork.codebloom.common.db.models.job.JobStatus;
import org.patinanetwork.codebloom.common.db.repos.BaseRepositoryTest;
import org.patinanetwork.codebloom.common.db.repos.question.QuestionRepository;
import org.patinanetwork.codebloom.common.time.StandardizedOffsetDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(OrderAnnotation.class)
@Slf4j
public class JobRepositoryTest extends BaseRepositoryTest {

    private final JobRepository jobRepository;
    private Job testJob;
    private String mockQuestionId = UUID.randomUUID().toString();

    @Autowired
    public JobRepositoryTest(final JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    @BeforeAll
    void setup() {
        testJob = Job.builder()
                .questionId(mockQuestionId)
                .status(JobStatus.INCOMPLETE)
                .nextAttemptAt(StandardizedOffsetDateTime.now().minusHours(1))
                .build();

        jobRepository.createJob(testJob);
    }

    @AfterAll
    void cleanup() {
        boolean isSuccessful = jobRepository.deleteJobById(testJob.getId());

        if (!isSuccessful) {
            fail("Failed to delete test job");
        }

        // would return false, but it's ok - super simple method - we just call it to
        // make sure it works.
        jobRepository.deleteAllJobs();
    }

    @Test
    @Order(1)
    void testFindJobById() {
        Job foundJob = jobRepository.findJobById(testJob.getId()).orElse(null);
        assertNotNull(foundJob);
        assertEquals(testJob, foundJob);
    }

    @Test
    @Order(2)
    void testFindIncompleteJobs() {
        List<Job> incompleteJobs = jobRepository.findIncompleteJobs(10);
        assertNotNull(incompleteJobs);
        assertTrue(incompleteJobs.contains(testJob));
    }

    @Test
    @Order(3)
    void testUpdateJob() {
        testJob.setProcessedAt(Optional.ofNullable(StandardizedOffsetDateTime.now()));
        testJob.setCompletedAt(
                Optional.ofNullable(StandardizedOffsetDateTime.now().plusMinutes(5)));
        testJob.setNextAttemptAt(StandardizedOffsetDateTime.now().plusMinutes(30));
        testJob.setStatus(JobStatus.COMPLETE);

        boolean updateResult = jobRepository.updateJob(testJob);
        assertTrue(updateResult);

        Job updatedJob = jobRepository.findJobById(testJob.getId()).orElse(null);
        assertNotNull(updatedJob);
        assertEquals(JobStatus.COMPLETE, updatedJob.getStatus());
        assertTrue(updatedJob.getProcessedAt().isPresent());
        assertTrue(updatedJob.getCompletedAt().isPresent());
        assertNotNull(updatedJob.getNextAttemptAt());
    }

    @Test
    @Order(4)
    void testFindJobsByQuestionId() {
        List<Job> jobs = jobRepository.findJobsByQuestionId(mockQuestionId);

        assertNotNull(jobs, "The returned list should never be null");
        assertFalse(jobs.isEmpty(), "The list should contain at least the test job created in setup");

        boolean containsTestJob = jobs.stream().anyMatch(j -> j.getId().equals(testJob.getId()));

        assertTrue(containsTestJob, "The list should contain the specific test job associated with this question ID");

        boolean allMatchId = jobs.stream().allMatch(j -> j.getQuestionId().equals(mockQuestionId));
        assertTrue(allMatchId, "All returned jobs must match the requested question ID");
    }

    @Test
    @Order(5)
    void testOnlyOneActiveJobPerQuestion() {
        // testJob is COMPLETE after Order(3), so it must not block a new active job.
        Job active = Job.builder()
                .questionId(mockQuestionId)
                .status(JobStatus.INCOMPLETE)
                .build();
        Job duplicate = Job.builder()
                .questionId(mockQuestionId)
                .status(JobStatus.INCOMPLETE)
                .build();
        Job completed = Job.builder()
                .questionId(mockQuestionId)
                .status(JobStatus.COMPLETE)
                .build();

        try {
            assertTrue(jobRepository.createJob(active), "COMPLETE jobs must not block a new active job");

            assertFalse(jobRepository.createJob(duplicate), "Second active job must be rejected");
            assertNull(duplicate.getId());

            assertTrue(jobRepository.createJob(completed), "COMPLETE jobs are outside the unique index");

            completed.setStatus(JobStatus.PROCESSING);
            assertThrows(
                    DuplicateKeyException.class,
                    () -> jobRepository.updateJob(completed),
                    "Reactivating a job while another is active must violate the index");

            active.setStatus(JobStatus.COMPLETE);
            assertTrue(jobRepository.updateJob(active));
            assertTrue(jobRepository.createJob(duplicate), "A new active job is allowed once the old one completes");
        } finally {
            for (Job j : List.of(active, duplicate, completed)) {
                if (j.getId() != null) {
                    jobRepository.deleteJobById(j.getId());
                }
            }
        }
    }

    @Test
    @Transactional
    void recoveryResetsOnlyProcessingJobsAtOrBeforeCutoff() {
        jobRepository.deleteAllJobs();
        OffsetDateTime cutoff = StandardizedOffsetDateTime.now().minusHours(1);
        Job stale = recoveryJob(JobStatus.PROCESSING, Optional.of(cutoff.minusSeconds(1)));
        Job boundary = recoveryJob(JobStatus.PROCESSING, Optional.of(cutoff));
        Job recent = recoveryJob(JobStatus.PROCESSING, Optional.of(cutoff.plusSeconds(1)));
        Job queued = recoveryJob(JobStatus.INCOMPLETE, Optional.of(cutoff.minusHours(1)));
        Job complete = recoveryJob(JobStatus.COMPLETE, Optional.of(cutoff.minusHours(1)));
        Job unknownStart = recoveryJob(JobStatus.PROCESSING, Optional.empty());

        OffsetDateTime beforeRecovery = StandardizedOffsetDateTime.now();
        assertEquals(2, jobRepository.requeueStaleProcessingJobs(cutoff));

        for (Job original : List.of(stale, boundary)) {
            Job recovered = jobRepository.findJobById(original.getId()).orElseThrow();
            assertEquals(JobStatus.INCOMPLETE, recovered.getStatus());
            assertEquals(0, recovered.getAttempts());
            assertTrue(recovered.getProcessedAt().isEmpty());
            assertTrue(recovered.getCompletedAt().isEmpty());
            assertFalse(recovered.getNextAttemptAt().isAfter(beforeRecovery));
            assertEquals(original.getCreatedAt(), recovered.getCreatedAt());
            assertEquals(List.of(recovered), jobRepository.findJobsByQuestionId(original.getQuestionId()));
            assertTrue(jobRepository.findIncompleteJobs(10).contains(recovered));
        }
        for (Job unchanged : List.of(recent, queued, complete, unknownStart)) {
            assertEquals(unchanged, jobRepository.findJobById(unchanged.getId()).orElseThrow());
        }
        assertEquals(0, jobRepository.requeueStaleProcessingJobs(cutoff));
    }

    @Test
    @Transactional
    void requeueCountsRecoveredJobsWithoutCreatingNewJobs() {
        jobRepository.deleteAllJobs();
        Job stale = recoveryJob(
                JobStatus.PROCESSING,
                Optional.of(StandardizedOffsetDateTime.now().minusHours(2)));
        QuestionRepository questions = mock(QuestionRepository.class);
        when(questions.getAllIncompleteQuestionsWithNoJob()).thenReturn(new ArrayList<>());
        var requeuer = new IncompleteQuestionRequeuer(questions, jobRepository);

        assertEquals(1, requeuer.requeueIncompleteQuestions());
        Job recovered = jobRepository.findJobById(stale.getId()).orElseThrow();
        assertEquals(JobStatus.INCOMPLETE, recovered.getStatus());
        assertEquals(0, recovered.getAttempts());
        assertEquals(List.of(recovered), jobRepository.findJobsByQuestionId(stale.getQuestionId()));
        assertEquals(0, requeuer.requeueIncompleteQuestions());
    }

    @Test
    void concurrentRequeuesRecoverAndCountTheStaleJobOnlyOnce() throws Exception {
        Job stale = recoveryJob(
                JobStatus.PROCESSING,
                Optional.of(StandardizedOffsetDateTime.now().minusHours(2)));
        QuestionRepository questions = mock(QuestionRepository.class);
        when(questions.getAllIncompleteQuestionsWithNoJob()).thenReturn(new ArrayList<>());
        var firstRequeuer = new IncompleteQuestionRequeuer(questions, jobRepository);
        var secondRequeuer = new IncompleteQuestionRequeuer(questions, jobRepository);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try (var executor = Executors.newFixedThreadPool(2)) {
            var results = List.of(firstRequeuer, secondRequeuer).stream()
                    .map(requeuer -> executor.submit(() -> {
                        ready.countDown();
                        assertTrue(start.await(10, TimeUnit.SECONDS));
                        return requeuer.requeueIncompleteQuestions();
                    }))
                    .toList();
            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();
            int firstCount = results.get(0).get(10, TimeUnit.SECONDS);
            int secondCount = results.get(1).get(10, TimeUnit.SECONDS);

            assertEquals(
                    List.of(0, 1),
                    List.of(firstCount, secondCount).stream().sorted().toList());
            Job recovered = jobRepository.findJobById(stale.getId()).orElseThrow();
            assertEquals(JobStatus.INCOMPLETE, recovered.getStatus());
            assertEquals(0, recovered.getAttempts());
            assertEquals(List.of(recovered), jobRepository.findJobsByQuestionId(stale.getQuestionId()));
        } finally {
            start.countDown();
            jobRepository.deleteJobById(stale.getId());
        }
    }

    private Job recoveryJob(final JobStatus status, final Optional<OffsetDateTime> processedAt) {
        Job job = Job.builder()
                .questionId(UUID.randomUUID().toString())
                .status(status)
                .build();
        assertTrue(jobRepository.createJob(job));
        job.setAttempts(3);
        job.setProcessedAt(processedAt);
        job.setCompletedAt(Optional.of(StandardizedOffsetDateTime.now().minusMinutes(30)));
        job.setNextAttemptAt(StandardizedOffsetDateTime.now().plusHours(2));
        assertTrue(jobRepository.updateJob(job));
        return jobRepository.findJobById(job.getId()).orElseThrow();
    }
}
