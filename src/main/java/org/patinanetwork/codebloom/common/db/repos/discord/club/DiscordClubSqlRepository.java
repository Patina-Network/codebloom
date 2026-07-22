package org.patinanetwork.codebloom.common.db.repos.discord.club;

import java.sql.Types;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.patinanetwork.codebloom.common.db.models.discord.DiscordClub;
import org.patinanetwork.codebloom.common.db.models.usertag.Tag;
import org.patinanetwork.codebloom.common.db.repos.discord.club.metadata.DiscordClubMetadataSqlRepository;
import org.patinanetwork.codebloom.common.time.StandardizedOffsetDateTime;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
public class DiscordClubSqlRepository implements DiscordClubRepository {

    private final DiscordClubMetadataSqlRepository discordClubMetadataSqlRepository;

    private final JdbcClient jdbcClient;

    private final RowMapper<DiscordClub> discordClubRowMapper;

    public DiscordClubSqlRepository(
            final JdbcClient jdbcClient, final DiscordClubMetadataSqlRepository discordClubMetadataSqlRepository) {
        this.jdbcClient = jdbcClient;
        this.discordClubMetadataSqlRepository = discordClubMetadataSqlRepository;
        this.discordClubRowMapper = (rs, rowNum) -> {
            var id = rs.getString("id");
            var name = rs.getString("name");
            var description = Optional.ofNullable(rs.getString("description"));
            var createdAt = StandardizedOffsetDateTime.normalize(rs.getObject("createdAt", OffsetDateTime.class));
            var deletedAt = Optional.ofNullable(rs.getObject("deletedAt", OffsetDateTime.class))
                    .map(StandardizedOffsetDateTime::normalize);
            Tag tag = Tag.valueOf(rs.getString("tag"));
            var metadata = this.discordClubMetadataSqlRepository.getDiscordClubMetadataByClubId(id);

            return DiscordClub.builder()
                    .id(id)
                    .name(name)
                    .description(description)
                    .tag(tag)
                    .discordClubMetadata(metadata)
                    .createdAt(createdAt)
                    .deletedAt(deletedAt)
                    .build();
        };
    }

    @Override
    public void createDiscordClub(final DiscordClub discordClub) {
        discordClub.setId(UUID.randomUUID().toString());
        String sql = """
            INSERT INTO "DiscordClub"
                (id, name, description, tag, "deletedAt")
            VALUES
                (:id, :name, :description, :tag, :deletedAt)
            RETURNING
                "createdAt"
            """;

        OffsetDateTime createdAt = jdbcClient
                .sql(sql)
                .param("id", UUID.fromString(discordClub.getId()))
                .param("name", discordClub.getName())
                .param("description", discordClub.getDescription().orElse(null))
                .param("tag", discordClub.getTag().name(), Types.OTHER)
                .param("deletedAt", discordClub.getDeletedAt().orElse(null))
                .query((rs, rowNum) ->
                        StandardizedOffsetDateTime.normalize(rs.getObject("createdAt", OffsetDateTime.class)))
                .optional()
                .orElse(null);

        discordClub.setCreatedAt(createdAt);
    }

    @Override
    public Optional<DiscordClub> getDiscordClubById(final String id) {
        String sql = """
            SELECT
                *
            FROM
                "DiscordClub"
            WHERE
                id = :id
            """;

        return jdbcClient
                .sql(sql)
                .param("id", UUID.fromString(id))
                .query(discordClubRowMapper)
                .optional();
    }

    @Override
    public boolean updateDiscordClubById(final DiscordClub discordClub) {
        String sql = """
            UPDATE
                "DiscordClub"
            SET
                "name" = :name,
                "description" = :description,
                "tag" = :tag,
                "deletedAt" = :deletedAt
            WHERE
                id = :id
            """;

        int rowsAffected = jdbcClient
                .sql(sql)
                .param("id", UUID.fromString(discordClub.getId()))
                .param("name", discordClub.getName())
                .param("description", discordClub.getDescription().orElse(null))
                .param("tag", discordClub.getTag().name(), Types.OTHER)
                .param("deletedAt", discordClub.getDeletedAt().orElse(null))
                .update();

        return rowsAffected == 1;
    }

    @Override
    public boolean deleteDiscordClubById(final String id) {
        String sql = """
            DELETE FROM "DiscordClub"
            WHERE id = :id
            """;

        int rowsAffected = jdbcClient.sql(sql).param("id", UUID.fromString(id)).update();
        return rowsAffected > 0;
    }

    @Override
    public List<DiscordClub> getAllActiveDiscordClubs() {
        String sql = """
            SELECT
                *
            FROM
                "DiscordClub"
            WHERE
                "deletedAt" IS NULL
            """;

        return jdbcClient.sql(sql).query(discordClubRowMapper).list();
    }

    @Override
    public Optional<DiscordClub> getDiscordClubByGuildId(String guildId) {
        String sql = """
            SELECT
                dc.*
            FROM
                "DiscordClub" dc
            INNER JOIN
                "DiscordClubMetadata" dcm
            ON
                dc.id = dcm."discordClubId"
            WHERE
                dcm."guildId" = :guildId
            AND
                dc."deletedAt" IS NULL
            """;

        return jdbcClient
                .sql(sql)
                .param("guildId", guildId)
                .query(discordClubRowMapper)
                .optional();
    }
}
