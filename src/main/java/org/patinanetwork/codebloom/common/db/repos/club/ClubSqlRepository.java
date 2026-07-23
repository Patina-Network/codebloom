package org.patinanetwork.codebloom.common.db.repos.club;

import java.sql.Types;
import java.util.UUID;
import org.patinanetwork.codebloom.common.db.models.club.Club;
import org.patinanetwork.codebloom.common.db.models.usertag.Tag;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
public class ClubSqlRepository implements ClubRepository {

    private static final RowMapper<Club> CLUB_ROW_MAPPER = (rs, rowNum) -> {
        var id = rs.getString("id");
        var name = rs.getString("name");
        var description = rs.getString("description");
        var slug = rs.getString("slug");
        var splashIconUrl = rs.getString("splashIconUrl");
        var password = rs.getString("password");
        var tagValue = rs.getString("tag");
        Tag tag = tagValue != null ? Tag.valueOf(tagValue) : null;

        return Club.builder()
                .id(id)
                .name(name)
                .description(description)
                .slug(slug)
                .splashIconUrl(splashIconUrl)
                .password(password)
                .tag(tag)
                .build();
    };

    private final JdbcClient jdbcClient;

    public ClubSqlRepository(final JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public void createClub(final Club club) {
        club.setId(UUID.randomUUID().toString());
        String sql = """
            INSERT INTO "Club"
                (id, name, description, slug, "splashIconUrl", password, tag)
            VALUES
                (:id, :name, :description, :slug, :splashIconUrl, :password, :tag)
            """;

        jdbcClient
                .sql(sql)
                .param("id", UUID.fromString(club.getId()))
                .param("name", club.getName())
                .param("description", club.getDescription())
                .param("slug", club.getSlug())
                .param("splashIconUrl", club.getSplashIconUrl())
                .param("password", club.getPassword())
                .param("tag", club.getTag() != null ? club.getTag().name() : null, Types.OTHER)
                .update();
    }

    @Override
    public Club updateClub(final Club club) {
        String sql = """
            UPDATE
                "Club"
            SET
                "name" = :name,
                "description" = :description,
                "splashIconUrl" = :splashIconUrl,
                "password" = :password,
                "tag" = :tag
            WHERE
                id = :id
            """;

        int rowsAffected = jdbcClient
                .sql(sql)
                .param("name", club.getName())
                .param("description", club.getDescription())
                .param("splashIconUrl", club.getSplashIconUrl())
                .param("password", club.getPassword())
                .param("tag", club.getTag() != null ? club.getTag().name() : null, Types.OTHER)
                .param("id", UUID.fromString(club.getId()))
                .update();

        if (rowsAffected > 0) {
            return getClubById(club.getId());
        }
        return null;
    }

    @Override
    public Club getClubById(final String id) {
        String sql = """
            SELECT
                id,
                "name",
                "description",
                "slug",
                "splashIconUrl",
                "password",
                "tag"
            FROM
                "Club"
            WHERE
                id = :id
            """;

        return jdbcClient
                .sql(sql)
                .param("id", UUID.fromString(id))
                .query(CLUB_ROW_MAPPER)
                .optional()
                .orElse(null);
    }

    @Override
    public Club getClubBySlug(final String slug) {
        String sql = """
            SELECT
                id,
                "name",
                "description",
                "slug",
                "splashIconUrl",
                "password",
                "tag"
            FROM
                "Club"
            WHERE
                "slug" = :slug
            """;

        return jdbcClient
                .sql(sql)
                .param("slug", slug)
                .query(CLUB_ROW_MAPPER)
                .optional()
                .orElse(null);
    }

    @Override
    public boolean deleteClubBySlug(final String slug) {
        String sql = """
            DELETE FROM "Club"
            WHERE "slug" = :slug
            """;

        int rowsAffected = jdbcClient.sql(sql).param("slug", slug).update();
        return rowsAffected > 0;
    }

    @Override
    public boolean deleteClubById(final String id) {
        String sql = """
            DELETE FROM "Club"
            WHERE id = :id
            """;

        int rowsAffected = jdbcClient.sql(sql).param("id", UUID.fromString(id)).update();
        return rowsAffected > 0;
    }
}
