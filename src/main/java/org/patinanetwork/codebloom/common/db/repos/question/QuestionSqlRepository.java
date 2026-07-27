package org.patinanetwork.codebloom.common.db.repos.question;

import java.sql.Array;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import javax.sql.DataSource;
import org.patinanetwork.codebloom.common.db.models.question.Question;
import org.patinanetwork.codebloom.common.db.models.question.QuestionDifficulty;
import org.patinanetwork.codebloom.common.db.models.question.QuestionWithUser;
import org.patinanetwork.codebloom.common.db.models.question.topic.LeetcodeTopicEnum;
import org.patinanetwork.codebloom.common.db.repos.question.topic.QuestionTopicRepository;
import org.patinanetwork.codebloom.common.db.repos.question.topic.service.QuestionTopicService;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
public class QuestionSqlRepository implements QuestionRepository {

    private final DataSource ds;
    private final JdbcClient jdbcClient;
    private final QuestionTopicRepository questionTopicRepository;
    private final QuestionTopicService questionTopicService;
    private final RowMapper<Question> questionRowMapper;
    private final RowMapper<QuestionWithUser> questionWithUserRowMapper;

    public QuestionSqlRepository(
            final DataSource ds,
            final JdbcClient jdbcClient,
            final QuestionTopicRepository questionTopicRepository,
            final QuestionTopicService questionTopicService) {
        this.ds = ds;
        this.jdbcClient = jdbcClient;
        this.questionTopicRepository = questionTopicRepository;
        this.questionTopicService = questionTopicService;
        this.questionRowMapper = (rs, rowNum) -> {
            var questionId = rs.getString("id");
            var userId = rs.getString("userId");
            var questionSlug = rs.getString("questionSlug");
            var questionDifficulty = QuestionDifficulty.valueOf(rs.getString("questionDifficulty"));
            var questionNumber = rs.getInt("questionNumber");
            var questionLink = rs.getString("questionLink");
            int points = rs.getInt("pointsAwarded");
            Optional<Integer> pointsAwarded = rs.wasNull() ? Optional.empty() : Optional.of(points);
            var questionTitle = rs.getString("questionTitle");
            var acceptanceRate = rs.getFloat("acceptanceRate");
            var createdAt = rs.getTimestamp("createdAt").toLocalDateTime();
            var submittedAt = rs.getTimestamp("submittedAt").toLocalDateTime();

            return Question.builder()
                    .id(questionId)
                    .userId(userId)
                    .questionSlug(questionSlug)
                    .questionDifficulty(questionDifficulty)
                    .questionNumber(questionNumber)
                    .questionLink(questionLink)
                    .pointsAwarded(pointsAwarded)
                    .questionTitle(questionTitle)
                    .description(Optional.ofNullable(rs.getString("description")))
                    .acceptanceRate(acceptanceRate)
                    .createdAt(createdAt)
                    .submittedAt(submittedAt)
                    .runtime(Optional.ofNullable(rs.getString("runtime")))
                    .memory(Optional.ofNullable(rs.getString("memory")))
                    .code(Optional.ofNullable(rs.getString("code")))
                    .language(Optional.ofNullable(rs.getString("language")))
                    .submissionId(Optional.ofNullable(rs.getString("submissionId")))
                    .topics(questionTopicRepository.findQuestionTopicsByQuestionId(questionId))
                    .build();
        };
        this.questionWithUserRowMapper = (rs, rowNum) -> {
            var questionId = rs.getString("id");
            var userId = rs.getString("userId");
            var questionSlug = rs.getString("questionSlug");
            var questionDifficulty = QuestionDifficulty.valueOf(rs.getString("questionDifficulty"));
            var questionNumber = rs.getInt("questionNumber");
            var questionLink = rs.getString("questionLink");
            int points = rs.getInt("pointsAwarded");
            Optional<Integer> pointsAwarded = rs.wasNull() ? Optional.empty() : Optional.of(points);
            var questionTitle = rs.getString("questionTitle");
            var acceptanceRate = rs.getFloat("acceptanceRate");
            var createdAt = rs.getTimestamp("createdAt").toLocalDateTime();
            var submittedAt = rs.getTimestamp("submittedAt").toLocalDateTime();

            return QuestionWithUser.builder()
                    .id(questionId)
                    .userId(userId)
                    .questionSlug(questionSlug)
                    .questionDifficulty(questionDifficulty)
                    .questionNumber(questionNumber)
                    .questionLink(questionLink)
                    .pointsAwarded(pointsAwarded)
                    .questionTitle(questionTitle)
                    .description(Optional.ofNullable(rs.getString("description")))
                    .acceptanceRate(acceptanceRate)
                    .createdAt(createdAt)
                    .submittedAt(submittedAt)
                    .runtime(Optional.ofNullable(rs.getString("runtime")))
                    .memory(Optional.ofNullable(rs.getString("memory")))
                    .code(Optional.ofNullable(rs.getString("code")))
                    .language(Optional.ofNullable(rs.getString("language")))
                    .submissionId(Optional.ofNullable(rs.getString("submissionId")))
                    .discordName(Optional.ofNullable(rs.getString("discordName")))
                    .leetcodeUsername(Optional.ofNullable(rs.getString("leetcodeUsername")))
                    .nickname(Optional.ofNullable(rs.getString("nickname")))
                    .topics(questionTopicRepository.findQuestionTopicsByQuestionId(questionId))
                    .build();
        };
    }

    @Override
    public Question createQuestion(final Question question) {
        String sql = """
                INSERT INTO "Question" (
                    id,
                    "userId",
                    "questionSlug",
                    "questionDifficulty",
                    "questionNumber",
                    "questionLink",
                    "pointsAwarded",
                    "questionTitle",
                    description,
                    "acceptanceRate",
                    "submittedAt",
                    runtime,
                    memory,
                    code,
                    language,
                    "submissionId"
                )
                VALUES
                    (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        question.setId(UUID.randomUUID().toString());

        int rowsAffected = jdbcClient
                .sql(sql)
                .param(1, UUID.fromString(question.getId()))
                .param(2, UUID.fromString(question.getUserId()))
                .param(3, question.getQuestionSlug())
                .param(4, question.getQuestionDifficulty().name(), Types.OTHER)
                .param(5, question.getQuestionNumber())
                .param(6, question.getQuestionLink())
                .param(7, question.getPointsAwarded().orElse(null), Types.INTEGER)
                .param(8, question.getQuestionTitle())
                .param(9, question.getDescription().orElse(null))
                .param(10, question.getAcceptanceRate())
                .param(11, question.getSubmittedAt())
                .param(12, question.getRuntime().orElse(null))
                .param(13, question.getMemory().orElse(null))
                .param(14, question.getCode().orElse(null))
                .param(15, question.getLanguage().orElse(null))
                .param(16, question.getSubmissionId().orElse(null))
                .update();

        if (rowsAffected > 0) {
            return getQuestionById(question.getId())
                    .orElseThrow(() -> new RuntimeException("Failed to retrieve created question."));
        } else {
            throw new RuntimeException("Failed to create question.");
        }
    }

    @Override
    public Optional<Question> getQuestionById(final String id) {
        String sql = """
                SELECT
                    id,
                    "userId",
                    "questionSlug",
                    "questionDifficulty",
                    "questionNumber",
                    "questionLink",
                    "pointsAwarded",
                    "questionTitle",
                    description,
                    "acceptanceRate",
                    "createdAt",
                    "submittedAt",
                    runtime,
                    memory,
                    code,
                    language,
                    "submissionId"
                FROM
                    "Question"
                WHERE
                    id = ?
            """;

        return jdbcClient
                .sql(sql)
                .param(UUID.fromString(id))
                .query(questionRowMapper)
                .optional();
    }

    @Override
    public Optional<QuestionWithUser> getQuestionWithUserById(final String id) {
        String sql = """
                SELECT
                    q.id,
                    q."userId",
                    q."questionSlug",
                    q."questionDifficulty",
                    q."questionNumber",
                    q."questionLink",
                    q."pointsAwarded",
                    q."questionTitle",
                    q.description,
                    q."acceptanceRate",
                    q."createdAt",
                    q."submittedAt",
                    q.runtime,
                    q.memory,
                    q.code,
                    q.language,
                    q."submissionId",
                    u."discordName",
                    u."leetcodeUsername",
                    u.nickname
                FROM "Question" q
                JOIN "User" u ON q."userId" = u.id
                WHERE q.id = ?
            """;

        return jdbcClient
                .sql(sql)
                .param(UUID.fromString(id))
                .query(questionWithUserRowMapper)
                .optional();
    }

    @Override
    public ArrayList<Question> getQuestionsByUserId(
            final String userId,
            final int page,
            final int pageSize,
            final String query,
            final boolean pointFilter,
            final LeetcodeTopicEnum[] topics,
            final OffsetDateTime startDate,
            final OffsetDateTime endDate) {
        String sql = """
            SELECT *
            FROM (
                SELECT DISTINCT ON (q.id)
                    q.id,
                    q."userId",
                    q."questionSlug",
                    q."questionDifficulty",
                    q."questionNumber",
                    q."questionLink",
                    q."pointsAwarded",
                    q."questionTitle",
                    q.description,
                    q."acceptanceRate",
                    q."createdAt",
                    q."submittedAt",
                    q.runtime,
                    q.memory,
                    q.code,
                    q.language,
                    q."submissionId"
                FROM
                    "Question" q
                JOIN "User" u ON q."userId" = u.id
                LEFT JOIN "QuestionTopic" t ON t."questionId" = q."id"
                WHERE
                    q."userId" = :userId
                    AND q."questionTitle" ILIKE :query
                    AND (NOT :pointFilter OR q."pointsAwarded" <> 0)
                    AND (
                        :topics = '{}'::"LeetcodeTopicEnum"[]
                        OR t."topic" = ANY(:topics)
                    )
                    AND (cast(:startDate AS timestamptz) IS NULL OR q."createdAt" >= :startDate)
                    AND (cast(:endDate AS timestamptz) IS NULL OR q."createdAt" <= :endDate)
                ORDER BY q.id, q."submittedAt" DESC
            ) sub
            ORDER BY "submittedAt" DESC
            LIMIT :pageSize OFFSET :offset;
            """;

        String[] sqlValues =
                Arrays.stream(topics).map(LeetcodeTopicEnum::getLeetcodeEnum).toArray(String[]::new);

        try (Connection conn = ds.getConnection()) {
            Array topicsArray = conn.createArrayOf("\"LeetcodeTopicEnum\"", sqlValues);
            List<Question> questions = jdbcClient
                    .sql(sql)
                    .param("userId", UUID.fromString(userId))
                    .param("query", "%" + query + "%")
                    .param("pointFilter", pointFilter)
                    .param("topics", topicsArray)
                    .param("startDate", startDate, Types.TIMESTAMP_WITH_TIMEZONE)
                    .param("endDate", endDate, Types.TIMESTAMP_WITH_TIMEZONE)
                    .param("pageSize", pageSize)
                    .param("offset", (page - 1) * pageSize)
                    .query(questionRowMapper)
                    .list();
            return new ArrayList<>(questions);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create SQL array", e);
        }
    }

    @Override
    public Question updateQuestion(final Question inputQuestion) {
        String sql = """
                UPDATE "Question"
                SET
                    "userId" = ?,
                    "questionSlug" = ?,
                    "questionDifficulty" = ?,
                    "questionNumber" = ?,
                    "questionLink" = ?,
                    "pointsAwarded" = ?,
                    "questionTitle" = ?,
                    description = ?,
                    "acceptanceRate" = ?,
                    "submittedAt" = ?,
                    runtime = ?,
                    memory = ?,
                    code = ?,
                    language = ?,
                    "submissionId" = ?
                WHERE
                    id = ?
            """;

        jdbcClient
                .sql(sql)
                .param(1, UUID.fromString(inputQuestion.getUserId()))
                .param(2, inputQuestion.getQuestionSlug())
                .param(3, inputQuestion.getQuestionDifficulty().name(), Types.OTHER)
                .param(4, inputQuestion.getQuestionNumber())
                .param(5, inputQuestion.getQuestionLink())
                .param(6, inputQuestion.getPointsAwarded().orElse(null), Types.INTEGER)
                .param(7, inputQuestion.getQuestionTitle())
                .param(8, inputQuestion.getDescription().orElse(null))
                .param(9, inputQuestion.getAcceptanceRate())
                .param(10, inputQuestion.getSubmittedAt())
                .param(11, inputQuestion.getRuntime().orElse(null))
                .param(12, inputQuestion.getMemory().orElse(null))
                .param(13, inputQuestion.getCode().orElse(null))
                .param(14, inputQuestion.getLanguage().orElse(null))
                .param(15, inputQuestion.getSubmissionId().orElse(null))
                .param(16, UUID.fromString(inputQuestion.getId()))
                .update();

        return getQuestionById(inputQuestion.getId())
                .orElseThrow(() -> new RuntimeException("Failed to retrieve updated question."));
    }

    @Override
    public boolean deleteQuestionById(final String id) {
        String sql = "DELETE FROM \"Question\" WHERE id=?";

        int rowsAffected = jdbcClient.sql(sql).param(UUID.fromString(id)).update();

        return rowsAffected > 0;
    }

    @Override
    public Optional<Question> getQuestionBySlugAndUserId(final String slug, final String inputtedUserId) {
        String sql = """
                SELECT
                    id,
                    "userId",
                    "questionSlug",
                    "questionDifficulty",
                    "questionNumber",
                    "questionLink",
                    "pointsAwarded",
                    "questionTitle",
                    description,
                    "acceptanceRate",
                    "createdAt",
                    "submittedAt",
                    runtime,
                    memory,
                    code,
                    language,
                    "submissionId"
                FROM
                    "Question"
                WHERE
                    "questionSlug" = ?
                AND
                    "userId" = ?
                LIMIT 1
            """;

        return jdbcClient
                .sql(sql)
                .param(slug)
                .param(UUID.fromString(inputtedUserId))
                .query(questionRowMapper)
                .optional();
    }

    @Override
    public int getQuestionCountByUserId(
            final String userId,
            final String query,
            final boolean pointFilter,
            final Set<String> topics,
            final OffsetDateTime startDate,
            final OffsetDateTime endDate) {
        String sql = """
            SELECT
                COUNT(DISTINCT q.id)
            FROM
                "Question" q
            LEFT JOIN "QuestionTopic" qt ON qt."questionId" = q.id
            WHERE
                q."userId" = :userId
            AND
                q."questionTitle" ILIKE :title
            AND
                (NOT :pointFilter OR q."pointsAwarded" <> 0)
            AND (
                :topics = '{}'::"LeetcodeTopicEnum"[]
                OR qt."topic" = ANY(:topics)
            )
            AND (cast(:startDate AS timestamptz) IS NULL OR q."createdAt" >= :startDate)
            AND (cast(:endDate AS timestamptz) IS NULL OR q."createdAt" <= :endDate)
            """;

        LeetcodeTopicEnum[] topicEnums = questionTopicService.stringsToEnums(topics);
        String[] sqlValues = Arrays.stream(topicEnums)
                .map(LeetcodeTopicEnum::getLeetcodeEnum)
                .toArray(String[]::new);

        try (Connection conn = ds.getConnection()) {
            Array topicsArray = conn.createArrayOf("\"LeetcodeTopicEnum\"", sqlValues);
            return jdbcClient
                    .sql(sql)
                    .param("userId", UUID.fromString(userId))
                    .param("title", "%" + query + "%")
                    .param("pointFilter", pointFilter)
                    .param("topics", topicsArray)
                    .param("startDate", startDate, Types.TIMESTAMP_WITH_TIMEZONE)
                    .param("endDate", endDate, Types.TIMESTAMP_WITH_TIMEZONE)
                    .query((rs, rowNum) -> rs.getInt(1))
                    .optional()
                    .orElse(0);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create SQL array", e);
        }
    }

    @Override
    public boolean questionExistsBySubmissionId(final String submissionId) {
        String sql = """
                SELECT
                    id
                FROM
                    "Question"
                WHERE
                    "submissionId" = ?
            """;

        return jdbcClient
                .sql(sql)
                .param(submissionId)
                .query((rs, rowNum) -> rs.getString("id"))
                .optional()
                .isPresent();
    }

    @Override
    public ArrayList<Question> getAllIncompleteQuestions() {
        String sql = """
            SELECT
                *
            FROM
                "Question"
            WHERE
                ("runtime" IS NULL OR "runtime" = '')
                OR ("memory" IS NULL OR "memory" = '')
                OR ("code" is NULL OR "code" = '')
                OR ("language" is NULL OR "language" = '')
            """;

        List<Question> questions = jdbcClient.sql(sql).query(questionRowMapper).list();

        return new ArrayList<>(questions);
    }

    @Override
    public List<Question> getAllQuestionsWithNoTopics() {
        String sql = """
            SELECT
                q.id,
                q."userId",
                q."questionSlug",
                q."questionDifficulty",
                q."questionNumber",
                q."questionLink",
                q."pointsAwarded",
                q."questionTitle",
                q.description,
                q."acceptanceRate",
                q."createdAt",
                q."submittedAt",
                q.runtime,
                q.memory,
                q.code,
                q.language,
                q."submissionId"
            FROM
                "Question" q
            WHERE NOT EXISTS (
                SELECT 1
                FROM "QuestionTopic" qt
                WHERE qt."questionId" = q.id
            );
            """;

        return jdbcClient.sql(sql).query(questionRowMapper).list();
    }

    @Override
    public ArrayList<QuestionWithUser> getAllIncompleteQuestionsWithUser() {
        String sql = """
            SELECT
                q.id,
                q."userId",
                q."questionSlug",
                q."questionDifficulty",
                q."questionNumber",
                q."questionLink",
                q."pointsAwarded",
                q."questionTitle",
                q.description,
                q."acceptanceRate",
                q."createdAt",
                q."submittedAt",
                q.runtime,
                q.memory,
                q.code,
                q.language,
                q."submissionId",
                u."discordName",
                u."leetcodeUsername",
                u.nickname
            FROM
                "Question" q
            JOIN
                "User" u ON q."userId" = u.id
            WHERE
                (q."runtime" IS NULL OR q."runtime" = '')
                OR (q."memory" IS NULL OR q."memory" = '')
                OR (q."code" IS NULL OR q."code" = '')
                OR (q."language" IS NULL OR q."language" = '')
            ORDER BY
                q."submittedAt" DESC
            """;

        List<QuestionWithUser> questions =
                jdbcClient.sql(sql).query(questionWithUserRowMapper).list();

        return new ArrayList<>(questions);
    }

    @Override
    public ArrayList<Question> getAllIncompleteQuestionsWithNoJob() {
        String sql = """
        SELECT
            q.*
        FROM
            "Question" q
        WHERE (
            (q."runtime" IS NULL OR q."runtime" = '')
            OR (q."memory" IS NULL OR q."memory" = '')
            OR (q."code" IS NULL OR q."code" = '')
            OR (q."language" IS NULL OR q."language" = '')
        )
        AND NOT EXISTS (
            SELECT 1
            FROM "Job" j
            WHERE j."questionId" = q.id::text
            AND j.status IN ('INCOMPLETE', 'PROCESSING')
        )
        """;

        List<Question> questions = jdbcClient.sql(sql).query(questionRowMapper).list();

        return new ArrayList<>(questions);
    }
}
