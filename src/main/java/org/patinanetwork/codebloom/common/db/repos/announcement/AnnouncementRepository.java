package org.patinanetwork.codebloom.common.db.repos.announcement;

import java.util.List;
import java.util.Optional;
import org.patinanetwork.codebloom.common.db.models.announcement.Announcement;

public interface AnnouncementRepository {
    List<Announcement> getAllAnnouncements();

    Optional<Announcement> getAnnouncementById(String id);

    Optional<Announcement> getRecentAnnouncement();

    /**
     * @note The id property of the object will be overriden.
     * @note the returned boolean indicates if the operation was a success or not.
     */
    boolean createAnnouncement(Announcement announcement);

    boolean deleteAnnouncementById(String id);

    boolean updateAnnouncement(Announcement announcement);
}
