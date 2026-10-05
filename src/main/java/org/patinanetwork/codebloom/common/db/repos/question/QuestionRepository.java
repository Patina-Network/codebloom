package org.patinanetwork.codebloom.common.db.repos.question;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.patinanetwork.codebloom.common.db.models.question.Question;
import org.patinanetwork.codebloom.common.db.models.question.QuestionWithUser;
import org.patinanetwork.codebloom.common.db.models.question.topic.LeetcodeTopicEnum;

public interface QuestionRepository {
    /**
     * @note - The provided object's methods will be overridden with any returned data from the database.
     * @param question - required fields:
     *     <ul>
     *       <li>userId
     *       <li>questionSlug
     *       <li>questionDifficulty
     *       <li>questionNumber
     *       <li>questionLink
     *       <li>questionTitle
     *       <li>description
     *       <li>pointsAwarded
     *       <li>acceptanceRate
     *       <li>submittedAt
     *     </ul>
     */
    Question createQuestion(Question question);

    Optional<Question> getQuestionById(String id);

    Optional<QuestionWithUser> getQuestionWithUserById(String id);

    ArrayList<Question> getQuestionsByUserId(
            String userId,
            int page,
            int pageSize,
            String query,
            boolean pointFilter,
            LeetcodeTopicEnum[] topics,
            OffsetDateTime startDate,
            OffsetDateTime endDate);

    /**
     * @note - The provided object's methods will be overridden with any returned data from the database.
     * @param question - overridden fields:
     *     <ul>
     *       <li>questionTitle
     *       <li>description
     *       <li>pointsAwarded
     *       <li>acceptanceRate
     *       <li>runtime
     *       <li>memory
     *       <li>code
     *       <li>language
     *       <li>submissionId
     *     </ul>
     *
     * @return updated question if sucessful
     */
    Question updateQuestion(Question question);

    /**
     * @note - Some fields may not be populated properly due to a service outage. So this method looks for any Questions
     *     missing fields populated by LeetCode's API.
     * @return all questions missing either a runtime, memory, code, language, or description
     */
    ArrayList<Question> getAllIncompleteQuestions();

    /**
     * @note - Finds all incomplete questions that don't currently have an attached {@code INCOMPLETE} or
     *     {@code PROCESSING} job attached.
     * @return questions missing a runtime, memory, code, language, or description and no attached job with
     *     {@code INCOMPLETE} or {@code PROCESSING}
     */
    ArrayList<Question> getAllIncompleteQuestionsWithNoJob();

    /**
     * @note - Special case that will do a reverse-lookup on `QuestionTopic` to return all `Question` rows that do not
     *     have any assigned topics yet.
     */
    List<Question> getAllQuestionsWithNoTopics();

    /**
     * @note - Returns all incomplete questions with user information, ordered by most recently submitted. Incomplete
     *     questions are those missing either a runtime, memory, code, or language.
     * @return all incomplete questions with user details, sorted by submittedAt DESC
     */
    ArrayList<QuestionWithUser> getAllIncompleteQuestionsWithUser();

    boolean deleteQuestionById(String id);

    Optional<Question> getQuestionBySlugAndUserId(String slug, String userId);

    boolean questionExistsBySubmissionId(String submissionId);

    int getQuestionCountByUserId(
            String userId,
            String query,
            boolean filterPoints,
            Set<String> topics,
            OffsetDateTime startDate,
            OffsetDateTime endDate);
}
