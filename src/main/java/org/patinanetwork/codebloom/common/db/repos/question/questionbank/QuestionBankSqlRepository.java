package org.patinanetwork.codebloom.common.db.repos.question.questionbank;

import java.sql.Types;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.patinanetwork.codebloom.common.db.models.question.QuestionDifficulty;
import org.patinanetwork.codebloom.common.db.models.question.bank.QuestionBank;
import org.patinanetwork.codebloom.common.db.models.question.topic.LeetcodeTopicEnum;
import org.patinanetwork.codebloom.common.db.repos.question.topic.QuestionTopicRepository;
import org.patinanetwork.codebloom.common.time.StandardizedOffsetDateTime;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
public class QuestionBankSqlRepository implements QuestionBankRepository {

    private final JdbcClient jdbcClient;
    private final QuestionTopicRepository questionTopicRepository;
    private final RowMapper<QuestionBank> questionBankRowMapper;

    public QuestionBankSqlRepository(
            final JdbcClient jdbcClient, final QuestionTopicRepository questionTopicRepository) {
        this.jdbcClient = jdbcClient;
        this.questionTopicRepository = questionTopicRepository;
        this.questionBankRowMapper = (rs, rowNum) -> {
            var questionBankId = rs.getString("id");
            var questionSlug = rs.getString("questionSlug");
            var questionDifficulty = QuestionDifficulty.valueOf(rs.getString("questionDifficulty"));
            var questionNumber = rs.getInt("questionNumber");
            var questionLink = rs.getString("questionLink");
            var questionTitle = rs.getString("questionTitle");
            var description = rs.getString("description");
            var acceptanceRate = rs.getFloat("acceptanceRate");
            var createdAt = StandardizedOffsetDateTime.normalize(rs.getObject("createdAt", OffsetDateTime.class));

            return QuestionBank.builder()
                    .id(questionBankId)
                    .questionSlug(questionSlug)
                    .questionDifficulty(questionDifficulty)
                    .questionNumber(questionNumber)
                    .questionLink(questionLink)
                    .questionTitle(questionTitle)
                    .description(Optional.ofNullable(description))
                    .acceptanceRate(acceptanceRate)
                    .createdAt(createdAt)
                    .topics(this.questionTopicRepository.findQuestionTopicsByQuestionBankId(questionBankId))
                    .build();
        };
    }

    @Override
    public void createQuestion(final QuestionBank question) {
        String sql = """
                INSERT INTO "QuestionBank" (
                    id,
                    "questionSlug",
                    "questionDifficulty",
                    "questionNumber",
                    "questionLink",
                    "questionTitle",
                    description,
                    "acceptanceRate"
                )
                VALUES
                    (:id, :slug, :difficulty, :number, :link, :title, :desc, :ac)
            """;

        question.setId(UUID.randomUUID().toString());

        jdbcClient
                .sql(sql)
                .param("id", UUID.fromString(question.getId()))
                .param("slug", question.getQuestionSlug())
                .param("difficulty", question.getQuestionDifficulty().name(), Types.OTHER)
                .param("number", question.getQuestionNumber())
                .param("link", question.getQuestionLink())
                .param("title", question.getQuestionTitle())
                .param("desc", question.getDescription().orElse(null))
                .param("ac", question.getAcceptanceRate())
                .update();
    }

    @Override
    public Optional<QuestionBank> getQuestionById(final String id) {
        String sql = """
                SELECT
                    id,
                    "questionSlug",
                    "questionDifficulty",
                    "questionNumber",
                    "questionLink",
                    "questionTitle",
                    description,
                    "acceptanceRate",
                    "createdAt"
                FROM
                    "QuestionBank"
                WHERE
                    id = :id
            """;

        return jdbcClient
                .sql(sql)
                .param("id", UUID.fromString(id))
                .query(questionBankRowMapper)
                .optional();
    }

    @Override
    public Optional<QuestionBank> getQuestionBySlug(final String slug) {
        String sql = """
                SELECT
                    id,
                    "questionSlug",
                    "questionDifficulty",
                    "questionNumber",
                    "questionLink",
                    "questionTitle",
                    description,
                    "acceptanceRate",
                    "createdAt"
                FROM
                    "QuestionBank"
                WHERE
                    "questionSlug" = :questionSlug
                LIMIT 1
            """;

        return jdbcClient
                .sql(sql)
                .param("questionSlug", slug)
                .query(questionBankRowMapper)
                .optional();
    }

    @Override
    public boolean updateQuestion(final QuestionBank inputQuestion) {
        String sql = """
                UPDATE "QuestionBank"
                SET
                    "questionSlug" = :slug,
                    "questionDifficulty" = :difficulty,
                    "questionNumber" = :number,
                    "questionLink" = :link,
                    "questionTitle" = :title,
                    description = :desc,
                    "acceptanceRate" = :ac
                WHERE
                    id = :id
            """;

        int rowsAffected = jdbcClient
                .sql(sql)
                .param("slug", inputQuestion.getQuestionSlug())
                .param("difficulty", inputQuestion.getQuestionDifficulty().name(), Types.OTHER)
                .param("number", inputQuestion.getQuestionNumber())
                .param("link", inputQuestion.getQuestionLink())
                .param("title", inputQuestion.getQuestionTitle())
                .param("desc", inputQuestion.getDescription().orElse(null))
                .param("ac", inputQuestion.getAcceptanceRate())
                .param("id", UUID.fromString(inputQuestion.getId()))
                .update();

        return rowsAffected > 0;
    }

    @Override
    public boolean deleteQuestionById(final String id) {
        String sql = "DELETE FROM \"QuestionBank\" WHERE id=:id";

        int rowsAffected = jdbcClient.sql(sql).param("id", UUID.fromString(id)).update();

        return rowsAffected > 0;
    }

    @Override
    public Optional<QuestionBank> getRandomQuestion() {
        String sql = """
                SELECT
                    id,
                    "questionSlug",
                    "questionDifficulty",
                    "questionNumber",
                    "questionLink",
                    "questionTitle",
                    description,
                    "acceptanceRate",
                    "createdAt"
                FROM
                    "QuestionBank"
                ORDER BY RANDOM()
                LIMIT 1
            """;

        return jdbcClient.sql(sql).query(questionBankRowMapper).optional();
    }

    @Override
    public List<QuestionBank> getQuestionsByTopic(final LeetcodeTopicEnum topic) {
        String sql = """
                SELECT DISTINCT
                    qb.id,
                    qb."questionSlug",
                    qb."questionDifficulty",
                    qb."questionNumber",
                    qb."questionLink",
                    qb."questionTitle",
                    qb.description,
                    qb."acceptanceRate",
                    qb."createdAt"
                FROM
                    "QuestionBank" qb
                INNER JOIN
                    "QuestionTopic" qt ON qb.id = qt."questionBankId"
                WHERE
                    qt.topic = :topic
            """;

        return jdbcClient
                .sql(sql)
                .param("topic", topic.getLeetcodeEnum(), Types.OTHER)
                .query(questionBankRowMapper)
                .list();
    }

    @Override
    public List<QuestionBank> getQuestionsByDifficulty(final QuestionDifficulty difficulty) {
        String sql = """
                SELECT
                    id,
                    "questionSlug",
                    "questionDifficulty",
                    "questionNumber",
                    "questionLink",
                    "questionTitle",
                    description,
                    "acceptanceRate",
                    "createdAt"
                FROM
                    "QuestionBank"
                WHERE
                    "questionDifficulty" = :difficulty
            """;

        return jdbcClient
                .sql(sql)
                .param("difficulty", difficulty, Types.OTHER)
                .query(questionBankRowMapper)
                .list();
    }

    @Override
    public List<QuestionBank> getAllQuestions() {
        String sql = """
                    SELECT
                        id,
                        "questionSlug",
                        "questionDifficulty",
                        "questionNumber",
                        "questionLink",
                        "questionTitle",
                        description,
                        "acceptanceRate",
                        "createdAt"
                    FROM "QuestionBank"
            """;

        return jdbcClient.sql(sql).query(questionBankRowMapper).list();
    }
}
