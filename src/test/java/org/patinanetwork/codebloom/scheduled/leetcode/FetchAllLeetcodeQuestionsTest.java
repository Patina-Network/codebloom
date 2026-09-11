package org.patinanetwork.codebloom.scheduled.leetcode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.patinanetwork.codebloom.common.db.models.question.bank.QuestionBank;
import org.patinanetwork.codebloom.common.db.models.task.BackgroundTask;
import org.patinanetwork.codebloom.common.db.models.task.BackgroundTaskEnum;
import org.patinanetwork.codebloom.common.db.repos.question.questionbank.QuestionBankRepository;
import org.patinanetwork.codebloom.common.db.repos.task.BackgroundTaskRepository;
import org.patinanetwork.codebloom.common.env.Env;
import org.patinanetwork.codebloom.common.leetcode.LeetcodeClient;
import org.patinanetwork.codebloom.common.leetcode.models.LeetcodeQuestion;

public class FetchAllLeetcodeQuestionsTest {

    private BackgroundTaskRepository backgroundTaskRepository;
    private LeetcodeClient leetcodeClient;
    private QuestionBankRepository questionBankRepository;
    private Env env;

    private FetchAllLeetcodeQuestions job;

    @BeforeEach
    void setup() {
        backgroundTaskRepository = mock(BackgroundTaskRepository.class);
        leetcodeClient = mock(LeetcodeClient.class);
        questionBankRepository = mock(QuestionBankRepository.class);
        env = mock(Env.class);

        job = new FetchAllLeetcodeQuestions(backgroundTaskRepository, leetcodeClient, questionBankRepository, env);

        BackgroundTask lastSync = BackgroundTask.builder()
                .task(BackgroundTaskEnum.LEETCODE_QUESTION_BANK)
                .completedAt(OffsetDateTime.now().minusHours(20))
                .build();

        when(backgroundTaskRepository.getMostRecentlyCompletedBackgroundTaskByTaskEnum(
                        BackgroundTaskEnum.LEETCODE_QUESTION_BANK))
                .thenReturn(Optional.of(lastSync));
    }

    @Test
    void testSyncCreatesAndDeletesQuestions() {
        LeetcodeQuestion q1 = LeetcodeQuestion.builder()
                .titleSlug("two-sum")
                .questionTitle("Two Sum")
                .questionId(1)
                .difficulty("Easy")
                .link("l1")
                .acceptanceRate(50.0f)
                .build();

        LeetcodeQuestion q2 = LeetcodeQuestion.builder()
                .titleSlug("add-two-numbers")
                .questionTitle("Add Two Numbers")
                .questionId(2)
                .difficulty("Medium")
                .link("l2")
                .acceptanceRate(33.0f)
                .build();

        LeetcodeQuestion q3 = LeetcodeQuestion.builder()
                .titleSlug("climbing-stairs")
                .questionTitle("Climbing Stairs")
                .questionId(3)
                .difficulty("Hard")
                .link("l3")
                .acceptanceRate(12.5f)
                .build();

        when(leetcodeClient.getAllProblems()).thenReturn(List.of(q1, q2, q3));

        QuestionBank existing1 =
                QuestionBank.builder().id("id-existing").questionSlug("two-sum").build();

        QuestionBank existing2 = QuestionBank.builder()
                .id("id-existing-2")
                .questionSlug("climbing-stairs")
                .build();

        QuestionBank outdated = QuestionBank.builder()
                .id("id-outdated")
                .questionSlug("old-question")
                .build();

        when(questionBankRepository.getAllQuestions()).thenReturn(List.of(existing1, existing2, outdated));

        job.updateQuestionBank();

        ArgumentCaptor<QuestionBank> createCaptor = ArgumentCaptor.forClass(QuestionBank.class);
        verify(questionBankRepository, times(1)).createQuestion(createCaptor.capture());
        assertEquals("add-two-numbers", createCaptor.getValue().getQuestionSlug());

        verify(questionBankRepository, times(1)).deleteQuestionById("id-outdated");
        ArgumentCaptor<BackgroundTask> taskCaptor = ArgumentCaptor.forClass(BackgroundTask.class);
        verify(backgroundTaskRepository, times(1)).createBackgroundTask(taskCaptor.capture());
        assertEquals(
                BackgroundTaskEnum.LEETCODE_QUESTION_BANK, taskCaptor.getValue().getTask());
    }

    @Test
    void testUpdateQuestionBankInvalidDifficulty() {
        LeetcodeQuestion q1 = LeetcodeQuestion.builder()
                .titleSlug("climbing-stairs")
                .questionTitle("Climbing Stairs")
                .questionId(1)
                .difficulty("Very Difficult")
                .link("l1")
                .acceptanceRate(50.0f)
                .build();

        when(leetcodeClient.getAllProblems()).thenReturn(List.of(q1));
        QuestionBank existing = QuestionBank.builder()
                .id("id-existing")
                .questionSlug("climbing-stairs")
                .build();
        QuestionBank outdated = QuestionBank.builder()
                .id("id-outdated")
                .questionSlug("old-question")
                .build();
        when(questionBankRepository.getAllQuestions()).thenReturn(List.of(existing, outdated));
        assertThrows(
                IllegalArgumentException.class,
                () -> {
                    job.updateQuestionBank();
                },
                "Updating with invalid leetcode question difficulty should throw exception");
    }

    @Test
    void testInit() {
        when(env.isDev()).thenReturn(true);
        FetchAllLeetcodeQuestions spyJob = spy(job);
        spyJob.init();
        verify(spyJob, times(1)).init();
    }

    @Test
    void preservesPremiumMetadataMissingFromFreeProblemList() {
        when(leetcodeClient.getAllProblems()).thenReturn(List.of());
        when(questionBankRepository.getAllQuestions())
                .thenReturn(List.of(QuestionBank.builder()
                        .id("premium-id")
                        .questionSlug("premium")
                        .isPaidOnly(true)
                        .build()));
        job.updateQuestionBank();
        verify(questionBankRepository, org.mockito.Mockito.never()).deleteQuestionById("premium-id");
    }
}
