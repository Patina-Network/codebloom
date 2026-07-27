package org.patinanetwork.codebloom.common.db.repos.session;

import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;
import org.patinanetwork.codebloom.common.db.models.Session;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
public class SessionSqlRepository implements SessionRepository {

    private static final RowMapper<Session> SESSION_ROW_MAPPER = (rs, rowNum) -> Session.builder()
            .id(Optional.of(rs.getString("id")))
            .userId(rs.getString("userId"))
            .expiresAt(rs.getTimestamp("expiresAt").toLocalDateTime())
            .build();

    private JdbcClient jdbcClient;

    public SessionSqlRepository(final JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public void createSession(final Session session) {
        String sql = "INSERT INTO \"Session\" (id, \"userId\", \"expiresAt\") VALUES (?, ?, ?) RETURNING \"id\"";
        // Don't want dashes inside of the cookie, so better to just remove it from the
        // ID altogether.
        session.setId(Optional.of(UUID.randomUUID().toString().replace("-", "")));

        jdbcClient
                .sql(sql)
                .param(
                        1,
                        session.getId()
                                .orElseThrow(
                                        () -> new IllegalStateException("Session ID must be present for insertion.")))
                .param(2, UUID.fromString(session.getUserId()))
                .param(3, session.getExpiresAt())
                .query((rs, rowNum) -> rs.getString("id"))
                .optional()
                .ifPresent(id -> session.setId(Optional.of(id)));
    }

    @Override
    public Optional<Session> getSessionById(final String id) {
        String sql = "SELECT id, \"userId\", \"expiresAt\" FROM \"Session\" WHERE id=?";

        return jdbcClient.sql(sql).param(1, id).query(SESSION_ROW_MAPPER).optional();
    }

    @Override
    public ArrayList<Session> getSessionsByUserId(final String id) {
        String sql = "SELECT id, \"userId\", \"expiresAt\" FROM \"Session\" WHERE \"userId\"=?";

        return new ArrayList<>(jdbcClient
                .sql(sql)
                .param(1, UUID.fromString(id))
                .query(SESSION_ROW_MAPPER)
                .list());
    }

    @Override
    public boolean deleteSessionById(final String id) {
        String sql = "DELETE FROM \"Session\" WHERE id=?";

        int rowsAffected = jdbcClient.sql(sql).param(1, id).update();

        return rowsAffected > 0;
    }

    @Override
    public boolean deleteSessionsByUserId(final String userId) {
        String sql = """
                    DELETE FROM
                        "Session"
                    WHERE
                        "userId" = :userId
                """;

        int rowsAffected =
                jdbcClient.sql(sql).param("userId", UUID.fromString(userId)).update();

        return rowsAffected > 0;
    }
}
