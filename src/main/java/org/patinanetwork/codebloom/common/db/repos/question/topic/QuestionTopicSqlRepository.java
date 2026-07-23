package org.patinanetwork.codebloom.common.db.repos.question.topic;

import java.sql.Types;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.patinanetwork.codebloom.common.db.models.question.topic.LeetcodeTopicEnum;
import org.patinanetwork.codebloom.common.db.models.question.topic.QuestionTopic;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
public class QuestionTopicSqlRepository implements QuestionTopicRepository {

    private static final RowMapper<QuestionTopic> QUESTION_TOPIC_ROW_MAPPER = (rs, rowNum) -> QuestionTopic.builder()
            .id(rs.getString("id"))
            .createdAt(rs.getTimestamp("createdAt").toLocalDateTime())
            .questionId(rs.getString("questionId"))
            .questionBankId(rs.getString("questionBankId"))
            .topicSlug(rs.getString("topicSlug"))
            .topic(LeetcodeTopicEnum.fromValue(rs.getString("topic")))
            .build();

    private final JdbcClient jdbcClient;

    public QuestionTopicSqlRepository(final JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public List<QuestionTopic> findQuestionTopicsByQuestionId(final String questionId) {
        String sql = """
                SELECT
                    id,
                    "questionId",
                    "questionBankId",
                    "topicSlug",
                    "createdAt",
                    "topic"
                FROM
                    "QuestionTopic" qt
                WHERE
                    qt."questionId" = :questionId
                """;

        return jdbcClient
                .sql(sql)
                .param("questionId", UUID.fromString(questionId))
                .query(QUESTION_TOPIC_ROW_MAPPER)
                .list();
    }

    @Override
    public List<QuestionTopic> findQuestionTopicsByQuestionBankId(final String questionBankId) {
        String sql = """
                SELECT
                    id,
                    "questionId",
                    "questionBankId",
                    "topicSlug",
                    "createdAt",
                    "topic"
                FROM
                    "QuestionTopic" qt
                WHERE
                    qt."questionBankId" = :questionBankId
                """;

        return jdbcClient
                .sql(sql)
                .param("questionBankId", UUID.fromString(questionBankId))
                .query(QUESTION_TOPIC_ROW_MAPPER)
                .list();
    }

    @Override
    public Optional<QuestionTopic> findQuestionTopicById(final String id) {
        String sql = """
                SELECT
                    id,
                    "questionId",
                    "questionBankId",
                    "topicSlug",
                    "createdAt",
                    "topic"
                FROM
                    "QuestionTopic" qt
                WHERE
                    qt.id = :id
                """;

        return jdbcClient
                .sql(sql)
                .param("id", UUID.fromString(id))
                .query(QUESTION_TOPIC_ROW_MAPPER)
                .optional();
    }

    @Override
    public Optional<QuestionTopic> findQuestionTopicByQuestionIdAndTopicEnum(
            final String questionId, final LeetcodeTopicEnum topicEnum) {
        String sql = """
                SELECT
                    id,
                    "questionId",
                    "questionBankId",
                    "topicSlug",
                    "createdAt",
                    "topic"
                FROM
                    "QuestionTopic" qt
                WHERE
                    qt."questionId" = :questionId
                AND
                    qt.topic = :topic
                """;

        return jdbcClient
                .sql(sql)
                .param("questionId", UUID.fromString(questionId))
                .param("topic", topicEnum.getLeetcodeEnum(), Types.OTHER)
                .query(QUESTION_TOPIC_ROW_MAPPER)
                .optional();
    }

    @Override
    public void createQuestionTopic(final QuestionTopic questionTopic) {
        String sql = """
                INSERT INTO "QuestionTopic" ("id", "questionId", "questionBankId", "topicSlug", "topic")
                VALUES (:id, :questionId, :questionBankId, :topicSlug, :topic)
                RETURNING "createdAt"
                """;

        questionTopic.setId(UUID.randomUUID().toString());

        LocalDateTime createdAt = jdbcClient
                .sql(sql)
                .param("id", UUID.fromString(questionTopic.getId()))
                .param(
                        "questionId",
                        questionTopic.getQuestionId().map(UUID::fromString).orElse(null))
                .param(
                        "questionBankId",
                        questionTopic.getQuestionBankId().map(UUID::fromString).orElse(null))
                .param("topicSlug", questionTopic.getTopicSlug())
                .param("topic", questionTopic.getTopic().getLeetcodeEnum(), Types.OTHER)
                .query((rs, rowNum) -> rs.getTimestamp("createdAt").toLocalDateTime())
                .optional()
                .orElse(null);

        questionTopic.setCreatedAt(createdAt);
    }

    @Override
    public boolean updateQuestionTopicById(final QuestionTopic questionTopic) {
        String sql = """
                UPDATE "QuestionTopic"
                SET
                    "questionId" = :questionId,
                    "questionBankId" = :questionBankId,
                    "topicSlug" = :topicSlug,
                    "topic" = :topic
                WHERE id = :id
                """;

        int rowsAffected = jdbcClient
                .sql(sql)
                .param("id", UUID.fromString(questionTopic.getId()))
                .param(
                        "questionId",
                        questionTopic.getQuestionId().map(UUID::fromString).orElse(null))
                .param(
                        "questionBankId",
                        questionTopic.getQuestionBankId().map(UUID::fromString).orElse(null))
                .param("topicSlug", questionTopic.getTopicSlug())
                .param("topic", questionTopic.getTopic().getLeetcodeEnum(), Types.OTHER)
                .update();

        return rowsAffected > 0;
    }

    @Override
    public boolean deleteQuestionTopicById(final String id) {
        String sql = """
                DELETE FROM "QuestionTopic"
                WHERE id = :id
                """;

        int rowsAffected = jdbcClient.sql(sql).param("id", UUID.fromString(id)).update();

        return rowsAffected > 0;
    }
}
