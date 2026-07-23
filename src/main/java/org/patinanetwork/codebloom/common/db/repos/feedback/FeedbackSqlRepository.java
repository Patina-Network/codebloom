package org.patinanetwork.codebloom.common.db.repos.feedback;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.patinanetwork.codebloom.common.db.models.feedback.Feedback;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
public class FeedbackSqlRepository implements FeedbackRepository {

    private static final RowMapper<Feedback> FEEDBACK_ROW_MAPPER = (rs, rowNum) -> Feedback.builder()
            .id(rs.getString("id"))
            .title(rs.getString("title"))
            .description(rs.getString("description"))
            .email(Optional.ofNullable(rs.getString("email")))
            .createdAt(rs.getObject("createdAt", OffsetDateTime.class))
            .build();

    private final JdbcClient jdbcClient;

    public FeedbackSqlRepository(final JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public Optional<Feedback> findFeedbackById(final String id) {
        String sql = """
                SELECT
                    id,
                    title,
                    description,
                    email,
                    "createdAt"
                FROM
                    "Report"
                WHERE
                    id = :id
            """;

        return jdbcClient
                .sql(sql)
                .param("id", UUID.fromString(id))
                .query(FEEDBACK_ROW_MAPPER)
                .optional();
    }

    @Override
    public void createFeedback(final Feedback feedback) {
        String sql = """
               INSERT INTO "Report"
                   (id, title, description, email)
               VALUES
                   (:id, :title, :description, :email)
                RETURNING
                    "createdAt"
            """;

        feedback.setId(UUID.randomUUID().toString());

        OffsetDateTime createdAt = jdbcClient
                .sql(sql)
                .param("id", UUID.fromString(feedback.getId()))
                .param("title", feedback.getTitle())
                .param("description", feedback.getDescription())
                .param("email", feedback.getEmail().orElse(null))
                .query((rs, rowNum) -> rs.getObject("createdAt", OffsetDateTime.class))
                .optional()
                .orElse(null);

        feedback.setCreatedAt(createdAt);
    }

    @Override
    public boolean deleteFeedbackById(final String id) {
        String sql = """
                DELETE FROM
                    "Report"
                WHERE
                    id = :id
            """;

        int rowsAffected = jdbcClient.sql(sql).param("id", UUID.fromString(id)).update();

        return rowsAffected == 1;
    }

    @Override
    public boolean updateFeedback(final Feedback feedback) {
        String sql = """
                UPDATE "Report"
                SET
                    title = :title,
                    description = :description,
                    email = :email
                WHERE
                    id = :id
            """;

        int rowsAffected = jdbcClient
                .sql(sql)
                .param("title", feedback.getTitle())
                .param("description", feedback.getDescription())
                .param("email", feedback.getEmail().orElse(null))
                .param("id", UUID.fromString(feedback.getId()))
                .update();

        return rowsAffected > 0;
    }
}
