package org.patinanetwork.codebloom.common.components;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.patinanetwork.codebloom.common.db.models.discord.DiscordClub;
import org.patinanetwork.codebloom.common.db.models.discord.DiscordClubMetadata;
import org.patinanetwork.codebloom.common.db.models.leaderboard.Leaderboard;
import org.patinanetwork.codebloom.common.db.models.user.User;
import org.patinanetwork.codebloom.common.db.models.user.UserWithScore;
import org.patinanetwork.codebloom.common.db.models.usertag.Tag;
import org.patinanetwork.codebloom.common.db.repos.discord.club.DiscordClubRepository;
import org.patinanetwork.codebloom.common.db.repos.leaderboard.LeaderboardRepository;
import org.patinanetwork.codebloom.common.db.repos.leaderboard.options.LeaderboardFilterOptions;
import org.patinanetwork.codebloom.common.db.repos.user.UserRepository;
import org.patinanetwork.codebloom.common.db.repos.user.options.UserFilterOptions;
import org.patinanetwork.codebloom.common.dto.refresh.RefreshResultDto;
import org.patinanetwork.codebloom.common.page.Indexed;
import org.patinanetwork.codebloom.common.url.ServerUrlUtils;
import org.patinanetwork.codebloom.jda.client.JDAClient;
import org.patinanetwork.codebloom.jda.client.options.EmbeddedImagesMessageOptions;
import org.patinanetwork.codebloom.playwright.PlaywrightClient;
import org.slf4j.LoggerFactory;

public class DiscordClubManagerTest {

    private JDAClient jdaClient = mock(JDAClient.class);
    private LeaderboardRepository leaderboardRepository = mock(LeaderboardRepository.class);
    private DiscordClubRepository discordClubRepository = mock(DiscordClubRepository.class);
    private PlaywrightClient playwrightClient = mock(PlaywrightClient.class);
    private ServerUrlUtils serverUrlUtils = mock(ServerUrlUtils.class);
    private UserRepository userRepository = mock(UserRepository.class);
    private LeaderboardManager leaderboardManager = mock(LeaderboardManager.class);

    private DiscordClubManager discordClubManager;

    private ListAppender<ILoggingEvent> logWatcher;

    @BeforeEach
    void setUp() {
        discordClubManager = new DiscordClubManager(
                serverUrlUtils,
                jdaClient,
                leaderboardRepository,
                discordClubRepository,
                userRepository,
                leaderboardManager);

        logWatcher = new ListAppender<>();
        logWatcher.start();
        ((Logger) LoggerFactory.getLogger(discordClubManager.getClass())).addAppender(logWatcher);
    }

    @AfterEach
    void teardown() {
        ((Logger) LoggerFactory.getLogger(discordClubManager.getClass())).detachAndStopAllAppenders();
    }

    @Test
    void testSendLeaderboardCompletedDiscordMessageToAllClubsSuccess() {
        DiscordClub mockClub = createMockDiscordClub("Test Club", Tag.Rpi);
        when(discordClubRepository.getAllActiveDiscordClubs()).thenReturn(Arrays.asList(mockClub));

        setupMockLeaderboardData();

        discordClubManager.sendLeaderboardCompletedDiscordMessageToAllClubs();

        verify(discordClubRepository).getAllActiveDiscordClubs();
        verify(jdaClient).sendEmbedWithImages(any(EmbeddedImagesMessageOptions.class));
    }

    @Test
    void testSendLeaderboardCompletedDiscordMessageToAllClubsEmptyClubList() {
        when(discordClubRepository.getAllActiveDiscordClubs()).thenReturn(Collections.emptyList());

        discordClubManager.sendLeaderboardCompletedDiscordMessageToAllClubs();

        verify(discordClubRepository).getAllActiveDiscordClubs();
        verify(jdaClient, never()).sendEmbedWithImages(any());
    }

    @Test
    void testSendLeaderboardCompletedDiscordMessageToAllClubsMultipleClubs() {
        DiscordClub club1 = createMockDiscordClub("Club 1", Tag.Rpi);
        DiscordClub club2 = createMockDiscordClub("Club 2", Tag.Baruch);
        when(discordClubRepository.getAllActiveDiscordClubs()).thenReturn(Arrays.asList(club1, club2));

        setupMockLeaderboardData();

        discordClubManager.sendLeaderboardCompletedDiscordMessageToAllClubs();

        verify(discordClubRepository).getAllActiveDiscordClubs();
        verify(jdaClient, times(2)).sendEmbedWithImages(any(EmbeddedImagesMessageOptions.class));
    }

    @Test
    void testSendLeaderboardCompletedDiscordMessageMissingGuildId() {
        DiscordClub mockClub = createMockDiscordClubWithoutMetadata("Test Club", Tag.Rpi);
        when(discordClubRepository.getAllActiveDiscordClubs()).thenReturn(Arrays.asList(mockClub));

        setupMockLeaderboardData();

        discordClubManager.sendLeaderboardCompletedDiscordMessageToAllClubs();

        verify(jdaClient, never()).sendEmbedWithImages(any());
    }

    @Test
    void testSendWeeklyLeaderboardUpdateDiscordMessageToAllClubsSuccess() {
        DiscordClub mockClub = createMockDiscordClub("Test Club", Tag.Rpi);
        when(discordClubRepository.getAllActiveDiscordClubs()).thenReturn(Arrays.asList(mockClub));
        when(discordClubRepository.getDiscordClubByGuildId(anyString())).thenReturn(Optional.of(mockClub));

        setupMockLeaderboardDataWithExpiration();

        discordClubManager.sendWeeklyLeaderboardUpdateDiscordMessageToAllClubs();

        verify(discordClubRepository).getAllActiveDiscordClubs();
        verify(jdaClient).sendEmbedWithImages(any(EmbeddedImagesMessageOptions.class));
    }

    @Test
    void testSendWeeklyLeaderboardUpdateDiscordMessageToAllClubsEmptyClubList() {
        when(discordClubRepository.getAllActiveDiscordClubs()).thenReturn(Collections.emptyList());

        discordClubManager.sendWeeklyLeaderboardUpdateDiscordMessageToAllClubs();

        verify(discordClubRepository).getAllActiveDiscordClubs();
        verify(jdaClient, never()).sendEmbedWithImages(any());
    }

    @Test
    void testSendWeeklyLeaderboardUpdateDiscordMessageMissingChannelId() {
        DiscordClub mockClub = createMockDiscordClubWithPartialMetadata("Test Club", Tag.Rpi);
        when(discordClubRepository.getAllActiveDiscordClubs()).thenReturn(Arrays.asList(mockClub));

        setupMockLeaderboardDataWithExpiration();

        discordClubManager.sendWeeklyLeaderboardUpdateDiscordMessageToAllClubs();

        verify(jdaClient, never()).sendEmbedWithImages(any());
    }

    @Test
    void testSendWeeklyLeaderboardUpdateWithoutExpiration() {
        DiscordClub mockClub = createMockDiscordClub("Test Club", Tag.Rpi);
        when(discordClubRepository.getAllActiveDiscordClubs()).thenReturn(Arrays.asList(mockClub));
        when(discordClubRepository.getDiscordClubByGuildId(anyString())).thenReturn(Optional.of(mockClub));

        setupMockLeaderboardDataWithoutExpiration();

        discordClubManager.sendWeeklyLeaderboardUpdateDiscordMessageToAllClubs();

        verify(discordClubRepository).getAllActiveDiscordClubs();
        verify(jdaClient).sendEmbedWithImages(any(EmbeddedImagesMessageOptions.class));
    }

    @Test
    void testSendLeaderboardCompletedDiscordMessageFiltersZeroPointUsers() {
        DiscordClub mockClub = createMockDiscordClub("Test Club", Tag.Rpi);
        when(discordClubRepository.getAllActiveDiscordClubs()).thenReturn(Arrays.asList(mockClub));

        setupMockLeaderboardDataWithMixedScoreUsers();

        discordClubManager.sendLeaderboardCompletedDiscordMessageToAllClubs();

        verify(discordClubRepository).getAllActiveDiscordClubs();
        verify(jdaClient).sendEmbedWithImages(any(EmbeddedImagesMessageOptions.class));
    }

    @Test
    void testSendLeaderboardCompletedDiscordMessageAllUsersHaveZeroPoints() {
        DiscordClub mockClub = createMockDiscordClub("Test Club", Tag.Rpi);
        when(discordClubRepository.getAllActiveDiscordClubs()).thenReturn(Arrays.asList(mockClub));

        setupMockLeaderboardDataWithAllZeroScoreUsers();

        discordClubManager.sendLeaderboardCompletedDiscordMessageToAllClubs();

        ArgumentCaptor<EmbeddedImagesMessageOptions> captor =
                ArgumentCaptor.forClass(EmbeddedImagesMessageOptions.class);
        verify(jdaClient).sendEmbedWithImages(captor.capture());

        String description = captor.getValue().getDescription();
        assertTrue(description.contains("No one claimed a spot on this leaderboard"));
        assertFalse(description.contains("CONGRATS TO THE WINNERS"));
        assertFalse(description.contains("🥇"));
        assertFalse(description.contains("🥈"));
        assertFalse(description.contains("🥉"));
    }

    @Test
    void testSendWeeklyLeaderboardUpdateDiscordMessageFiltersZeroPointUsers() {
        DiscordClub mockClub = createMockDiscordClub("Test Club", Tag.Rpi);
        when(discordClubRepository.getAllActiveDiscordClubs()).thenReturn(Arrays.asList(mockClub));
        when(discordClubRepository.getDiscordClubByGuildId(anyString())).thenReturn(Optional.of(mockClub));

        setupMockLeaderboardDataWithExpirationAndMixedScoreUsers();

        discordClubManager.sendWeeklyLeaderboardUpdateDiscordMessageToAllClubs();

        verify(discordClubRepository).getAllActiveDiscordClubs();
        verify(jdaClient).sendEmbedWithImages(any(EmbeddedImagesMessageOptions.class));
    }

    @Test
    void testSendWeeklyLeaderboardUpdateDiscordMessageAllUsersHaveZeroPoints() {
        DiscordClub mockClub = createMockDiscordClub("Test Club", Tag.Rpi);
        when(discordClubRepository.getAllActiveDiscordClubs()).thenReturn(Arrays.asList(mockClub));
        when(discordClubRepository.getDiscordClubByGuildId(anyString())).thenReturn(Optional.of(mockClub));

        setupMockLeaderboardDataWithExpirationAndAllZeroScoreUsers();

        discordClubManager.sendWeeklyLeaderboardUpdateDiscordMessageToAllClubs();

        ArgumentCaptor<EmbeddedImagesMessageOptions> captor =
                ArgumentCaptor.forClass(EmbeddedImagesMessageOptions.class);
        verify(jdaClient).sendEmbedWithImages(captor.capture());

        String description = captor.getValue().getDescription();
        assertTrue(description.contains("no scores yet"));
        assertFalse(description.contains("🥇"));
        assertFalse(description.contains("🥈"));
        assertFalse(description.contains("🥉"));
    }

    @Test
    void testSendLeaderboardCompletedDiscordMessageOnlyOneUserWithPoints() {
        DiscordClub mockClub = createMockDiscordClub("Test Club", Tag.Rpi);
        when(discordClubRepository.getAllActiveDiscordClubs()).thenReturn(Arrays.asList(mockClub));

        setupMockLeaderboardDataWithOneUser();

        discordClubManager.sendLeaderboardCompletedDiscordMessageToAllClubs();

        ArgumentCaptor<EmbeddedImagesMessageOptions> captor =
                ArgumentCaptor.forClass(EmbeddedImagesMessageOptions.class);
        verify(jdaClient).sendEmbedWithImages(captor.capture());

        String description = captor.getValue().getDescription();
        assertTrue(description.contains("🥇"));
        assertFalse(description.contains("🥈"));
        assertFalse(description.contains("🥉"));
    }

    @Test
    void testSendLeaderboardCompletedDiscordMessageOnlyTwoUsersWithPoints() {
        DiscordClub mockClub = createMockDiscordClub("Test Club", Tag.Rpi);
        when(discordClubRepository.getAllActiveDiscordClubs()).thenReturn(Arrays.asList(mockClub));

        setupMockLeaderboardDataWithTwoUsers();

        discordClubManager.sendLeaderboardCompletedDiscordMessageToAllClubs();

        ArgumentCaptor<EmbeddedImagesMessageOptions> captor =
                ArgumentCaptor.forClass(EmbeddedImagesMessageOptions.class);
        verify(jdaClient).sendEmbedWithImages(captor.capture());

        String description = captor.getValue().getDescription();
        assertTrue(description.contains("🥇"));
        assertTrue(description.contains("🥈"));
        assertFalse(description.contains("🥉"));
    }

    @Test
    void testSendWeeklyLeaderboardUpdateDiscordMessageOnlyOneUserWithPoints() {
        DiscordClub mockClub = createMockDiscordClub("Test Club", Tag.Rpi);
        when(discordClubRepository.getAllActiveDiscordClubs()).thenReturn(Arrays.asList(mockClub));
        when(discordClubRepository.getDiscordClubByGuildId(anyString())).thenReturn(Optional.of(mockClub));

        setupMockLeaderboardDataWithExpirationAndOneUser();

        discordClubManager.sendWeeklyLeaderboardUpdateDiscordMessageToAllClubs();

        ArgumentCaptor<EmbeddedImagesMessageOptions> captor =
                ArgumentCaptor.forClass(EmbeddedImagesMessageOptions.class);
        verify(jdaClient).sendEmbedWithImages(captor.capture());

        String description = captor.getValue().getDescription();
        assertTrue(description.contains("🥇"));
        assertFalse(description.contains("🥈"));
        assertFalse(description.contains("🥉"));
    }

    @Test
    void testSendWeeklyLeaderboardUpdateDiscordMessageOnlyTwoUsersWithPoints() {
        DiscordClub mockClub = createMockDiscordClub("Test Club", Tag.Rpi);
        when(discordClubRepository.getAllActiveDiscordClubs()).thenReturn(Arrays.asList(mockClub));
        when(discordClubRepository.getDiscordClubByGuildId(anyString())).thenReturn(Optional.of(mockClub));

        setupMockLeaderboardDataWithExpirationAndTwoUsers();

        discordClubManager.sendWeeklyLeaderboardUpdateDiscordMessageToAllClubs();

        ArgumentCaptor<EmbeddedImagesMessageOptions> captor =
                ArgumentCaptor.forClass(EmbeddedImagesMessageOptions.class);
        verify(jdaClient).sendEmbedWithImages(captor.capture());

        String description = captor.getValue().getDescription();
        assertTrue(description.contains("🥇"));
        assertTrue(description.contains("🥈"));
        assertFalse(description.contains("🥉"));
    }

    @Test
    void testBuildWeeklyLeaderboardMessageForClub() {
        DiscordClub club = createMockDiscordClub("Mock Club", Tag.Rpi);

        when(discordClubRepository.getDiscordClubByGuildId(anyString())).thenReturn(Optional.of(club));

        setupMockLeaderboardData();

        MessageCreateData message = discordClubManager.buildLeaderboardMessageForClub(club.getId(), true);

        List<MessageEmbed> embeds = message.getEmbeds();
        assertEquals(1, embeds.size());

        String description = embeds.get(0).getDescription();
        assertNotNull(description);

        assertTrue(description.contains("Here is a weekly update"));
    }

    @Test
    void testSendTestEmbedMessageToClubNoGuildOrClub() {
        DiscordClub mockClub = mock(DiscordClub.class);

        when(mockClub.getId()).thenReturn("club-id-1");
        when(mockClub.getDiscordClubMetadata()).thenReturn(Optional.empty());

        boolean result = discordClubManager.sendTestEmbedMessageToClub(mockClub);

        assertFalse(result);
        verify(jdaClient, never()).sendEmbedWithImages(any());

        assertTrue(logWatcher.list.stream()
                .anyMatch(e -> e.getFormattedMessage().contains("Missing guildId or leaderboardChannelId")));
    }

    @Test
    void testSendTestEmbedMessageToClubFailure() {
        DiscordClub mockClub = createMockDiscordClub("Test Club", Tag.Rpi);

        doThrow(new RuntimeException()).when(jdaClient).sendEmbedWithImages(any(EmbeddedImagesMessageOptions.class));

        boolean result = discordClubManager.sendTestEmbedMessageToClub(mockClub);

        assertFalse(result);

        verify(jdaClient).sendEmbedWithImages(any(EmbeddedImagesMessageOptions.class));

        assertTrue(logWatcher.list.stream().anyMatch(e -> e.getFormattedMessage()
                .contains("Error in DiscordClubManager when sending test message")));
    }

    @Test
    void testSendTestEmbedMessageToClubSuccess() {
        DiscordClub mockClub = createMockDiscordClub("Test Club", Tag.Rpi);

        boolean result = discordClubManager.sendTestEmbedMessageToClub(mockClub);
        assertTrue(result);

        ArgumentCaptor<EmbeddedImagesMessageOptions> captor =
                ArgumentCaptor.forClass(EmbeddedImagesMessageOptions.class);
        verify(jdaClient).sendEmbedWithImages(captor.capture());

        String description = captor.getValue().getDescription();
        assertTrue(description.contains("test message"));
    }

    @Test
    void testRefreshSubmissionsSuccess() throws LeaderboardException {
        DiscordClub club = createMockDiscordClub("Test Club", Tag.Rpi);
        when(discordClubRepository.getDiscordClubByGuildId("guild-123")).thenReturn(Optional.of(club));

        User mockUser = mock(User.class);
        when(mockUser.getId()).thenReturn("user-id-1");
        when(leaderboardManager.refreshUserSubmissions("discord-456")).thenReturn(mockUser);

        Leaderboard mockLeaderboard = mock(Leaderboard.class);
        when(mockLeaderboard.getId()).thenReturn("leaderboard-id");
        when(mockLeaderboard.getName()).thenReturn("Week 10");
        when(leaderboardRepository.getRecentLeaderboardMetadata()).thenReturn(Optional.of(mockLeaderboard));

        UserWithScore scoredUser = mock(UserWithScore.class);
        when(scoredUser.getTotalScore()).thenReturn(42);
        when(userRepository.getUserWithScoreByIdAndLeaderboardId(
                        eq("user-id-1"), eq("leaderboard-id"), eq(UserFilterOptions.DEFAULT)))
                .thenReturn(Optional.of(scoredUser));

        Indexed<UserWithScore> globalIndex = Indexed.of(scoredUser, 5);
        Indexed<UserWithScore> clubIndex = Indexed.of(scoredUser, 2);
        when(leaderboardRepository.getGlobalRankedUserById("leaderboard-id", "user-id-1"))
                .thenReturn(Optional.of(globalIndex));
        when(leaderboardRepository.getFilteredRankedUserById(
                        eq("leaderboard-id"), eq("user-id-1"), any(LeaderboardFilterOptions.class)))
                .thenReturn(Optional.of(clubIndex));

        RefreshResultDto result = discordClubManager.refreshSubmissions("guild-123", "discord-456");

        assertEquals(42, result.getScore());
        assertEquals(5, result.getGlobalRank());
        assertEquals(2, result.getClubRank());
        assertEquals("Week 10", result.getLeaderboardName());
        assertEquals("Test Club", result.getClubName());

        verify(leaderboardManager).refreshUserSubmissions("discord-456");
        verify(discordClubRepository).getDiscordClubByGuildId("guild-123");
    }

    @Test
    void testRefreshSubmissionsClubNotFound() {
        when(discordClubRepository.getDiscordClubByGuildId("unknown-guild")).thenReturn(Optional.empty());

        LeaderboardException exception = assertThrows(
                LeaderboardException.class,
                () -> discordClubManager.refreshSubmissions("unknown-guild", "discord-456"));

        assertEquals("Club does not exist", exception.getTitle());
        assertEquals("This club does not exist!", exception.getDescription());

        verifyNoInteractions(leaderboardManager);
        verifyNoInteractions(userRepository);
    }

    @Test
    void testRefreshSubmissionsUserRefreshThrowsLeaderboardException() throws LeaderboardException {
        DiscordClub club = createMockDiscordClub("Test Club", Tag.Rpi);
        when(discordClubRepository.getDiscordClubByGuildId("guild-123")).thenReturn(Optional.of(club));

        when(leaderboardManager.refreshUserSubmissions("discord-456"))
                .thenThrow(new LeaderboardException(
                        "Cannot refresh submissions", "Your Discord Account is not linked to a LeetCode username."));

        LeaderboardException exception = assertThrows(
                LeaderboardException.class, () -> discordClubManager.refreshSubmissions("guild-123", "discord-456"));

        assertEquals("Cannot refresh submissions", exception.getTitle());
        assertEquals("Your Discord Account is not linked to a LeetCode username.", exception.getDescription());

        verify(leaderboardManager).refreshUserSubmissions("discord-456");
        verifyNoInteractions(userRepository);
    }

    @Test
    void testRefreshSubmissionsReturnsCorrectClubRankWithDifferentTags() throws LeaderboardException {
        DiscordClub club = createMockDiscordClub("Baruch Club", Tag.Baruch);
        when(discordClubRepository.getDiscordClubByGuildId("guild-789")).thenReturn(Optional.of(club));

        User mockUser = mock(User.class);
        when(mockUser.getId()).thenReturn("user-id-2");
        when(leaderboardManager.refreshUserSubmissions("discord-111")).thenReturn(mockUser);

        Leaderboard mockLeaderboard = mock(Leaderboard.class);
        when(mockLeaderboard.getId()).thenReturn("lb-id");
        when(mockLeaderboard.getName()).thenReturn("Week 5");
        when(leaderboardRepository.getRecentLeaderboardMetadata()).thenReturn(Optional.of(mockLeaderboard));

        UserWithScore scoredUser = mock(UserWithScore.class);
        when(scoredUser.getTotalScore()).thenReturn(100);
        when(userRepository.getUserWithScoreByIdAndLeaderboardId(
                        eq("user-id-2"), eq("lb-id"), eq(UserFilterOptions.DEFAULT)))
                .thenReturn(Optional.of(scoredUser));

        Indexed<UserWithScore> globalIndex = Indexed.of(scoredUser, 10);
        Indexed<UserWithScore> clubIndex = Indexed.of(scoredUser, 1);
        when(leaderboardRepository.getGlobalRankedUserById("lb-id", "user-id-2"))
                .thenReturn(Optional.of(globalIndex));
        when(leaderboardRepository.getFilteredRankedUserById(
                        eq("lb-id"), eq("user-id-2"), any(LeaderboardFilterOptions.class)))
                .thenReturn(Optional.of(clubIndex));

        RefreshResultDto result = discordClubManager.refreshSubmissions("guild-789", "discord-111");

        assertEquals(100, result.getScore());
        assertEquals(10, result.getGlobalRank());
        assertEquals(1, result.getClubRank());
        assertEquals("Week 5", result.getLeaderboardName());
        assertEquals("Baruch Club", result.getClubName());
    }

    @Test
    void testRefreshSubmissionsWithZeroScore() throws LeaderboardException {
        DiscordClub club = createMockDiscordClub("Test Club", Tag.Rpi);
        when(discordClubRepository.getDiscordClubByGuildId("guild-123")).thenReturn(Optional.of(club));

        User mockUser = mock(User.class);
        when(mockUser.getId()).thenReturn("user-id-1");
        when(leaderboardManager.refreshUserSubmissions("discord-456")).thenReturn(mockUser);

        Leaderboard mockLeaderboard = mock(Leaderboard.class);
        when(mockLeaderboard.getId()).thenReturn("leaderboard-id");
        when(mockLeaderboard.getName()).thenReturn("Week 1");
        when(leaderboardRepository.getRecentLeaderboardMetadata()).thenReturn(Optional.of(mockLeaderboard));

        UserWithScore scoredUser = mock(UserWithScore.class);
        when(scoredUser.getTotalScore()).thenReturn(0);
        when(userRepository.getUserWithScoreByIdAndLeaderboardId(
                        eq("user-id-1"), eq("leaderboard-id"), eq(UserFilterOptions.DEFAULT)))
                .thenReturn(Optional.of(scoredUser));

        Indexed<UserWithScore> globalIndex = Indexed.of(scoredUser, 50);
        Indexed<UserWithScore> clubIndex = Indexed.of(scoredUser, 20);
        when(leaderboardRepository.getGlobalRankedUserById("leaderboard-id", "user-id-1"))
                .thenReturn(Optional.of(globalIndex));
        when(leaderboardRepository.getFilteredRankedUserById(
                        eq("leaderboard-id"), eq("user-id-1"), any(LeaderboardFilterOptions.class)))
                .thenReturn(Optional.of(clubIndex));

        RefreshResultDto result = discordClubManager.refreshSubmissions("guild-123", "discord-456");

        assertEquals(0, result.getScore());
        assertEquals(50, result.getGlobalRank());
        assertEquals(20, result.getClubRank());
    }

    private DiscordClub createMockDiscordClub(final String name, final Tag tag) {
        DiscordClubMetadata metadata = mock(DiscordClubMetadata.class);
        when(metadata.getGuildId()).thenReturn(Optional.of("123456789"));
        when(metadata.getLeaderboardChannelId()).thenReturn(Optional.of("987654321"));

        DiscordClub club = mock(DiscordClub.class);
        when(club.getId()).thenReturn("707b035a-078a-11f1-b78e-03f62df4519f");
        when(club.getName()).thenReturn(name);
        when(club.getTag()).thenReturn(tag);
        when(club.getDiscordClubMetadata()).thenReturn(Optional.of(metadata));

        return club;
    }

    private DiscordClub createMockDiscordClubWithoutMetadata(final String name, final Tag tag) {
        DiscordClub club = mock(DiscordClub.class);
        when(club.getName()).thenReturn(name);
        when(club.getTag()).thenReturn(tag);
        when(club.getDiscordClubMetadata()).thenReturn(Optional.empty());

        return club;
    }

    private DiscordClub createMockDiscordClubWithPartialMetadata(final String name, final Tag tag) {
        DiscordClubMetadata metadata = mock(DiscordClubMetadata.class);
        when(metadata.getGuildId()).thenReturn(Optional.of("123456789"));
        when(metadata.getLeaderboardChannelId()).thenReturn(Optional.empty());

        DiscordClub club = mock(DiscordClub.class);
        when(club.getName()).thenReturn(name);
        when(club.getTag()).thenReturn(tag);
        when(club.getDiscordClubMetadata()).thenReturn(Optional.of(metadata));

        return club;
    }

    private void setupMockLeaderboardData() {
        Leaderboard mockLeaderboard = mock(Leaderboard.class);
        when(mockLeaderboard.getId()).thenReturn("leaderboard-id");
        when(mockLeaderboard.getName()).thenReturn("Test Leaderboard");
        when(mockLeaderboard.getShouldExpireBy())
                .thenReturn(Optional.of(LocalDateTime.now().plusDays(7)));

        when(leaderboardRepository.getRecentLeaderboardMetadata()).thenReturn(Optional.of(mockLeaderboard));
        when(leaderboardRepository.getLeaderboardUserCountById(eq("leaderboard-id"), any()))
                .thenReturn(25);

        List<UserWithScore> mockUsers = createMockUsers();
        when(leaderboardRepository.getLeaderboardUsersById(eq("leaderboard-id"), any(LeaderboardFilterOptions.class)))
                .thenReturn(mockUsers);

        when(playwrightClient.getCodebloomLeaderboardScreenshot(anyInt(), any(Tag.class)))
                .thenReturn("mock-screenshot".getBytes());

        when(serverUrlUtils.getUrl()).thenReturn("http://localhost:3000");
    }

    private void setupMockLeaderboardDataWithExpiration() {
        setupMockLeaderboardData();

        Leaderboard mockLeaderboard = mock(Leaderboard.class);
        when(mockLeaderboard.getId()).thenReturn("leaderboard-id");
        when(mockLeaderboard.getName()).thenReturn("Test Leaderboard");
        when(mockLeaderboard.getShouldExpireBy())
                .thenReturn(Optional.of(LocalDateTime.now().plusDays(7)));

        when(leaderboardRepository.getRecentLeaderboardMetadata()).thenReturn(Optional.of(mockLeaderboard));
    }

    private void setupMockLeaderboardDataWithoutExpiration() {
        setupMockLeaderboardData();

        Leaderboard mockLeaderboard = mock(Leaderboard.class);
        when(mockLeaderboard.getId()).thenReturn("leaderboard-id");
        when(mockLeaderboard.getName()).thenReturn("Test Leaderboard");
        when(mockLeaderboard.getShouldExpireBy()).thenReturn(Optional.empty());

        when(leaderboardRepository.getRecentLeaderboardMetadata()).thenReturn(Optional.of(mockLeaderboard));
    }

    private List<UserWithScore> createMockUsers() {
        UserWithScore user1 = mock(UserWithScore.class);
        when(user1.getDiscordId()).thenReturn("discord1");
        when(user1.getTotalScore()).thenReturn(100);

        UserWithScore user2 = mock(UserWithScore.class);
        when(user2.getDiscordId()).thenReturn("discord2");
        when(user2.getTotalScore()).thenReturn(80);

        UserWithScore user3 = mock(UserWithScore.class);
        when(user3.getDiscordId()).thenReturn("discord3");
        when(user3.getTotalScore()).thenReturn(60);

        return Arrays.asList(user1, user2, user3);
    }

    private List<UserWithScore> createMockUsersWithMixedScores() {
        UserWithScore user1 = mock(UserWithScore.class);
        when(user1.getDiscordId()).thenReturn("discord1");
        when(user1.getTotalScore()).thenReturn(100);

        UserWithScore user2 = mock(UserWithScore.class);
        when(user2.getDiscordId()).thenReturn("discord2");
        when(user2.getTotalScore()).thenReturn(0);

        UserWithScore user3 = mock(UserWithScore.class);
        when(user3.getDiscordId()).thenReturn("discord3");
        when(user3.getTotalScore()).thenReturn(50);

        UserWithScore user4 = mock(UserWithScore.class);
        when(user4.getDiscordId()).thenReturn("discord4");
        when(user4.getTotalScore()).thenReturn(0);

        return Arrays.asList(user1, user2, user3, user4);
    }

    private List<UserWithScore> createMockUsersAllZeroScores() {
        UserWithScore user1 = mock(UserWithScore.class);
        when(user1.getDiscordId()).thenReturn("discord1");
        when(user1.getTotalScore()).thenReturn(0);

        UserWithScore user2 = mock(UserWithScore.class);
        when(user2.getDiscordId()).thenReturn("discord2");
        when(user2.getTotalScore()).thenReturn(0);

        UserWithScore user3 = mock(UserWithScore.class);
        when(user3.getDiscordId()).thenReturn("discord3");
        when(user3.getTotalScore()).thenReturn(0);

        return Arrays.asList(user1, user2, user3);
    }

    private void setupMockLeaderboardDataWithMixedScoreUsers() {
        Leaderboard mockLeaderboard = mock(Leaderboard.class);
        when(mockLeaderboard.getId()).thenReturn("leaderboard-id");
        when(mockLeaderboard.getName()).thenReturn("Test Leaderboard");

        when(leaderboardRepository.getRecentLeaderboardMetadata()).thenReturn(Optional.of(mockLeaderboard));

        List<UserWithScore> mockUsers = createMockUsersWithMixedScores();
        when(leaderboardRepository.getLeaderboardUsersById(eq("leaderboard-id"), any(LeaderboardFilterOptions.class)))
                .thenReturn(mockUsers);

        when(serverUrlUtils.getUrl()).thenReturn("http://localhost:3000");
    }

    private void setupMockLeaderboardDataWithAllZeroScoreUsers() {
        Leaderboard mockLeaderboard = mock(Leaderboard.class);
        when(mockLeaderboard.getId()).thenReturn("leaderboard-id");
        when(mockLeaderboard.getName()).thenReturn("Test Leaderboard");

        when(leaderboardRepository.getRecentLeaderboardMetadata()).thenReturn(Optional.of(mockLeaderboard));

        List<UserWithScore> mockUsers = createMockUsersAllZeroScores();
        when(leaderboardRepository.getLeaderboardUsersById(eq("leaderboard-id"), any(LeaderboardFilterOptions.class)))
                .thenReturn(mockUsers);

        when(serverUrlUtils.getUrl()).thenReturn("http://localhost:3000");
    }

    private void setupMockLeaderboardDataWithExpirationAndMixedScoreUsers() {
        setupMockLeaderboardDataWithMixedScoreUsers();

        Leaderboard mockLeaderboard = mock(Leaderboard.class);
        when(mockLeaderboard.getId()).thenReturn("leaderboard-id");
        when(mockLeaderboard.getName()).thenReturn("Test Leaderboard");
        when(mockLeaderboard.getShouldExpireBy())
                .thenReturn(Optional.of(LocalDateTime.now().plusDays(7)));

        when(leaderboardRepository.getRecentLeaderboardMetadata()).thenReturn(Optional.of(mockLeaderboard));
    }

    private void setupMockLeaderboardDataWithExpirationAndAllZeroScoreUsers() {
        setupMockLeaderboardDataWithAllZeroScoreUsers();

        Leaderboard mockLeaderboard = mock(Leaderboard.class);
        when(mockLeaderboard.getId()).thenReturn("leaderboard-id");
        when(mockLeaderboard.getName()).thenReturn("Test Leaderboard");
        when(mockLeaderboard.getShouldExpireBy())
                .thenReturn(Optional.of(LocalDateTime.now().plusDays(7)));

        when(leaderboardRepository.getRecentLeaderboardMetadata()).thenReturn(Optional.of(mockLeaderboard));
    }

    private List<UserWithScore> createMockUsersOneUser() {
        UserWithScore user1 = mock(UserWithScore.class);
        when(user1.getDiscordId()).thenReturn("discord1");
        when(user1.getTotalScore()).thenReturn(100);

        return Arrays.asList(user1);
    }

    private List<UserWithScore> createMockUsersTwoUsers() {
        UserWithScore user1 = mock(UserWithScore.class);
        when(user1.getDiscordId()).thenReturn("discord1");
        when(user1.getTotalScore()).thenReturn(100);

        UserWithScore user2 = mock(UserWithScore.class);
        when(user2.getDiscordId()).thenReturn("discord2");
        when(user2.getTotalScore()).thenReturn(80);

        return Arrays.asList(user1, user2);
    }

    private void setupMockLeaderboardDataWithOneUser() {
        Leaderboard mockLeaderboard = mock(Leaderboard.class);
        when(mockLeaderboard.getId()).thenReturn("leaderboard-id");
        when(mockLeaderboard.getName()).thenReturn("Test Leaderboard");

        when(leaderboardRepository.getRecentLeaderboardMetadata()).thenReturn(Optional.of(mockLeaderboard));

        List<UserWithScore> mockUsers = createMockUsersOneUser();
        when(leaderboardRepository.getLeaderboardUsersById(eq("leaderboard-id"), any(LeaderboardFilterOptions.class)))
                .thenReturn(mockUsers);

        when(serverUrlUtils.getUrl()).thenReturn("http://localhost:3000");
    }

    private void setupMockLeaderboardDataWithTwoUsers() {
        Leaderboard mockLeaderboard = mock(Leaderboard.class);
        when(mockLeaderboard.getId()).thenReturn("leaderboard-id");
        when(mockLeaderboard.getName()).thenReturn("Test Leaderboard");

        when(leaderboardRepository.getRecentLeaderboardMetadata()).thenReturn(Optional.of(mockLeaderboard));

        List<UserWithScore> mockUsers = createMockUsersTwoUsers();
        when(leaderboardRepository.getLeaderboardUsersById(eq("leaderboard-id"), any(LeaderboardFilterOptions.class)))
                .thenReturn(mockUsers);

        when(serverUrlUtils.getUrl()).thenReturn("http://localhost:3000");
    }

    private void setupMockLeaderboardDataWithExpirationAndOneUser() {
        setupMockLeaderboardDataWithOneUser();

        Leaderboard mockLeaderboard = mock(Leaderboard.class);
        when(mockLeaderboard.getId()).thenReturn("leaderboard-id");
        when(mockLeaderboard.getName()).thenReturn("Test Leaderboard");
        when(mockLeaderboard.getShouldExpireBy())
                .thenReturn(Optional.of(LocalDateTime.now().plusDays(7)));

        when(leaderboardRepository.getRecentLeaderboardMetadata()).thenReturn(Optional.of(mockLeaderboard));
    }

    private void setupMockLeaderboardDataWithExpirationAndTwoUsers() {
        setupMockLeaderboardDataWithTwoUsers();

        Leaderboard mockLeaderboard = mock(Leaderboard.class);
        when(mockLeaderboard.getId()).thenReturn("leaderboard-id");
        when(mockLeaderboard.getName()).thenReturn("Test Leaderboard");
        when(mockLeaderboard.getShouldExpireBy())
                .thenReturn(Optional.of(LocalDateTime.now().plusDays(7)));

        when(leaderboardRepository.getRecentLeaderboardMetadata()).thenReturn(Optional.of(mockLeaderboard));
    }
}
