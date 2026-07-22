package org.patinanetwork.codebloom.common.db.repos.potd;

import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;
import org.patinanetwork.codebloom.common.db.models.potd.POTD;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
public class POTDSqlRepository implements POTDRepository {

    private static final RowMapper<POTD> POTD_ROW_MAPPER = (rs, rowNum) -> POTD.builder()
            .id(rs.getString("id"))
            .title(rs.getString("title"))
            .slug(rs.getString("slug"))
            .multiplier(rs.getFloat("multiplier"))
            .createdAt(rs.getTimestamp("createdAt").toLocalDateTime())
            .build();

    private final JdbcClient jdbcClient;

    public POTDSqlRepository(final JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public POTD createPOTD(final POTD potd) {
        String sql = """
            INSERT INTO "POTD"
                ("id", "title", "slug", "multiplier", "createdAt")
            VALUES
                (?, ?, ?, ?, ?)
            """;

        potd.setId(UUID.randomUUID().toString());

        jdbcClient
                .sql(sql)
                .param(1, UUID.fromString(potd.getId()))
                .param(2, potd.getTitle())
                .param(3, potd.getSlug())
                .param(4, potd.getMultiplier())
                .param(5, potd.getCreatedAt())
                .update();

        return potd;
    }

    @Override
    public Optional<POTD> getPOTDById(final String id) {
        String sql = "SELECT id, \"title\", \"slug\", \"multiplier\", \"createdAt\" FROM \"POTD\" WHERE id = ?";

        return jdbcClient
                .sql(sql)
                .param(1, UUID.fromString(id))
                .query(POTD_ROW_MAPPER)
                .optional();
    }

    @Override
    public ArrayList<POTD> getAllPOTDS() {
        String sql = "SELECT id, \"title\", \"slug\", \"multiplier\", \"createdAt\" FROM \"POTD\"";

        return new ArrayList<>(jdbcClient.sql(sql).query(POTD_ROW_MAPPER).list());
    }

    @Override
    public void updatePOTD(final POTD potd) {
        String sql = "UPDATE \"POTD\" SET title = ?, slug = ?, multiplier = ? WHERE id = ?";

        jdbcClient
                .sql(sql)
                .param(1, potd.getTitle())
                .param(2, potd.getSlug())
                .param(3, potd.getMultiplier())
                .param(4, UUID.fromString(potd.getId()))
                .update();
    }

    @Override
    public void deletePOTD(final String id) {
        String sql = "DELETE FROM \"POTD\" WHERE id = ?";

        jdbcClient.sql(sql).param(1, UUID.fromString(id)).update();
    }

    @Override
    public Optional<POTD> getCurrentPOTD() {
        String sql = """
            SELECT "id", "title", "slug", "multiplier", "createdAt"
            FROM "POTD"
            ORDER BY "createdAt" DESC
            LIMIT 1
            """;

        return jdbcClient.sql(sql).query(POTD_ROW_MAPPER).optional();
    }
}
