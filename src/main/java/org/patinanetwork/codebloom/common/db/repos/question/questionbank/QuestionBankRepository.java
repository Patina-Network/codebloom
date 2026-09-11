package org.patinanetwork.codebloom.common.db.repos.question.questionbank;

import java.util.List;
import java.util.Optional;
import org.patinanetwork.codebloom.common.db.models.question.QuestionDifficulty;
import org.patinanetwork.codebloom.common.db.models.question.bank.QuestionBank;
import org.patinanetwork.codebloom.common.db.models.question.topic.LeetcodeTopicEnum;

public interface QuestionBankRepository {
    void createQuestion(QuestionBank question);

    void createQuestionWithTopics(QuestionBank question);

    Optional<QuestionBank> getQuestionById(String id);

    Optional<QuestionBank> getQuestionBySlug(String slug);

    /**
     * @note - The provided object's methods will be overridden with any returned data from the database.
     * @param inputQuestion - overridable fields:
     *     <ul>
     *       <li>questionSlug
     *       <li>questionDifficulty
     *       <li>questionNumber
     *       <li>questionLink
     *       <li>questionTitle
     *       <li>description
     *       <li>acceptanceRate
     *     </ul>
     */
    boolean updateQuestion(QuestionBank inputQuestion);

    boolean deleteQuestionById(String id);

    Optional<QuestionBank> getRandomQuestion();

    List<QuestionBank> getQuestionsByTopic(LeetcodeTopicEnum topic);

    List<QuestionBank> getQuestionsByDifficulty(QuestionDifficulty difficulty);

    List<QuestionBank> getAllQuestions();
}
