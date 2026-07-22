package org.patinanetwork.codebloom.common.db.repos.auth;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.patinanetwork.codebloom.common.db.models.auth.Auth;
import org.patinanetwork.codebloom.common.time.StandardizedOffsetDateTime;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
public class AuthSqlRepository implements AuthRepository {

    private static final RowMapper<Auth> AUTH_ROW_MAPPER = (rs, rowNum) -> Auth.builder()
            .id(rs.getString("id"))
            .token(rs.getString("token"))
            .csrf(rs.getString("csrf"))
            .createdAt(StandardizedOffsetDateTime.normalize(rs.getObject("createdAt", OffsetDateTime.class)))
            .build();

    private JdbcClient jdbcClient;

    public AuthSqlRepository(final JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public void createAuth(final Auth auth) {
        String sql = """
            INSERT INTO "Auth"
                (id, token, csrf)
            VALUES
                (:id, :token, :csrf)
            RETURNING
                "createdAt"
            """;
        auth.setId(UUID.randomUUID().toString());

        OffsetDateTime createdAt = jdbcClient
                .sql(sql)
                .param("id", UUID.fromString(auth.getId()))
                .param("token", auth.getToken())
                .param("csrf", auth.getCsrf())
                .query((rs, rowNum) ->
                        StandardizedOffsetDateTime.normalize(rs.getObject("createdAt", OffsetDateTime.class)))
                .optional()
                .orElse(null);

        auth.setCreatedAt(createdAt);
    }

    @Override
    public boolean updateAuthById(final Auth auth) {
        String sql = """
            UPDATE "Auth"
            SET
                token = :token,
                csrf = :csrf
            WHERE
                id = :id
            """;

        int rowsAffected = jdbcClient
                .sql(sql)
                .param("id", UUID.fromString(auth.getId()))
                .param("token", auth.getToken())
                .param("csrf", auth.getCsrf())
                .update();

        return rowsAffected > 0;
    }

    @Override
    public Auth getAuthById(final String inputtedId) {
        String sql = """
            SELECT
                id, token, csrf, "createdAt"
            FROM "Auth"
            WHERE
                id = :id;
            """;

        return jdbcClient
                .sql(sql)
                .param("id", UUID.fromString(inputtedId))
                .query(AUTH_ROW_MAPPER)
                .optional()
                .orElse(null);
    }

    @Override
    public Auth getMostRecentAuth() {
        String sql = """
            SELECT
                id, token, csrf, "createdAt"
            FROM "Auth"
            ORDER BY "createdAt" DESC
            LIMIT 1
            """;

        return jdbcClient.sql(sql).query(AUTH_ROW_MAPPER).optional().orElse(null);
    }

    @Override
    public boolean deleteAuthById(final String id) {
        String sql = """
                DELETE FROM
                    "Auth"
                WHERE
                    id = :id
            """;

        int rowsAffected = jdbcClient.sql(sql).param("id", UUID.fromString(id)).update();
        return rowsAffected > 0;
    }
}
