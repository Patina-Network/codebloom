package org.patinanetwork.codebloom.common.submissions;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.patinanetwork.codebloom.common.db.models.job.Job;
import org.patinanetwork.codebloom.common.db.models.leaderboard.Leaderboard;
import org.patinanetwork.codebloom.common.db.models.potd.POTD;
import org.patinanetwork.codebloom.common.db.models.question.Question;
import org.patinanetwork.codebloom.common.db.models.question.QuestionDifficulty;
import org.patinanetwork.codebloom.common.db.models.question.bank.QuestionBank;
import org.patinanetwork.codebloom.common.db.models.question.topic.LeetcodeTopicEnum;
import org.patinanetwork.codebloom.common.db.models.question.topic.QuestionTopic;
import org.patinanetwork.codebloom.common.db.models.user.User;
import org.patinanetwork.codebloom.common.db.models.user.UserWithScore;
import org.patinanetwork.codebloom.common.db.repos.job.JobRepository;
import org.patinanetwork.codebloom.common.db.repos.leaderboard.LeaderboardRepository;
import org.patinanetwork.codebloom.common.db.repos.potd.POTDRepository;
import org.patinanetwork.codebloom.common.db.repos.question.QuestionRepository;
import org.patinanetwork.codebloom.common.db.repos.question.questionbank.QuestionBankRepository;
import org.patinanetwork.codebloom.common.db.repos.question.topic.QuestionTopicRepository;
import org.patinanetwork.codebloom.common.db.repos.user.UserRepository;
import org.patinanetwork.codebloom.common.leetcode.models.LeetcodeQuestion;
import org.patinanetwork.codebloom.common.leetcode.models.LeetcodeSubmission;
import org.patinanetwork.codebloom.common.leetcode.models.LeetcodeTopicTag;
import org.patinanetwork.codebloom.common.leetcode.throttled.ThrottledLeetcodeClient;
import org.patinanetwork.codebloom.common.reporter.throttled.ThrottledReporter;
import org.patinanetwork.codebloom.common.submissions.object.AcceptedSubmission;

@DisplayName("SubmissionsHandler")
class SubmissionsHandlerTest {
    private final QuestionRepository questionRepository = mock(QuestionRepository.class);
    private final ThrottledLeetcodeClient leetcodeClient = mock(ThrottledLeetcodeClient.class);
    private final LeaderboardRepository leaderboardRepository = mock(LeaderboardRepository.class);
    private final POTDRepository potdRepository = mock(POTDRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final QuestionTopicRepository questionTopicRepository = mock(QuestionTopicRepository.class);
    private final ThrottledReporter throttledReporter = mock(ThrottledReporter.class);
    private final JobRepository jobRepository = mock(JobRepository.class);
    private final QuestionBankRepository questionBankRepository = mock(QuestionBankRepository.class);

    private final SubmissionsHandler handler = new SubmissionsHandler(
            questionRepository,
            leetcodeClient,
            leaderboardRepository,
            potdRepository,
            userRepository,
            questionTopicRepository,
            throttledReporter,
            jobRepository,
            questionBankRepository);

    private static final String USER_ID = "user-42";
    private static final String LEADERBOARD_ID = "lb-99";

    private final User user = User.builder()
            .id(USER_ID)
            .discordId("d123")
            .discordName("tester")
            .leetcodeUsername("leet_tester")
            .build();

    private final Leaderboard leaderboard = Leaderboard.builder()
            .id(LEADERBOARD_ID)
            .createdAt(LocalDateTime.of(2025, 1, 1, 0, 0))
            .build();

    private LeetcodeSubmission acceptedSubmission(int id, String slug, LocalDateTime timestamp) {
        return new LeetcodeSubmission(id, slug, slug, timestamp, "Accepted");
    }

    private LeetcodeQuestion leetcodeQuestion(String slug, String difficulty, float acceptanceRate) {
        return LeetcodeQuestion.builder()
                .questionId(1)
                .questionTitle(slug)
                .titleSlug(slug)
                .difficulty(difficulty)
                .acceptanceRate(acceptanceRate)
                .question("Description of " + slug)
                .topics(List.of(
                        LeetcodeTopicTag.builder().name("Array").slug("array").build()))
                .build();
    }

    @BeforeEach
    void commonStubs() {
        when(leaderboardRepository.getRecentLeaderboardMetadata()).thenReturn(Optional.of(leaderboard));
        when(potdRepository.getCurrentPOTD()).thenReturn(Optional.empty());

        when(questionRepository.createQuestion(any(Question.class))).thenAnswer(inv -> {
            Question q = inv.getArgument(0);
            q.setId("q-new");
            return q;
        });

        when(userRepository.getUserWithScoreByIdAndLeaderboardId(eq(USER_ID), eq(LEADERBOARD_ID), any()))
                .thenReturn(UserWithScore.builder()
                        .id(USER_ID)
                        .discordId("d123")
                        .discordName("tester")
                        .totalScore(0)
                        .build());
    }

    @Test
    @DisplayName("ignores submissions that are not 'Accepted'")
    void skipsNonAcceptedSubmissions() {
        LeetcodeSubmission rejected =
                new LeetcodeSubmission(1, "two-sum", "two-sum", LocalDateTime.now(), "Wrong Answer");
        when(leetcodeClient.findQuestionBySlug("two-sum")).thenReturn(leetcodeQuestion("two-sum", "Easy", 50f));

        ArrayList<AcceptedSubmission> result = handler.handleSubmissions(List.of(rejected), user, false);

        assertTrue(result.isEmpty());
        verify(questionRepository, never()).createQuestion(any());
    }

    @Test
    @DisplayName("ignores submissions that already exist (by submission ID)")
    void skipsDuplicateSubmissions() {
        LeetcodeSubmission sub = acceptedSubmission(100, "two-sum", LocalDateTime.now());
        when(questionRepository.questionExistsBySubmissionId("100")).thenReturn(true);

        when(leetcodeClient.findQuestionBySlug("two-sum")).thenReturn(leetcodeQuestion("two-sum", "Easy", 50f));

        ArrayList<AcceptedSubmission> result = handler.handleSubmissions(List.of(sub), user, false);

        assertTrue(result.isEmpty());
        verify(questionRepository, never()).createQuestion(any());
    }

    @Test
    @DisplayName("creates a question in the database for a new accepted submission")
    void createsQuestion() {
        LeetcodeSubmission submission = acceptedSubmission(200, "two-sum", LocalDateTime.of(2025, 6, 1, 12, 0));
        when(questionRepository.questionExistsBySubmissionId("200")).thenReturn(false);
        when(questionRepository.getQuestionBySlugAndUserId("two-sum", USER_ID)).thenReturn(Optional.empty());
        when(leetcodeClient.findQuestionBySlug("two-sum")).thenReturn(leetcodeQuestion("two-sum", "Easy", 50f));

        handler.handleSubmissions(List.of(submission), user, false);

        ArgumentCaptor<Question> qCaptor = ArgumentCaptor.forClass(Question.class);
        verify(questionRepository).createQuestion(qCaptor.capture());

        Question created = qCaptor.getValue();
        assertEquals("two-sum", created.getQuestionSlug());
        assertEquals(QuestionDifficulty.Easy, created.getQuestionDifficulty());
        assertEquals(USER_ID, created.getUserId());
    }

    @Test
    @DisplayName("creates a job for a new accepted submission")
    void createsJob() {
        LeetcodeSubmission submission = acceptedSubmission(200, "two-sum", LocalDateTime.of(2025, 6, 1, 12, 0));
        when(questionRepository.questionExistsBySubmissionId("200")).thenReturn(false);
        when(questionRepository.getQuestionBySlugAndUserId("two-sum", USER_ID)).thenReturn(Optional.empty());
        when(leetcodeClient.findQuestionBySlug("two-sum")).thenReturn(leetcodeQuestion("two-sum", "Easy", 50f));

        handler.handleSubmissions(List.of(submission), user, false);

        verify(jobRepository).createJob(any(Job.class));
    }

    @Test
    @DisplayName("creates topic entries for a new accepted submission")
    void createsTopics() {
        LeetcodeSubmission submission = acceptedSubmission(200, "two-sum", LocalDateTime.of(2025, 6, 1, 12, 0));
        when(questionRepository.questionExistsBySubmissionId("200")).thenReturn(false);
        when(questionRepository.getQuestionBySlugAndUserId("two-sum", USER_ID)).thenReturn(Optional.empty());
        when(leetcodeClient.findQuestionBySlug("two-sum")).thenReturn(leetcodeQuestion("two-sum", "Easy", 50f));

        handler.handleSubmissions(List.of(submission), user, false);

        verify(questionTopicRepository).createQuestionTopic(argThat(qt -> "array".equals(qt.getTopicSlug())));
    }

    @Test
    @DisplayName("returns the accepted submission with a title and question ID")
    void returnsAcceptedSubmission() {
        LeetcodeSubmission submission = acceptedSubmission(200, "two-sum", LocalDateTime.of(2025, 6, 1, 12, 0));
        when(questionRepository.questionExistsBySubmissionId("200")).thenReturn(false);
        when(questionRepository.getQuestionBySlugAndUserId("two-sum", USER_ID)).thenReturn(Optional.empty());
        when(leetcodeClient.findQuestionBySlug("two-sum")).thenReturn(leetcodeQuestion("two-sum", "Easy", 50f));

        ArrayList<AcceptedSubmission> result = handler.handleSubmissions(List.of(submission), user, false);

        assertEquals(1, result.size());
        assertEquals("two-sum", result.get(0).title());
        assertNotNull(result.get(0).questionId());
    }

    @Test
    @DisplayName("updates the user's leaderboard score for a new accepted submission")
    void updatesLeaderboardScore() {
        LeetcodeSubmission submission = acceptedSubmission(200, "two-sum", LocalDateTime.of(2025, 6, 1, 12, 0));
        when(questionRepository.questionExistsBySubmissionId("200")).thenReturn(false);
        when(questionRepository.getQuestionBySlugAndUserId("two-sum", USER_ID)).thenReturn(Optional.empty());
        when(leetcodeClient.findQuestionBySlug("two-sum")).thenReturn(leetcodeQuestion("two-sum", "Easy", 50f));

        handler.handleSubmissions(List.of(submission), user, false);

        verify(leaderboardRepository).updateUserPointsFromLeaderboard(eq(LEADERBOARD_ID), eq(USER_ID), anyInt());
    }

    @Test
    @DisplayName("awards 0 points when the user already solved the same question")
    void zeroPtsForDuplicateQuestion() {
        LeetcodeSubmission sub = acceptedSubmission(300, "two-sum", LocalDateTime.of(2025, 6, 1, 12, 0));
        when(questionRepository.questionExistsBySubmissionId("300")).thenReturn(false);

        Question existingQ =
                Question.builder().id("q-old").questionSlug("two-sum").build();
        when(questionRepository.getQuestionBySlugAndUserId("two-sum", USER_ID)).thenReturn(Optional.of(existingQ));
        when(leetcodeClient.findQuestionBySlug("two-sum")).thenReturn(leetcodeQuestion("two-sum", "Easy", 50f));

        ArrayList<AcceptedSubmission> result = handler.handleSubmissions(List.of(sub), user, false);

        assertEquals(1, result.size());
        assertEquals(0, result.get(0).points());
    }

    @Test
    @DisplayName("awards 0 points when submission timestamp is before leaderboard creation")
    void zeroPtsForOldSubmission() {
        LeetcodeSubmission oldSub = acceptedSubmission(400, "two-sum", LocalDateTime.of(2024, 12, 31, 23, 59));
        when(questionRepository.questionExistsBySubmissionId("400")).thenReturn(false);
        when(questionRepository.getQuestionBySlugAndUserId("two-sum", USER_ID)).thenReturn(Optional.empty());
        when(leetcodeClient.findQuestionBySlug("two-sum")).thenReturn(leetcodeQuestion("two-sum", "Easy", 50f));

        ArrayList<AcceptedSubmission> result = handler.handleSubmissions(List.of(oldSub), user, false);

        assertEquals(1, result.size());
        assertEquals(0, result.get(0).points());
    }

    @Test
    @DisplayName("throws RuntimeException when no recent leaderboard exists")
    void throwsWhenNoLeaderboard() {
        when(leaderboardRepository.getRecentLeaderboardMetadata()).thenReturn(Optional.empty());

        LeetcodeSubmission sub = acceptedSubmission(500, "two-sum", LocalDateTime.now());
        when(questionRepository.questionExistsBySubmissionId("500")).thenReturn(false);
        when(leetcodeClient.findQuestionBySlug("two-sum")).thenReturn(leetcodeQuestion("two-sum", "Easy", 50f));

        assertThrows(RuntimeException.class, () -> handler.handleSubmissions(List.of(sub), user, false));
    }

    @Test
    @DisplayName("uses findQuestionBySlugFast when fast=true")
    void usesSlugFastInFastMode() {
        LeetcodeSubmission sub = acceptedSubmission(600, "two-sum", LocalDateTime.of(2025, 6, 1, 12, 0));
        when(questionRepository.questionExistsBySubmissionId("600")).thenReturn(false);
        when(questionRepository.getQuestionBySlugAndUserId("two-sum", USER_ID)).thenReturn(Optional.empty());
        when(leetcodeClient.findQuestionBySlugFast("two-sum")).thenReturn(leetcodeQuestion("two-sum", "Easy", 50f));

        handler.handleSubmissions(List.of(sub), user, true);

        verify(leetcodeClient).findQuestionBySlugFast("two-sum");
        verify(leetcodeClient, never()).findQuestionBySlug(anyString());
    }

    @Test
    @DisplayName("uses findQuestionBySlug when fast=false")
    void usesSlugSlowInNonFastMode() {
        LeetcodeSubmission sub = acceptedSubmission(601, "hello-world", LocalDateTime.of(2025, 6, 1, 12, 0));
        when(questionRepository.questionExistsBySubmissionId("601")).thenReturn(false);
        when(questionRepository.getQuestionBySlugAndUserId("hello-world", USER_ID))
                .thenReturn(Optional.empty());
        when(leetcodeClient.findQuestionBySlug("hello-world")).thenReturn(leetcodeQuestion("hello-world", "Easy", 50f));

        ArrayList<AcceptedSubmission> result = handler.handleSubmissions(List.of(sub), user, false);

        assertEquals(1, result.size());
        verify(leetcodeClient).findQuestionBySlug("hello-world");
        verify(leetcodeClient, never()).findQuestionBySlugFast(anyString());
    }

    @Test
    @DisplayName("uses question bank entry and skips leetcode fetch when bank question exists")
    void testNoBank() {
        LeetcodeSubmission sub = acceptedSubmission(602, "hello-world", LocalDateTime.of(2025, 6, 1, 12, 0));
        when(questionRepository.questionExistsBySubmissionId("602")).thenReturn(false);
        when(questionRepository.getQuestionBySlugAndUserId("hello-world", USER_ID))
                .thenReturn(Optional.empty());

        LeetcodeQuestion lcQuestion = leetcodeQuestion("hello-world", "Easy", 50f);

        QuestionBank bankQuestion = QuestionBank.builder()
                .questionSlug(lcQuestion.getTitleSlug())
                .questionDifficulty(QuestionDifficulty.valueOf(lcQuestion.getDifficulty()))
                .questionTitle(lcQuestion.getQuestionTitle())
                .questionNumber(lcQuestion.getQuestionId())
                .questionLink("https://leetcode.com/problems/" + lcQuestion.getTitleSlug())
                .description(Optional.ofNullable(lcQuestion.getQuestion()))
                .acceptanceRate(lcQuestion.getAcceptanceRate())
                .topics(lcQuestion.getTopics().stream()
                        .map(t -> QuestionTopic.builder()
                                .topicSlug(t.getSlug())
                                .topic(LeetcodeTopicEnum.fromValue(t.getSlug()))
                                .build())
                        .toList())
                .build();

        when(questionBankRepository.getQuestionBySlug(anyString())).thenReturn(Optional.of(bankQuestion));

        ArrayList<AcceptedSubmission> result = handler.handleSubmissions(List.of(sub), user, true);

        assertEquals(1, result.size());
        verify(leetcodeClient, never()).findQuestionBySlugFast(anyString());
        verify(leetcodeClient, never()).findQuestionBySlug(anyString());
    }

    @Test
    @DisplayName("re-fetches and backfills the question bank entry when its description is missing")
    void backfillsMissingDescription() {
        LeetcodeSubmission sub = acceptedSubmission(603, "two-sum", LocalDateTime.of(2025, 6, 1, 12, 0));
        when(questionRepository.questionExistsBySubmissionId("603")).thenReturn(false);
        when(questionRepository.getQuestionBySlugAndUserId("two-sum", USER_ID)).thenReturn(Optional.empty());

        QuestionBank staleBankQuestion = QuestionBank.builder()
                .id("bank-1")
                .questionSlug("two-sum")
                .questionDifficulty(QuestionDifficulty.Easy)
                .questionTitle("two-sum")
                .questionNumber(1)
                .questionLink("https://leetcode.com/problems/two-sum")
                .description(Optional.empty())
                .acceptanceRate(50f)
                .topics(List.of())
                .build();

        when(questionBankRepository.getQuestionBySlug("two-sum")).thenReturn(Optional.of(staleBankQuestion));
        when(leetcodeClient.findQuestionBySlug("two-sum")).thenReturn(leetcodeQuestion("two-sum", "Easy", 50f));

        handler.handleSubmissions(List.of(sub), user, false);

        verify(leetcodeClient).findQuestionBySlug("two-sum");

        ArgumentCaptor<QuestionBank> bankCaptor = ArgumentCaptor.forClass(QuestionBank.class);
        verify(questionBankRepository).updateQuestion(bankCaptor.capture());
        assertEquals("bank-1", bankCaptor.getValue().getId());
        assertEquals(
                Optional.of("Description of two-sum"), bankCaptor.getValue().getDescription());

        ArgumentCaptor<Question> qCaptor = ArgumentCaptor.forClass(Question.class);
        verify(questionRepository).createQuestion(qCaptor.capture());
        assertEquals(Optional.of("Description of two-sum"), qCaptor.getValue().getDescription());
    }

    @Test
    @DisplayName("distinctByKey filters duplicates by the given key")
    void distinctByKeyWorks() {
        List<String> input = List.of("aaa", "abc", "aab", "bbb");
        List<String> result = input.stream()
                .filter(SubmissionsHandler.distinctByKey(s -> s.charAt(0)))
                .toList();

        assertEquals(2, result.size());
        assertEquals("aaa", result.get(0));
        assertEquals("bbb", result.get(1));
    }

    @Test
    @DisplayName("applies POTD multiplier when the submission matches the POTD slug")
    void appliesPotdMultiplier() {
        POTD potd = POTD.builder()
                .slug("two-sum")
                .multiplier(2.0f)
                .createdAt(LocalDateTime.now())
                .build();
        when(potdRepository.getCurrentPOTD()).thenReturn(Optional.of(potd));

        LeetcodeSubmission sub = acceptedSubmission(700, "two-sum", LocalDateTime.of(2025, 6, 1, 12, 0));
        when(questionRepository.questionExistsBySubmissionId("700")).thenReturn(false);
        when(questionRepository.getQuestionBySlugAndUserId("two-sum", USER_ID)).thenReturn(Optional.empty());
        when(leetcodeClient.findQuestionBySlug("two-sum")).thenReturn(leetcodeQuestion("two-sum", "Easy", 50f));

        when(potdRepository.getCurrentPOTD()).thenReturn(Optional.empty());
        handler.handleSubmissions(
                List.of(acceptedSubmission(701, "two-sum", LocalDateTime.of(2025, 6, 1, 12, 0))), user, false);

        reset(questionRepository);
        when(questionRepository.questionExistsBySubmissionId("700")).thenReturn(false);
        when(questionRepository.getQuestionBySlugAndUserId("two-sum", USER_ID)).thenReturn(Optional.empty());
        when(questionRepository.createQuestion(any(Question.class))).thenAnswer(inv -> {
            Question q = inv.getArgument(0);
            q.setId("q-potd");
            return q;
        });
        when(potdRepository.getCurrentPOTD()).thenReturn(Optional.of(potd));

        ArrayList<AcceptedSubmission> potdResult = handler.handleSubmissions(List.of(sub), user, false);

        assertFalse(potdResult.isEmpty());
        verify(questionRepository, atLeast(1)).createQuestion(any(Question.class));
    }
}
