package org.patinanetwork.codebloom.scheduled.leetcode;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BlockingBucket;
import io.github.bucket4j.Bucket;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import lombok.extern.slf4j.Slf4j;
import org.patinanetwork.codebloom.common.db.models.job.Job;
import org.patinanetwork.codebloom.common.db.models.job.JobStatus;
import org.patinanetwork.codebloom.common.db.models.question.Question;
import org.patinanetwork.codebloom.common.db.repos.job.JobRepository;
import org.patinanetwork.codebloom.common.db.repos.question.QuestionRepository;
import org.patinanetwork.codebloom.common.dto.Empty;
import org.patinanetwork.codebloom.common.leetcode.LeetcodeClient;
import org.patinanetwork.codebloom.common.leetcode.throttled.ThrottledLeetcodeClient;
import org.patinanetwork.codebloom.common.time.StandardizedOffsetDateTime;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("!ci | thread")
public class LeetcodeQuestionProcessService {

    private static final ReentrantLock LOCK = new ReentrantLock();

    private static final int MAX_JOBS_PER_RUN = 10;
    private static final long REQUESTS_OVER_TIME = 1L;
    private static final long MILLISECONDS_TO_WAIT = 750L;
    private static final int MAX_ATTEMPTS = 3;

    private final JobRepository jobRepository;
    private final LeetcodeClient leetcodeClient;
    private final QuestionRepository questionRepository;
    private final BlockingBucket rateLimiter;

    private BlockingBucket initializeBucket() {
        var bandwidth = Bandwidth.builder()
                .capacity(REQUESTS_OVER_TIME)
                .refillIntervally(REQUESTS_OVER_TIME, Duration.ofMillis(MILLISECONDS_TO_WAIT))
                .build();

        return Bucket.builder().addLimit(bandwidth).build().asBlocking();
    }

    private void waitForToken() {
        try {
            rateLimiter.consume(1);
        } catch (InterruptedException e) {
            throw new RuntimeException(
                    "Failed to consume rate limit bucket token in leetcode question process service", e);
        }
    }

    public LeetcodeQuestionProcessService(
            final JobRepository jobRepository,
            final ThrottledLeetcodeClient throttledLeetcodeClient,
            final QuestionRepository questionRepository) {
        this.jobRepository = jobRepository;
        this.leetcodeClient = throttledLeetcodeClient;
        this.questionRepository = questionRepository;
        this.rateLimiter = initializeBucket();
    }

    /**
     * Queries the repo to find incomplete jobs by max size and returns them.
     *
     * @param maxSize the maximum number of jobs to claim
     * @return list of incomplete jobs to process
     */
    private List<Job> claimBatch(final int maxSize) {
        return jobRepository.findIncompleteJobs(maxSize);
    }

    @Scheduled(initialDelay = 15, fixedDelay = 30, timeUnit = TimeUnit.MINUTES)
    @Async
    public CompletableFuture<Empty> drainQueue() {
        if (!LOCK.tryLock()) {
            log.info("thread attempted to drain queue, but queue is already being drained.");
            return CompletableFuture.completedFuture(Empty.of());
        }

        try {
            while (true) {
                List<Job> jobs = claimBatch(MAX_JOBS_PER_RUN);

                if (jobs.isEmpty()) {
                    log.info("No more work to do");
                    break;
                }

                log.info("Found {} jobs to process", jobs.size());

                for (Job job : jobs) {
                    try {
                        waitForToken();
                        fetchAndUpdate(job);
                    } catch (Exception e) {
                        log.error(
                                "Failed to process job with id: {} for questionId: {}",
                                job.getId(),
                                job.getQuestionId(),
                                e);
                    }
                }
            }
        } finally {
            LOCK.unlock();
        }
        return CompletableFuture.completedFuture(Empty.of());
    }

    /**
     * Actually processes the job, makes updates to the repo as necessary to indicate the state that the job is in. This
     * will fetch the question from our backend first, then use the leetcode ID to get submission details.
     */
    private void fetchAndUpdate(final Job job) {
        if (job.getAttempts() >= MAX_ATTEMPTS) {
            log.warn("Job {} reached max attempts. Marking as completed.", job.getId());
            job.setStatus(JobStatus.COMPLETE);
            jobRepository.updateJob(job);
            return;
        }

        log.info("Processing job {} for questionId: {}", job.getId(), job.getQuestionId());
        job.setAttempts(job.getAttempts() + 1);
        job.setStatus(JobStatus.PROCESSING);
        job.setProcessedAt(StandardizedOffsetDateTime.now());
        boolean success = jobRepository.updateJob(job);
        if (!success) {
            throw new RuntimeException("Failed to update job status to PROCESSING");
        }

        try {
            log.info("Fetching question from backend with ID: {}", job.getQuestionId());
            Question question = questionRepository
                    .getQuestionById(job.getQuestionId())
                    .orElseThrow(
                            () -> new RuntimeException("No question found in backend with ID: " + job.getQuestionId()));

            log.info("Found question: {} ({})", question.getQuestionTitle(), question.getQuestionSlug());

            boolean dataFound = false;

            if (question.getSubmissionId().isPresent()
                    && !question.getSubmissionId().get().isEmpty()) {
                try {
                    int submissionId =
                            Integer.parseInt(question.getSubmissionId().get());
                    log.info("Fetching submission details from Leetcode for submission ID: {}", submissionId);

                    var detailedSubmission = leetcodeClient.findSubmissionDetailBySubmissionId(submissionId);

                    if (detailedSubmission != null) {
                        log.info("Successfully fetched submission details for submission ID: {}", submissionId);

                        question.setRuntime(Optional.ofNullable(detailedSubmission.getRuntimeDisplay()));
                        question.setMemory(Optional.ofNullable(detailedSubmission.getMemoryDisplay()));
                        question.setCode(Optional.ofNullable(detailedSubmission.getCode()));

                        if (detailedSubmission.getLang() != null) {
                            question.setLanguage(
                                    Optional.of(detailedSubmission.getLang().getName()));
                        }

                        questionRepository.updateQuestion(question);
                        log.info("Successfully updated question ID: {} with submission details", question.getId());
                        dataFound = true;
                    } else {
                        log.warn("No detailed submission found for submission ID: {}", submissionId);
                    }
                } catch (NumberFormatException e) {
                    log.warn(
                            "Invalid submission ID format '{}' for question {}",
                            question.getSubmissionId().get(),
                            question.getId());
                }
            }

            if (dataFound) {
                job.setStatus(JobStatus.COMPLETE);
                job.setCompletedAt(StandardizedOffsetDateTime.now());

                log.info(
                        "Successfully completed job {} for question: {} (ID: {})",
                        job.getId(),
                        question.getQuestionTitle(),
                        question.getId());
            } else {
                job.setNextAttemptAt(StandardizedOffsetDateTime.now().plusMinutes(30));

                log.info(
                        "No submission data found for job {} for question: {} (ID: {}), scheduled for retry in 30 minutes",
                        job.getId(),
                        question.getQuestionTitle(),
                        question.getId());
            }

            success = jobRepository.updateJob(job);
            if (!success) {
                throw new RuntimeException("Failed to update job");
            }
        } catch (Exception e) {
            job.setStatus(JobStatus.INCOMPLETE);
            job.setProcessedAt(null);
            jobRepository.updateJob(job);

            log.error("Failed to process job {} for questionId: {}", job.getId(), job.getQuestionId(), e);
            throw new RuntimeException("Job processing failed", e);
        }
    }
}
