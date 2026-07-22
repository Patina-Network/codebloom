package org.patinanetwork.codebloom.common.db.repos.leaderboard;

import java.sql.Array;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.patinanetwork.codebloom.common.db.models.leaderboard.Leaderboard;
import org.patinanetwork.codebloom.common.db.models.user.UserWithScore;
import org.patinanetwork.codebloom.common.db.repos.leaderboard.options.LeaderboardFilterOptions;
import org.patinanetwork.codebloom.common.db.repos.user.UserRepository;
import org.patinanetwork.codebloom.common.db.repos.user.options.UserFilterOptions;
import org.patinanetwork.codebloom.common.page.Indexed;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
public class LeaderboardSqlRepository implements LeaderboardRepository {

    private static final String SHOULD_EXPIRE_BY = "shouldExpireBy";

    private static final RowMapper<Leaderboard> LEADERBOARD_ROW_MAPPER = (rs, rowNum) -> Leaderboard.builder()
            .id(rs.getString("id"))
            .createdAt(rs.getTimestamp("createdAt").toLocalDateTime())
            .deletedAt(Optional.ofNullable(rs.getTimestamp("deletedAt")).map(Timestamp::toLocalDateTime))
            .name(rs.getString("name"))
            .shouldExpireBy(
                    Optional.ofNullable(rs.getTimestamp(SHOULD_EXPIRE_BY)).map(Timestamp::toLocalDateTime))
            .syntaxHighlightingLanguage(Optional.ofNullable(rs.getString("syntaxHighlightingLanguage")))
            .build();

    private final DataSource ds;
    private final JdbcClient jdbcClient;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final UserRepository userRepository;

    public LeaderboardSqlRepository(
            final DataSource ds,
            final JdbcClient jdbcClient,
            final NamedParameterJdbcTemplate namedParameterJdbcTemplate,
            final UserRepository userRepository) {
        this.ds = ds;
        this.jdbcClient = jdbcClient;
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
        this.userRepository = userRepository;
    }

    @Override
    public boolean disableLeaderboardById(final String leaderboardId) {
        String sql = """
            UPDATE "Leaderboard"
            SET
                "deletedAt" = NOW()
            WHERE
                id = :id
            AND
                "deletedAt" IS NULL
            """;

        int rowsAffected =
                jdbcClient.sql(sql).param("id", UUID.fromString(leaderboardId)).update();

        return rowsAffected > 0;
    }

    @Override
    public void addNewLeaderboard(final Leaderboard leaderboard) {
        String sql = """
            INSERT INTO "Leaderboard"
                (id, name, "shouldExpireBy", "syntaxHighlightingLanguage")
            VALUES
                (:id, :name, :shouldExpireBy, :syntaxHighlightingLanguage)
            RETURNING
                "createdAt"
            """;
        leaderboard.setId(UUID.randomUUID().toString());

        var createdAt = jdbcClient
                .sql(sql)
                .param("id", UUID.fromString(leaderboard.getId()))
                .param("name", leaderboard.getName())
                .param(SHOULD_EXPIRE_BY, leaderboard.getShouldExpireBy().orElse(null))
                .param(
                        "syntaxHighlightingLanguage",
                        leaderboard.getSyntaxHighlightingLanguage().orElse(null))
                .query((rs, rowNum) -> rs.getTimestamp("createdAt").toLocalDateTime())
                .optional()
                .orElse(null);

        leaderboard.setCreatedAt(createdAt);
    }

    @Override
    public Optional<Leaderboard> getRecentLeaderboardMetadata() {
        String sql = """
            SELECT
                id,
                name,
                "createdAt",
                "deletedAt",
                "shouldExpireBy",
                "syntaxHighlightingLanguage"
            FROM "Leaderboard"
            WHERE
                "deletedAt" IS NULL
            ORDER BY "createdAt" DESC
            LIMIT 1
            """;

        return jdbcClient.sql(sql).query(LEADERBOARD_ROW_MAPPER).optional();
    }

    @Override
    public Optional<Leaderboard> getLeaderboardMetadataById(final String id) {
        String sql = """
            SELECT
                id,
                name,
                "createdAt",
                "deletedAt",
                "shouldExpireBy",
                "syntaxHighlightingLanguage"
            FROM "Leaderboard"
            WHERE
                id = :id
            """;

        return jdbcClient
                .sql(sql)
                .param("id", UUID.fromString(id))
                .query(LEADERBOARD_ROW_MAPPER)
                .optional();
    }

    @Override
    public List<Indexed<UserWithScore>> getGlobalRankedIndexedLeaderboardUsersById(
            final String leaderboardId, final LeaderboardFilterOptions options) {
        List<UserWithScore> users = this.getLeaderboardUsersById(leaderboardId, options);
        Map<String, UserWithScore> userIdToUserMap =
                users.stream().collect(Collectors.toMap(user -> user.getId(), Function.identity()));

        String sql = """
                WITH ranks AS (
                    SELECT
                        m."userId",
                        ROW_NUMBER() OVER (
                            ORDER BY
                                m."totalScore" DESC,
                                -- The following case is used to put users with linked leetcode names before
                                -- those who don't.
                                CASE
                                    WHEN m."totalScore" = 0 THEN
                                        CASE WHEN u."leetcodeUsername" IS NOT NULL THEN 0 ELSE 1 END
                                    ELSE 0
                                END,
                                -- This is the tie breaker if we can't sort them by the above conditions.
                                m."createdAt" ASC,
                                -- This is extremely rare, but if the createdAt time is somehow not unique,
                                -- this serves to be the final tiebreaker.
                                m."userId"
                        ) AS rank
                    FROM "Metadata" m
                    JOIN "User" u ON u.id = m."userId"
                    WHERE m."leaderboardId" = :leaderboardId
                )
                SELECT
                    r."userId",
                    r.rank
                FROM
                    ranks r
                WHERE
                    r."userId" = ANY(:userIds)
                ORDER BY
                    r.rank ASC
            """;

        UUID[] userIds =
                users.stream().map(user -> UUID.fromString(user.getId())).toArray(size -> new UUID[size]);

        try (Connection conn = ds.getConnection()) {
            Array userIdsArray = conn.createArrayOf("UUID", userIds);
            return jdbcClient
                    .sql(sql)
                    .param("userIds", userIdsArray)
                    .param("leaderboardId", UUID.fromString(leaderboardId))
                    .query((rs, rowNum) -> {
                        var userId = rs.getString("userId");
                        var rank = rs.getInt("rank");
                        return Indexed.of(userIdToUserMap.get(userId), rank);
                    })
                    .list();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create SQL array", e);
        }
    }

    @Override
    public List<Indexed<UserWithScore>> getRankedIndexedLeaderboardUsersById(
            final String leaderboardId, final LeaderboardFilterOptions options) {
        List<UserWithScore> users = this.getLeaderboardUsersById(leaderboardId, options);
        Map<String, UserWithScore> userIdToUserMap =
                users.stream().collect(Collectors.toMap(user -> user.getId(), Function.identity()));

        String sql = """
            WITH ranks AS (
                SELECT
                    m."userId",
                    ROW_NUMBER() OVER (
                        ORDER BY
                            m."totalScore" DESC,
                            CASE
                                WHEN m."totalScore" = 0 THEN
                                    CASE WHEN u."leetcodeUsername" IS NOT NULL THEN 0 ELSE 1 END
                                ELSE 0
                            END,
                            m."createdAt" ASC,
                            m."userId"
                    ) AS rank
                FROM
                    "Metadata" m
                JOIN
                    "User" u
                ON
                    u.id = m."userId"
                JOIN
                    "Leaderboard" l
                ON
                    m."leaderboardId" = l.id
                WHERE
                    m."leaderboardId" = :leaderboardId
                AND (
                    EXISTS (
                        SELECT 1 FROM "UserTag" ut
                        WHERE ut."userId" = m."userId"
                        AND (
                            (:patina = TRUE AND ut.tag = 'Patina') OR
                            (:hunter = TRUE AND ut.tag = 'Hunter') OR
                            (:nyu = TRUE AND ut.tag = 'Nyu') OR
                            (:baruch = TRUE AND ut.tag = 'Baruch') OR
                            (:rpi = TRUE AND ut.tag = 'Rpi') OR
                            (:gwc = TRUE AND ut.tag = 'Gwc') OR
                            (:sbu = TRUE AND ut.tag = 'Sbu') OR
                            (:ccny = TRUE AND ut.tag = 'Ccny') OR
                            (:columbia = TRUE AND ut.tag = 'Columbia') OR
                            (:cornell = TRUE AND ut.tag = 'Cornell') OR
                            (:bmcc = TRUE AND ut.tag = 'Bmcc') OR
                            (:mhcplusplus = TRUE AND ut.tag = 'MHCPlusPlus')
                        )
                        AND (
                            -- Any tag is valid for current leaderboard
                            (l."deletedAt" IS NULL)
                            OR
                            -- Tag is only valid for previous leaderboards if it was created before
                            -- leaderboard started, or during the lifespan of leaderboard.
                            (l."deletedAt" IS NOT NULL AND ut."createdAt" <= l."deletedAt")
                        )
                    )
                    OR (:patina = FALSE AND :hunter = FALSE AND :nyu = FALSE AND :baruch = FALSE
                    AND :rpi = FALSE AND :gwc = FALSE AND :sbu = FALSE AND :ccny = FALSE AND :columbia = FALSE
                    AND :cornell = FALSE AND :bmcc = FALSE AND :mhcplusplus = FALSE)
                )
            )
            SELECT
                r."userId",
                r.rank
            FROM
                ranks r
            WHERE
                r."userId" = ANY(:userIds)
            ORDER BY
                r.rank ASC
                                    """;

        UUID[] userIds =
                users.stream().map(user -> UUID.fromString(user.getId())).toArray(size -> new UUID[size]);

        try (Connection conn = ds.getConnection()) {
            Array userIdsArray = conn.createArrayOf("UUID", userIds);
            return jdbcClient
                    .sql(sql)
                    .param("userIds", userIdsArray)
                    .param("leaderboardId", UUID.fromString(leaderboardId))
                    .param("patina", options.isPatina())
                    .param("hunter", options.isHunter())
                    .param("nyu", options.isNyu())
                    .param("baruch", options.isBaruch())
                    .param("rpi", options.isRpi())
                    .param("gwc", options.isGwc())
                    .param("sbu", options.isSbu())
                    .param("ccny", options.isCcny())
                    .param("columbia", options.isColumbia())
                    .param("cornell", options.isCornell())
                    .param("bmcc", options.isBmcc())
                    .param("mhcplusplus", options.isMhcplusplus())
                    .query((rs, rowNum) -> {
                        var userId = rs.getString("userId");
                        var rank = rs.getInt("rank");
                        return Indexed.of(userIdToUserMap.get(userId), rank);
                    })
                    .list();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create SQL array", e);
        }
    }

    @Override
    public Optional<Indexed<UserWithScore>> getGlobalRankedUserById(final String leaderboardId, final String userId) {
        UserWithScore user = userRepository.getUserWithScoreByIdAndLeaderboardId(
                userId, leaderboardId, UserFilterOptions.builder().build());
        if (user == null) {
            return Optional.empty();
        }

        String sql = """
            WITH ranks AS (
                SELECT
                    m."userId",
                    ROW_NUMBER() OVER (
                        ORDER BY
                            m."totalScore" DESC,
                            -- The following case is used to put users with linked leetcode names before
                            -- those who don't.
                            CASE
                                WHEN m."totalScore" = 0 THEN
                                    CASE WHEN u."leetcodeUsername" IS NOT NULL THEN 0 ELSE 1 END
                                ELSE 0
                            END,
                            -- This is the tie breaker if we can't sort them by the above conditions.
                            m."createdAt" ASC,
                            -- This is extremely rare, but if the createdAt time is somehow not unique,
                            -- this serves to be the final tiebreaker.
                            m."userId"
                    ) AS rank
                FROM "Metadata" m
                JOIN "User" u ON u.id = m."userId"
                WHERE m."leaderboardId" = :leaderboardId
            )
            SELECT
                r.rank
            FROM
                ranks r
            WHERE
                r."userId" = :userId
            """;

        Integer rank = jdbcClient
                .sql(sql)
                .param("leaderboardId", UUID.fromString(leaderboardId))
                .param("userId", UUID.fromString(userId))
                .query((rs, rowNum) -> rs.getInt("rank"))
                .optional()
                .orElse(null);

        if (rank == null) {
            return Optional.empty();
        }

        return Optional.of(Indexed.of(user, rank));
    }

    @Override
    public Optional<Indexed<UserWithScore>> getFilteredRankedUserById(
            final String leaderboardId, final String userId, final LeaderboardFilterOptions options) {
        UserWithScore user = userRepository.getUserWithScoreByIdAndLeaderboardId(
                userId, leaderboardId, UserFilterOptions.builder().build());
        if (user == null) {
            return Optional.empty();
        }

        String sql = """
            WITH ranks AS (
                SELECT
                    m."userId",
                    ROW_NUMBER() OVER (
                        ORDER BY
                            m."totalScore" DESC,
                            CASE
                                WHEN m."totalScore" = 0 THEN
                                    CASE WHEN u."leetcodeUsername" IS NOT NULL THEN 0 ELSE 1 END
                                ELSE 0
                            END,
                            m."createdAt" ASC,
                            m."userId"
                    ) AS rank
                FROM
                    "Metadata" m
                JOIN
                    "User" u
                ON
                    u.id = m."userId"
                JOIN
                    "Leaderboard" l
                ON
                    m."leaderboardId" = l.id
                WHERE
                    m."leaderboardId" = :leaderboardId
                AND (
                    EXISTS (
                        SELECT 1 FROM "UserTag" ut
                        WHERE ut."userId" = m."userId"
                        AND (
                            (:patina = TRUE AND ut.tag = 'Patina') OR
                            (:hunter = TRUE AND ut.tag = 'Hunter') OR
                            (:nyu = TRUE AND ut.tag = 'Nyu') OR
                            (:baruch = TRUE AND ut.tag = 'Baruch') OR
                            (:rpi = TRUE AND ut.tag = 'Rpi') OR
                            (:gwc = TRUE AND ut.tag = 'Gwc') OR
                            (:sbu = TRUE AND ut.tag = 'Sbu') OR
                            (:ccny = TRUE AND ut.tag = 'Ccny') OR
                            (:columbia = TRUE AND ut.tag = 'Columbia') OR
                            (:cornell = TRUE AND ut.tag = 'Cornell') OR
                            (:bmcc = TRUE AND ut.tag = 'Bmcc') OR
                            (:mhcplusplus = TRUE AND ut.tag = 'MHCPlusPlus')
                        )
                        AND (
                            -- Any tag is valid for current leaderboard
                            (l."deletedAt" IS NULL)
                            OR
                            -- Tag is only valid for previous leaderboards if it was created before
                            -- leaderboard started, or during the lifespan of leaderboard.
                            (l."deletedAt" IS NOT NULL AND ut."createdAt" <= l."deletedAt")
                        )
                    )
                    OR (:patina = FALSE AND :hunter = FALSE AND :nyu = FALSE AND :baruch = FALSE
                    AND :rpi = FALSE AND :gwc = FALSE AND :sbu = FALSE AND :ccny = FALSE AND :columbia = FALSE
                    AND :cornell = FALSE AND :bmcc = FALSE AND :mhcplusplus = FALSE)
                )
            )
            SELECT
                r.rank
            FROM
                ranks r
            WHERE
                r."userId" = :userId
            """;

        Integer rank = jdbcClient
                .sql(sql)
                .param("leaderboardId", UUID.fromString(leaderboardId))
                .param("userId", UUID.fromString(userId))
                .param("patina", options.isPatina())
                .param("hunter", options.isHunter())
                .param("nyu", options.isNyu())
                .param("baruch", options.isBaruch())
                .param("rpi", options.isRpi())
                .param("gwc", options.isGwc())
                .param("sbu", options.isSbu())
                .param("ccny", options.isCcny())
                .param("columbia", options.isColumbia())
                .param("cornell", options.isCornell())
                .param("bmcc", options.isBmcc())
                .param("mhcplusplus", options.isMhcplusplus())
                .query((rs, rowNum) -> rs.getInt("rank"))
                .optional()
                .orElse(null);

        if (rank == null) {
            return Optional.empty();
        }

        return Optional.of(Indexed.of(user, rank));
    }

    /** @deprecated This method is no longer recommended. Use {@link #getLeaderboardUsersById} instead. */
    @Deprecated
    @Override
    public List<UserWithScore> getRecentLeaderboardUsers(final LeaderboardFilterOptions options) {
        String sql = """
            WITH latest_leaderboard AS (
                SELECT
                    id
                FROM
                    "Leaderboard"
                WHERE
                    "deletedAt" IS NULL
                ORDER BY
                    "createdAt" DESC
                LIMIT 1
            )
            SELECT
                m."userId",
                ll.id as "leaderboardId",
                l."deletedAt" as "leaderboardDeletedAt"
            FROM
                "Leaderboard" l
            JOIN
                latest_leaderboard ll
            ON
                ll.id = l.id
            JOIN "Metadata" m ON
                m."leaderboardId" = ll.id
            JOIN "User" u ON
                u.id = m."userId"
            WHERE (
                EXISTS (
                    SELECT 1 FROM "UserTag" ut
                    WHERE ut."userId" = m."userId"
                    AND (
                        (:patina = TRUE AND ut.tag = 'Patina') OR
                        (:hunter = TRUE AND ut.tag = 'Hunter') OR
                        (:nyu = TRUE AND ut.tag = 'Nyu') OR
                        (:baruch = TRUE AND ut.tag = 'Baruch') OR
                        (:rpi = TRUE AND ut.tag = 'Rpi') OR
                        (:gwc = TRUE AND ut.tag = 'Gwc') OR
                        (:sbu = TRUE AND ut.tag = 'Sbu') OR
                        (:ccny = TRUE AND ut.tag = 'Ccny') OR
                        (:columbia = TRUE AND ut.tag = 'Columbia') OR
                        (:cornell = TRUE AND ut.tag = 'Cornell') OR
                        (:bmcc = TRUE AND ut.tag = 'Bmcc') OR
                        (:mhcplusplus = TRUE AND ut.tag = 'MHCPlusPlus')
                    )
                    AND (
                        -- Any tag is valid for current leaderboard
                        (l."deletedAt" IS NULL)
                        OR
                        -- Tag is only valid for previous leaderboards if it was created before
                        -- leaderboard started, or during the lifespan of leaderboard.
                        (l."deletedAt" IS NOT NULL AND ut."createdAt" <= l."deletedAt")
                    )
                )
                OR (:patina = FALSE AND :hunter = FALSE AND :nyu = FALSE AND :baruch = FALSE
                AND :rpi = FALSE AND :gwc = FALSE AND :sbu = FALSE AND :ccny = FALSE AND :columbia = FALSE
                AND :cornell = FALSE AND :bmcc = FALSE AND :mhcplusplus = FALSE)
            )
            AND
                (u."discordName" ILIKE :searchQuery OR u."leetcodeUsername" ILIKE :searchQuery OR u."nickname" ILIKE :searchQuery)
            ORDER BY
                m."totalScore" DESC,
                -- The following case is used to put users with linked leetcode names before
                -- those who don't.
                CASE
                    WHEN m."totalScore" = 0 THEN
                        CASE WHEN u."leetcodeUsername" IS NOT NULL THEN 0 ELSE 1 END
                    ELSE 0
                END,
                -- This is the tie breaker if we can't sort them by the above conditions.
                m."createdAt" ASC,
                -- This is extremely rare, but if the createdAt time is somehow not unique,
                -- this serves to be the final tiebreaker.
                m."userId"
            LIMIT :pageSize OFFSET :pageNumber;
                            """;

        List<UserWithScore> users = jdbcClient
                .sql(sql)
                .param("patina", options.isPatina())
                .param("hunter", options.isHunter())
                .param("nyu", options.isNyu())
                .param("baruch", options.isBaruch())
                .param("rpi", options.isRpi())
                .param("gwc", options.isGwc())
                .param("sbu", options.isSbu())
                .param("ccny", options.isCcny())
                .param("columbia", options.isColumbia())
                .param("cornell", options.isCornell())
                .param("bmcc", options.isBmcc())
                .param("mhcplusplus", options.isMhcplusplus())
                .param("searchQuery", "%" + options.getQuery() + "%")
                .param("pageSize", options.getPageSize())
                .param("pageNumber", (options.getPage() - 1) * options.getPageSize())
                .query((rs, rowNum) -> {
                    var userId = rs.getString("userId");
                    var leaderboardId = rs.getString("leaderboardId");
                    var leaderboardDeletedAt = rs.getObject("leaderboardDeletedAt", OffsetDateTime.class);

                    return userRepository.getUserWithScoreByIdAndLeaderboardId(
                            userId,
                            leaderboardId,
                            UserFilterOptions.builder()
                                    .pointOfTime(leaderboardDeletedAt)
                                    .build());
                })
                .list();

        return users.stream().filter(user -> user != null).collect(Collectors.toList());
    }

    @Override
    public List<UserWithScore> getLeaderboardUsersById(final String id, final LeaderboardFilterOptions options) {
        String sql = """
            SELECT
                m."userId",
                l.id as "leaderboardId",
                l."deletedAt" as "leaderboardDeletedAt"
            FROM
                "Leaderboard" l
            JOIN "Metadata" m ON
                m."leaderboardId" = l.id
            JOIN "User" u ON
                u.id = m."userId"
            WHERE
                l.id = :leaderboardId
            AND (
                EXISTS (
                    SELECT 1 FROM "UserTag" ut
                    WHERE ut."userId" = m."userId"
                    AND (
                        (:patina = TRUE AND ut.tag = 'Patina') OR
                        (:hunter = TRUE AND ut.tag = 'Hunter') OR
                        (:nyu = TRUE AND ut.tag = 'Nyu') OR
                        (:baruch = TRUE AND ut.tag = 'Baruch') OR
                        (:rpi = TRUE AND ut.tag = 'Rpi') OR
                        (:gwc = TRUE AND ut.tag = 'Gwc') OR
                        (:sbu = TRUE AND ut.tag = 'Sbu') OR
                        (:ccny = TRUE AND ut.tag = 'Ccny') OR
                        (:columbia = TRUE AND ut.tag = 'Columbia') OR
                        (:cornell = TRUE AND ut.tag = 'Cornell') OR
                        (:bmcc = TRUE AND ut.tag = 'Bmcc') OR
                        (:mhcplusplus = TRUE AND ut.tag = 'MHCPlusPlus')
                    )
                    AND (
                        -- Any tag is valid for current leaderboard
                        (l."deletedAt" IS NULL)
                        OR
                        -- Tag is only valid for previous leaderboards if it was created before
                        -- leaderboard started, or during the lifespan of leaderboard.
                        (l."deletedAt" IS NOT NULL AND ut."createdAt" <= l."deletedAt")
                    )
                )
                OR (:patina = FALSE AND :hunter = FALSE AND :nyu = FALSE AND :baruch = FALSE
                AND :rpi = FALSE AND :gwc = FALSE AND :sbu = FALSE AND :ccny = FALSE AND :columbia = FALSE
                AND :cornell = FALSE AND :bmcc = FALSE AND :mhcplusplus = FALSE)
            )
            AND
                (u."discordName" ILIKE :searchQuery OR u."leetcodeUsername" ILIKE :searchQuery OR u."nickname" ILIKE :searchQuery)
            ORDER BY
                m."totalScore" DESC,
                -- The following case is used to put users with linked leetcode names before
                -- those who don't.
                CASE
                    WHEN m."totalScore" = 0 THEN
                        CASE WHEN u."leetcodeUsername" IS NOT NULL THEN 0 ELSE 1 END
                    ELSE 0
                END,
                 -- This is the tie breaker if we can't sort them by the above conditions.
                m."createdAt" ASC,
                -- This is extremely rare, but if the createdAt time is somehow not unique,
                -- this serves to be the final tiebreaker.
                m."userId"
            LIMIT :pageSize OFFSET :pageNumber;
                            """;

        List<UserWithScore> users = jdbcClient
                .sql(sql)
                .param("leaderboardId", UUID.fromString(id))
                .param("patina", options.isPatina())
                .param("hunter", options.isHunter())
                .param("nyu", options.isNyu())
                .param("baruch", options.isBaruch())
                .param("rpi", options.isRpi())
                .param("gwc", options.isGwc())
                .param("sbu", options.isSbu())
                .param("ccny", options.isCcny())
                .param("columbia", options.isColumbia())
                .param("cornell", options.isCornell())
                .param("bmcc", options.isBmcc())
                .param("mhcplusplus", options.isMhcplusplus())
                .param("searchQuery", "%" + options.getQuery() + "%")
                .param("pageSize", options.getPageSize())
                .param("pageNumber", (options.getPage() - 1) * options.getPageSize())
                .query((rs, rowNum) -> {
                    var userId = rs.getString("userId");
                    var leaderboardId = rs.getString("leaderboardId");
                    var leaderboardDeletedAt = rs.getObject("leaderboardDeletedAt", OffsetDateTime.class);

                    return userRepository.getUserWithScoreByIdAndLeaderboardId(
                            userId,
                            leaderboardId,
                            UserFilterOptions.builder()
                                    .pointOfTime(leaderboardDeletedAt)
                                    .build());
                })
                .list();

        return users.stream().filter(user -> user != null).collect(Collectors.toList());
    }

    @Override
    public boolean updateLeaderboard(final Leaderboard leaderboard) {
        String sql = """
            UPDATE "Leaderboard"
            SET
                name = :name,
                "createdAt" = :createdAt,
                "deletedAt" = :deletedAt,
                "shouldExpireBy" = :shouldExpireBy,
                "syntaxHighlightingLanguage" = :syntaxHighlightingLanguage
            WHERE id = :id
            """;

        int rowsAffected = jdbcClient
                .sql(sql)
                .param("name", leaderboard.getName())
                .param("createdAt", leaderboard.getCreatedAt())
                .param("deletedAt", leaderboard.getDeletedAt().orElse(null))
                .param(SHOULD_EXPIRE_BY, leaderboard.getShouldExpireBy().orElse(null))
                .param("id", UUID.fromString(leaderboard.getId()))
                .param(
                        "syntaxHighlightingLanguage",
                        leaderboard.getSyntaxHighlightingLanguage().orElse(null))
                .update();

        return rowsAffected > 0;
    }

    @Override
    public boolean addUserToLeaderboard(final String userId, final String leaderboardId) {
        String sql = """
            INSERT INTO "Metadata"
                (id, "userId", "leaderboardId")
            VALUES
                (:id, :userId, :leaderboardId)
            """;

        int rowsAffected = jdbcClient
                .sql(sql)
                .param("id", UUID.randomUUID())
                .param("userId", UUID.fromString(userId))
                .param("leaderboardId", UUID.fromString(leaderboardId))
                .update();

        return rowsAffected > 0;
    }

    @Override
    public boolean updateUserPointsFromLeaderboard(
            final String leaderboardId, final String userId, final int totalScore) {
        String sql = """
            UPDATE "Metadata"
            SET
                "totalScore" = :totalScore
            WHERE
                "userId" = :userId
            AND
                "leaderboardId" = :leaderboardId
            """;

        int rowsAffected = jdbcClient
                .sql(sql)
                .param("totalScore", totalScore)
                .param("userId", UUID.fromString(userId))
                .param("leaderboardId", UUID.fromString(leaderboardId))
                .update();

        return rowsAffected > 0;
    }

    /** @deprecated This method is no longer recommended. Use {@link #getLeaderboardUserCountById} instead. */
    @Deprecated
    @Override
    public int getRecentLeaderboardUserCount(final LeaderboardFilterOptions options) {
        String sql = """
                WITH latest_leaderboard AS (
                    SELECT id
                    FROM "Leaderboard"
                    WHERE "deletedAt" IS NULL
                    ORDER BY "createdAt" DESC
                    LIMIT 1
                )
                SELECT
                    COUNT(m.id)
                FROM
                    "Leaderboard" l
                INNER JOIN latest_leaderboard ON latest_leaderboard.id = l.id
                JOIN
                    "Metadata" m
                ON
                    m."leaderboardId" = l.id
                JOIN
                    "User" u
                ON
                    u.id = m."userId"
                WHERE (
                    EXISTS (
                        SELECT 1 FROM "UserTag" ut
                        WHERE ut."userId" = m."userId"
                        AND (
                            (:patina = TRUE AND ut.tag = 'Patina') OR
                            (:hunter = TRUE AND ut.tag = 'Hunter') OR
                            (:nyu = TRUE AND ut.tag = 'Nyu') OR
                            (:baruch = TRUE AND ut.tag = 'Baruch') OR
                            (:rpi = TRUE AND ut.tag = 'Rpi') OR
                            (:gwc = TRUE AND ut.tag = 'Gwc') OR
                            (:sbu = TRUE AND ut.tag = 'Sbu') OR
                            (:ccny = TRUE AND ut.tag = 'Ccny') OR
                            (:columbia = TRUE AND ut.tag = 'Columbia') OR
                            (:cornell = TRUE AND ut.tag = 'Cornell') OR
                            (:bmcc = TRUE AND ut.tag = 'Bmcc') OR
                            (:mhcplusplus = TRUE AND ut.tag = 'MHCPlusPlus')
                        )
                        AND (
                            -- Any tag is valid for current leaderboard
                            (l."deletedAt" IS NULL)
                            OR
                            -- Tag is only valid for previous leaderboards if it was created before
                            -- leaderboard started, or during the lifespan of leaderboard.
                            (l."deletedAt" IS NOT NULL AND ut."createdAt" <= l."deletedAt")
                        )
                    )
                    OR (:patina = FALSE AND :hunter = FALSE AND :nyu = FALSE AND :baruch = FALSE
                    AND :rpi = FALSE AND :gwc = FALSE AND :sbu = FALSE AND :ccny = FALSE AND :columbia = FALSE
                    AND :cornell = FALSE AND :bmcc = FALSE AND :mhcplusplus = FALSE)
                )
                AND
                    (u."discordName" ILIKE :searchQuery OR u."leetcodeUsername" ILIKE :searchQuery)
            """;

        return jdbcClient
                .sql(sql)
                .param("patina", options.isPatina())
                .param("hunter", options.isHunter())
                .param("nyu", options.isNyu())
                .param("baruch", options.isBaruch())
                .param("rpi", options.isRpi())
                .param("gwc", options.isGwc())
                .param("sbu", options.isSbu())
                .param("ccny", options.isCcny())
                .param("columbia", options.isColumbia())
                .param("cornell", options.isCornell())
                .param("bmcc", options.isBmcc())
                .param("mhcplusplus", options.isMhcplusplus())
                .param("searchQuery", "%" + options.getQuery() + "%")
                .query((rs, rowNum) -> rs.getInt(1))
                .optional()
                .orElse(0);
    }

    @Override
    public int getLeaderboardUserCountById(final String id, final LeaderboardFilterOptions options) {
        String sql = """
                SELECT
                    COUNT(m.id)
                FROM
                    "Leaderboard" l
                JOIN
                    "Metadata" m
                ON
                    m."leaderboardId" = l.id
                JOIN
                    "User" u
                ON
                    u.id = m."userId"
                WHERE
                    l.id = :leaderboardId
                AND (
                    EXISTS (
                        SELECT 1 FROM "UserTag" ut
                        WHERE ut."userId" = m."userId"
                        AND (
                            (:patina = TRUE AND ut.tag = 'Patina') OR
                            (:hunter = TRUE AND ut.tag = 'Hunter') OR
                            (:nyu = TRUE AND ut.tag = 'Nyu') OR
                            (:baruch = TRUE AND ut.tag = 'Baruch') OR
                            (:rpi = TRUE AND ut.tag = 'Rpi') OR
                            (:gwc = TRUE AND ut.tag = 'Gwc') OR
                            (:sbu = TRUE AND ut.tag = 'Sbu') OR
                            (:ccny = TRUE AND ut.tag = 'Ccny') OR
                            (:columbia = TRUE AND ut.tag = 'Columbia') OR
                            (:cornell = TRUE AND ut.tag = 'Cornell') OR
                            (:bmcc = TRUE AND ut.tag = 'Bmcc') OR
                            (:mhcplusplus = TRUE AND ut.tag = 'MHCPlusPlus')
                        )
                        AND (
                            -- Any tag is valid for current leaderboard
                            (l."deletedAt" IS NULL)
                            OR
                            -- Tag is only valid for previous leaderboards if it was created before
                            -- leaderboard started, or during the lifespan of leaderboard.
                            (l."deletedAt" IS NOT NULL AND ut."createdAt" <= l."deletedAt")
                        )
                    )
                    OR (:patina = FALSE AND :hunter = FALSE AND :nyu = FALSE AND :baruch = FALSE
                    AND :rpi = FALSE AND :gwc = FALSE AND :sbu = FALSE AND :ccny = FALSE AND :columbia = FALSE
                    AND :cornell = FALSE AND :bmcc = FALSE AND :mhcplusplus = FALSE)
                )
                AND
                    (u."discordName" ILIKE :searchQuery OR u."leetcodeUsername" ILIKE :searchQuery)
            """;

        return jdbcClient
                .sql(sql)
                .param("leaderboardId", UUID.fromString(id))
                .param("patina", options.isPatina())
                .param("hunter", options.isHunter())
                .param("nyu", options.isNyu())
                .param("baruch", options.isBaruch())
                .param("rpi", options.isRpi())
                .param("gwc", options.isGwc())
                .param("sbu", options.isSbu())
                .param("ccny", options.isCcny())
                .param("columbia", options.isColumbia())
                .param("cornell", options.isCornell())
                .param("bmcc", options.isBmcc())
                .param("mhcplusplus", options.isMhcplusplus())
                .param("searchQuery", "%" + options.getQuery() + "%")
                .query((rs, rowNum) -> rs.getInt(1))
                .optional()
                .orElse(0);
    }

    @Override
    public List<Leaderboard> getAllLeaderboardsShallow(final LeaderboardFilterOptions options) {
        String sql = """
                SELECT
                    id,
                    name,
                    "createdAt",
                    "deletedAt",
                    "shouldExpireBy",
                    "syntaxHighlightingLanguage"
                FROM "Leaderboard"
                WHERE name ILIKE :searchQuery
                ORDER BY
                    "createdAt" DESC
                LIMIT :pageSize OFFSET :pageNumber
            """;

        return jdbcClient
                .sql(sql)
                .param("searchQuery", "%" + options.getQuery() + "%")
                .param("pageSize", options.getPageSize())
                .param("pageNumber", (options.getPage() - 1) * options.getPageSize())
                .query(LEADERBOARD_ROW_MAPPER)
                .list();
    }

    @Override
    public boolean addAllUsersToLeaderboard(final String leaderboardId) {
        var users = userRepository.getAllUsers();
        String sql = """
            INSERT INTO "Metadata"
                (id, "userId", "leaderboardId")
            VALUES
                (:id, :userId, :leaderboardId)
            """;

        MapSqlParameterSource[] paramSources = users.stream()
                .map(user -> new MapSqlParameterSource()
                        .addValue("id", UUID.fromString(UUID.randomUUID().toString()))
                        .addValue("userId", UUID.fromString(user.getId()))
                        .addValue("leaderboardId", UUID.fromString(leaderboardId)))
                .toArray(MapSqlParameterSource[]::new);

        int[] updates = namedParameterJdbcTemplate.batchUpdate(sql, paramSources);
        long successfulInsertions =
                Arrays.stream(updates).filter(count -> count > 0).count();
        return successfulInsertions == users.size();
    }

    @Override
    public int getLeaderboardCount() {
        String sql = """
            SELECT
                COUNT(*)
            FROM
                "Leaderboard"
            """;

        return jdbcClient
                .sql(sql)
                .query((rs, rowNum) -> rs.getInt(1))
                .optional()
                .orElse(0);
    }

    /** Internal use only. Intended for testing use cases (access via reflection). */
    private boolean deleteLeaderboardById(final String id) {
        String sql = """
                DELETE FROM "Leaderboard"
                WHERE
                    id = :id
            """;

        int rowsAffected = jdbcClient.sql(sql).param("id", UUID.fromString(id)).update();

        return rowsAffected > 0;
    }

    /**
     * Internal use only. Intended for testing use cases (access via reflection).
     *
     * @note This will only re-activate a leaderboard if it's the most recent leaderboard entry.
     */
    private boolean enableLeaderboardById(final String id) {
        String sql = """
                UPDATE "Leaderboard"
                SET
                    "deletedAt" = NULL
                WHERE
                    id = :id
            """;

        int rowsAffected = jdbcClient.sql(sql).param("id", UUID.fromString(id)).update();

        return rowsAffected > 0;
    }
}
