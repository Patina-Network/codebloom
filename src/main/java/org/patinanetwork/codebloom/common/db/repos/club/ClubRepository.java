package org.patinanetwork.codebloom.common.db.repos.club;

import java.util.Optional;
import org.patinanetwork.codebloom.common.db.models.club.Club;

public interface ClubRepository {
    /**
     * Creates a new club in the database.
     *
     * @note - The provided object's methods will be overridden with any returned data from the database.
     * @param club - required fields:
     *     <ul>
     *       <li>name
     *       <li>slug
     *       <li>password
     *       <li>tag
     *     </ul>
     */
    void createClub(Club club);

    /**
     * Updates an existing club by its ID.
     *
     * @note - The provided object's methods will be overridden with any returned data from the database.
     * @param club - overridden fields:
     *     <ul>
     *       <li>name
     *       <li>description
     *       <li>splashIconUrl
     *       <li>password
     *       <li>tag
     *     </ul>
     *
     * @return the updated club if found, or an empty Optional if no matching row exists
     */
    Optional<Club> updateClub(Club club);

    Optional<Club> getClubById(String id);

    Optional<Club> getClubBySlug(String slug);

    boolean deleteClubBySlug(String slug);

    boolean deleteClubById(String id);
}
