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
     * Restarts jobs stuck in PROCESSING for at least one hour and creates a new job for every incomplete question that
     * has no active job.
     *
     * <p>Concurrent calls only count jobs they recover or create: recovery checks the current status, and the
     * active-job unique index rejects duplicate inserts.
     *
     * @return number of jobs recovered or created.
     */
    public int requeueIncompleteQuestions() {
        int requeuedCount = jobRepository.requeueStaleProcessingJobs(
                StandardizedOffsetDateTime.now().minusHours(1));

        ArrayList<Question> incompleteQuestionsWithNoJob = questionRepository.getAllIncompleteQuestionsWithNoJob();

        if (requeuedCount == 0 && incompleteQuestionsWithNoJob.isEmpty()) {
            log.info("No incomplete questions need requeueing.");
            return 0;
        }

        for (Question question : incompleteQuestionsWithNoJob) {
            String questionId = question.getId();
            log.warn("Question with id of {} does not have complete information.", questionId);
            Job newJob = Job.builder()
                    .status(JobStatus.INCOMPLETE)
                    .questionId(questionId)
                    .nextAttemptAt(StandardizedOffsetDateTime.now().minusHours(1))
                    .build();

            if (jobRepository.createJob(newJob)) {
                log.info("Added job for question of id {}", questionId);
                requeuedCount++;
            } else {
                log.info("Question of id {} already has an active job, skipping", questionId);
            }
        }
        return requeuedCount;
    }
}
