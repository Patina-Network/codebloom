package org.patinanetwork.codebloom.common.db.repos.usertag;

import java.sql.Types;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;
import org.patinanetwork.codebloom.common.db.models.usertag.Tag;
import org.patinanetwork.codebloom.common.db.models.usertag.UserTag;
import org.patinanetwork.codebloom.common.db.repos.usertag.options.UserTagFilterOptions;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
public class UserTagSqlRepository implements UserTagRepository {

    private static final RowMapper<UserTag> USER_TAG_ROW_MAPPER = (rs, rowNum) -> {
        var id = rs.getString("id");
        var createdAt = rs.getTimestamp("createdAt").toLocalDateTime();
        var userId = rs.getString("userId");
        var tag = Tag.valueOf(rs.getString("tag"));
        return new UserTag(id, createdAt, userId, tag);
    };

    private final JdbcClient jdbcClient;

    public UserTagSqlRepository(final JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public Optional<UserTag> findTagByTagId(final String tagId) {
        String sql = """
            SELECT
                id,
                "createdAt",
                "userId",
                tag
            FROM
                "UserTag"
            WHERE
                id = :id
                    """;

        return jdbcClient
                .sql(sql)
                .param("id", UUID.fromString(tagId))
                .query(USER_TAG_ROW_MAPPER)
                .optional();
    }

    @Override
    public Optional<UserTag> findTagByUserIdAndTag(final String userId, final Tag tag) {
        String sql = """
            SELECT
                id,
                "createdAt",
                "userId",
                tag
            FROM
                "UserTag"
            WHERE
                tag = :tag
                AND
                "userId" = :userId
                    """;

        return jdbcClient
                .sql(sql)
                .param("tag", tag.name(), Types.OTHER)
                .param("userId", UUID.fromString(userId))
                .query(USER_TAG_ROW_MAPPER)
                .optional();
    }

    @Override
    public ArrayList<UserTag> findTagsByUserId(final String userId) {
        String sql = """
            SELECT
                id,
                "createdAt",
                "userId",
                tag
            FROM
                "UserTag"
            WHERE
                "userId" = :userId
                    """;

        return new ArrayList<>(jdbcClient
                .sql(sql)
                .param("userId", UUID.fromString(userId))
                .query(USER_TAG_ROW_MAPPER)
                .list());
    }

    @Override
    public ArrayList<UserTag> findTagsByUserId(final String userId, final UserTagFilterOptions options) {
        String sql = """
            SELECT
                id,
                "createdAt",
                "userId",
                tag
            FROM
                "UserTag"
            WHERE
                "userId" = :userId
            AND
                (cast(:pointOfTime AS timestamptz) IS NULL OR "createdAt" <= :pointOfTime)
                    """;

        return new ArrayList<>(jdbcClient
                .sql(sql)
                .param("userId", UUID.fromString(userId))
                .param("pointOfTime", options.getPointOfTime(), Types.TIMESTAMP_WITH_TIMEZONE)
                .query(USER_TAG_ROW_MAPPER)
                .list());
    }

    @Override
    public void createTag(final UserTag userTag) {
        userTag.setId(UUID.randomUUID().toString());
        String sql = """
                INSERT INTO "UserTag"
                    (id, "userId", tag)
                VALUES
                    (:id, :userId, :tag)
                RETURNING
                    "createdAt"
            """;

        var createdAt = jdbcClient
                .sql(sql)
                .param("id", UUID.fromString(userTag.getId()))
                .param("userId", UUID.fromString(userTag.getUserId()))
                .param("tag", userTag.getTag().name(), Types.OTHER)
                .query((rs, rowNum) -> rs.getTimestamp("createdAt").toLocalDateTime())
                .optional()
                .orElse(null);

        userTag.setCreatedAt(createdAt);
    }

    @Override
    public boolean deleteTagByTagId(final String tagId) {
        String sql = """
            DELETE FROM
                "UserTag"
            WHERE
                id = :id
            """;

        int rowsAffected =
                jdbcClient.sql(sql).param("id", UUID.fromString(tagId)).update();

        return rowsAffected > 0;
    }

    @Override
    public boolean deleteTagByUserIdAndTag(final String userId, final Tag tag) {
        String sql = """
                DELETE FROM
                    "UserTag"
                WHERE
                    "userId" = :userId
                    AND
                    tag = :tag
            """;

        int rowsAffected = jdbcClient
                .sql(sql)
                .param("userId", UUID.fromString(userId))
                .param("tag", tag.name(), Types.OTHER)
                .update();

        return rowsAffected > 0;
    }
}
