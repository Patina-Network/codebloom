package org.patinanetwork.codebloom.common.db.repos.discord.club.metadata;

import java.util.Optional;
import java.util.UUID;
import org.patinanetwork.codebloom.common.db.models.discord.DiscordClubMetadata;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
public class DiscordClubMetadataSqlRepository implements DiscordClubMetadataRepository {

    private static final RowMapper<DiscordClubMetadata> DISCORD_CLUB_METADATA_ROW_MAPPER = (rs, rowNum) -> {
        var id = rs.getString("id");
        var guildId = Optional.ofNullable(rs.getString("guildId"));
        var leaderboardChannelId = Optional.ofNullable(rs.getString("leaderboardChannelId"));
        var discordClubId = rs.getString("discordClubId");

        return DiscordClubMetadata.builder()
                .id(id)
                .guildId(guildId)
                .leaderboardChannelId(leaderboardChannelId)
                .discordClubId(discordClubId)
                .build();
    };

    private final JdbcClient jdbcClient;

    public DiscordClubMetadataSqlRepository(final JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public void createDiscordClubMetadata(final DiscordClubMetadata discordClubMetadata) {
        discordClubMetadata.setId(UUID.randomUUID().toString());
        String sql = """
            INSERT INTO "DiscordClubMetadata"
                (id, "guildId", "leaderboardChannelId", "discordClubId")
            VALUES
                (:id, :guildId, :leaderboardChannelId, :discordClubId)
            """;

        jdbcClient
                .sql(sql)
                .param("id", UUID.fromString(discordClubMetadata.getId()))
                .param("guildId", discordClubMetadata.getGuildId().orElse(null))
                .param(
                        "leaderboardChannelId",
                        discordClubMetadata.getLeaderboardChannelId().orElse(null))
                .param("discordClubId", UUID.fromString(discordClubMetadata.getDiscordClubId()))
                .update();
    }

    @Override
    public Optional<DiscordClubMetadata> getDiscordClubMetadataById(final String id) {
        String sql = """
            SELECT
                id,
                "guildId",
                "leaderboardChannelId",
                "discordClubId"
            FROM
                "DiscordClubMetadata"
            WHERE
                id = :id
            """;

        return jdbcClient
                .sql(sql)
                .param("id", UUID.fromString(id))
                .query(DISCORD_CLUB_METADATA_ROW_MAPPER)
                .optional();
    }

    @Override
    public boolean updateDiscordClubMetadata(final DiscordClubMetadata discordClubMetadata) {
        String sql = """
            UPDATE
                "DiscordClubMetadata"
            SET
                "guildId" = :guildId,
                "leaderboardChannelId" = :leaderboardChannelId,
                "discordClubId" = :discordClubId
            WHERE
                id = :id
            """;

        int rowsAffected = jdbcClient
                .sql(sql)
                .param("guildId", discordClubMetadata.getGuildId().orElse(null))
                .param(
                        "leaderboardChannelId",
                        discordClubMetadata.getLeaderboardChannelId().orElse(null))
                .param("discordClubId", UUID.fromString(discordClubMetadata.getDiscordClubId()))
                .param("id", UUID.fromString(discordClubMetadata.getId()))
                .update();

        return rowsAffected == 1;
    }

    @Override
    public boolean deleteDiscordClubMetadataById(final String id) {
        String sql = """
            DELETE FROM "DiscordClubMetadata"
            WHERE id = :id
            """;

        int rowsAffected = jdbcClient.sql(sql).param("id", UUID.fromString(id)).update();
        return rowsAffected > 0;
    }

    @Override
    public Optional<DiscordClubMetadata> getDiscordClubMetadataByClubId(final String id) {
        String sql = """
            SELECT
                id,
                "guildId",
                "leaderboardChannelId",
                "discordClubId"
            FROM
                "DiscordClubMetadata"
            WHERE
                "discordClubId" = :id
            """;

        return jdbcClient
                .sql(sql)
                .param("id", UUID.fromString(id))
                .query(DISCORD_CLUB_METADATA_ROW_MAPPER)
                .optional();
    }
}
