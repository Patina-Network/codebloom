package org.patinanetwork.codebloom.api.leaderboard;

import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Optional;
import org.patinanetwork.codebloom.common.components.LeaderboardManager;
import org.patinanetwork.codebloom.common.db.models.leaderboard.Leaderboard;
import org.patinanetwork.codebloom.common.db.models.user.UserWithScore;
import org.patinanetwork.codebloom.common.db.repos.leaderboard.LeaderboardRepository;
import org.patinanetwork.codebloom.common.db.repos.leaderboard.options.LeaderboardFilterOptions;
import org.patinanetwork.codebloom.common.db.repos.user.UserRepository;
import org.patinanetwork.codebloom.common.db.repos.user.options.UserFilterOptions;
import org.patinanetwork.codebloom.common.dto.ApiResponder;
import org.patinanetwork.codebloom.common.dto.autogen.UnsafeGenericFailureResponse;
import org.patinanetwork.codebloom.common.dto.leaderboard.LeaderboardDto;
import org.patinanetwork.codebloom.common.dto.user.UserWithScoreDto;
import org.patinanetwork.codebloom.common.page.Indexed;
import org.patinanetwork.codebloom.common.page.Page;
import org.patinanetwork.codebloom.common.security.AuthenticationObject;
import org.patinanetwork.codebloom.common.security.annotation.Protected;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/leaderboard")
@Tag(name = "Leaderboard routes")
@Timed(value = "controller.execution")
public class LeaderboardController {

    private static final int MAX_LEADERBOARD_PAGE_SIZE = 20;

    private final LeaderboardRepository leaderboardRepository;
    private final UserRepository userRepository;
    private final LeaderboardManager leaderboardManager;

    public LeaderboardController(
            final LeaderboardRepository leaderboardRepository,
            final UserRepository userRepository,
            final LeaderboardManager leaderboardManager) {
        this.leaderboardRepository = leaderboardRepository;
        this.userRepository = userRepository;
        this.leaderboardManager = leaderboardManager;
    }

    @GetMapping("/{leaderboardId}/metadata")
    @Operation(
            summary = "Unprotected route that fetches metadata of a leaderboard via leaderboard ID.",
            responses = {
                @ApiResponse(responseCode = "200"),
                @ApiResponse(
                        responseCode = "401",
                        description = "Not authenticated",
                        content = @Content(schema = @Schema(implementation = UnsafeGenericFailureResponse.class))),
            })
    public ResponseEntity<ApiResponder<LeaderboardDto>> getLeaderboardMetadataByLeaderboardId(
            final @PathVariable String leaderboardId, final HttpServletRequest request) {

        Optional<Leaderboard> leaderboardData = leaderboardManager.getLeaderboardMetadata(leaderboardId);

        if (leaderboardData.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Leaderboard cannot be found or does not exist.");
        }

        return ResponseEntity.ok()
                .body(ApiResponder.success(
                        "Leaderboard metadata found!", LeaderboardDto.fromLeaderboard(leaderboardData.get())));
    }

    @GetMapping("/{leaderboardId}/user/all")
    @Operation(
            summary = "Unprotected route that fetches metadata of a leaderboard via leaderboard ID.",
            responses = {
                @ApiResponse(responseCode = "200"),
                @ApiResponse(
                        responseCode = "401",
                        description = "Not authenticated",
                        content = @Content(schema = @Schema(implementation = UnsafeGenericFailureResponse.class))),
            })
    public ResponseEntity<ApiResponder<Page<Indexed<UserWithScoreDto>>>> getLeaderboardUsersById(
            @PathVariable final String leaderboardId,
            @Parameter(description = "Page index", example = "1") @RequestParam(required = false, defaultValue = "1")
                    final int page,
            @Parameter(description = "Page size (maximum of " + MAX_LEADERBOARD_PAGE_SIZE)
                    @RequestParam(required = false, defaultValue = "" + MAX_LEADERBOARD_PAGE_SIZE)
                    final int pageSize,
            @Parameter(description = "Discord name", example = "tahmid")
                    @RequestParam(required = false, defaultValue = "")
                    final String query,
            @Parameter(description = "Filter for Patina users") @RequestParam(required = false, defaultValue = "false")
                    final boolean patina,
            @Parameter(description = "Filter for Hunter College users")
                    @RequestParam(required = false, defaultValue = "false")
                    final boolean hunter,
            @Parameter(description = "Filter for NYU users") @RequestParam(required = false, defaultValue = "false")
                    final boolean nyu,
            @Parameter(description = "Filter for Baruch College users")
                    @RequestParam(required = false, defaultValue = "false")
                    final boolean baruch,
            @Parameter(description = "Filter for RPI users") @RequestParam(required = false, defaultValue = "false")
                    final boolean rpi,
            @Parameter(description = "Filter for GWC users") @RequestParam(required = false, defaultValue = "false")
                    final boolean gwc,
            @Parameter(description = "Filter for SBU users") @RequestParam(required = false, defaultValue = "false")
                    final boolean sbu,
            @Parameter(description = "Filter for CCNY users") @RequestParam(required = false, defaultValue = "false")
                    final boolean ccny,
            @Parameter(description = "Filter for Columbia users")
                    @RequestParam(required = false, defaultValue = "false")
                    final boolean columbia,
            @Parameter(description = "Filter for Cornell users") @RequestParam(required = false, defaultValue = "false")
                    final boolean cornell,
            @Parameter(description = "Filter for BMCC users") @RequestParam(required = false, defaultValue = "false")
                    final boolean bmcc,
            @Parameter(description = "Filter for MHC++ users") @RequestParam(required = false, defaultValue = "false")
                    final boolean mhcplusplus,
            @Parameter(description = "Enable global leaderboard index")
                    @RequestParam(required = false, defaultValue = "false")
                    final boolean globalIndex,
            final HttpServletRequest request) {

        final int parsedPageSize = Math.min(pageSize, MAX_LEADERBOARD_PAGE_SIZE);

        LeaderboardFilterOptions options = LeaderboardFilterOptions.builder()
                .page(page)
                .pageSize(parsedPageSize)
                .query(query)
                .patina(patina)
                .hunter(hunter)
                .nyu(nyu)
                .baruch(baruch)
                .rpi(rpi)
                .gwc(gwc)
                .sbu(sbu)
                .ccny(ccny)
                .columbia(columbia)
                .cornell(cornell)
                .bmcc(bmcc)
                .mhcplusplus(mhcplusplus)
                .build();

        Page<Indexed<UserWithScoreDto>> createdPage =
                leaderboardManager.getLeaderboardUsers(leaderboardId, options, globalIndex);

        return ResponseEntity.ok().body(ApiResponder.success("All leaderboards found!", createdPage));
    }

    @GetMapping("/current/metadata")
    @Operation(
            summary =
                    "Unprotected route that fetches the currently active leaderboard data, attaching only the top 5 users for each leaderboard.",
            responses = {
                @ApiResponse(responseCode = "200"),
                @ApiResponse(
                        responseCode = "401",
                        description = "Not authenticated",
                        content = @Content(schema = @Schema(implementation = UnsafeGenericFailureResponse.class))),
            })
    public ResponseEntity<ApiResponder<LeaderboardDto>> getCurrentLeaderboardMetadata(
            final HttpServletRequest request) {

        var current = leaderboardRepository
                .getRecentLeaderboardMetadata()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No active leaderboard found."));

        Optional<Leaderboard> leaderboardData = leaderboardManager.getLeaderboardMetadata(current.getId());

        if (leaderboardData.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Leaderboard cannot be found or does not exist.");
        }

        return ResponseEntity.ok()
                .body(ApiResponder.success(
                        "All leaderboards found!", LeaderboardDto.fromLeaderboard(leaderboardData.get())));
    }

    @GetMapping("/current/user/all")
    @Operation(
            summary = "Fetch the currently active leaderboard data, attaching the users for each leaderboard.",
            responses = {
                @ApiResponse(responseCode = "200"),
                @ApiResponse(
                        responseCode = "401",
                        description = "Not authenticated",
                        content = @Content(schema = @Schema(implementation = UnsafeGenericFailureResponse.class))),
            })
    public ResponseEntity<ApiResponder<Page<Indexed<UserWithScoreDto>>>> getCurrentLeaderboardUsers(
            final HttpServletRequest request,
            @Parameter(description = "Page index", example = "1") @RequestParam(required = false, defaultValue = "1")
                    final int page,
            @Parameter(description = "Page size (maximum of " + MAX_LEADERBOARD_PAGE_SIZE)
                    @RequestParam(required = false, defaultValue = "" + MAX_LEADERBOARD_PAGE_SIZE)
                    final int pageSize,
            @Parameter(description = "Discord name", example = "tahmid")
                    @RequestParam(required = false, defaultValue = "")
                    final String query,
            @Parameter(description = "Filter for Patina users") @RequestParam(required = false, defaultValue = "false")
                    final boolean patina,
            @Parameter(description = "Filter for Hunter College users")
                    @RequestParam(required = false, defaultValue = "false")
                    final boolean hunter,
            @Parameter(description = "Filter for NYU users") @RequestParam(required = false, defaultValue = "false")
                    final boolean nyu,
            @Parameter(description = "Filter for Baruch College users")
                    @RequestParam(required = false, defaultValue = "false")
                    final boolean baruch,
            @Parameter(description = "Filter for RPI users") @RequestParam(required = false, defaultValue = "false")
                    final boolean rpi,
            @Parameter(description = "Filter for GWC users") @RequestParam(required = false, defaultValue = "false")
                    final boolean gwc,
            @Parameter(description = "Filter for SBU users") @RequestParam(required = false, defaultValue = "false")
                    final boolean sbu,
            @Parameter(description = "Filter for CCNY users") @RequestParam(required = false, defaultValue = "false")
                    final boolean ccny,
            @Parameter(description = "Filter for Columbia users")
                    @RequestParam(required = false, defaultValue = "false")
                    final boolean columbia,
            @Parameter(description = "Filter for Cornell users") @RequestParam(required = false, defaultValue = "false")
                    final boolean cornell,
            @Parameter(description = "Filter for BMCC users") @RequestParam(required = false, defaultValue = "false")
                    final boolean bmcc,
            @Parameter(description = "Filter for MHC++ users") @RequestParam(required = false, defaultValue = "false")
                    final boolean mhcplusplus,
            @Parameter(description = "Enable global leaderboard index")
                    @RequestParam(required = false, defaultValue = "false")
                    final boolean globalIndex) {

        final int parsedPageSize = Math.min(pageSize, MAX_LEADERBOARD_PAGE_SIZE);

        LeaderboardFilterOptions options = LeaderboardFilterOptions.builder()
                .page(page)
                .pageSize(parsedPageSize)
                .query(query)
                .patina(patina)
                .hunter(hunter)
                .nyu(nyu)
                .baruch(baruch)
                .rpi(rpi)
                .gwc(gwc)
                .sbu(sbu)
                .ccny(ccny)
                .columbia(columbia)
                .cornell(cornell)
                .bmcc(bmcc)
                .mhcplusplus(mhcplusplus)
                .build();

        String currentLeaderboardId = leaderboardRepository
                .getRecentLeaderboardMetadata()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No active leaderboard found."))
                .getId();
        Page<Indexed<UserWithScoreDto>> createdPage =
                leaderboardManager.getLeaderboardUsers(currentLeaderboardId, options, globalIndex);

        return ResponseEntity.ok().body(ApiResponder.success("All leaderboards found!", createdPage));
    }

    @GetMapping("/current/user/{userId}")
    @Operation(
            summary = "Fetch the specific user data in the currently active leaderboard data.",
            responses = {
                @ApiResponse(responseCode = "200"),
                @ApiResponse(
                        responseCode = "401",
                        description = "Not authenticated",
                        content = @Content(schema = @Schema(implementation = UnsafeGenericFailureResponse.class))),
            })
    public ResponseEntity<ApiResponder<UserWithScoreDto>> getUserCurrentLeaderboardFull(
            final HttpServletRequest request, @PathVariable final String userId) {

        Optional<Leaderboard> leaderboardData = leaderboardRepository.getRecentLeaderboardMetadata();

        if (leaderboardData.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Active leaderboard could not be found.");
        }

        // we do not support point of time in this endpoint currently
        UserWithScore user = userRepository.getUserWithScoreByIdAndLeaderboardId(
                userId,
                leaderboardData.get().getId(),
                UserFilterOptions.builder().build());

        // if (user == null) {
        // return
        // ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponder.failure("This
        // user does not exist on this leaderboard."));
        // }

        return ResponseEntity.ok().body(ApiResponder.success("User found!", UserWithScoreDto.fromUserWithScore(user)));
    }

    @GetMapping("/current/user/rank")
    @Operation(
            summary = "Fetch the authenticated user's current rank/position on the active leaderboard.",
            responses = {
                @ApiResponse(
                        responseCode = "401",
                        description = "Not authenticated",
                        content = @Content(schema = @Schema(implementation = UnsafeGenericFailureResponse.class))),
                @ApiResponse(responseCode = "200", description = "Successfully retrieved user rank"),
                @ApiResponse(responseCode = "404", description = "User not found on leaderboard"),
            })
    public ResponseEntity<ApiResponder<Indexed<UserWithScoreDto>>> getUserCurrentLeaderboardRank(
            @Protected final AuthenticationObject authenticationObject,
            @Parameter(description = "Filter for Patina users") @RequestParam(required = false, defaultValue = "false")
                    final boolean patina,
            @Parameter(description = "Filter for Hunter College users")
                    @RequestParam(required = false, defaultValue = "false")
                    final boolean hunter,
            @Parameter(description = "Filter for NYU users") @RequestParam(required = false, defaultValue = "false")
                    final boolean nyu,
            @Parameter(description = "Filter for Baruch College users")
                    @RequestParam(required = false, defaultValue = "false")
                    final boolean baruch,
            @Parameter(description = "Filter for RPI users") @RequestParam(required = false, defaultValue = "false")
                    final boolean rpi,
            @Parameter(description = "Filter for GWC users") @RequestParam(required = false, defaultValue = "false")
                    final boolean gwc,
            @Parameter(description = "Filter for SBU users") @RequestParam(required = false, defaultValue = "false")
                    final boolean sbu,
            @Parameter(description = "Filter for CCNY users") @RequestParam(required = false, defaultValue = "false")
                    final boolean ccny,
            @Parameter(description = "Filter for Cornell users") @RequestParam(required = false, defaultValue = "false")
                    final boolean cornell,
            @Parameter(description = "Filter for Columbia users")
                    @RequestParam(required = false, defaultValue = "false")
                    final boolean columbia,
            @Parameter(description = "Filter for BMCC users") @RequestParam(required = false, defaultValue = "false")
                    final boolean bmcc) {

        String userId = authenticationObject.getUser().getId();

        Optional<Leaderboard> leaderboardData = leaderboardRepository.getRecentLeaderboardMetadata();

        if (leaderboardData.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No active leaderboard found.");
        }

        Indexed<UserWithScore> userWithRank;

        if (!patina && !hunter && !nyu && !baruch && !rpi && !gwc && !sbu && !ccny && !columbia && !cornell && !bmcc) {
            // Use global ranking when no filters are applied
            userWithRank = leaderboardRepository
                    .getGlobalRankedUserById(leaderboardData.get().getId(), userId)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND, "User not found on the current leaderboard."));
        } else {
            // Use filtered ranking when filters are applied
            LeaderboardFilterOptions options = LeaderboardFilterOptions.builder()
                    .patina(patina)
                    .hunter(hunter)
                    .nyu(nyu)
                    .baruch(baruch)
                    .rpi(rpi)
                    .gwc(gwc)
                    .sbu(sbu)
                    .ccny(ccny)
                    .columbia(columbia)
                    .cornell(cornell)
                    .bmcc(bmcc)
                    .build();
            userWithRank = leaderboardRepository
                    .getFilteredRankedUserById(leaderboardData.get().getId(), userId, options)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "User not found on the current leaderboard or does not match the specified filters."));
        }

        Indexed<UserWithScoreDto> indexedUserWithScoreDto =
                Indexed.of(UserWithScoreDto.fromUserWithScore(userWithRank.getItem()), userWithRank.getIndex());

        return ResponseEntity.ok().body(ApiResponder.success("User rank found!", indexedUserWithScoreDto));
    }

    @GetMapping("/all/metadata")
    @Operation(
            summary = "Returns the metadata for all leaderboards.",
            responses = {
                @ApiResponse(responseCode = "200"),
                @ApiResponse(
                        responseCode = "401",
                        description = "Not authenticated",
                        content = @Content(schema = @Schema(implementation = UnsafeGenericFailureResponse.class))),
            })
    public ResponseEntity<ApiResponder<Page<LeaderboardDto>>> getAllLeaderboardMetadata(
            final HttpServletRequest request,
            @Parameter(description = "Page index", example = "1") @RequestParam(required = false, defaultValue = "1")
                    final int page,
            @Parameter(description = "Question Title", example = "Two")
                    @RequestParam(required = false, defaultValue = "")
                    final String query,
            @Parameter(description = "Page size (maximum of " + MAX_LEADERBOARD_PAGE_SIZE)
                    @RequestParam(required = false, defaultValue = "" + MAX_LEADERBOARD_PAGE_SIZE)
                    final int pageSize) {

        final int parsedPageSize = Math.min(pageSize, MAX_LEADERBOARD_PAGE_SIZE);

        LeaderboardFilterOptions options = LeaderboardFilterOptions.builder()
                .page(page)
                .pageSize(parsedPageSize)
                .query(query)
                .build();

        List<Leaderboard> leaderboardMetaData = leaderboardRepository.getAllLeaderboardsShallow(options);

        int totalLeaderboards = leaderboardRepository.getLeaderboardCount();
        int totalPages = (int) Math.ceil((double) totalLeaderboards / parsedPageSize);
        boolean hasNextPage = page < totalPages;

        List<LeaderboardDto> leaderboardDtos = leaderboardMetaData.stream()
                .map(l -> LeaderboardDto.fromLeaderboard(l))
                .toList();

        Page<LeaderboardDto> createdPage = new Page<>(hasNextPage, leaderboardDtos, totalPages, parsedPageSize);

        return ResponseEntity.ok()
                .body(ApiResponder.success("All leaderboard metadatas have been found!", createdPage));
    }
}
