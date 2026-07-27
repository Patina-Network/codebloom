package org.patinanetwork.codebloom.common.db.repos.announcement;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.patinanetwork.codebloom.common.db.models.announcement.Announcement;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
public class AnnouncementSqlRepository implements AnnouncementRepository {

    private static final RowMapper<Announcement> ANNOUNCEMENT_ROW_MAPPER = (rs, rowNum) -> Announcement.builder()
            .id(rs.getString("id"))
            .createdAt(rs.getObject("createdAt", OffsetDateTime.class))
            .expiresAt(rs.getObject("expiresAt", OffsetDateTime.class))
            .showTimer(rs.getBoolean("showTimer"))
            .message(rs.getString("message"))
            .build();

    private final JdbcClient jdbcClient;

    public AnnouncementSqlRepository(final JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public List<Announcement> getAllAnnouncements() {
        String sql = """
                SELECT
                    id,
                    "createdAt",
                    "expiresAt",
                    "showTimer",
                    message
                FROM
                    "Announcement"
                ORDER BY
                    "createdAt" ASC
            """;

        return jdbcClient.sql(sql).query(ANNOUNCEMENT_ROW_MAPPER).list();
    }

    @Override
    public Announcement getAnnouncementById(final String id) {
        String sql = """
                                SELECT
                                    id,
                                    "createdAt",
                                    "expiresAt",
                                    "showTimer",
                                    message
                                FROM
                                    "Announcement"
                                WHERE
                                    id = ?
            """;

        return jdbcClient
                .sql(sql)
                .param(UUID.fromString(id))
                .query(ANNOUNCEMENT_ROW_MAPPER)
                .optional()
                .orElse(null);
    }

    @Override
    public Announcement getRecentAnnouncement() {
        String sql = """
                                SELECT
                                    id,
                                    "createdAt",
                                    "expiresAt",
                                    "showTimer",
                                    message
                                FROM
                                    "Announcement"
                                ORDER BY
                                    "createdAt" DESC
                                LIMIT 1
            """;

        return jdbcClient.sql(sql).query(ANNOUNCEMENT_ROW_MAPPER).optional().orElse(null);
    }

    @Override
    public boolean createAnnouncement(final Announcement announcement) {
        String sql = """
               INSERT INTO "Announcement"
                   (id, "expiresAt", "showTimer", "message")
               VALUES
                   (?, ?, ?, ?)
                RETURNING
                    id, "createdAt"
            """;

        return jdbcClient
                .sql(sql)
                .param(UUID.randomUUID())
                .param(announcement.getExpiresAt())
                .param(announcement.isShowTimer())
                .param(announcement.getMessage())
                .query((rs, rowNum) -> {
                    announcement.setId(rs.getString("id"));
                    announcement.setCreatedAt(rs.getObject("createdAt", OffsetDateTime.class));
                    return true;
                })
                .optional()
                .orElse(false);
    }

    @Override
    public boolean deleteAnnouncementById(final String id) {
        String sql = """
                DELETE FROM
                    "Announcement"
                WHERE
                    id = ?
            """;

        int rowsAffected = jdbcClient.sql(sql).param(UUID.fromString(id)).update();

        return rowsAffected == 1;
    }

    @Override
    public boolean updateAnnouncement(final Announcement announcement) {
        String sql = """
            UPDATE
                "Announcement"
            SET
                "expiresAt" = :expiresAt,
                "showTimer" = :showTimer,
                "message"  = :message
            WHERE
                id = :id
            """;

        int rowsAffected = jdbcClient
                .sql(sql)
                .param("expiresAt", announcement.getExpiresAt())
                .param("showTimer", announcement.isShowTimer())
                .param("message", announcement.getMessage())
                .param("id", UUID.fromString(announcement.getId()))
                .update();

        return rowsAffected > 0;
    }
}
