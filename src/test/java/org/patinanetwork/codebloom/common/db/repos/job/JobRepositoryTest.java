package org.patinanetwork.codebloom.common.db.repos.job;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.patinanetwork.codebloom.common.db.models.job.Job;
import org.patinanetwork.codebloom.common.db.models.job.JobStatus;
import org.patinanetwork.codebloom.common.db.repos.BaseRepositoryTest;
import org.patinanetwork.codebloom.common.time.StandardizedOffsetDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

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
}
