package org.patinanetwork.codebloom.common.db.repos.question;

import static org.junit.jupiter.api.Assertions.*;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.patinanetwork.codebloom.common.db.models.question.Question;
import org.patinanetwork.codebloom.common.db.models.question.QuestionDifficulty;
import org.patinanetwork.codebloom.common.db.models.question.QuestionWithUser;
import org.patinanetwork.codebloom.common.db.models.question.topic.LeetcodeTopicEnum;
import org.patinanetwork.codebloom.common.db.repos.BaseRepositoryTest;
import org.patinanetwork.codebloom.common.time.StandardizedOffsetDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(OrderAnnotation.class)
@Slf4j
public class QuestionRepositoryTest extends BaseRepositoryTest {

    private QuestionRepository questionRepository;
    private Question testQuestion;
    private String mockSuperUserId = "ed3bfe18-e42a-467f-b4fa-07e8da4d2555";

    @Autowired
    public QuestionRepositoryTest(final QuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    @BeforeAll
    void createQuestion() {
        testQuestion = Question.builder()
                .userId(mockSuperUserId)
                .questionSlug("test-question-slug-123")
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
                .language(Optional.of("python"))
                .submissionId(Optional.of("test-submission-123"))
                .build();

        testQuestion = questionRepository.createQuestion(testQuestion);
        assertNotNull(testQuestion.getId(), "Question ID should be generated");
        assertNotNull(testQuestion.getCreatedAt(), "Question createdAt should be set");

        log.info("Created test question with ID: {}", testQuestion.getId());
    }

    @AfterAll
    void cleanUp() {
        if (testQuestion != null && testQuestion.getId() != null) {
            boolean isSuccessful = questionRepository.deleteQuestionById(testQuestion.getId());
            if (!isSuccessful) {
                fail("Failed to delete test question");
            }
        }
    }

    @Test
    @Order(1)
    void testGetQuestionById() {
        Question possibleTestQuestion =
                questionRepository.getQuestionById(testQuestion.getId()).orElseThrow();

        log.info("testQuestion: {}", testQuestion);
        log.info("possibleTestQuestion: {}", possibleTestQuestion);
        assertEquals(testQuestion.getId(), possibleTestQuestion.getId(), "Question IDs should match");
        assertEquals(testQuestion.getUserId(), possibleTestQuestion.getUserId(), "User IDs should match");
        assertEquals(
                testQuestion.getQuestionSlug(), possibleTestQuestion.getQuestionSlug(), "Question slugs should match");
        assertEquals(
                testQuestion.getQuestionTitle(),
                possibleTestQuestion.getQuestionTitle(),
                "Question titles should match");
    }

    @Test
    @Order(2)
    void testGetQuestionWithUserById() {
        QuestionWithUser questionWithUser =
                questionRepository.getQuestionWithUserById(testQuestion.getId()).orElseThrow();

        assertEquals(testQuestion.getId(), questionWithUser.getId(), "Question IDs should match");
        assertEquals(testQuestion.getUserId(), questionWithUser.getUserId(), "User IDs should match");

        assertTrue(questionWithUser.getDiscordName().isPresent(), "Discord name should be populated");
        assertTrue(questionWithUser.getLeetcodeUsername().isPresent(), "Leetcode username should be populated");
        assertTrue(questionWithUser.getNickname().isPresent(), "Nickname should be populated");

        log.info("Successfully retrieved question with user data for ID: {}", questionWithUser.getId());
    }

    @Test
    @Order(3)
    void testGetQuestionsByUserId() {
        ArrayList<Question> questions = questionRepository.getQuestionsByUserId(
                testQuestion.getUserId(), 1, 20, "", false, new LeetcodeTopicEnum[] {}, null, null);

        assertNotNull(questions, "Questions list should not be null");
        assertTrue(questions.size() > 0, "Questions list should contain at least one question");

        boolean foundTestQuestion = questions.stream().anyMatch(q -> q.getId().equals(testQuestion.getId()));
        assertTrue(foundTestQuestion, "Test question should be found in the user's questions");

        log.info("Successfully retrieved {} questions for user ID: {}", questions.size(), testQuestion.getUserId());
    }

    @Test
    @Order(4)
    void testUpdateQuestion() {
        Optional<String> originalCode = testQuestion.getCode();
        Optional<String> originalRuntime = testQuestion.getRuntime();

        testQuestion.setCode(Optional.of("def twoSum(self, nums, target): # updated test code"));
        testQuestion.setRuntime(Optional.of("2 ms"));

        Question updatedResult = questionRepository.updateQuestion(testQuestion);

        assertNotNull(updatedResult, "Question should be successfully updated");

        String questionId = testQuestion.getId();
        Question updatedQuestion =
                questionRepository.getQuestionById(questionId).orElseThrow();
        assertEquals(
                "def twoSum(self, nums, target): # updated test code",
                updatedQuestion.getCode().orElseThrow(),
                "Code should be updated");
        assertEquals("2 ms", updatedQuestion.getRuntime().orElseThrow(), "Runtime should be updated");

        log.info("Successfully updated question with ID: {}", testQuestion.getId());

        testQuestion.setCode(originalCode);
        testQuestion.setRuntime(originalRuntime);
    }

    @Test
    @Order(5)
    void testGetQuestionBySlugAndUserId() {
        Question foundQuestion = questionRepository
                .getQuestionBySlugAndUserId(testQuestion.getQuestionSlug(), testQuestion.getUserId())
                .orElseThrow();

        assertEquals(testQuestion.getId(), foundQuestion.getId(), "Question IDs should match");
        assertEquals(testQuestion.getQuestionSlug(), foundQuestion.getQuestionSlug(), "Question slugs should match");
        assertEquals(testQuestion.getUserId(), foundQuestion.getUserId(), "User IDs should match");

        log.info(
                "Successfully found question by slug '{}' and user ID '{}'",
                testQuestion.getQuestionSlug(),
                testQuestion.getUserId());
    }

    @Test
    @Order(6)
    void testGetQuestionCountByUserId() {
        int count = questionRepository.getQuestionCountByUserId(
                testQuestion.getUserId(), "", false, Collections.emptySet(), null, null);

        assertTrue(count > 0, "Question count should be greater than 0");

        log.info("Question count for user ID {}: {}", testQuestion.getUserId(), count);
    }

    @Test
    @Order(7)
    void testQuestionExistsBySubmissionId() {
        boolean exists = questionRepository.questionExistsBySubmissionId(
                testQuestion.getSubmissionId().orElseThrow());

        assertTrue(exists, "Question should exist with the given submission ID");

        boolean notExists = questionRepository.questionExistsBySubmissionId("non-existent-submission-id");
        assertFalse(notExists, "Question should not exist with non-existent submission ID");

        log.info("Successfully verified question existence by submission ID: {}", testQuestion.getSubmissionId());
    }

    @Test
    @Order(8)
    void testGetQuestionsWithNoTopics() {
        List<Question> questions = questionRepository.getAllQuestionsWithNoTopics();

        assertNotNull(questions);
        assertTrue(questions.size() > 0);
    }

    @Test
    @Order(9)
    void testGetAllIncompleteQuestionsWithUser() {
        ArrayList<QuestionWithUser> incompleteQuestions = questionRepository.getAllIncompleteQuestionsWithUser();

        assertNotNull(incompleteQuestions);
        assertTrue(incompleteQuestions.size() >= 0);
    }

    @Test
    @Order(10)
    void testGetQuestionsByUserIdWithStartDateFilter() {
        OffsetDateTime pastDate =
                StandardizedOffsetDateTime.normalize(OffsetDateTime.now().minusYears(1));
        ArrayList<Question> questions = questionRepository.getQuestionsByUserId(
                testQuestion.getUserId(), 1, 20, "", false, new LeetcodeTopicEnum[] {}, pastDate, null);

        assertNotNull(questions, "Questions list should not be null");
        boolean foundTestQuestion = questions.stream().anyMatch(q -> q.getId().equals(testQuestion.getId()));
        assertTrue(foundTestQuestion, "Test question should be found when startDate is in the past");

        OffsetDateTime futureDate =
                StandardizedOffsetDateTime.normalize(OffsetDateTime.now().plusYears(1));
        ArrayList<Question> emptyQuestions = questionRepository.getQuestionsByUserId(
                testQuestion.getUserId(), 1, 20, "", false, new LeetcodeTopicEnum[] {}, futureDate, null);

        assertNotNull(emptyQuestions, "Questions list should not be null");
        boolean foundTestQuestionInFuture =
                emptyQuestions.stream().anyMatch(q -> q.getId().equals(testQuestion.getId()));
        assertFalse(foundTestQuestionInFuture, "Test question should not be found when startDate is in the future");

        log.info("Successfully verified startDate filtering");
    }

    @Test
    @Order(11)
    void testGetQuestionsByUserIdWithEndDateFilter() {
        OffsetDateTime futureDate =
                StandardizedOffsetDateTime.normalize(OffsetDateTime.now().plusYears(1));
        ArrayList<Question> questions = questionRepository.getQuestionsByUserId(
                testQuestion.getUserId(), 1, 20, "", false, new LeetcodeTopicEnum[] {}, null, futureDate);

        assertNotNull(questions, "Questions list should not be null");
        boolean foundTestQuestion = questions.stream().anyMatch(q -> q.getId().equals(testQuestion.getId()));
        assertTrue(foundTestQuestion, "Test question should be found when endDate is in the future");

        OffsetDateTime pastDate =
                StandardizedOffsetDateTime.normalize(OffsetDateTime.now().minusYears(1));
        ArrayList<Question> emptyQuestions = questionRepository.getQuestionsByUserId(
                testQuestion.getUserId(), 1, 20, "", false, new LeetcodeTopicEnum[] {}, null, pastDate);

        assertNotNull(emptyQuestions, "Questions list should not be null");
        boolean foundTestQuestionInPast =
                emptyQuestions.stream().anyMatch(q -> q.getId().equals(testQuestion.getId()));
        assertFalse(foundTestQuestionInPast, "Test question should not be found when endDate is in the past");

        log.info("Successfully verified endDate filtering");
    }

    @Test
    @Order(12)
    void testGetQuestionsByUserIdWithDateRange() {
        OffsetDateTime pastDate =
                StandardizedOffsetDateTime.normalize(OffsetDateTime.now().minusYears(1));
        OffsetDateTime futureDate =
                StandardizedOffsetDateTime.normalize(OffsetDateTime.now().plusYears(1));
        ArrayList<Question> questions = questionRepository.getQuestionsByUserId(
                testQuestion.getUserId(), 1, 20, "", false, new LeetcodeTopicEnum[] {}, pastDate, futureDate);

        assertNotNull(questions, "Questions list should not be null");
        boolean foundTestQuestion = questions.stream().anyMatch(q -> q.getId().equals(testQuestion.getId()));
        assertTrue(foundTestQuestion, "Test question should be found within date range");

        for (Question q : questions) {
            assertNotNull(q.getCreatedAt(), "Question createdAt should not be null");
        }

        log.info("Successfully verified date range filtering with {} questions", questions.size());
    }

    @Test
    @Order(13)
    void testGetQuestionCountByUserIdWithDateFilters() {
        int totalCount = questionRepository.getQuestionCountByUserId(
                testQuestion.getUserId(), "", false, Collections.emptySet(), null, null);

        OffsetDateTime pastDate =
                StandardizedOffsetDateTime.normalize(OffsetDateTime.now().minusYears(1));
        int countWithPastStart = questionRepository.getQuestionCountByUserId(
                testQuestion.getUserId(), "", false, Collections.emptySet(), pastDate, null);

        assertEquals(totalCount, countWithPastStart, "Count with past startDate should equal total count");

        OffsetDateTime futureDate =
                StandardizedOffsetDateTime.normalize(OffsetDateTime.now().plusYears(1));
        int countWithFutureStart = questionRepository.getQuestionCountByUserId(
                testQuestion.getUserId(), "", false, Collections.emptySet(), futureDate, null);

        assertEquals(0, countWithFutureStart, "Count with future startDate should be 0");

        log.info(
                "Successfully verified date filtering on count: total={}, withPastStart={}, withFutureStart={}",
                totalCount,
                countWithPastStart,
                countWithFutureStart);
    }

    @Test
    @Order(14)
    void testGetAllIncompleteQuestionsWithNoJob() {
        List<Question> incompleteQuestions = questionRepository.getAllIncompleteQuestionsWithNoJob();

        assertNotNull(incompleteQuestions, "The list of incomplete questions should not be null");
        if (!incompleteQuestions.isEmpty()) {
            boolean foundQuestion = incompleteQuestions.stream().anyMatch(q -> q.getId() != null);
            assertTrue(foundQuestion, "Returned questions should have valid IDs");
        }

        log.info("Successfully retrieved {} incomplete questions with no associated job", incompleteQuestions.size());
    }
}
