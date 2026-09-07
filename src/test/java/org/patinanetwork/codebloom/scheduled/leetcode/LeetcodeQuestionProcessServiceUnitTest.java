package org.patinanetwork.codebloom.scheduled.leetcode;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.patinanetwork.codebloom.common.db.models.job.Job;
import org.patinanetwork.codebloom.common.db.models.job.JobStatus;
import org.patinanetwork.codebloom.common.db.models.question.Question;
import org.patinanetwork.codebloom.common.db.repos.job.JobRepository;
import org.patinanetwork.codebloom.common.db.repos.question.QuestionRepository;
import org.patinanetwork.codebloom.common.leetcode.models.LeetcodeQuestion;
import org.patinanetwork.codebloom.common.leetcode.throttled.ThrottledLeetcodeClient;
import org.patinanetwork.codebloom.common.time.StandardizedOffsetDateTime;

class LeetcodeQuestionProcessServiceUnitTest {
    private final JobRepository jobs = mock(JobRepository.class);
    private final QuestionRepository questions = mock(QuestionRepository.class);
    private final ThrottledLeetcodeClient client = mock(ThrottledLeetcodeClient.class);
    private final Question question = Question.builder()
            .id("question-id")
            .questionSlug("two-sum")
            .description(Optional.empty())
            .submissionId(Optional.empty())
            .runtime(Optional.of("3 ms"))
            .memory(Optional.of("14 MB"))
            .code(Optional.of("existing code"))
            .language(Optional.of("java"))
            .build();
    private final Job job = Job.builder()
            .id("job-id")
            .questionId("question-id")
            .status(JobStatus.INCOMPLETE)
            .build();

    @BeforeEach
    void setup() {
        when(jobs.findIncompleteJobs(10)).thenReturn(List.of(job), List.of());
        when(jobs.updateJob(any())).thenReturn(true);
        when(questions.getQuestionById(question.getId())).thenReturn(Optional.of(question));
    }

    private void respondWith(String content) {
        var fetched = mock(LeetcodeQuestion.class);
        when(fetched.getQuestion()).thenReturn(content);
        when(client.findQuestionBySlug("two-sum")).thenReturn(fetched);
    }

    private void runQueue() {
        new LeetcodeQuestionProcessService(jobs, client, questions).drainQueue().join();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t\n"})
    void repairsExistingSubmissionWithoutLosingOtherFields(String missingDescription) {
        question.setDescription(Optional.ofNullable(missingDescription));
        respondWith("<p>Problem description</p>");
        runQueue();
        assertEquals(Optional.of("<p>Problem description</p>"), question.getDescription());
        assertEquals(Optional.of("existing code"), question.getCode());
        assertEquals(Optional.of("3 ms"), question.getRuntime());
        assertEquals(JobStatus.COMPLETE, job.getStatus());
        assertNotNull(job.getCompletedAt());
        verify(questions).updateQuestion(question);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t\n"})
    void retriesWhenLeetcodeHasNoDescription(String content) {
        respondWith(content);
        var before = StandardizedOffsetDateTime.now();
        runQueue();
        assertEquals(JobStatus.INCOMPLETE, job.getStatus());
        assertTrue(job.getNextAttemptAt().isAfter(before.plusMinutes(29)));
        assertNull(job.getCompletedAt());
        assertEquals(Optional.empty(), question.getDescription());
        verify(questions, never()).updateQuestion(any());
    }

    @Test
    void preservesExistingDescriptionWithoutRefetching() {
        question.setDescription(Optional.of("Existing description"));
        runQueue();
        verify(client, never()).findQuestionBySlug(anyString());
        verify(questions, never()).updateQuestion(any());
        assertEquals(Optional.of("Existing description"), question.getDescription());
    }

    @Test
    void failedFetchLeavesJobRetryable() {
        when(client.findQuestionBySlug("two-sum")).thenThrow(new RuntimeException("LeetCode unavailable"));
        runQueue();
        assertEquals(JobStatus.INCOMPLETE, job.getStatus());
        assertNull(job.getProcessedAt());
        assertNull(job.getCompletedAt());
        verify(questions, never()).updateQuestion(any());
    }

    @Test
    void failedSaveDoesNotMarkJobComplete() {
        respondWith("Description");
        when(questions.updateQuestion(question)).thenThrow(new RuntimeException("Database unavailable"));
        runQueue();
        assertEquals(JobStatus.INCOMPLETE, job.getStatus());
        assertNull(job.getCompletedAt());
    }
}
