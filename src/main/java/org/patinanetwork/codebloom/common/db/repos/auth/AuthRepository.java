package org.patinanetwork.codebloom.common.db.repos.auth;

import java.util.Optional;
import org.patinanetwork.codebloom.common.db.models.auth.Auth;

public interface AuthRepository {
    /**
     * NOTE - Modifies the passed in Auth object and overrides any new properties from the database.
     *
     * @param auth - required fields:
     *     <ul>
     *       <li>token
     *       <li>csrf
     *     </ul>
     */
    void createAuth(Auth auth);

    /**
     * NOTE - Modifies the passed in Auth object and overrides any new properties from the database.
     *
     * @param auth - overridden fields:
     *     <ul>
     *       <li>token
     *       <li>csrf
     *     </ul>
     */
    boolean updateAuthById(Auth auth);

    Optional<Auth> getAuthById(String id);

    Optional<Auth> getMostRecentAuth();

    boolean deleteAuthById(String id);
}
