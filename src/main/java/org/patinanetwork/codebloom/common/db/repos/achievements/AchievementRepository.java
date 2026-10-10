package org.patinanetwork.codebloom.common.db.repos.achievements;

import java.util.List;
import java.util.Optional;
import org.patinanetwork.codebloom.common.db.models.achievements.Achievement;

public interface AchievementRepository {
    /**
     * Creates a new achievement in the database.
     *
     * @note - The provided object's methods will be overridden with any returned data from the database.
     * @param achievement - required fields:
     *     <ul>
     *       <li>userId
     *       <li>iconUrl
     *       <li>title
     *       <li>description
     *       <li>isActive
     *       <li>leaderboardId
     *     </ul>
     */
    void createAchievement(Achievement achievement);
    /**
     * Updates an existing achievement by its ID.
     *
     * @note - The provided object's methods will be overridden with any returned data from the database.
     * @param achievement - overridden fields:
     *     <ul>
     *       <li>iconUrl
     *       <li>title
     *       <li>description
     *       <li>isActive
     *       <li>deletedAt
     *       <li>leaderboardId
     *     </ul>
     *
     * @return the updated achievement if found, or an empty Optional if no matching row exists
     */
    Optional<Achievement> updateAchievement(Achievement achievement);

    boolean deleteAchievementById(String id);

    Optional<Achievement> getAchievementById(String id);

    List<Achievement> getAchievementsByUserId(String userId);
}
