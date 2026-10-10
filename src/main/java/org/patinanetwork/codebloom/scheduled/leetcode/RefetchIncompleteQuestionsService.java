package org.patinanetwork.codebloom.scheduled.leetcode;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import org.patinanetwork.codebloom.common.db.models.job.Job;
import org.patinanetwork.codebloom.common.db.models.job.JobStatus;
import org.patinanetwork.codebloom.common.db.models.question.Question;
import org.patinanetwork.codebloom.common.db.repos.job.JobRepository;
import org.patinanetwork.codebloom.common.db.repos.question.QuestionRepository;
import org.patinanetwork.codebloom.common.time.StandardizedOffsetDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Profile("!ci")
public class RefetchIncompleteQuestionsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RefetchIncompleteQuestionsService.class);

    private final QuestionRepository questionRepository;
    private final JobRepository jobRepository;

    public RefetchIncompleteQuestionsService(
            final QuestionRepository questionRepository, final JobRepository jobRepository) {
        this.questionRepository = questionRepository;
        this.jobRepository = jobRepository;
    }

    @Scheduled(initialDelay = 0, fixedDelay = 24, timeUnit = TimeUnit.HOURS)
    public void refetchIncompleteQuestions() {
        ArrayList<Question> questions = questionRepository.getAllIncompleteQuestionsWithNoJob();

        if (questions.isEmpty()) {
            LOGGER.info("All questions have complete information.");
            return;
        }

        for (Question question : questions) {
            String questionId = question.getId();
            LOGGER.warn("Question with id of {} does not have complete information.", questionId);
            Job newJob = Job.builder()
                    .status(JobStatus.INCOMPLETE)
                    .questionId(questionId)
                    .nextAttemptAt(StandardizedOffsetDateTime.now().minusHours(1))
                    .build();

            LOGGER.info("Adding job for question of id " + questionId);
            jobRepository.createJob(newJob);
        }
    }

    @Scheduled(initialDelay = 0, fixedDelay = 24, timeUnit = TimeUnit.HOURS)
    public void unstickStuckQuestionJobs() {
        int unstuckJobs = jobRepository.unstickStuckJobs();

        LOGGER.info("unstuck {} jobs that were stuck", unstuckJobs);
    }
}
