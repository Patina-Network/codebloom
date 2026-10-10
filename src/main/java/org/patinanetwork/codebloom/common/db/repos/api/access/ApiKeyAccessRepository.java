package org.patinanetwork.codebloom.common.db.repos.api.access;

import java.util.List;
import java.util.Optional;
import org.patinanetwork.codebloom.common.db.models.api.access.ApiKeyAccess;

public interface ApiKeyAccessRepository {
    Optional<ApiKeyAccess> getApiKeyAccessById(String id);

    List<ApiKeyAccess> getApiKeyAccessesByApiKeyId(String apiKeyId);

    /**
     * @note - The provided object's methods will be overridden with any returned data from the database.
     * @param userTag - required fields:
     *     <ul>
     *       <li>apiKeyAccess
     *       <li>apiKeyId
     *     </ul>
     */
    void createApiKeyAccess(ApiKeyAccess apiKeyAccess);

    /**
     * @note - The provided object's methods will be overridden with any returned data from the database.
     * @param agent - overridden fields:
     *     <ul>
     *       <li>apiKeyId
     *       <li>apiKeyAccess
     *     </ul>
     */
    boolean updateApiKeyAccessById(ApiKeyAccess apiKeyAccess);

    boolean deleteApiKeyAccessesByApiKeyId(String apiKeyId);

    boolean deleteApiKeyAccessById(String id);
}
