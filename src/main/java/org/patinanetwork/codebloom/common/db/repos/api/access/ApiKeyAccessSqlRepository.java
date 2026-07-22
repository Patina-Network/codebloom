package org.patinanetwork.codebloom.common.db.repos.api.access;

import java.sql.Types;
import java.util.ArrayList;
import java.util.UUID;
import org.patinanetwork.codebloom.common.db.models.api.ApiKeyAccessEnum;
import org.patinanetwork.codebloom.common.db.models.api.access.ApiKeyAccess;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
public class ApiKeyAccessSqlRepository implements ApiKeyAccessRepository {

    private static final RowMapper<ApiKeyAccess> API_KEY_ACCESS_ROW_MAPPER = (rs, rowNum) -> ApiKeyAccess.builder()
            .id(rs.getString("id"))
            .apiKeyId(rs.getString("apiKeyId"))
            .access(ApiKeyAccessEnum.valueOf(rs.getString("access")))
            .build();

    private JdbcClient jdbcClient;

    public ApiKeyAccessSqlRepository(final JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public ApiKeyAccess getApiKeyAccessById(final String id) {
        String sql = """
            SELECT
                id,
                "apiKeyId",
                access
            FROM
                "ApiKeyAccess"
            WHERE
                id = :id
            """;

        return jdbcClient
                .sql(sql)
                .param("id", UUID.fromString(id))
                .query(API_KEY_ACCESS_ROW_MAPPER)
                .optional()
                .orElse(null);
    }

    @Override
    public ArrayList<ApiKeyAccess> getApiKeyAccessesByApiKeyId(final String apiKeyId) {
        String sql = """
            SELECT
                id,
                "apiKeyId",
                access
            FROM
                "ApiKeyAccess"
            WHERE
                "apiKeyId" = :apiKeyId
            """;

        return new ArrayList<>(jdbcClient
                .sql(sql)
                .param("apiKeyId", java.util.UUID.fromString(apiKeyId))
                .query(API_KEY_ACCESS_ROW_MAPPER)
                .list());
    }

    @Override
    public void createApiKeyAccess(final ApiKeyAccess apiKeyAccess) {
        String sql = """
            INSERT INTO
                "ApiKeyAccess" (id, "apiKeyId", access)
            VALUES
                (:id, :apiKeyId, :access)
            """;

        jdbcClient
                .sql(sql)
                .param("id", java.util.UUID.fromString(apiKeyAccess.getId()))
                .param("apiKeyId", java.util.UUID.fromString(apiKeyAccess.getApiKeyId()))
                .param("access", apiKeyAccess.getAccess().name(), Types.OTHER)
                .update();
    }

    @Override
    public boolean updateApiKeyAccessById(final ApiKeyAccess apiKeyAccess) {
        String sql = """
            UPDATE
                "ApiKeyAccess"
            SET
                "apiKeyId" = :apiKeyId,
                access = :access
            WHERE
                id = :id
            """;

        int rowsAffected = jdbcClient
                .sql(sql)
                .param("id", UUID.fromString(apiKeyAccess.getId()))
                .param("apiKeyId", UUID.fromString(apiKeyAccess.getApiKeyId()))
                .param("access", apiKeyAccess.getAccess().name(), Types.OTHER)
                .update();
        return rowsAffected > 0;
    }

    @Override
    public boolean deleteApiKeyAccessesByApiKeyId(final String apiKeyId) {
        String sql = """
            DELETE FROM
                "ApiKeyAccess"
            WHERE
                "apiKeyId" = :apiKeyId
            """;

        int rowsAffected =
                jdbcClient.sql(sql).param("apiKeyId", UUID.fromString(apiKeyId)).update();
        return rowsAffected > 0;
    }

    @Override
    public boolean deleteApiKeyAccessById(final String id) {
        String sql = """
            DELETE FROM
                "ApiKeyAccess"
            WHERE
                id = :id
            """;

        int rowsAffected = jdbcClient.sql(sql).param("id", UUID.fromString(id)).update();
        return rowsAffected > 0;
    }
}
