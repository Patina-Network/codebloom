package org.patinanetwork.codebloom.scheduled.leetcode;

import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.patinanetwork.codebloom.common.db.models.question.Question;
import org.patinanetwork.codebloom.common.db.models.question.topic.LeetcodeTopicEnum;
import org.patinanetwork.codebloom.common.db.models.question.topic.QuestionTopic;
import org.patinanetwork.codebloom.common.db.repos.question.QuestionRepository;
import org.patinanetwork.codebloom.common.db.repos.question.topic.QuestionTopicRepository;
import org.patinanetwork.codebloom.common.leetcode.LeetcodeClient;
import org.patinanetwork.codebloom.common.leetcode.models.LeetcodeQuestion;
import org.patinanetwork.codebloom.common.leetcode.throttled.ThrottledLeetcodeClient;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Profile("!ci")
@Slf4j
public class AttachTagsToExistingQuestion {

    private final QuestionRepository questionRepository;
    private final QuestionTopicRepository questionTopicRepository;
    private final LeetcodeClient leetcodeClient;

    public AttachTagsToExistingQuestion(
            final QuestionRepository questionRepository,
            final QuestionTopicRepository questionTopicRepository,
            final ThrottledLeetcodeClient throttledLeetcodeClient) {
        this.questionRepository = questionRepository;
        this.questionTopicRepository = questionTopicRepository;
        this.leetcodeClient = throttledLeetcodeClient;
    }

    @Scheduled(initialDelay = 15, fixedDelay = 120, timeUnit = TimeUnit.MINUTES)
    void attachTagsToExistingQuestions() {
        log.info("Attempting to attach tags to existing questions that are missing any...");
        List<Question> questions = questionRepository.getAllQuestionsWithNoTopics();

        if (questions.size() == 0) {
            log.info("No questions with topics. This task is complete.");
            return;
        }

        for (var question : questions) {
            log.info("Updating question with id of {}", question.getId());
            LeetcodeQuestion leetcodeQuestion;
            try {
                leetcodeQuestion = leetcodeClient.findQuestionBySlug(question.getQuestionSlug());
            } catch (Exception e) {
                log.error("LeetcodeClient threw an exception", e);
                continue;
            }

            for (var leetcodeTopicTag : leetcodeQuestion.getTopics()) {
                String leetcodeTopicTagSlug = leetcodeTopicTag.getSlug();

                var newQuestionTopic = QuestionTopic.builder()
                        .questionId(question.getId())
                        .topicSlug(leetcodeTopicTagSlug)
                        .topic(LeetcodeTopicEnum.fromValue(leetcodeTopicTagSlug))
                        .build();

                questionTopicRepository.createQuestionTopic(newQuestionTopic);
            }
        }

        log.info("This task is complete.");
    }
}
