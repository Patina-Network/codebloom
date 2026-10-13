package org.patinanetwork.codebloom.scheduled.leetcode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.patinanetwork.codebloom.common.components.IncompleteQuestionRequeuer;
import org.patinanetwork.codebloom.common.db.models.job.Job;
import org.patinanetwork.codebloom.common.db.models.job.JobStatus;
import org.patinanetwork.codebloom.common.db.models.question.Question;
import org.patinanetwork.codebloom.common.db.models.question.QuestionDifficulty;
import org.patinanetwork.codebloom.common.db.repos.job.JobRepository;
import org.patinanetwork.codebloom.common.db.repos.question.QuestionRepository;
import org.patinanetwork.codebloom.common.time.StandardizedOffsetDateTime;
import org.patinanetwork.codebloom.config.NoJdaRequired;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class RefetchIncompleteQuestionsServiceTest extends NoJdaRequired {

    private final QuestionRepository questionRepository;
    private final JobRepository jobRepository;
    private final RefetchIncompleteQuestionsService service;
    private final IncompleteQuestionRequeuer requeuer;

    private Question testQuestion;
    private final String mockUserId = "ed3bfe18-e42a-467f-b4fa-07e8da4d2555";

    @Autowired
    public RefetchIncompleteQuestionsServiceTest(
            final QuestionRepository questionRepository,
            final JobRepository jobRepository,
            final RefetchIncompleteQuestionsService service,
            final IncompleteQuestionRequeuer requeuer) {
        this.questionRepository = questionRepository;
        this.jobRepository = jobRepository;
        this.service = service;
        this.requeuer = requeuer;
    }

    @BeforeAll
    void setup() {
        jobRepository.deleteAllJobs();

        testQuestion = Question.builder()
                .userId(mockUserId)
                .questionSlug("base-question-" + System.currentTimeMillis())
                .questionTitle("Base Question")
                .questionDifficulty(QuestionDifficulty.Easy)
                .questionNumber(100)
                .questionLink("https://leetcode.com/problems/base-question/")
                .description(Optional.of("Baseline question for testing"))
                .pointsAwarded(Optional.of(50))
                .language(Optional.empty())
                .acceptanceRate(0.9f)
                .submittedAt(java.time.LocalDateTime.now())
                .build();

        testQuestion = questionRepository.createQuestion(testQuestion);

        Job existingJob = Job.builder()
                .questionId(testQuestion.getId())
                .status(JobStatus.INCOMPLETE)
                .nextAttemptAt(StandardizedOffsetDateTime.now())
                .build();

        jobRepository.createJob(existingJob);
    }

    @AfterAll
    void cleanup() {
        jobRepository.deleteAllJobs();
        questionRepository.deleteQuestionById(testQuestion.getId());
    }

    @Test
    void serviceValid() {
        assertNotNull(service);
    }

    @Test
    void refetchIncompleteQuestionsValid() {
        Question orphanedQuestion = Question.builder()
                .userId(mockUserId)
                .questionSlug("orphaned-test-" + System.currentTimeMillis())
                .questionTitle("Orphaned Question")
                .questionDifficulty(QuestionDifficulty.Medium)
                .questionNumber(200)
                .questionLink("https://leetcode.com/problems/orphaned-test/")
                .description(Optional.of("This question has no job and should be picked up."))
                .pointsAwarded(Optional.of(100))
                .acceptanceRate(0.75f)
                .submittedAt(java.time.LocalDateTime.now())
                .build();

        questionRepository.createQuestion(orphanedQuestion);

        service.refetchIncompleteQuestions();

        List<Job> incompleteJobs = jobRepository.findIncompleteJobs(50);

        Job createdJob = incompleteJobs.stream()
                .filter(job -> job.getQuestionId().equals(orphanedQuestion.getId()))
                .findFirst()
                .orElse(null);

        assertNotNull(createdJob, "Service should have created a job for the orphaned question");
        assertEquals(JobStatus.INCOMPLETE, createdJob.getStatus());

        jobRepository.deleteJobById(createdJob.getId());
        questionRepository.deleteQuestionById(orphanedQuestion.getId());
    }

    @Test
    void requeueSkipsQuestionsThatAlreadyHaveAnActiveJob() {
        Question orphanedQuestion = Question.builder()
                .userId(mockUserId)
                .questionSlug("requeue-twice-" + System.currentTimeMillis())
                .questionTitle("Requeue Twice")
                .questionDifficulty(QuestionDifficulty.Medium)
                .questionNumber(201)
                .questionLink("https://leetcode.com/problems/requeue-twice/")
                .description(Optional.of("Requeued twice; must end up with one active job."))
                .pointsAwarded(Optional.of(100))
                .acceptanceRate(0.75f)
                .submittedAt(java.time.LocalDateTime.now())
                .build();
        questionRepository.createQuestion(orphanedQuestion);

        try {
            assertTrue(requeuer.requeueIncompleteQuestions() >= 1);
            requeuer.requeueIncompleteQuestions();

            List<Job> jobs = jobRepository.findJobsByQuestionId(orphanedQuestion.getId());
            assertEquals(1, jobs.size(), "A second requeue must not add another active job");
        } finally {
            jobRepository
                    .findJobsByQuestionId(orphanedQuestion.getId())
                    .forEach(j -> jobRepository.deleteJobById(j.getId()));
            questionRepository.deleteQuestionById(orphanedQuestion.getId());
        }
    }

    @Test
    void secondActiveJobForSameQuestionIsRejected() {

        Question collisionQuestion = Question.builder()
                .userId(mockUserId)
                .questionSlug("collision")
                .questionTitle("Two Sum Test")
                .questionDifficulty(QuestionDifficulty.Easy)
                .questionNumber(1)
                .questionLink("https://leetcode.com/problems/two-sum/")
                .description(
                        Optional.of(
                                "Given an array of integers nums and an integer target, return indices of the two numbers such that they add up to target."))
                .pointsAwarded(Optional.of(100))
                .acceptanceRate(0.8f)
                .submittedAt(java.time.LocalDateTime.now())
                .runtime(Optional.of("3 ms"))
                .memory(Optional.of("14.2 MB"))
                .code(Optional.of("def twoSum(self, nums, target): # test code"))
                .language(Optional.empty())
                .submissionId(Optional.of("test-submission-1234"))
                .build();

        questionRepository.createQuestion(collisionQuestion);

        Job firstJob = Job.builder()
                .questionId(collisionQuestion.getId())
                .status(JobStatus.INCOMPLETE)
                .build();
        assertTrue(jobRepository.createJob(firstJob));

        Job secondJob = Job.builder()
                .questionId(collisionQuestion.getId())
                .status(JobStatus.INCOMPLETE)
                .build();

        assertFalse(jobRepository.createJob(secondJob));
        assertNull(secondJob.getId());

        List<Job> jobs = jobRepository.findJobsByQuestionId(collisionQuestion.getId());
        assertEquals(1, jobs.size());
        assertEquals(firstJob.getId(), jobs.get(0).getId());

        jobRepository.deleteJobById(firstJob.getId());
        questionRepository.deleteQuestionById(collisionQuestion.getId());
    }
}
