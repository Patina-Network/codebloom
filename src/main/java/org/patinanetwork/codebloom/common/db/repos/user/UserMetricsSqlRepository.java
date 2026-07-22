package org.patinanetwork.codebloom.common.db.repos.user;

import java.sql.Types;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.patinanetwork.codebloom.common.db.models.user.UserMetrics;
import org.patinanetwork.codebloom.common.db.repos.user.options.UserMetricsFilterOptions;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
public class UserMetricsSqlRepository implements UserMetricsRepository {

    private static final RowMapper<UserMetrics> USER_METRICS_ROW_MAPPER = (rs, rowNum) -> UserMetrics.builder()
            .id(rs.getString("id"))
            .userId(rs.getString("userId"))
            .points(rs.getInt("points"))
            .createdAt(rs.getObject("createdAt", OffsetDateTime.class))
            .deletedAt(Optional.ofNullable(rs.getObject("deletedAt", OffsetDateTime.class)))
            .build();

    private final JdbcClient jdbcClient;

    public UserMetricsSqlRepository(final JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public void createUserMetrics(final UserMetrics userMetrics) {
        String sql = """
                INSERT INTO "UserMetrics"
                    (id, "userId", points)
                VALUES
                    (:id, :userId, :points)
                RETURNING
                    id, "createdAt"
                """;

        userMetrics.setId(UUID.randomUUID().toString());

        OffsetDateTime createdAt = jdbcClient
                .sql(sql)
                .param("id", UUID.fromString(userMetrics.getId()))
                .param("userId", UUID.fromString(userMetrics.getUserId()))
                .param("points", userMetrics.getPoints())
                .query((rs, rowNum) -> rs.getObject("createdAt", OffsetDateTime.class))
                .optional()
                .orElse(null);

        userMetrics.setCreatedAt(createdAt);
    }

    @Override
    public Optional<UserMetrics> findUserMetricsById(final String id) {
        String sql = """
                SELECT
                    id,
                    "userId",
                    points,
                    "createdAt",
                    "deletedAt"
                FROM
                    "UserMetrics"
                WHERE
                    id = :id
                    AND "deletedAt" IS NULL
                """;

        return jdbcClient
                .sql(sql)
                .param("id", UUID.fromString(id))
                .query(USER_METRICS_ROW_MAPPER)
                .optional();
    }

    @Override
    public List<UserMetrics> findUserMetrics(final String userId, final UserMetricsFilterOptions options) {
        String sql = """
                SELECT
                    id,
                    "userId",
                    points,
                    "createdAt",
                    "deletedAt"
                FROM
                    "UserMetrics"
                WHERE
                    "userId" = :userId
                    AND "deletedAt" IS NULL
                    AND (cast(:from AS timestamptz) IS NULL OR "createdAt" >= :from)
                    AND (cast(:to AS timestamptz) IS NULL OR "createdAt" <= :to)
                LIMIT CASE WHEN :pageSize > 0 THEN :pageSize END
                OFFSET :offset
                """;

        return jdbcClient
                .sql(sql)
                .param("userId", UUID.fromString(userId))
                .param("from", options.getFrom(), Types.TIMESTAMP_WITH_TIMEZONE)
                .param("to", options.getTo(), Types.TIMESTAMP_WITH_TIMEZONE)
                .param("pageSize", options.getPageSize())
                .param("offset", options.getPageSize() > 0 ? (options.getPage() - 1) * options.getPageSize() : 0)
                .query(USER_METRICS_ROW_MAPPER)
                .list();
    }

    @Override
    public int countUserMetrics(final String userId, final UserMetricsFilterOptions options) {
        String sql = """
                SELECT COUNT(*)
                FROM
                    "UserMetrics"
                WHERE
                    "userId" = :userId
                    AND "deletedAt" IS NULL
                    AND (cast(:from AS timestamptz) IS NULL OR "createdAt" >= :from)
                    AND (cast(:to AS timestamptz) IS NULL OR "createdAt" <= :to)
                """;

        return jdbcClient
                .sql(sql)
                .param("userId", UUID.fromString(userId))
                .param("from", options.getFrom(), Types.TIMESTAMP_WITH_TIMEZONE)
                .param("to", options.getTo(), Types.TIMESTAMP_WITH_TIMEZONE)
                .query((rs, rowNum) -> rs.getInt(1))
                .optional()
                .orElse(0);
    }

    @Override
    public boolean deleteUserMetricsById(final String id) {
        String sql = """
                UPDATE "UserMetrics"
                SET
                    "deletedAt" = NOW()
                WHERE
                    id = :id
                    AND "deletedAt" IS NULL
                """;

        int rowsAffected = jdbcClient.sql(sql).param("id", UUID.fromString(id)).update();

        return rowsAffected == 1;
    }
}
