package org.patinanetwork.codebloom.common.db.repos.auth;

import java.util.Optional;
import org.patinanetwork.codebloom.common.db.models.auth.Auth;

@Deprecated
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
    @Deprecated
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
    @Deprecated
    boolean updateAuthById(Auth auth);

    @Deprecated
    Optional<Auth> getAuthById(String id);

    @Deprecated
    Optional<Auth> getMostRecentAuth();

    @Deprecated
    boolean deleteAuthById(String id);
}
