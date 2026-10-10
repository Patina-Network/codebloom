package org.patinanetwork.codebloom.common.db.repos.weekly;

import java.util.Optional;
import org.patinanetwork.codebloom.common.db.models.weekly.WeeklyMessage;

public interface WeeklyMessageRepository {
    Optional<WeeklyMessage> getLatestWeeklyMessage();

    Optional<WeeklyMessage> getWeeklyMessageById(String id);

    /**
     * @note - The provided object's methods will be overriden with any returned data from the database.
     * @param message - required fields:
     *     <ul>
     *     </ul>
     */
    boolean createLatestWeeklyMessage(WeeklyMessage message);

    /**
     * If you have no purpose for the newly created object, just use this.
     *
     * @note - The provided object's methods will be overriden with any returned data from the database.
     * @param message - required fields:
     *     <ul>
     *     </ul>
     */
    boolean createLatestWeeklyMessage();

    boolean deleteWeeklyMessageById(String id);

    boolean deleteLatestWeeklyMessage();
}
