package org.patinanetwork.codebloom.api.external;

import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.patinanetwork.codebloom.common.db.models.api.ApiKey;
import org.patinanetwork.codebloom.common.db.models.api.ApiKeyAccessEnum;
import org.patinanetwork.codebloom.common.db.models.api.access.ApiKeyAccess;
import org.patinanetwork.codebloom.common.db.models.user.UserWithScore;
import org.patinanetwork.codebloom.common.db.repos.api.ApiKeyRepository;
import org.patinanetwork.codebloom.common.db.repos.api.access.ApiKeyAccessRepository;
import org.patinanetwork.codebloom.common.db.repos.leaderboard.LeaderboardRepository;
import org.patinanetwork.codebloom.common.db.repos.leaderboard.options.LeaderboardFilterOptions;
import org.patinanetwork.codebloom.common.dto.ApiResponder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@Tag(name = "External Api Routes", description = """
    These routes house the logic for External Apis. """)
@RequestMapping("/api/external")
@Timed(value = "controller.execution")
public class ExternalController {

    private final ApiKeyRepository apiKeyRepository;
    private final ApiKeyAccessRepository apiKeyAccessRepository;
    private final LeaderboardRepository leaderboardRepository;

    public ExternalController(
            final ApiKeyRepository apiKeyRepository,
            final ApiKeyAccessRepository apiKeyAccessRepository,
            final LeaderboardRepository leaderboardRepository) {
        this.apiKeyRepository = apiKeyRepository;
        this.apiKeyAccessRepository = apiKeyAccessRepository;
        this.leaderboardRepository = leaderboardRepository;
    }

    @Operation(
            summary = "Get GWC users from a specific leaderboard",
            description = "Returns a list of users with GWC tags from the specified leaderboard. "
                    + "Requires a valid API key with GWC_READ_BY_USER access.")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved GWC users")
    @ApiResponse(responseCode = "401", description = "Invalid API key")
    @ApiResponse(responseCode = "403", description = "API key does not have required permissions")
    @ApiResponse(responseCode = "404", description = "Leaderboard not found")
    @GetMapping("/gwc/users")
    public ResponseEntity<ApiResponder<List<UserWithScore>>> getGwcUsers(
            @RequestHeader("X-API-Key") final String apiKey,
            @RequestParam("leaderboardId") final String leaderboardId) {
        ApiKey validApiKey = apiKeyRepository
                .getApiKeyByHash(apiKey)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid API key"));

        List<ApiKeyAccess> accesses = apiKeyAccessRepository.getApiKeyAccessesByApiKeyId(validApiKey.getId());
        boolean hasGwcAccess =
                accesses.stream().anyMatch(access -> access.getAccess() == ApiKeyAccessEnum.GWC_READ_BY_USER);

        if (!hasGwcAccess) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "API key does not have GWC read access");
        }

        if (leaderboardRepository.getLeaderboardMetadataById(leaderboardId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Leaderboard not found");
        }
        LeaderboardFilterOptions options =
                LeaderboardFilterOptions.builder().gwc(true).build();

        List<UserWithScore> usersWithScore = leaderboardRepository.getLeaderboardUsersById(leaderboardId, options);

        return ResponseEntity.ok(ApiResponder.success("Gwc Users successfully fetched", usersWithScore));
    }
}
