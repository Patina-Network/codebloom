package org.patinanetwork.codebloom.common.db.repos.api;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.patinanetwork.codebloom.common.db.models.api.ApiKey;
import org.patinanetwork.codebloom.common.time.StandardizedLocalDateTime;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
public class ApiKeySqlRepository implements ApiKeyRepository {

    private static final RowMapper<ApiKey> API_KEY_ROW_MAPPER = (rs, rowNum) -> ApiKey.builder()
            .id(rs.getString("id"))
            .apiKey(rs.getString("apiKeyHash"))
            .expiresAt(Optional.ofNullable(rs.getTimestamp("expiresAt"))
                    .map(Timestamp::toLocalDateTime)
                    .orElse(null))
            .createdAt(rs.getTimestamp("createdAt").toLocalDateTime())
            .updatedAt(rs.getTimestamp("updatedAt").toLocalDateTime())
            .updatedBy(rs.getString("updatedBy"))
            .build();

    private final JdbcClient jdbcClient;

    public ApiKeySqlRepository(final JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public ApiKey getApiKeyById(final String id) {
        String sql = """
            SELECT
                id,
                "apiKeyHash",
                "expiresAt",
                "createdAt",
                "updatedAt",
                "updatedBy"
            FROM
                "ApiKey"
            WHERE
                id = :id
            """;

        return jdbcClient
                .sql(sql)
                .param("id", UUID.fromString(id))
                .query(API_KEY_ROW_MAPPER)
                .optional()
                .orElse(null);
    }

    @Override
    public ApiKey getApiKeyByHash(final String hash) {
        String sql = """
            SELECT
                id,
                "apiKeyHash",
                "expiresAt",
                "createdAt",
                "updatedAt",
                "updatedBy"
            FROM
                "ApiKey"
            WHERE
                "apiKeyHash" = :hash
            """;

        return jdbcClient
                .sql(sql)
                .param("hash", hash)
                .query(API_KEY_ROW_MAPPER)
                .optional()
                .orElse(null);
    }

    @Override
    public List<ApiKey> getAllApiKeys() {
        String sql = """
            SELECT
                id,
                "apiKeyHash",
                "expiresAt",
                "createdAt",
                "updatedAt",
                "updatedBy"
            FROM
                "ApiKey"
            """;

        return jdbcClient.sql(sql).query(API_KEY_ROW_MAPPER).list();
    }

    @Override
    public void createApiKey(final ApiKey apiKey) {
        final String sql = """
            INSERT INTO "ApiKey" (
                id,
                "apiKeyHash",
                "expiresAt",
                "updatedBy"
            )
            VALUES (
                :id,
                :apiKeyHash,
                :expiresAt,
                :updatedBy
            )
            """;

        final UUID id = UUID.fromString(apiKey.getId());
        final UUID updatedBy = UUID.fromString(apiKey.getUpdatedBy());

        jdbcClient
                .sql(sql)
                .param("id", id)
                .param("apiKeyHash", apiKey.getApiKey())
                .param("expiresAt", apiKey.getExpiresAt())
                .param("updatedBy", updatedBy)
                .update();
    }

    @Override
    public boolean updateApiKeyById(final ApiKey apiKey) {
        final String sql = """
            UPDATE
                "ApiKey"
            SET
                "apiKeyHash"  = :apiKeyHash,
                "expiresAt" = :expiresAt,
                "updatedAt" = :updatedAt,
                "updatedBy" = :updatedBy
            WHERE
                "id" = :id
            """;
        UUID id = UUID.fromString(apiKey.getId());
        var localTime = StandardizedLocalDateTime.now();
        apiKey.setUpdatedAt(localTime);

        int rows = jdbcClient
                .sql(sql)
                .param("id", id)
                .param("apiKeyHash", apiKey.getApiKey())
                .param("expiresAt", apiKey.getExpiresAt())
                .param("updatedAt", apiKey.getUpdatedAt())
                .param("updatedBy", UUID.fromString(apiKey.getUpdatedBy()))
                .update();
        return rows > 0;
    }

    @Override
    public boolean deleteApiKeyById(final String id) {
        final String sql = """
            DELETE FROM
                "ApiKey"
            WHERE
                "id" = :id
            """;

        final int rowsAffected =
                jdbcClient.sql(sql).param("id", UUID.fromString(id)).update();
        return rowsAffected > 0;
    }

    @Override
    public boolean deleteApiKeyByHash(final String hash) {
        final String sql = """
            DELETE FROM
                "ApiKey"
            WHERE
                "apiKeyHash" = :hash
            """;

        final int rowsAffected = jdbcClient.sql(sql).param("hash", hash).update();
        return rowsAffected > 0;
    }
}
