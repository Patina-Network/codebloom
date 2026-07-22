package org.patinanetwork.codebloom.common.db.repos.weekly;

import java.util.Optional;
import java.util.UUID;
import org.patinanetwork.codebloom.common.db.models.weekly.WeeklyMessage;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
public class WeeklyMessageSqlRepository implements WeeklyMessageRepository {

    private static final RowMapper<WeeklyMessage> WEEKLY_MESSAGE_ROW_MAPPER = (rs, rowNum) -> WeeklyMessage.builder()
            .id(rs.getString("id"))
            .createdAt(rs.getTimestamp("createdAt").toLocalDateTime())
            .build();

    private final JdbcClient jdbcClient;

    public WeeklyMessageSqlRepository(final JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public WeeklyMessage getLatestWeeklyMessage() {
        String sql = """
            SELECT
                id,
                "createdAt"
            FROM
                "WeeklyMessage"
            ORDER BY
                "createdAt" DESC
            LIMIT 1
                                """;

        return jdbcClient.sql(sql).query(WEEKLY_MESSAGE_ROW_MAPPER).optional().orElse(null);
    }

    @Override
    public WeeklyMessage getWeeklyMessageById(final String id) {
        String sql = """
            SELECT
                id,
                "createdAt"
            FROM
                "WeeklyMessage"
            WHERE
                id = ?
            LIMIT 1
                                """;

        return jdbcClient
                .sql(sql)
                .param(1, UUID.fromString(id))
                .query(WEEKLY_MESSAGE_ROW_MAPPER)
                .optional()
                .orElse(null);
    }

    @Override
    public boolean createLatestWeeklyMessage(final WeeklyMessage message) {
        String sql = """
                INSERT INTO "WeeklyMessage"
                    (id)
                VALUES
                    (?)
                RETURNING
                    id, "createdAt"
            """;

        Optional<WeeklyMessage> created = jdbcClient
                .sql(sql)
                .param(1, UUID.randomUUID())
                .query(WEEKLY_MESSAGE_ROW_MAPPER)
                .optional();

        if (created.isPresent()) {
            message.setId(created.get().getId());
            message.setCreatedAt(created.get().getCreatedAt());
            return true;
        }

        return false;
    }

    @Override
    public boolean createLatestWeeklyMessage() {
        String sql = """
                INSERT INTO "WeeklyMessage"
                    (id)
                VALUES
                    (?)
            """;

        int rowsAffected = jdbcClient.sql(sql).param(1, UUID.randomUUID()).update();

        return rowsAffected > 0;
    }

    @Override
    public boolean deleteWeeklyMessageById(final String id) {
        String sql = """
            DELETE FROM
                "WeeklyMessage"
            WHERE
                id = ?
            """;

        int rowsAffected = jdbcClient.sql(sql).param(1, UUID.fromString(id)).update();

        return rowsAffected > 0;
    }

    @Override
    public boolean deleteLatestWeeklyMessage() {
        String sql = """
            WITH to_delete AS (
                SELECT *
                FROM "WeeklyMessage"
                ORDER BY "createdAt" DESC
                LIMIT 1
            )
            DELETE FROM "WeeklyMessage" wm
            USING to_delete td
            WHERE wm.id = td.id
            """;

        int rowsAffected = jdbcClient.sql(sql).update();

        return rowsAffected > 0;
    }
}
