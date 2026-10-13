package org.patinanetwork.codebloom.common.components;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.patinanetwork.codebloom.common.db.models.job.Job;
import org.patinanetwork.codebloom.common.db.models.question.Question;
import org.patinanetwork.codebloom.common.db.repos.job.JobRepository;
import org.patinanetwork.codebloom.common.db.repos.question.QuestionRepository;

public class IncompleteQuestionRequeuerTest {

    private final QuestionRepository questionRepository = mock(QuestionRepository.class);
    private final JobRepository jobRepository = mock(JobRepository.class);
    private final IncompleteQuestionRequeuer requeuer =
            new IncompleteQuestionRequeuer(questionRepository, jobRepository);

    private static Question question(final String id) {
        Question q = mock(Question.class);
        when(q.getId()).thenReturn(id);
        return q;
    }

    @Test
    void countsOnlyJobsThatWereActuallyCreated() {
        Question queued = question("queued");
        Question raced = question("raced");
        when(questionRepository.getAllIncompleteQuestionsWithNoJob())
                .thenReturn(new ArrayList<>(List.of(queued, raced)));
        when(jobRepository.createJob(argThat(j -> j != null && "queued".equals(j.getQuestionId()))))
                .thenReturn(true);
        when(jobRepository.createJob(argThat(j -> j != null && "raced".equals(j.getQuestionId()))))
                .thenReturn(false);

        assertEquals(1, requeuer.requeueIncompleteQuestions());
    }

    @Test
    void returnsZeroWithoutCreatingJobsWhenNothingIsIncomplete() {
        when(questionRepository.getAllIncompleteQuestionsWithNoJob()).thenReturn(new ArrayList<>());

        assertEquals(0, requeuer.requeueIncompleteQuestions());
        verify(jobRepository, never()).createJob(any(Job.class));
    }
}
