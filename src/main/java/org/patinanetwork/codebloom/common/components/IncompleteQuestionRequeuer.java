package org.patinanetwork.codebloom.common.components;

import java.util.ArrayList;
import lombok.extern.slf4j.Slf4j;
import org.patinanetwork.codebloom.common.db.models.job.Job;
import org.patinanetwork.codebloom.common.db.models.job.JobStatus;
import org.patinanetwork.codebloom.common.db.models.question.Question;
import org.patinanetwork.codebloom.common.db.repos.job.JobRepository;
import org.patinanetwork.codebloom.common.db.repos.question.QuestionRepository;
import org.patinanetwork.codebloom.common.time.StandardizedOffsetDateTime;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class IncompleteQuestionRequeuer {

    private final QuestionRepository questionRepository;
    private final JobRepository jobRepository;

    public IncompleteQuestionRequeuer(final QuestionRepository questionRepository, final JobRepository jobRepository) {
        this.questionRepository = questionRepository;
        this.jobRepository = jobRepository;
    }

    /**
     * Creates a new job for every incomplete question that has no active job.
     *
     * <p>Safe to call concurrently: the active-job unique index rejects duplicates, which are counted as skipped.
     *
     * @return number of jobs actually created.
     */
    public int requeueIncompleteQuestions() {
        ArrayList<Question> questions = questionRepository.getAllIncompleteQuestionsWithNoJob();

        if (questions.isEmpty()) {
            log.info("All questions have complete information.");
            return 0;
        }

        int queued = 0;
        for (Question question : questions) {
            String questionId = question.getId();
            log.warn("Question with id of {} does not have complete information.", questionId);
            Job newJob = Job.builder()
                    .status(JobStatus.INCOMPLETE)
                    .questionId(questionId)
                    .nextAttemptAt(StandardizedOffsetDateTime.now().minusHours(1))
                    .build();

            if (jobRepository.createJob(newJob)) {
                log.info("Added job for question of id {}", questionId);
                queued++;
            } else {
                log.info("Question of id {} already has an active job, skipping", questionId);
            }
        }
        return queued;
    }
}
