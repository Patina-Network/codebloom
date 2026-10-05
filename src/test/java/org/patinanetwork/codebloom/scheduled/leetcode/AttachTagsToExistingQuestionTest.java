package org.patinanetwork.codebloom.scheduled.leetcode;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.patinanetwork.codebloom.common.db.models.question.Question;
import org.patinanetwork.codebloom.common.db.repos.question.QuestionRepository;
import org.patinanetwork.codebloom.common.db.repos.question.topic.QuestionTopicRepository;
import org.patinanetwork.codebloom.common.leetcode.LeetcodeClientException;
import org.patinanetwork.codebloom.common.leetcode.throttled.ThrottledLeetcodeClient;
import org.patinanetwork.codebloom.common.time.StandardizedLocalDateTime;
import org.slf4j.LoggerFactory;

public class AttachTagsToExistingQuestionTest {
    private final QuestionRepository questionRepository = mock(QuestionRepository.class);
    private final QuestionTopicRepository questionTopicRepository = mock(QuestionTopicRepository.class);
    private final ThrottledLeetcodeClient leetcodeClient = mock(ThrottledLeetcodeClient.class);

    private final AttachTagsToExistingQuestion attachTagsToExistingQuestion;

    private ListAppender<ILoggingEvent> logWatcher;

    public AttachTagsToExistingQuestionTest() {
        attachTagsToExistingQuestion =
                new AttachTagsToExistingQuestion(questionRepository, questionTopicRepository, leetcodeClient);
    }

    @BeforeEach
    void setUp() {
        logWatcher = new ListAppender<>();
        logWatcher.start();
        ((Logger) LoggerFactory.getLogger(attachTagsToExistingQuestion.getClass())).addAppender(logWatcher);
    }

    @AfterEach
    void teardown() {
        ((Logger) LoggerFactory.getLogger(attachTagsToExistingQuestion.getClass())).detachAndStopAllAppenders();
    }

    @Test
    void testAttachTagsToExistingQuestionsLogsExceptions() {
        Question mockQuestion = Question.builder()
                .id(UUID.randomUUID().toString())
                .acceptanceRate(37.2f)
                .code(Optional.of("""
        function hello() {
            return "hello world";
        }
        """))
                .createdAt(StandardizedLocalDateTime.now())
                .description(Optional.of("Hello"))
                .questionSlug("123-hello")
                .build();

        when(questionRepository.getAllQuestionsWithNoTopics()).thenReturn(List.of(mockQuestion));

        when(leetcodeClient.findQuestionBySlug(eq(mockQuestion.getQuestionSlug())))
                .thenThrow(new RuntimeException("Expected!"));

        attachTagsToExistingQuestion.attachTagsToExistingQuestions();
        verifyNoInteractions(questionTopicRepository);
        assertTrue(logWatcher.list.stream()
                .anyMatch(log -> log.getLevel().equals(Level.ERROR)
                        && log.getFormattedMessage().contains("LeetcodeClient threw an exception")
                        && log.getFormattedMessage().contains(mockQuestion.getId())
                        && log.getFormattedMessage().contains(mockQuestion.getQuestionSlug())));
    }

    @Test
    void notFoundQuestionIsSkippedAndOtherQuestionsContinue() {
        var missing = Question.builder().id("missing").questionSlug("old-slug").build();
        var valid = Question.builder().id("valid").questionSlug("valid-slug").build();
        when(questionRepository.getAllQuestionsWithNoTopics()).thenReturn(List.of(missing, valid));
        when(leetcodeClient.findQuestionBySlug("old-slug"))
                .thenThrow(new LeetcodeClientException("Question not found", true));
        when(leetcodeClient.findQuestionBySlug("valid-slug"))
                .thenReturn(org.patinanetwork.codebloom.common.leetcode.models.LeetcodeQuestion.builder()
                        .topics(List.of())
                        .build());

        attachTagsToExistingQuestion.attachTagsToExistingQuestions();
        attachTagsToExistingQuestion.attachTagsToExistingQuestions();

        verify(leetcodeClient, times(2)).findQuestionBySlug("old-slug");
        verify(leetcodeClient, times(2)).findQuestionBySlug("valid-slug");
        verifyNoInteractions(questionTopicRepository);
        assertTrue(logWatcher.list.stream()
                .anyMatch(log -> log.getLevel().equals(Level.INFO)
                        && log.getFormattedMessage().contains("Skipping topic lookup for question id missing")));
        assertFalse(logWatcher.list.stream().anyMatch(log -> log.getLevel().equals(Level.ERROR)));
    }

    @Test
    void attachesTopicsAndContinuesPastFailedQuestion() {
        var failed = Question.builder().id("failed").questionSlug("old-slug").build();
        var valid = Question.builder()
                .id("valid")
                .questionSlug("classes-with-at-least-5-students")
                .build();
        when(questionRepository.getAllQuestionsWithNoTopics()).thenReturn(List.of(failed, valid));
        when(leetcodeClient.findQuestionBySlug("old-slug")).thenThrow(new RuntimeException("Missing"));
        when(leetcodeClient.findQuestionBySlug(valid.getQuestionSlug()))
                .thenReturn(org.patinanetwork.codebloom.common.leetcode.models.LeetcodeQuestion.builder()
                        .topics(List.of(org.patinanetwork.codebloom.common.leetcode.models.LeetcodeTopicTag.builder()
                                .name("Database")
                                .slug("database")
                                .build()))
                        .build());

        attachTagsToExistingQuestion.attachTagsToExistingQuestions();

        verify(questionTopicRepository)
                .createQuestionTopic(
                        argThat(topic -> topic.getQuestionId().orElseThrow().equals("valid")
                                && topic.getTopicSlug().equals("database")));
        assertTrue(logWatcher.list.stream().anyMatch(log -> log.getFormattedMessage()
                .contains("Attached 1 topics to question id valid and slug classes-with-at-least-5-students")));
    }
}
