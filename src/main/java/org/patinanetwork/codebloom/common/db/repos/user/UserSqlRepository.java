package org.patinanetwork.codebloom.common.db.repos.user;

import java.util.ArrayList;
import java.util.UUID;
import org.patinanetwork.codebloom.common.db.models.user.User;
import org.patinanetwork.codebloom.common.db.models.user.UserWithScore;
import org.patinanetwork.codebloom.common.db.repos.achievements.AchievementRepository;
import org.patinanetwork.codebloom.common.db.repos.user.options.UserFilterOptions;
import org.patinanetwork.codebloom.common.db.repos.usertag.UserTagRepository;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
public class UserSqlRepository implements UserRepository {

    private final JdbcClient jdbcClient;
    private final UserTagRepository userTagRepository;
    private final AchievementRepository achievementRepository;

    private final RowMapper<User> userRowMapper;
    private final RowMapper<UserWithScore> userWithScoreRowMapper;

    public UserSqlRepository(
            final JdbcClient jdbcClient,
            final UserTagRepository userTagRepository,
            final AchievementRepository achievementRepository) {
        this.jdbcClient = jdbcClient;
        this.userTagRepository = userTagRepository;
        this.achievementRepository = achievementRepository;

        this.userRowMapper = (rs, rowNum) -> {
            var id = rs.getString("id");
            return User.builder()
                    .id(id)
                    .discordId(rs.getString("discordId"))
                    .discordName(rs.getString("discordName"))
                    .leetcodeUsername(rs.getString("leetcodeUsername"))
                    .nickname(rs.getString("nickname"))
                    .verifyKey(rs.getString("verifyKey"))
                    .admin(rs.getBoolean("admin"))
                    .schoolEmail(rs.getString("schoolEmail"))
                    .profileUrl(rs.getString("profileUrl"))
                    .tags(this.userTagRepository.findTagsByUserId(id))
                    .achievements(this.achievementRepository.getAchievementsByUserId(id))
                    .build();
        };

        this.userWithScoreRowMapper = (rs, rowNum) -> {
            var id = rs.getString("id");
            return UserWithScore.builder()
                    .id(id)
                    .discordId(rs.getString("discordId"))
                    .discordName(rs.getString("discordName"))
                    .leetcodeUsername(rs.getString("leetcodeUsername"))
                    .nickname(rs.getString("nickname"))
                    .verifyKey(rs.getString("verifyKey"))
                    .admin(rs.getBoolean("admin"))
                    .schoolEmail(rs.getString("schoolEmail"))
                    .profileUrl(rs.getString("profileUrl"))
                    .tags(this.userTagRepository.findTagsByUserId(id))
                    .achievements(this.achievementRepository.getAchievementsByUserId(id))
                    .totalScore(rs.getInt("totalScore"))
                    .build();
        };
    }

    /**
     * @implNote - You can not set tags on a new user. Create the user, and if the returned user is not null, you can
     *     use updateUserTagById from {@link UserTagRepository}
     */
    @Override
    public void createUser(final User user) {
        String sql = """
            INSERT INTO "User"
                (id, "discordName", "discordId","leetcodeUsername","nickname","schoolEmail",admin, "profileUrl")
            VALUES
                (:id, :discordName, :discordId, :leetcodeUsername, :nickname, :schoolEmail, :admin , :profileUrl)
            RETURNING
                "verifyKey"
            """;
        user.setId(UUID.randomUUID().toString());

        String verifyKey = jdbcClient
                .sql(sql)
                .param("id", UUID.fromString(user.getId()))
                .param("discordName", user.getDiscordName())
                .param("discordId", user.getDiscordId())
                .param("leetcodeUsername", user.getLeetcodeUsername())
                .param("nickname", user.getNickname())
                .param("schoolEmail", user.getSchoolEmail())
                .param("admin", user.isAdmin())
                .param("profileUrl", user.getProfileUrl())
                .query((rs, rowNum) -> rs.getString("verifyKey"))
                .optional()
                .orElse(null);

        user.setVerifyKey(verifyKey);
    }

    @Override
    public User getUserById(final String inputId) {
        String sql = """
            SELECT
                id,
                "discordId",
                "discordName",
                "leetcodeUsername",
                "nickname",
                "schoolEmail",
                admin,
                "verifyKey",
                "profileUrl"
            FROM "User"
            WHERE
                id=:id
            """;

        return jdbcClient
                .sql(sql)
                .param("id", UUID.fromString(inputId))
                .query(userRowMapper)
                .optional()
                .orElse(null);
    }

    @Override
    public User getUserByLeetcodeUsername(final String inputLeetcodeUsername) {
        String sql = """
                SELECT
                    id,
                    "discordId",
                    "discordName",
                    "leetcodeUsername",
                    "nickname",
                    "schoolEmail",
                    admin,
                    "verifyKey",
                    "profileUrl"
                FROM "User"
                WHERE "leetcodeUsername" = :leetcodeUsername
            """;

        return jdbcClient
                .sql(sql)
                .param("leetcodeUsername", inputLeetcodeUsername)
                .query(userRowMapper)
                .optional()
                .orElse(null);
    }

    @Override
    public User getUserByDiscordId(final String inputDiscordId) {
        String sql = """
            SELECT
                id,
                "discordId",
                "discordName",
                "leetcodeUsername",
                "nickname",
                "schoolEmail",
                admin,
                "verifyKey",
                "profileUrl"
            FROM "User"
            WHERE
                "discordId" = :discordId
            """;

        return jdbcClient
                .sql(sql)
                .param("discordId", inputDiscordId)
                .query(userRowMapper)
                .optional()
                .orElse(null);
    }

    @Override
    public int getUserCount() {
        String sql = "SELECT COUNT(*) FROM \"User\"";

        return jdbcClient.sql(sql).query((rs, rowNum) -> rs.getInt(1)).single();
    }

    @Override
    public boolean updateUser(final User inputUser) {
        String sql = """
            UPDATE "User"
            SET
                "discordName" = :discordName,
                "discordId" = :discordId,
                "leetcodeUsername" = :leetcodeUsername,
                "nickname" = :nickname,
                "admin" = :admin,
                "profileUrl"= :profileUrl,
                "schoolEmail" = :schoolEmail
            WHERE
                id = :id
            """;

        // We don't care what this actually returns, it can never be more than 1 anyways
        // because id is UNIQUE. Just return the new user every time if we want to do
        // any work on it.
        int rowsAffected = jdbcClient
                .sql(sql)
                .param("id", UUID.fromString(inputUser.getId()))
                .param("discordName", inputUser.getDiscordName())
                .param("discordId", inputUser.getDiscordId())
                .param("leetcodeUsername", inputUser.getLeetcodeUsername())
                .param("nickname", inputUser.getNickname())
                .param("admin", inputUser.isAdmin())
                .param("profileUrl", inputUser.getProfileUrl())
                .param("schoolEmail", inputUser.getSchoolEmail())
                .update();

        return rowsAffected > 0;
    }

    @Override
    public ArrayList<User> getAllUsers() {
        String sql = """
            SELECT
                id,
                "discordId",
                "discordName",
                "leetcodeUsername",
                "nickname",
                "schoolEmail",
                admin,
                "verifyKey",
                "profileUrl"
            FROM "User"
            """;

        return new ArrayList<>(jdbcClient.sql(sql).query(userRowMapper).list());
    }

    @Override
    public ArrayList<User> getAllUsers(final int page, final int pageSize, final String query) {
        String sql = """
                SELECT
                    id,
                    "discordId",
                    "discordName",
                    "leetcodeUsername",
                    "nickname",
                    "schoolEmail",
                    admin,
                    "verifyKey",
                    "profileUrl"
                FROM
                    "User"
                WHERE
                    ("discordName" ILIKE :query OR "leetcodeUsername" ILIKE :query OR "nickname" ILIKE :query)
                ORDER BY
                    id
                LIMIT :limit OFFSET :offset
            """;

        return new ArrayList<>(jdbcClient
                .sql(sql)
                .param("query", "%" + query + "%")
                .param("limit", pageSize)
                .param("offset", (page - 1) * pageSize)
                .query(userRowMapper)
                .list());
    }

    @Override
    public boolean userExistsByLeetcodeUsername(final String leetcodeUsername) {
        String sql = """
            SELECT
                1
            FROM "User"
            WHERE "leetcodeUsername" = ?
            LIMIT 1
            """;

        return jdbcClient
                .sql(sql)
                .param(1, leetcodeUsername)
                .query((rs, rowNum) -> 1)
                .optional()
                .isPresent();
    }

    @Override
    public UserWithScore getUserWithScoreByIdAndLeaderboardId(
            final String userId, final String leaderboardId, final UserFilterOptions options) {
        String sql = """
                SELECT
                    u.id,
                    u."discordId",
                    u."discordName",
                    u."leetcodeUsername",
                    u."nickname",
                    u."schoolEmail",
                    u.admin,
                    u."verifyKey",
                    u."profileUrl",
                    m."totalScore"
                FROM
                    "User" u
                JOIN "Metadata" m ON m."userId" = u.id
                WHERE
                    u.id = :id
                    AND
                    m."leaderboardId" = :leaderboardId
            """;

        return jdbcClient
                .sql(sql)
                .param("id", UUID.fromString(userId))
                .param("leaderboardId", UUID.fromString(leaderboardId))
                .query(userWithScoreRowMapper)
                .optional()
                .orElse(null);
    }

    @Override
    public UserWithScore getUserWithScoreByLeetcodeUsernameAndLeaderboardId(
            final String userLeetcodeUsername, final String leaderboardId) {
        String sql = """
                SELECT
                    u.id,
                    u."discordId",
                    u."discordName",
                    u."leetcodeUsername",
                    u.nickname,
                    u.admin,
                    u."profileUrl",
                    u."verifyKey",
                    m."totalScore"
                FROM
                    "User" u
                JOIN "Metadata" m ON m."userLeetcodeUsername" = u."leetcodeUsername"
                WHERE
                    u."leetcodeUsername" = :leetcodeUsername
                    AND
                    m."leaderboardId" = :leaderboardId
            """;

        return jdbcClient
                .sql(sql)
                .param("leetcodeUsername", userLeetcodeUsername)
                .param("leaderboardId", UUID.fromString(leaderboardId))
                .query(userWithScoreRowMapper)
                .optional()
                .orElse(null);
    }

    @Override
    public int getUserCount(final String query) {
        String sql = """
            SELECT
                COUNT(*)
            FROM
                "User"
            WHERE
                ("discordName" ILIKE :query OR "leetcodeUsername" ILIKE :query OR "nickname" ILIKE :query)
            """;

        return jdbcClient
                .sql(sql)
                .param("query", "%" + query + "%")
                .query((rs, rowNum) -> rs.getInt(1))
                .single();
    }

    @Override
    public boolean deleteUserById(final String id) {
        String sql = """
                DELETE FROM "User"
                WHERE
                    id = :id
            """;

        int rowsAffected = jdbcClient.sql(sql).param("id", UUID.fromString(id)).update();

        return rowsAffected > 0;
    }
}
