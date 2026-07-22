package org.patinanetwork.codebloom.common.db.repos.achievements;

import java.sql.Types;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.patinanetwork.codebloom.common.db.models.achievements.Achievement;
import org.patinanetwork.codebloom.common.db.models.achievements.AchievementPlaceEnum;
import org.patinanetwork.codebloom.common.db.models.usertag.Tag;
import org.patinanetwork.codebloom.common.time.StandardizedOffsetDateTime;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
public class AchievementSqlRepository implements AchievementRepository {

    private static final RowMapper<Achievement> ACHIEVEMENT_ROW_MAPPER = (rs, rowNum) -> {
        var id = rs.getString("id");
        var userId = rs.getString("userId");
        var place = AchievementPlaceEnum.valueOf(rs.getString("place"));
        var leaderboard = Optional.ofNullable(rs.getString("leaderboard"))
                .map(Tag::valueOf)
                .orElse(null);
        var title = rs.getString("title");
        var description = rs.getString("description");
        var isActive = rs.getBoolean("isActive");
        var createdAt = StandardizedOffsetDateTime.normalize(rs.getObject("createdAt", OffsetDateTime.class));
        OffsetDateTime deletedAt =
                StandardizedOffsetDateTime.normalize(rs.getObject("deletedAt", OffsetDateTime.class));
        return Achievement.builder()
                .id(id)
                .userId(userId)
                .place(place)
                .leaderboard(leaderboard)
                .title(title)
                .description(description)
                .isActive(isActive)
                .createdAt(createdAt)
                .deletedAt(deletedAt)
                .build();
    };

    private final JdbcClient jdbcClient;

    public AchievementSqlRepository(final JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public void createAchievement(final Achievement achievement) {
        achievement.setId(UUID.randomUUID().toString());
        String sql = """
            INSERT INTO "Achievement"
                (id, "userId", place, leaderboard, title, description, "isActive", "deletedAt")
            VALUES
                (:id, :userId, :place, :leaderboard, :title, :description, :isActive, :deletedAt)
            RETURNING
                "createdAt"
            """;

        OffsetDateTime createdAt = jdbcClient
                .sql(sql)
                .param("id", UUID.fromString(achievement.getId()))
                .param("userId", UUID.fromString(achievement.getUserId()))
                .param("place", achievement.getPlace().name(), Types.OTHER)
                .param(
                        "leaderboard",
                        Optional.ofNullable(achievement.getLeaderboard())
                                .map(Enum::name)
                                .orElse(null),
                        Types.OTHER)
                .param("title", achievement.getTitle())
                .param("description", achievement.getDescription())
                .param("isActive", achievement.isActive())
                .param("deletedAt", achievement.getDeletedAt())
                .query((rs, rowNum) ->
                        StandardizedOffsetDateTime.normalize(rs.getObject("createdAt", OffsetDateTime.class)))
                .optional()
                .orElse(null);

        achievement.setCreatedAt(createdAt);
    }

    @Override
    public Achievement updateAchievement(final Achievement achievement) {
        String sql = """
            UPDATE
                "Achievement"
            SET
                place = :place,
                leaderboard = :leaderboard,
                title = :title,
                description = :description,
                "isActive" = :isActive,
                "deletedAt" = :deletedAt
            WHERE
                id = :id
            """;

        jdbcClient
                .sql(sql)
                .param("place", achievement.getPlace().name(), Types.OTHER)
                .param(
                        "leaderboard",
                        Optional.ofNullable(achievement.getLeaderboard())
                                .map(Enum::name)
                                .orElse(null),
                        Types.OTHER)
                .param("title", achievement.getTitle())
                .param("description", achievement.getDescription())
                .param("isActive", achievement.isActive())
                .param("deletedAt", achievement.getDeletedAt())
                .param("id", UUID.fromString(achievement.getId()))
                .update();

        return getAchievementById(achievement.getId());
    }

    @Override
    public boolean deleteAchievementById(final String id) {
        String sql = """
            UPDATE
                "Achievement"
            SET
                "deletedAt" = :deletedAt
            WHERE
                id = :id
            """;

        int rowsAffected = jdbcClient
                .sql(sql)
                .param("deletedAt", LocalDateTime.now())
                .param("id", UUID.fromString(id))
                .update();

        return rowsAffected > 0;
    }

    @Override
    public Achievement getAchievementById(final String id) {
        String sql = """
            SELECT
                id,
                "userId",
                place,
                leaderboard,
                title,
                description,
                "isActive",
                "createdAt",
                "deletedAt"
            FROM
                "Achievement"
            WHERE
                id = :id
                AND "deletedAt" IS NULL
            """;

        return jdbcClient
                .sql(sql)
                .param("id", UUID.fromString(id))
                .query(ACHIEVEMENT_ROW_MAPPER)
                .optional()
                .orElse(null);
    }

    @Override
    public List<Achievement> getAchievementsByUserId(final String userId) {
        String sql = """
            SELECT
                id,
                "userId",
                place,
                leaderboard,
                title,
                description,
                "isActive",
                "createdAt",
                "deletedAt"
            FROM
                "Achievement"
            WHERE
                "userId" = :userId
                AND "deletedAt" IS NULL
            ORDER BY
                "createdAt" DESC,
                (leaderboard IS NULL) DESC
            """;

        return jdbcClient
                .sql(sql)
                .param("userId", UUID.fromString(userId))
                .query(ACHIEVEMENT_ROW_MAPPER)
                .list();
    }
}
