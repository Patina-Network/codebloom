package org.patinanetwork.codebloom.api.leaderboard;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.patinanetwork.codebloom.common.components.LeaderboardManager;
import org.patinanetwork.codebloom.common.db.models.leaderboard.Leaderboard;
import org.patinanetwork.codebloom.common.db.models.user.User;
import org.patinanetwork.codebloom.common.db.models.user.UserWithScore;
import org.patinanetwork.codebloom.common.db.repos.leaderboard.LeaderboardRepository;
import org.patinanetwork.codebloom.common.db.repos.leaderboard.options.LeaderboardFilterOptions;
import org.patinanetwork.codebloom.common.db.repos.user.UserRepository;
import org.patinanetwork.codebloom.common.dto.ApiResponder;
import org.patinanetwork.codebloom.common.dto.leaderboard.LeaderboardDto;
import org.patinanetwork.codebloom.common.dto.user.UserWithScoreDto;
import org.patinanetwork.codebloom.common.page.Indexed;
import org.patinanetwork.codebloom.common.page.Page;
import org.patinanetwork.codebloom.common.security.AuthenticationObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

@DisplayName("LeaderboardController")
public class LeaderboardControllerTest {
    private static final int PAGE_SIZE = 20;
    private static final int PAGE = 1;
    private static final String LEADERBOARD_ID = UUID.randomUUID().toString();

    private final LeaderboardRepository leaderboardRepository = mock(LeaderboardRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final LeaderboardManager leaderboardManager = mock(LeaderboardManager.class);

    private final LeaderboardController leaderboardController;

    public LeaderboardControllerTest() {
        leaderboardController = new LeaderboardController(leaderboardRepository, userRepository, leaderboardManager);
    }

    @BeforeEach
    void setup() {
        when(leaderboardManager.getLeaderboardUsers(eq(LEADERBOARD_ID), any(), anyBoolean()))
                .thenReturn(null);
    }

    @Test
    void testGetLeaderboardUsersByIdMhcPlusPlus() {
        leaderboardController.getLeaderboardUsersById(
                LEADERBOARD_ID,
                null,
                PAGE,
                PAGE_SIZE,
                "",
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                true,
                false,
                null);

        ArgumentCaptor<LeaderboardFilterOptions> optsCaptor = ArgumentCaptor.forClass(LeaderboardFilterOptions.class);
        verify(leaderboardManager, times(1)).getLeaderboardUsers(eq(LEADERBOARD_ID), optsCaptor.capture(), eq(false));
        var opts = optsCaptor.getValue();

        assertNotNull(opts);
        assertTrue(opts.isMhcplusplus());
        assertEquals(PAGE_SIZE, opts.getPageSize());
        assertEquals(PAGE, opts.getPage());
    }

    @Test
    void testCurrentLeaderboardUsersMhcPlusPlus() {
        when(leaderboardRepository.getRecentLeaderboardMetadata())
                .thenReturn(Optional.of(Leaderboard.builder().id(LEADERBOARD_ID).build()));

        leaderboardController.getCurrentLeaderboardUsers(
                null, PAGE, PAGE_SIZE, "", false, false, false, false, false, false, false, false, false, false, false,
                true, false);

        ArgumentCaptor<LeaderboardFilterOptions> optsCaptor = ArgumentCaptor.forClass(LeaderboardFilterOptions.class);
        verify(leaderboardManager, times(1)).getLeaderboardUsers(eq(LEADERBOARD_ID), optsCaptor.capture(), eq(false));
        var opts = optsCaptor.getValue();

        assertNotNull(opts);
        assertTrue(opts.isMhcplusplus());
        assertEquals(PAGE_SIZE, opts.getPageSize());
        assertEquals(PAGE, opts.getPage());
    }

    @Test
    @DisplayName("returns 200 with metadata when leaderboard exists")
    void returnsMetadata() {
        Leaderboard lb = Leaderboard.builder()
                .id(LEADERBOARD_ID)
                .name("Season 1")
                .createdAt(LocalDateTime.of(2025, 1, 1, 0, 0))
                .build();
        when(leaderboardManager.getLeaderboardMetadata(LEADERBOARD_ID)).thenReturn(Optional.of(lb));

        ResponseEntity<ApiResponder<LeaderboardDto>> response =
                leaderboardController.getLeaderboardMetadataByLeaderboardId(LEADERBOARD_ID, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
        assertEquals("Season 1", response.getBody().getPayload().getName());
    }

    @Test
    @DisplayName("throws 404 when leaderboard does not exist")
    void throws404WhenNotFound() {
        when(leaderboardManager.getLeaderboardMetadata("missing-id")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> leaderboardController.getLeaderboardMetadataByLeaderboardId("missing-id", null));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    @DisplayName("caps page size to MAX_LEADERBOARD_PAGE_SIZE (20)")
    void capsPageSize() {
        leaderboardController.getLeaderboardUsersById(
                LEADERBOARD_ID,
                null,
                1,
                999,
                "",
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                null);

        ArgumentCaptor<LeaderboardFilterOptions> captor = ArgumentCaptor.forClass(LeaderboardFilterOptions.class);
        verify(leaderboardManager).getLeaderboardUsers(eq(LEADERBOARD_ID), captor.capture(), eq(false));

        assertEquals(PAGE_SIZE, captor.getValue().getPageSize());
    }

    @Test
    @DisplayName("passes all filter flags through to LeaderboardFilterOptions")
    void passesAllFilters() {
        leaderboardController.getLeaderboardUsersById(
                LEADERBOARD_ID,
                null,
                2,
                10,
                "tahmid",
                true /* patina */,
                true /* hunter */,
                true /* nyu */,
                true /* baruch */,
                true /* rpi */,
                true /* gwc */,
                true /* sbu */,
                true /* ccny */,
                true /* columbia */,
                true /* cornell */,
                true /* bmcc */,
                true /* mhc++ */,
                true /* globalIndex */,
                null);

        ArgumentCaptor<LeaderboardFilterOptions> captor = ArgumentCaptor.forClass(LeaderboardFilterOptions.class);
        verify(leaderboardManager).getLeaderboardUsers(eq(LEADERBOARD_ID), captor.capture(), eq(true));

        var opts = captor.getValue();
        assertEquals(2, opts.getPage());
        assertEquals(10, opts.getPageSize());
        assertEquals("tahmid", opts.getQuery());
        assertTrue(opts.isPatina());
        assertTrue(opts.isHunter());
        assertTrue(opts.isNyu());
        assertTrue(opts.isBaruch());
        assertTrue(opts.isRpi());
        assertTrue(opts.isGwc());
        assertTrue(opts.isSbu());
        assertTrue(opts.isCcny());
        assertTrue(opts.isColumbia());
        assertTrue(opts.isCornell());
        assertTrue(opts.isBmcc());
        assertTrue(opts.isMhcplusplus());
    }

    @Test
    @DisplayName("uses filters Set when provided, ignoring individual boolean params")
    void usesFiltersSetWhenProvided() {
        leaderboardController.getLeaderboardUsersById(
                LEADERBOARD_ID,
                Set.of("nyu", "hunter"),
                PAGE,
                PAGE_SIZE,
                "",
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                null);

        ArgumentCaptor<LeaderboardFilterOptions> captor = ArgumentCaptor.forClass(LeaderboardFilterOptions.class);
        verify(leaderboardManager).getLeaderboardUsers(eq(LEADERBOARD_ID), captor.capture(), eq(false));
        var opts = captor.getValue();
        assertTrue(opts.isNyu());
        assertTrue(opts.isHunter());
        assertFalse(opts.isPatina());
    }

    @Test
    @DisplayName("sets globalIndex from filters Set when it contains globalIndex")
    void setsGlobalIndexFromFiltersSet() {
        leaderboardController.getLeaderboardUsersById(
                LEADERBOARD_ID,
                Set.of("nyu", "globalIndex"),
                PAGE,
                PAGE_SIZE,
                "",
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                null);

        ArgumentCaptor<LeaderboardFilterOptions> captor = ArgumentCaptor.forClass(LeaderboardFilterOptions.class);
        verify(leaderboardManager).getLeaderboardUsers(eq(LEADERBOARD_ID), captor.capture(), eq(true));
        assertTrue(captor.getValue().isNyu());
    }

    @Test
    @DisplayName("falls back to boolean params when filters Set is empty")
    void fallsBackToBooleansWhenFiltersEmpty() {
        leaderboardController.getLeaderboardUsersById(
                LEADERBOARD_ID,
                Set.of(),
                PAGE,
                PAGE_SIZE,
                "",
                true,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                null);

        ArgumentCaptor<LeaderboardFilterOptions> captor = ArgumentCaptor.forClass(LeaderboardFilterOptions.class);
        verify(leaderboardManager).getLeaderboardUsers(eq(LEADERBOARD_ID), captor.capture(), eq(false));
        assertTrue(captor.getValue().isPatina());
    }

    @Test
    @DisplayName("returns metadata for the most recent leaderboard")
    void returnsCurrentMetadata() {
        Leaderboard lb = Leaderboard.builder()
                .id(LEADERBOARD_ID)
                .name("Current Season")
                .createdAt(LocalDateTime.of(2025, 6, 1, 0, 0))
                .build();
        when(leaderboardRepository.getRecentLeaderboardMetadata()).thenReturn(Optional.of(lb));
        when(leaderboardManager.getLeaderboardMetadata(LEADERBOARD_ID)).thenReturn(Optional.of(lb));

        ResponseEntity<ApiResponder<LeaderboardDto>> response =
                leaderboardController.getCurrentLeaderboardMetadata(null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Current Season", response.getBody().getPayload().getName());
    }

    @Test
    @DisplayName("throws 404 when there is no active leaderboard")
    void throws404WhenNoActive() {
        when(leaderboardRepository.getRecentLeaderboardMetadata()).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> leaderboardController.getCurrentLeaderboardMetadata(null));
    }

    @Test
    @DisplayName("throws 404 when manager can't find the active leaderboard by id")
    void throws404WhenManagerReturnsEmpty() {
        Leaderboard lb = Leaderboard.builder().id("lb-ghost").build();
        when(leaderboardRepository.getRecentLeaderboardMetadata()).thenReturn(Optional.of(lb));
        when(leaderboardManager.getLeaderboardMetadata("lb-ghost")).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> leaderboardController.getCurrentLeaderboardMetadata(null));
    }

    @Test
    @DisplayName("delegates to leaderboardManager with current leaderboard ID")
    void delegatesToManager() {
        when(leaderboardRepository.getRecentLeaderboardMetadata())
                .thenReturn(Optional.of(Leaderboard.builder().id(LEADERBOARD_ID).build()));

        Page<Indexed<UserWithScoreDto>> fakePage = new Page<>(false, List.of(), 1, 20);
        when(leaderboardManager.getLeaderboardUsers(eq(LEADERBOARD_ID), any(), eq(false)))
                .thenReturn(fakePage);

        ResponseEntity<ApiResponder<Page<Indexed<UserWithScoreDto>>>> response =
                leaderboardController.getCurrentLeaderboardUsers(
                        null, 1, 20, "", false, false, false, false, false, false, false, false, false, false, false,
                        false, false);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
    }

    @Test
    @DisplayName("throws 404 when no current leaderboard exists for user list")
    void throws404WhenNoLeaderboardForUsers() {
        when(leaderboardRepository.getRecentLeaderboardMetadata()).thenReturn(Optional.empty());

        assertThrows(
                ResponseStatusException.class,
                () -> leaderboardController.getCurrentLeaderboardUsers(
                        null, 1, 20, "", false, false, false, false, false, false, false, false, false, false, false,
                        false, false));
    }

    @Test
    @DisplayName("returns the user's data from the active leaderboard")
    void returnsUserData() {
        Leaderboard lb = Leaderboard.builder().id(LEADERBOARD_ID).build();
        when(leaderboardRepository.getRecentLeaderboardMetadata()).thenReturn(Optional.of(lb));

        UserWithScore uws = UserWithScore.builder()
                .id("u1")
                .discordId("d1")
                .discordName("testuser")
                .totalScore(500)
                .build();
        when(userRepository.getUserWithScoreByIdAndLeaderboardId(eq("u1"), eq(LEADERBOARD_ID), any()))
                .thenReturn(uws);

        ResponseEntity<ApiResponder<UserWithScoreDto>> response =
                leaderboardController.getUserCurrentLeaderboardFull(null, "u1");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("testuser", response.getBody().getPayload().getDiscordName());
        assertEquals(500, response.getBody().getPayload().getTotalScore());
    }

    @Test
    @DisplayName("throws 404 when no active leaderboard exists for user lookup")
    void throws404WhenNoLeaderboardForUser() {
        when(leaderboardRepository.getRecentLeaderboardMetadata()).thenReturn(Optional.empty());

        assertThrows(
                ResponseStatusException.class, () -> leaderboardController.getUserCurrentLeaderboardFull(null, "u1"));
    }

    @Test
    @DisplayName("returns global rank when no filters are applied")
    void returnsGlobalRank() {
        User authUser = User.builder()
                .id("u-rank")
                .discordId("d1")
                .discordName("ranker")
                .build();
        AuthenticationObject auth = new AuthenticationObject(authUser, null);
        when(leaderboardRepository.getRecentLeaderboardMetadata())
                .thenReturn(Optional.of(Leaderboard.builder().id(LEADERBOARD_ID).build()));

        UserWithScore uws = UserWithScore.builder()
                .id("u-rank")
                .discordId("d1")
                .discordName("ranker")
                .totalScore(1000)
                .build();
        Indexed<UserWithScore> indexed = Indexed.of(uws, 3);
        when(leaderboardRepository.getGlobalRankedUserById(LEADERBOARD_ID, "u-rank"))
                .thenReturn(Optional.of(indexed));

        var response = leaderboardController.getUserCurrentLeaderboardRank(
                auth, false, false, false, false, false, false, false, false, false, false, false);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(3, response.getBody().getPayload().getIndex());
        assertEquals("ranker", response.getBody().getPayload().getItem().getDiscordName());
    }

    @Test
    @DisplayName("returns filtered rank when filter flags are applied")
    void returnsFilteredRank() {
        User authUser = User.builder()
                .id("u-rank")
                .discordId("d1")
                .discordName("ranker")
                .build();
        AuthenticationObject auth = new AuthenticationObject(authUser, null);
        when(leaderboardRepository.getRecentLeaderboardMetadata())
                .thenReturn(Optional.of(Leaderboard.builder().id(LEADERBOARD_ID).build()));

        UserWithScore uws = UserWithScore.builder()
                .id("u-rank")
                .discordId("d1")
                .discordName("ranker")
                .totalScore(800)
                .build();
        Indexed<UserWithScore> indexed = Indexed.of(uws, 1);
        when(leaderboardRepository.getFilteredRankedUserById(eq(LEADERBOARD_ID), eq("u-rank"), any()))
                .thenReturn(Optional.of(indexed));

        var response = leaderboardController.getUserCurrentLeaderboardRank(
                auth, true, false, false, false, false, false, false, false, false, false, false);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().getPayload().getIndex());
    }

    @Test
    @DisplayName("throws 404 when user is not found on the leaderboard (global)")
    void throws404GlobalNotFound() {
        User authUser = User.builder()
                .id("u-rank")
                .discordId("d1")
                .discordName("ranker")
                .build();
        AuthenticationObject auth = new AuthenticationObject(authUser, null);
        when(leaderboardRepository.getRecentLeaderboardMetadata())
                .thenReturn(Optional.of(Leaderboard.builder().id(LEADERBOARD_ID).build()));

        when(leaderboardRepository.getGlobalRankedUserById(LEADERBOARD_ID, "u-rank"))
                .thenReturn(Optional.empty());

        assertThrows(
                ResponseStatusException.class,
                () -> leaderboardController.getUserCurrentLeaderboardRank(
                        auth, false, false, false, false, false, false, false, false, false, false, false));
    }

    @Test
    @DisplayName("throws 404 when user is not found with filters")
    void throws404FilteredNotFound() {
        User authUser = User.builder()
                .id("u-rank")
                .discordId("d1")
                .discordName("ranker")
                .build();
        AuthenticationObject auth = new AuthenticationObject(authUser, null);
        when(leaderboardRepository.getRecentLeaderboardMetadata())
                .thenReturn(Optional.of(Leaderboard.builder().id(LEADERBOARD_ID).build()));

        when(leaderboardRepository.getFilteredRankedUserById(eq(LEADERBOARD_ID), eq("u-rank"), any()))
                .thenReturn(Optional.empty());

        assertThrows(
                ResponseStatusException.class,
                () -> leaderboardController.getUserCurrentLeaderboardRank(
                        auth, true, false, false, false, false, false, false, false, false, false, false));
    }

    @Test
    @DisplayName("throws 404 when no active leaderboard exists for rank lookup")
    void throws404NoLeaderboardForRank() {
        User authUser = User.builder()
                .id("u-rank")
                .discordId("d1")
                .discordName("ranker")
                .build();
        AuthenticationObject auth = new AuthenticationObject(authUser, null);
        when(leaderboardRepository.getRecentLeaderboardMetadata()).thenReturn(Optional.empty());

        assertThrows(
                ResponseStatusException.class,
                () -> leaderboardController.getUserCurrentLeaderboardRank(
                        auth, false, false, false, false, false, false, false, false, false, false, false));
    }

    @Test
    @DisplayName("returns a page of all leaderboard metadata")
    void returnsAllMetadata() {
        Leaderboard lb1 = Leaderboard.builder()
                .id("lb-1")
                .name("Season 1")
                .createdAt(LocalDateTime.of(2025, 1, 1, 0, 0))
                .build();
        Leaderboard lb2 = Leaderboard.builder()
                .id("lb-2")
                .name("Season 2")
                .createdAt(LocalDateTime.of(2025, 6, 1, 0, 0))
                .build();
        when(leaderboardRepository.getAllLeaderboardsShallow(any())).thenReturn(List.of(lb1, lb2));
        when(leaderboardRepository.getLeaderboardCount()).thenReturn(2);

        var response = leaderboardController.getAllLeaderboardMetadata(null, 1, "", 20);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(2, response.getBody().getPayload().getItems().size());
        assertFalse(response.getBody().getPayload().isHasNextPage());
    }

    @Test
    @DisplayName("reports hasNextPage=true when there are more pages")
    void hasNextPageWhenMoreExist() {
        when(leaderboardRepository.getAllLeaderboardsShallow(any()))
                .thenReturn(List.of(Leaderboard.builder()
                        .id("lb-1")
                        .name("S1")
                        .createdAt(LocalDateTime.of(2025, 1, 1, 0, 0))
                        .build()));
        when(leaderboardRepository.getLeaderboardCount()).thenReturn(25);

        var response = leaderboardController.getAllLeaderboardMetadata(null, 1, "", 10);

        assertTrue(response.getBody().getPayload().isHasNextPage());
        assertEquals(3, response.getBody().getPayload().getPages());
    }

    @Test
    @DisplayName("caps page size at MAX_LEADERBOARD_PAGE_SIZE for all metadata")
    void capsAllMetadataPageSize() {
        when(leaderboardRepository.getAllLeaderboardsShallow(any())).thenReturn(List.of());
        when(leaderboardRepository.getLeaderboardCount()).thenReturn(0);

        leaderboardController.getAllLeaderboardMetadata(null, 1, "", 999);

        ArgumentCaptor<LeaderboardFilterOptions> captor = ArgumentCaptor.forClass(LeaderboardFilterOptions.class);
        verify(leaderboardRepository).getAllLeaderboardsShallow(captor.capture());
        assertEquals(20, captor.getValue().getPageSize());
    }
}
