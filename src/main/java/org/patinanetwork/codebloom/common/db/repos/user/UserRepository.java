package org.patinanetwork.codebloom.common.db.repos.user;

import java.util.ArrayList;
import java.util.Optional;
import org.patinanetwork.codebloom.common.db.models.user.User;
import org.patinanetwork.codebloom.common.db.models.user.UserWithScore;
import org.patinanetwork.codebloom.common.db.repos.user.options.UserFilterOptions;

public interface UserRepository {
    /**
     * @note - The provided object's methods will be overridden with any returned data from the database.
     * @param user - required fields:
     *     <ul>
     *       <li>discordId
     *       <li>discordName
     *     </ul>
     */
    void createUser(User user);

    /**
     * @note - The provided object's methods will be overridden with any returned data from the database.
     * @param user - overridden fields:
     *     <ul>
     *       <li>discordName
     *       <li>discordId
     *       <li>leetcodeUsername
     *       <li>nickname
     *       <li>admin
     *       <li>profileUrl
     *       <li>schoolEmail
     *     </ul>
     */
    boolean updateUser(User user);

    Optional<User> getUserById(String id);

    Optional<User> getUserByLeetcodeUsername(String leetcodeUsername);

    Optional<UserWithScore> getUserWithScoreByIdAndLeaderboardId(
            String userId, String leaderboardId, UserFilterOptions options);

    Optional<UserWithScore> getUserWithScoreByLeetcodeUsernameAndLeaderboardId(
            String userLeetcodeUsername, String leaderboardId);

    Optional<User> getUserByDiscordId(String discordId);

    int getUserCount();

    int getUserCount(String query);

    ArrayList<User> getAllUsers();

    ArrayList<User> getAllUsers(int page, int pageSize, String query);

    boolean userExistsByLeetcodeUsername(String leetcodeUsername);

    boolean deleteUserById(String id);
}
