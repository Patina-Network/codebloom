package org.patinanetwork.codebloom.common.components;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.github.javafaker.Faker;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.apache.logging.log4j.util.Strings;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.patinanetwork.codebloom.common.db.models.achievements.Achievement;
import org.patinanetwork.codebloom.common.db.models.achievements.AchievementPlaceEnum;
import org.patinanetwork.codebloom.common.db.models.leaderboard.Leaderboard;
import org.patinanetwork.codebloom.common.db.models.user.UserWithScore;
import org.patinanetwork.codebloom.common.db.models.usertag.Tag;
import org.patinanetwork.codebloom.common.db.models.usertag.UserTag;
import org.patinanetwork.codebloom.common.db.repos.achievements.AchievementRepository;
import org.patinanetwork.codebloom.common.db.repos.leaderboard.LeaderboardRepository;
import org.patinanetwork.codebloom.common.db.repos.leaderboard.options.LeaderboardFilterGenerator;
import org.patinanetwork.codebloom.common.db.repos.leaderboard.options.LeaderboardFilterGeneratorTest;
import org.patinanetwork.codebloom.common.db.repos.leaderboard.options.LeaderboardFilterOptions;
import org.patinanetwork.codebloom.common.db.repos.user.UserRepository;
import org.patinanetwork.codebloom.common.leetcode.LeetcodeClient;
import org.patinanetwork.codebloom.common.page.Indexed;
import org.patinanetwork.codebloom.common.submissions.SubmissionsHandler;
import org.patinanetwork.codebloom.common.time.StandardizedLocalDateTime;

public class LeaderboardManagerTest {

    private final LeaderboardManager leaderboardManager;
    private final Faker faker;

    private LeaderboardRepository leaderboardRepository = mock(LeaderboardRepository.class);
    private AchievementRepository achievementRepository = mock(AchievementRepository.class);
    private UserRepository userRepository = mock(UserRepository.class);
    private LeetcodeClient leetcodeClient = mock(LeetcodeClient.class);
    private SubmissionsHandler submissionsHandler = mock(SubmissionsHandler.class);

    private final int validLeaderboardTags = LeaderboardFilterGeneratorTest.VALID_LEADERBOARD_TAGS.size();

    public LeaderboardManagerTest() {
        this.leaderboardManager = new LeaderboardManager(
                leaderboardRepository, achievementRepository, userRepository, leetcodeClient, submissionsHandler);
        this.faker = Faker.instance();
    }

    private String randomSnowflake() {
        return String.valueOf(faker.number().randomNumber(18, true));
    }

    private UserWithScore.UserWithScoreBuilder<?, ?> randomPartialUserWithScore() {
        return UserWithScore.builder()
                .id(UUID.randomUUID().toString())
                .discordId(randomSnowflake())
                .discordName(faker.name().username())
                .leetcodeUsername(faker.name().username())
                .admin(faker.bool().bool())
                .verifyKey(faker.crypto().md5());
    }

    private void assertAchievement(
            final Achievement achievement,
            final Tag leaderboard,
            final AchievementPlaceEnum placeEnum,
            final String userId,
            final String leaderboardId) {
        assertEquals(leaderboard, achievement.getLeaderboard());
        assertEquals(placeEnum, achievement.getPlace());
        assertEquals(userId, achievement.getUserId());
        assertEquals(leaderboardId, achievement.getLeaderboardId());
    }

    private static Stream<Arguments> tagGenerator() {
        return LeaderboardFilterGenerator.generateAllSupportedTagToggles().stream()
                .map(tag -> Arguments.of(tag.getLeft()));
    }

    @Test
    void testGetLeaderboardUsersDelegation() {
        String leaderboardId = UUID.randomUUID().toString();
        LeaderboardFilterOptions options = LeaderboardFilterOptions.DEFAULT;

        int expectedCount = 1;
        when(leaderboardRepository.getLeaderboardUserCountById(leaderboardId, options))
                .thenReturn(expectedCount);

        List<Indexed<UserWithScore>> filteredUsers = Indexed.ofDefaultList(
                List.of(randomPartialUserWithScore().totalScore(80_000).build()));

        when(leaderboardRepository.getRankedIndexedLeaderboardUsersById(leaderboardId, options))
                .thenReturn(filteredUsers);

        var page = leaderboardManager.getLeaderboardUsers(leaderboardId, options, false);

        assertNotNull(page);
        assertEquals(filteredUsers.size(), page.getItems().size());
        assertEquals(
                filteredUsers.get(0).getItem().getId(),
                page.getItems().get(0).getItem().getId());

        verify(leaderboardRepository, times(1)).getLeaderboardUserCountById(leaderboardId, options);
        verify(leaderboardRepository, times(1)).getRankedIndexedLeaderboardUsersById(leaderboardId, options);
        verify(leaderboardRepository, times(0)).getGlobalRankedIndexedLeaderboardUsersById(any(), any());
    }

    @Test
    void testWithNoAvailableLeaderboard() {
        when(leaderboardRepository.getRecentLeaderboardMetadata()).thenReturn(Optional.empty());

        leaderboardManager.generateAchievementsForAllWinners();

        verify(leaderboardRepository, times(0)).getRankedIndexedLeaderboardUsersById(any(), any());
        verify(leaderboardRepository, times(0)).getGlobalRankedIndexedLeaderboardUsersById(any(), any());
        verify(achievementRepository, times(0)).createAchievement(any());
    }

    @Test
    void testWithAvailableLeaderboardButNoUsers() {
        var latestLeaderboard = Leaderboard.builder()
                .id(UUID.randomUUID().toString())
                .name("Test Leaderboard")
                .createdAt(StandardizedLocalDateTime.now())
                .build();

        when(leaderboardRepository.getRecentLeaderboardMetadata()).thenReturn(Optional.of(latestLeaderboard));
        when(leaderboardRepository.getRankedIndexedLeaderboardUsersById(eq(latestLeaderboard.getId()), any()))
                .thenReturn(List.of());

        leaderboardManager.generateAchievementsForAllWinners();

        verify(leaderboardRepository, times(validLeaderboardTags)).getRankedIndexedLeaderboardUsersById(any(), any());
        verify(leaderboardRepository, times(1)).getGlobalRankedIndexedLeaderboardUsersById(any(), any());
        verify(achievementRepository, times(0)).createAchievement(any());
    }

    @Test
    void testWithAvailableLeaderboardAndTwoWinnersGlobal() {
        var latestLeaderboard = Leaderboard.builder()
                .id(UUID.randomUUID().toString())
                .name("Test Leaderboard")
                .createdAt(StandardizedLocalDateTime.now())
                .build();

        var winners = Indexed.ofDefaultList(List.of(
                randomPartialUserWithScore().totalScore(150_000).build(),
                randomPartialUserWithScore().totalScore(70_000).build()));

        when(leaderboardRepository.getRecentLeaderboardMetadata()).thenReturn(Optional.of(latestLeaderboard));
        when(leaderboardRepository.getGlobalRankedIndexedLeaderboardUsersById(eq(latestLeaderboard.getId()), any()))
                .thenReturn(winners);

        leaderboardManager.generateAchievementsForAllWinners();

        ArgumentCaptor<Achievement> captor = ArgumentCaptor.forClass(Achievement.class);
        verify(achievementRepository, times(winners.size())).createAchievement(captor.capture());

        verify(leaderboardRepository, times(validLeaderboardTags)).getRankedIndexedLeaderboardUsersById(any(), any());
        verify(leaderboardRepository, times(1)).getGlobalRankedIndexedLeaderboardUsersById(any(), any());

        var achievements = captor.getAllValues();
        var userOneAchievement = achievements.get(0);
        var userTwoAchievement = achievements.get(1);
        assertAchievement(
                userOneAchievement,
                null,
                AchievementPlaceEnum.ONE,
                winners.get(0).getItem().getId(),
                latestLeaderboard.getId());
        assertAchievement(
                userTwoAchievement,
                null,
                AchievementPlaceEnum.TWO,
                winners.get(1).getItem().getId(),
                latestLeaderboard.getId());
    }

    @Test
    void testWithAvailableLeaderboardAndTwoWinnersGlobalWithOneValidTag() {
        var latestLeaderboard = Leaderboard.builder()
                .id(UUID.randomUUID().toString())
                .name("Test Leaderboard")
                .createdAt(StandardizedLocalDateTime.now())
                .build();

        var winners = Indexed.ofDefaultList(List.of(
                randomPartialUserWithScore().totalScore(150_000).build(),
                randomPartialUserWithScore().totalScore(70_000).build()));

        winners.forEach(winner -> {
            var user = winner.getItem();
            user.setTags(List.of(UserTag.builder()
                    .id(UUID.randomUUID().toString())
                    .tag(Tag.Sbu)
                    .userId(user.getId())
                    .build()));
        });

        when(leaderboardRepository.getRecentLeaderboardMetadata()).thenReturn(Optional.of(latestLeaderboard));
        when(leaderboardRepository.getGlobalRankedIndexedLeaderboardUsersById(eq(latestLeaderboard.getId()), any()))
                .thenReturn(winners);

        when(leaderboardRepository.getRankedIndexedLeaderboardUsersById(
                        eq(latestLeaderboard.getId()), argThat(opt -> opt.isSbu())))
                .thenReturn(winners);

        leaderboardManager.generateAchievementsForAllWinners();

        ArgumentCaptor<Achievement> captor = ArgumentCaptor.forClass(Achievement.class);
        verify(achievementRepository, times(winners.size() * 2)).createAchievement(captor.capture());

        verify(leaderboardRepository, times(validLeaderboardTags)).getRankedIndexedLeaderboardUsersById(any(), any());
        verify(leaderboardRepository, times(1)).getGlobalRankedIndexedLeaderboardUsersById(any(), any());

        var achievements = captor.getAllValues();
        var userOneSbuAchievement = achievements.get(0);
        var userTwoSbuAchievement = achievements.get(1);
        assertAchievement(
                userOneSbuAchievement,
                Tag.Sbu,
                AchievementPlaceEnum.ONE,
                winners.get(0).getItem().getId(),
                latestLeaderboard.getId());
        assertAchievement(
                userTwoSbuAchievement,
                Tag.Sbu,
                AchievementPlaceEnum.TWO,
                winners.get(1).getItem().getId(),
                latestLeaderboard.getId());
        var userOneGlobalAchievement = achievements.get(2);
        var userTwoGlobalAchievement = achievements.get(3);
        assertAchievement(
                userOneGlobalAchievement,
                null,
                AchievementPlaceEnum.ONE,
                winners.get(0).getItem().getId(),
                latestLeaderboard.getId());
        assertAchievement(
                userTwoGlobalAchievement,
                null,
                AchievementPlaceEnum.TWO,
                winners.get(1).getItem().getId(),
                latestLeaderboard.getId());
    }

    @Test
    void testWithAvailableLeaderboardAndTwoWinnersGlobalWithTwoValidTags() {
        var latestLeaderboard = Leaderboard.builder()
                .id(UUID.randomUUID().toString())
                .name("Test Leaderboard")
                .createdAt(StandardizedLocalDateTime.now())
                .build();

        var winners = Indexed.ofDefaultList(List.of(
                randomPartialUserWithScore().totalScore(150_000).build(),
                randomPartialUserWithScore().totalScore(70_000).build()));

        winners.forEach(winner -> {
            var user = winner.getItem();
            user.setTags(List.of(
                    UserTag.builder()
                            .id(UUID.randomUUID().toString())
                            .tag(Tag.Sbu)
                            .userId(user.getId())
                            .build(),
                    UserTag.builder()
                            .id(UUID.randomUUID().toString())
                            .tag(Tag.Patina)
                            .userId(user.getId())
                            .build()));
        });

        when(leaderboardRepository.getRecentLeaderboardMetadata()).thenReturn(Optional.of(latestLeaderboard));
        when(leaderboardRepository.getGlobalRankedIndexedLeaderboardUsersById(eq(latestLeaderboard.getId()), any()))
                .thenReturn(winners);

        when(leaderboardRepository.getRankedIndexedLeaderboardUsersById(
                        eq(latestLeaderboard.getId()), argThat(opt -> opt.isSbu())))
                .thenReturn(winners);

        when(leaderboardRepository.getRankedIndexedLeaderboardUsersById(
                        eq(latestLeaderboard.getId()), argThat(opt -> opt.isPatina())))
                .thenReturn(winners);

        leaderboardManager.generateAchievementsForAllWinners();

        ArgumentCaptor<Achievement> captor = ArgumentCaptor.forClass(Achievement.class);
        verify(achievementRepository, times(winners.size() * 3)).createAchievement(captor.capture());

        verify(leaderboardRepository, times(validLeaderboardTags)).getRankedIndexedLeaderboardUsersById(any(), any());
        verify(leaderboardRepository, times(1)).getGlobalRankedIndexedLeaderboardUsersById(any(), any());

        /** keep in mind the order of how we check filters is ordered. check LeaderboardFilterGenerator for order. */
        var achievements = captor.getAllValues();
        var userOneSbuAchievement = achievements.get(0);
        var userTwoSbuAchievement = achievements.get(1);
        assertAchievement(
                userOneSbuAchievement,
                Tag.Sbu,
                AchievementPlaceEnum.ONE,
                winners.get(0).getItem().getId(),
                latestLeaderboard.getId());
        assertAchievement(
                userTwoSbuAchievement,
                Tag.Sbu,
                AchievementPlaceEnum.TWO,
                winners.get(1).getItem().getId(),
                latestLeaderboard.getId());
        var userOnePatinaAchievement = achievements.get(2);
        var userTwoPatinaAchievement = achievements.get(3);
        assertAchievement(
                userOnePatinaAchievement,
                Tag.Patina,
                AchievementPlaceEnum.ONE,
                winners.get(0).getItem().getId(),
                latestLeaderboard.getId());
        assertAchievement(
                userTwoPatinaAchievement,
                Tag.Patina,
                AchievementPlaceEnum.TWO,
                winners.get(1).getItem().getId(),
                latestLeaderboard.getId());
        var userOneGlobalAchievement = achievements.get(4);
        var userTwoGlobalAchievement = achievements.get(5);
        assertAchievement(
                userOneGlobalAchievement,
                null,
                AchievementPlaceEnum.ONE,
                winners.get(0).getItem().getId(),
                latestLeaderboard.getId());
        assertAchievement(
                userTwoGlobalAchievement,
                null,
                AchievementPlaceEnum.TWO,
                winners.get(1).getItem().getId(),
                latestLeaderboard.getId());
    }

    @Test
    void testWithAvailableLeaderboardAndTwoWinnersGlobalWithOneValidOneInvalidTag() {
        var latestLeaderboard = Leaderboard.builder()
                .id(UUID.randomUUID().toString())
                .name("Test Leaderboard")
                .createdAt(StandardizedLocalDateTime.now())
                .build();

        var winners = Indexed.ofDefaultList(List.of(
                randomPartialUserWithScore().totalScore(150_000).build(),
                randomPartialUserWithScore().totalScore(70_000).build()));

        winners.forEach(winner -> {
            var user = winner.getItem();
            user.setTags(List.of(
                    UserTag.builder()
                            .id(UUID.randomUUID().toString())
                            .tag(Tag.Sbu)
                            .userId(user.getId())
                            .build(),
                    UserTag.builder()
                            .id(UUID.randomUUID().toString())
                            .tag(Tag.Gwc)
                            .userId(user.getId())
                            .build()));
        });

        when(leaderboardRepository.getRecentLeaderboardMetadata()).thenReturn(Optional.of(latestLeaderboard));
        when(leaderboardRepository.getGlobalRankedIndexedLeaderboardUsersById(eq(latestLeaderboard.getId()), any()))
                .thenReturn(winners);

        when(leaderboardRepository.getRankedIndexedLeaderboardUsersById(
                        eq(latestLeaderboard.getId()), argThat(opt -> opt.isSbu())))
                .thenReturn(winners);

        when(leaderboardRepository.getRankedIndexedLeaderboardUsersById(
                        eq(latestLeaderboard.getId()), argThat(opt -> opt.isGwc())))
                .thenReturn(winners);

        leaderboardManager.generateAchievementsForAllWinners();

        ArgumentCaptor<Achievement> captor = ArgumentCaptor.forClass(Achievement.class);
        verify(achievementRepository, times(winners.size() * 3)).createAchievement(captor.capture());

        verify(leaderboardRepository, times(validLeaderboardTags)).getRankedIndexedLeaderboardUsersById(any(), any());
        verify(leaderboardRepository, times(1)).getGlobalRankedIndexedLeaderboardUsersById(any(), any());

        /** keep in mind the order of how we check filters is ordered. check LeaderboardFilterGenerator for order. */
        var achievements = captor.getAllValues();
        var userOneSbuAchievement = achievements.get(0);
        var userTwoSbuAchievement = achievements.get(1);
        assertAchievement(
                userOneSbuAchievement,
                Tag.Sbu,
                AchievementPlaceEnum.ONE,
                winners.get(0).getItem().getId(),
                latestLeaderboard.getId());
        assertAchievement(
                userTwoSbuAchievement,
                Tag.Sbu,
                AchievementPlaceEnum.TWO,
                winners.get(1).getItem().getId(),
                latestLeaderboard.getId());
        var userOneGwcAchievement = achievements.get(2);
        var userTwoGwcAchievement = achievements.get(3);
        assertAchievement(
                userOneGwcAchievement,
                Tag.Gwc,
                AchievementPlaceEnum.ONE,
                winners.get(0).getItem().getId(),
                latestLeaderboard.getId());
        assertAchievement(
                userTwoGwcAchievement,
                Tag.Gwc,
                AchievementPlaceEnum.TWO,
                winners.get(1).getItem().getId(),
                latestLeaderboard.getId());
        var userOneGlobalAchievement = achievements.get(4);
        var userTwoGlobalAchievement = achievements.get(5);
        assertAchievement(
                userOneGlobalAchievement,
                null,
                AchievementPlaceEnum.ONE,
                winners.get(0).getItem().getId(),
                latestLeaderboard.getId());
        assertAchievement(
                userTwoGlobalAchievement,
                null,
                AchievementPlaceEnum.TWO,
                winners.get(1).getItem().getId(),
                latestLeaderboard.getId());
    }

    @Test
    void testWithAvailableLeaderboardAndTwoWinnersWithOnlyGwcTag() {
        var latestLeaderboard = Leaderboard.builder()
                .id(UUID.randomUUID().toString())
                .name("Test Leaderboard")
                .createdAt(StandardizedLocalDateTime.now())
                .build();

        var winners = Indexed.ofDefaultList(List.of(
                randomPartialUserWithScore().totalScore(150_000).build(),
                randomPartialUserWithScore().totalScore(70_000).build()));

        winners.forEach(winner -> {
            var user = winner.getItem();
            user.setTags(List.of(UserTag.builder()
                    .id(UUID.randomUUID().toString())
                    .tag(Tag.Gwc)
                    .userId(user.getId())
                    .build()));
        });

        when(leaderboardRepository.getRecentLeaderboardMetadata()).thenReturn(Optional.of(latestLeaderboard));
        when(leaderboardRepository.getGlobalRankedIndexedLeaderboardUsersById(eq(latestLeaderboard.getId()), any()))
                .thenReturn(winners);

        when(leaderboardRepository.getRankedIndexedLeaderboardUsersById(
                        eq(latestLeaderboard.getId()), argThat(opt -> opt.isGwc())))
                .thenReturn(winners);

        leaderboardManager.generateAchievementsForAllWinners();

        ArgumentCaptor<Achievement> captor = ArgumentCaptor.forClass(Achievement.class);
        verify(achievementRepository, times(winners.size() * 2)).createAchievement(captor.capture());

        verify(leaderboardRepository, times(validLeaderboardTags)).getRankedIndexedLeaderboardUsersById(any(), any());
        verify(leaderboardRepository, times(1)).getGlobalRankedIndexedLeaderboardUsersById(any(), any());

        var achievements = captor.getAllValues();
        var userOneGwcAchievement = achievements.get(0);
        var userTwoGwcAchievement = achievements.get(1);
        assertAchievement(
                userOneGwcAchievement,
                Tag.Gwc,
                AchievementPlaceEnum.ONE,
                winners.get(0).getItem().getId(),
                latestLeaderboard.getId());
        assertAchievement(
                userTwoGwcAchievement,
                Tag.Gwc,
                AchievementPlaceEnum.TWO,
                winners.get(1).getItem().getId(),
                latestLeaderboard.getId());
        var userOneGlobalAchievement = achievements.get(2);
        var userTwoGlobalAchievement = achievements.get(3);
        assertAchievement(
                userOneGlobalAchievement,
                null,
                AchievementPlaceEnum.ONE,
                winners.get(0).getItem().getId(),
                latestLeaderboard.getId());
        assertAchievement(
                userTwoGlobalAchievement,
                null,
                AchievementPlaceEnum.TWO,
                winners.get(1).getItem().getId(),
                latestLeaderboard.getId());
    }

    @Test
    void testWithAvailableLeaderboardAndTwoWinnersWithGwcAndMhcPlusPlusTags() {
        var latestLeaderboard = Leaderboard.builder()
                .id(UUID.randomUUID().toString())
                .name("Test Leaderboard")
                .createdAt(StandardizedLocalDateTime.now())
                .build();

        var winners = Indexed.ofDefaultList(List.of(
                randomPartialUserWithScore().totalScore(150_000).build(),
                randomPartialUserWithScore().totalScore(70_000).build()));

        winners.forEach(winner -> {
            var user = winner.getItem();
            user.setTags(List.of(
                    UserTag.builder()
                            .id(UUID.randomUUID().toString())
                            .tag(Tag.Gwc)
                            .userId(user.getId())
                            .build(),
                    UserTag.builder()
                            .id(UUID.randomUUID().toString())
                            .tag(Tag.MHCPlusPlus)
                            .userId(user.getId())
                            .build()));
        });

        when(leaderboardRepository.getRecentLeaderboardMetadata()).thenReturn(Optional.of(latestLeaderboard));
        when(leaderboardRepository.getGlobalRankedIndexedLeaderboardUsersById(eq(latestLeaderboard.getId()), any()))
                .thenReturn(winners);

        when(leaderboardRepository.getRankedIndexedLeaderboardUsersById(
                        eq(latestLeaderboard.getId()), argThat(opt -> opt.isGwc())))
                .thenReturn(winners);

        when(leaderboardRepository.getRankedIndexedLeaderboardUsersById(
                        eq(latestLeaderboard.getId()), argThat(opt -> opt.isMhcplusplus())))
                .thenReturn(winners);

        leaderboardManager.generateAchievementsForAllWinners();

        ArgumentCaptor<Achievement> captor = ArgumentCaptor.forClass(Achievement.class);
        verify(achievementRepository, times(winners.size() * 3)).createAchievement(captor.capture());

        verify(leaderboardRepository, times(validLeaderboardTags)).getRankedIndexedLeaderboardUsersById(any(), any());
        verify(leaderboardRepository, times(1)).getGlobalRankedIndexedLeaderboardUsersById(any(), any());

        /** keep in mind the order of how we check filters is ordered. check LeaderboardFilterGenerator for order. */
        var achievements = captor.getAllValues();
        var userOneMhcPlusPlusAchievement = achievements.get(0);
        var userTwoMhcPlusPlusAchievement = achievements.get(1);
        assertAchievement(
                userOneMhcPlusPlusAchievement,
                Tag.MHCPlusPlus,
                AchievementPlaceEnum.ONE,
                winners.get(0).getItem().getId(),
                latestLeaderboard.getId());
        assertAchievement(
                userTwoMhcPlusPlusAchievement,
                Tag.MHCPlusPlus,
                AchievementPlaceEnum.TWO,
                winners.get(1).getItem().getId(),
                latestLeaderboard.getId());
        var userOneGwcAchievement = achievements.get(2);
        var userTwoGwcAchievement = achievements.get(3);
        assertAchievement(
                userOneGwcAchievement,
                Tag.Gwc,
                AchievementPlaceEnum.ONE,
                winners.get(0).getItem().getId(),
                latestLeaderboard.getId());
        assertAchievement(
                userTwoGwcAchievement,
                Tag.Gwc,
                AchievementPlaceEnum.TWO,
                winners.get(1).getItem().getId(),
                latestLeaderboard.getId());
        var userOneGlobalAchievement = achievements.get(4);
        var userTwoGlobalAchievement = achievements.get(5);
        assertAchievement(
                userOneGlobalAchievement,
                null,
                AchievementPlaceEnum.ONE,
                winners.get(0).getItem().getId(),
                latestLeaderboard.getId());
        assertAchievement(
                userTwoGlobalAchievement,
                null,
                AchievementPlaceEnum.TWO,
                winners.get(1).getItem().getId(),
                latestLeaderboard.getId());
    }

    @Test
    void testGwcUsersWithZeroPointsAreExcludedFromTagAchievements() {
        var latestLeaderboard = Leaderboard.builder()
                .id(UUID.randomUUID().toString())
                .name("Test Leaderboard")
                .createdAt(StandardizedLocalDateTime.now())
                .build();

        var users = Indexed.ofDefaultList(List.of(
                randomPartialUserWithScore().totalScore(0).build(),
                randomPartialUserWithScore().totalScore(0).build()));

        users.forEach(winner -> {
            var user = winner.getItem();
            user.setTags(List.of(UserTag.builder()
                    .id(UUID.randomUUID().toString())
                    .tag(Tag.Gwc)
                    .userId(user.getId())
                    .build()));
        });

        when(leaderboardRepository.getRecentLeaderboardMetadata()).thenReturn(Optional.of(latestLeaderboard));
        when(leaderboardRepository.getGlobalRankedIndexedLeaderboardUsersById(eq(latestLeaderboard.getId()), any()))
                .thenReturn(users);

        when(leaderboardRepository.getRankedIndexedLeaderboardUsersById(
                        eq(latestLeaderboard.getId()), argThat(opt -> opt.isGwc())))
                .thenReturn(users);

        leaderboardManager.generateAchievementsForAllWinners();

        verify(achievementRepository, times(0)).createAchievement(any());
    }

    @Test
    void testWithAvailableLeaderboardAndThreeWinnersGlobalButFourUsers() {
        var latestLeaderboard = Leaderboard.builder()
                .id(UUID.randomUUID().toString())
                .name("Test Leaderboard")
                .createdAt(StandardizedLocalDateTime.now())
                .build();

        var winners = Indexed.ofDefaultList(List.of(
                randomPartialUserWithScore().totalScore(150_000).build(),
                randomPartialUserWithScore().totalScore(70_000).build(),
                randomPartialUserWithScore().totalScore(30_000).build(),
                randomPartialUserWithScore().totalScore(29_999).build()));

        when(leaderboardRepository.getRecentLeaderboardMetadata()).thenReturn(Optional.of(latestLeaderboard));
        when(leaderboardRepository.getGlobalRankedIndexedLeaderboardUsersById(eq(latestLeaderboard.getId()), any()))
                .thenReturn(winners);

        leaderboardManager.generateAchievementsForAllWinners();

        ArgumentCaptor<Achievement> captor = ArgumentCaptor.forClass(Achievement.class);
        verify(achievementRepository, times(leaderboardManager.MAX_POSSIBLE_WINNERS))
                .createAchievement(captor.capture());

        verify(leaderboardRepository, times(validLeaderboardTags)).getRankedIndexedLeaderboardUsersById(any(), any());
        verify(leaderboardRepository, times(1)).getGlobalRankedIndexedLeaderboardUsersById(any(), any());

        var achievements = captor.getAllValues();
        var userOneAchievement = achievements.get(0);
        var userTwoAchievement = achievements.get(1);
        assertAchievement(
                userOneAchievement,
                null,
                AchievementPlaceEnum.ONE,
                winners.get(0).getItem().getId(),
                latestLeaderboard.getId());
        assertAchievement(
                userTwoAchievement,
                null,
                AchievementPlaceEnum.TWO,
                winners.get(1).getItem().getId(),
                latestLeaderboard.getId());
    }

    @Test
    void testUsersWithZeroPointsAreExcludedFromAchievements() {
        var latestLeaderboard = Leaderboard.builder()
                .id(UUID.randomUUID().toString())
                .name("Test Leaderboard")
                .createdAt(StandardizedLocalDateTime.now())
                .build();

        var users = Indexed.ofDefaultList(List.of(
                randomPartialUserWithScore().totalScore(150_000).build(),
                randomPartialUserWithScore().totalScore(0).build(),
                randomPartialUserWithScore().totalScore(0).build()));

        when(leaderboardRepository.getRecentLeaderboardMetadata()).thenReturn(Optional.of(latestLeaderboard));
        when(leaderboardRepository.getGlobalRankedIndexedLeaderboardUsersById(eq(latestLeaderboard.getId()), any()))
                .thenReturn(users);

        leaderboardManager.generateAchievementsForAllWinners();

        ArgumentCaptor<Achievement> captor = ArgumentCaptor.forClass(Achievement.class);
        verify(achievementRepository, times(1)).createAchievement(captor.capture());

        var achievements = captor.getAllValues();
        var userOneAchievement = achievements.get(0);
        assertAchievement(
                userOneAchievement,
                null,
                AchievementPlaceEnum.ONE,
                users.get(0).getItem().getId(),
                latestLeaderboard.getId());
    }

    @Test
    void testUsersWithZeroPointsAreExcludedFromTagAchievements() {
        var latestLeaderboard = Leaderboard.builder()
                .id(UUID.randomUUID().toString())
                .name("Test Leaderboard")
                .createdAt(StandardizedLocalDateTime.now())
                .build();

        var users = Indexed.ofDefaultList(List.of(
                randomPartialUserWithScore().totalScore(0).build(),
                randomPartialUserWithScore().totalScore(0).build()));

        users.forEach(winner -> {
            var user = winner.getItem();
            user.setTags(List.of(UserTag.builder()
                    .id(UUID.randomUUID().toString())
                    .tag(Tag.Sbu)
                    .userId(user.getId())
                    .build()));
        });

        when(leaderboardRepository.getRecentLeaderboardMetadata()).thenReturn(Optional.of(latestLeaderboard));
        when(leaderboardRepository.getGlobalRankedIndexedLeaderboardUsersById(eq(latestLeaderboard.getId()), any()))
                .thenReturn(users);

        when(leaderboardRepository.getRankedIndexedLeaderboardUsersById(
                        eq(latestLeaderboard.getId()), argThat(opt -> opt.isSbu())))
                .thenReturn(users);

        leaderboardManager.generateAchievementsForAllWinners();

        verify(achievementRepository, times(0)).createAchievement(any());
    }

    @Test
    void testWinnersWithNullTagsAreSkippedInTagLeaderboards() {
        var latestLeaderboard = Leaderboard.builder()
                .id(UUID.randomUUID().toString())
                .name("Test Leaderboard")
                .createdAt(StandardizedLocalDateTime.now())
                .build();

        var winners = Indexed.ofDefaultList(List.of(
                randomPartialUserWithScore().totalScore(150_000).build(),
                randomPartialUserWithScore().totalScore(70_000).build()));

        when(leaderboardRepository.getRecentLeaderboardMetadata()).thenReturn(Optional.of(latestLeaderboard));
        when(leaderboardRepository.getGlobalRankedIndexedLeaderboardUsersById(eq(latestLeaderboard.getId()), any()))
                .thenReturn(winners);

        leaderboardManager.generateAchievementsForAllWinners();

        ArgumentCaptor<Achievement> captor = ArgumentCaptor.forClass(Achievement.class);
        verify(achievementRepository, times(winners.size())).createAchievement(captor.capture());

        var achievements = captor.getAllValues();
        for (var achievement : achievements) {
            assertNull(achievement.getLeaderboard());
            assertEquals(latestLeaderboard.getId(), achievement.getLeaderboardId());
        }
    }

    @Test
    void testWinnersWithNonMatchingTagsAreSkippedInTagLeaderboards() {
        var latestLeaderboard = Leaderboard.builder()
                .id(UUID.randomUUID().toString())
                .name("Test Leaderboard")
                .createdAt(StandardizedLocalDateTime.now())
                .build();

        var winners = Indexed.ofDefaultList(List.of(
                randomPartialUserWithScore().totalScore(150_000).build(),
                randomPartialUserWithScore().totalScore(70_000).build()));

        winners.forEach(winner -> {
            var user = winner.getItem();
            user.setTags(List.of(UserTag.builder()
                    .id(UUID.randomUUID().toString())
                    .tag(Tag.Patina)
                    .userId(user.getId())
                    .build()));
        });

        when(leaderboardRepository.getRecentLeaderboardMetadata()).thenReturn(Optional.of(latestLeaderboard));
        when(leaderboardRepository.getGlobalRankedIndexedLeaderboardUsersById(eq(latestLeaderboard.getId()), any()))
                .thenReturn(winners);

        when(leaderboardRepository.getRankedIndexedLeaderboardUsersById(
                        eq(latestLeaderboard.getId()), argThat(opt -> opt.isPatina())))
                .thenReturn(winners);

        leaderboardManager.generateAchievementsForAllWinners();

        ArgumentCaptor<Achievement> captor = ArgumentCaptor.forClass(Achievement.class);
        verify(achievementRepository, times(winners.size() * 2)).createAchievement(captor.capture());

        var achievements = captor.getAllValues();
        var userOnePatinaAchievement = achievements.get(0);
        var userTwoPatinaAchievement = achievements.get(1);
        assertAchievement(
                userOnePatinaAchievement,
                Tag.Patina,
                AchievementPlaceEnum.ONE,
                winners.get(0).getItem().getId(),
                latestLeaderboard.getId());
        assertAchievement(
                userTwoPatinaAchievement,
                Tag.Patina,
                AchievementPlaceEnum.TWO,
                winners.get(1).getItem().getId(),
                latestLeaderboard.getId());
        var userOneGlobalAchievement = achievements.get(2);
        var userTwoGlobalAchievement = achievements.get(3);
        assertAchievement(
                userOneGlobalAchievement,
                null,
                AchievementPlaceEnum.ONE,
                winners.get(0).getItem().getId(),
                latestLeaderboard.getId());
        assertAchievement(
                userTwoGlobalAchievement,
                null,
                AchievementPlaceEnum.TWO,
                winners.get(1).getItem().getId(),
                latestLeaderboard.getId());
    }

    @Test
    void testGetLeaderboardMetadata() {
        String testId = UUID.randomUUID().toString();
        Leaderboard leaderboard = Leaderboard.builder()
                .id(testId)
                .name("Testing Leaderboard")
                .createdAt(StandardizedLocalDateTime.now())
                .build();

        when(leaderboardRepository.getLeaderboardMetadataById(testId)).thenReturn(Optional.of(leaderboard));

        Leaderboard leaderboardData =
                leaderboardManager.getLeaderboardMetadata(testId).get();

        assertNotNull(leaderboardData);
        assertEquals(leaderboard.getId(), leaderboardData.getId());
        assertEquals(leaderboard.getName(), leaderboardData.getName());
        assertEquals(leaderboard.getCreatedAt(), leaderboardData.getCreatedAt());

        verify(leaderboardRepository, times(1)).getLeaderboardMetadataById(testId);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 21})
    void testGetLeaderboardUsersCorrectPageCalculationLogic(int usersSize) {
        String leaderboardId = Strings.EMPTY;
        int pageSize = 20;
        List<UserWithScore> users = IntStream.of(usersSize)
                .mapToObj((_) -> (UserWithScore)
                        randomPartialUserWithScore().totalScore(0).build())
                .toList();
        var opts = LeaderboardFilterOptions.builder().pageSize(pageSize).build();

        when(leaderboardRepository.getLeaderboardUserCountById(eq(leaderboardId), eq(opts)))
                .thenReturn(usersSize);

        when(leaderboardRepository.getGlobalRankedIndexedLeaderboardUsersById(eq(leaderboardId), eq(opts)))
                .thenReturn(Indexed.ofDefaultList(users));

        var page = leaderboardManager.getLeaderboardUsers(leaderboardId, opts, true);

        assertNotNull(page);
        assertTrue(page.getPageSize() == 20);
        assertTrue(page.getPages() == (usersSize == 1 ? 1 : 2));
        if (usersSize == 21) {
            assertTrue(page.isHasNextPage());
        }
    }

    @ParameterizedTest()
    @MethodSource("tagGenerator")
    void testGetLeaderboardUsersWhereGlobalIndexIsTrueAndOneTagIsEnabled(LeaderboardFilterOptions opts) {
        String leaderboardId = Strings.EMPTY;
        var user = randomPartialUserWithScore().totalScore(0).build();

        when(leaderboardRepository.getLeaderboardUserCountById(eq(leaderboardId), eq(opts)))
                .thenReturn(1);

        when(leaderboardRepository.getGlobalRankedIndexedLeaderboardUsersById(eq(leaderboardId), eq(opts)))
                .thenReturn(List.of(Indexed.of(user, 1)));

        leaderboardManager.getLeaderboardUsers(leaderboardId, opts, true);

        verify(leaderboardRepository, times(1)).getGlobalRankedIndexedLeaderboardUsersById(eq(leaderboardId), eq(opts));
        verify(leaderboardRepository, never()).getRankedIndexedLeaderboardUsersById(eq(leaderboardId), eq(opts));
    }

    @ParameterizedTest()
    @MethodSource("tagGenerator")
    void testGetLeaderboardUsersOneTagIsEnabled(LeaderboardFilterOptions opts) {
        String leaderboardId = Strings.EMPTY;
        var user = randomPartialUserWithScore().totalScore(0).build();

        when(leaderboardRepository.getLeaderboardUserCountById(eq(leaderboardId), eq(opts)))
                .thenReturn(1);

        when(leaderboardRepository.getGlobalRankedIndexedLeaderboardUsersById(eq(leaderboardId), eq(opts)))
                .thenReturn(List.of(Indexed.of(user, 1)));

        leaderboardManager.getLeaderboardUsers(leaderboardId, opts, false);

        verify(leaderboardRepository, never()).getGlobalRankedIndexedLeaderboardUsersById(eq(leaderboardId), eq(opts));
        verify(leaderboardRepository, times(1)).getRankedIndexedLeaderboardUsersById(eq(leaderboardId), eq(opts));
    }
}
