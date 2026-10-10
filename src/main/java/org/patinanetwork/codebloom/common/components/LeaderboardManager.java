package org.patinanetwork.codebloom.common.components;

import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.patinanetwork.codebloom.common.db.models.achievements.Achievement;
import org.patinanetwork.codebloom.common.db.models.achievements.AchievementPlaceEnum;
import org.patinanetwork.codebloom.common.db.models.leaderboard.Leaderboard;
import org.patinanetwork.codebloom.common.db.models.user.User;
import org.patinanetwork.codebloom.common.db.models.user.UserWithScore;
import org.patinanetwork.codebloom.common.db.models.usertag.Tag;
import org.patinanetwork.codebloom.common.db.repos.achievements.AchievementRepository;
import org.patinanetwork.codebloom.common.db.repos.leaderboard.LeaderboardRepository;
import org.patinanetwork.codebloom.common.db.repos.leaderboard.options.LeaderboardFilterGenerator;
import org.patinanetwork.codebloom.common.db.repos.leaderboard.options.LeaderboardFilterOptions;
import org.patinanetwork.codebloom.common.db.repos.user.UserRepository;
import org.patinanetwork.codebloom.common.dto.user.UserWithScoreDto;
import org.patinanetwork.codebloom.common.leetcode.LeetcodeClient;
import org.patinanetwork.codebloom.common.leetcode.models.LeetcodeSubmission;
import org.patinanetwork.codebloom.common.page.Indexed;
import org.patinanetwork.codebloom.common.page.Page;
import org.patinanetwork.codebloom.common.submissions.SubmissionsHandler;
import org.patinanetwork.codebloom.common.utils.leaderboard.LeaderboardUtils;
import org.patinanetwork.codebloom.common.utils.pair.Pair;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class LeaderboardManager {

    private final LeaderboardRepository leaderboardRepository;
    private final AchievementRepository achievementRepository;
    private final UserRepository userRepository;
    private final LeetcodeClient leetcodeClient;
    private final SubmissionsHandler submissionsHandler;

    public LeaderboardManager(
            final LeaderboardRepository leaderboardRepository,
            final AchievementRepository achievementRepository,
            final UserRepository userRepository,
            final LeetcodeClient leetcodeClient,
            final SubmissionsHandler submissionsHandler) {
        this.leaderboardRepository = leaderboardRepository;
        this.achievementRepository = achievementRepository;
        this.userRepository = userRepository;
        this.leetcodeClient = leetcodeClient;
        this.submissionsHandler = submissionsHandler;
    }

    public static final int MIN_POSSIBLE_WINNERS = 0;
    public static final int MAX_POSSIBLE_WINNERS = 3;

    private int maxWinners(final int size) {
        return Math.min(Math.max(size, MIN_POSSIBLE_WINNERS), MAX_POSSIBLE_WINNERS);
    }

    private String calculatePlaceString(final int place) {
        return switch (place) {
            case 1 -> "1st";
            case 2 -> "2nd";
            case 3 -> "3rd";
            default -> throw new IllegalArgumentException("Can only process 1st, 2nd, and 3rd place");
        };
    }

    public void generateAchievementsForAllWinners() {
        log.info("generating achievements for all winners...");
        Optional<Leaderboard> currentLeaderboard = leaderboardRepository.getRecentLeaderboardMetadata();

        if (currentLeaderboard.isEmpty()) {
            return;
        }

        List<Pair<LeaderboardFilterOptions, Tag>> filterOptsAndTags =
                LeaderboardFilterGenerator.generateAllSupportedTagToggles();

        for (var pair : filterOptsAndTags) {
            log.info("on leaderboard for {}", pair.getRight().getResolvedName());
            List<Indexed<UserWithScore>> users = leaderboardRepository.getRankedIndexedLeaderboardUsersById(
                    currentLeaderboard.get().getId(), pair.getLeft());
            List<UserWithScore> usersWithPoints = LeaderboardUtils.filterUsersWithPoints(
                    users.stream().map(Indexed::getItem).toList());
            List<UserWithScore> winners = usersWithPoints.subList(0, maxWinners(usersWithPoints.size()));

            for (int i = 0; i < winners.size(); i++) {
                UserWithScore user = winners.get(i);
                int place = i + 1;
                log.info("on leaderboard for {} for winner #{}", pair.getRight().getResolvedName(), place);
                String placeString = calculatePlaceString(place);
                Achievement achievement = Achievement.builder()
                        .userId(user.getId())
                        .leaderboardId(currentLeaderboard.get().getId())
                        .place(AchievementPlaceEnum.fromInteger(place))
                        .leaderboard(Optional.of(pair.getRight()))
                        .title(String.format(
                                "%s - %s - %s Place",
                                currentLeaderboard.get().getName(),
                                pair.getRight().getResolvedName(),
                                placeString))
                        .build();
                achievementRepository.createAchievement(achievement);
            }
        }

        // handle global leaderboard
        List<Indexed<UserWithScore>> users = leaderboardRepository.getGlobalRankedIndexedLeaderboardUsersById(
                currentLeaderboard.get().getId(), LeaderboardFilterOptions.DEFAULT);
        List<UserWithScore> usersWithPoints = LeaderboardUtils.filterUsersWithPoints(
                users.stream().map(Indexed::getItem).toList());
        List<UserWithScore> winners = usersWithPoints.subList(0, maxWinners(usersWithPoints.size()));

        for (int i = 0; i < winners.size(); i++) {
            int place = i + 1;
            log.info(
                    "on leaderboard for {} for global winner #{}",
                    currentLeaderboard.get().getName(),
                    place);
            String placeString = calculatePlaceString(place);
            UserWithScore user = winners.get(i);
            Achievement achievement = Achievement.builder()
                    .userId(user.getId())
                    .leaderboardId(currentLeaderboard.get().getId())
                    .place(AchievementPlaceEnum.fromInteger(place))
                    .title(String.format(
                            "%s - %s Place", currentLeaderboard.get().getName(), placeString))
                    .build();
            achievementRepository.createAchievement(achievement);
        }
    }

    public Optional<Leaderboard> getLeaderboardMetadata(final String id) {
        return leaderboardRepository.getLeaderboardMetadataById(id);
    }

    public int getLeaderboardUserCountById(final String leaderboardId, final LeaderboardFilterOptions options) {
        return leaderboardRepository.getLeaderboardUserCountById(leaderboardId, options);
    }

    public Page<Indexed<UserWithScoreDto>> getLeaderboardUsers(
            final String currentLeaderboardId, final LeaderboardFilterOptions options, final boolean globalIndex) {
        final int page = options.getPage();
        final int parsedPageSize = options.getPageSize();

        int totalUsers = leaderboardRepository.getLeaderboardUserCountById(currentLeaderboardId, options);
        int totalPages = (int) Math.ceil((double) totalUsers / parsedPageSize);
        boolean hasNextPage = page < totalPages;

        List<Indexed<UserWithScore>> leaderboardData;
        // don't use globalIndex when there are no filters enabled.
        if (globalIndex
                && (options.isPatina()
                        || options.isNyu()
                        || options.isHunter()
                        || options.isBaruch()
                        || options.isRpi()
                        || options.isGwc()
                        || options.isSbu()
                        || options.isCcny()
                        || options.isColumbia()
                        || options.isCornell()
                        || options.isBmcc()
                        || options.isMhcplusplus())) {
            leaderboardData =
                    leaderboardRepository.getGlobalRankedIndexedLeaderboardUsersById(currentLeaderboardId, options);
        } else {
            leaderboardData = leaderboardRepository.getRankedIndexedLeaderboardUsersById(currentLeaderboardId, options);
        }

        List<Indexed<UserWithScoreDto>> indexedUserWithScoreDtos = leaderboardData.stream()
                .map(indexed -> indexed.map(UserWithScoreDto::fromUserWithScore))
                .toList();

        Page<Indexed<UserWithScoreDto>> createdPage =
                new Page<>(hasNextPage, indexedUserWithScoreDtos, totalPages, parsedPageSize);
        return createdPage;
    }

    public User refreshUserSubmissions(final String discordId) throws LeaderboardException {
        User user = userRepository
                .getUserByDiscordId(discordId)
                .orElseThrow(
                        () -> new LeaderboardException(
                                "Cannot refresh submissions",
                                "Please link your account by [logging in to Codebloom](https://codebloom.patinanetwork.org/login) and completing onboarding."));

        if (user.getLeetcodeUsername().isEmpty()) {
            throw new LeaderboardException(
                    "Cannot refresh submissions", "Your Discord Account is not linked to a LeetCode username.");
        }
        try {
            log.info(
                    "Fetching recent LeetCode submissions for user: {}",
                    user.getLeetcodeUsername().orElse(null));
            List<LeetcodeSubmission> leetcodeSubmissions = leetcodeClient.findSubmissionsByUsername(
                    user.getLeetcodeUsername().orElse(null), 20);

            submissionsHandler.handleSubmissions(leetcodeSubmissions, user, true);
        } catch (Exception e) {
            log.error(
                    "Failed to fetch or process submissions for user {}",
                    user.getLeetcodeUsername().orElse(null),
                    e);
            throw new LeaderboardException(
                    "Cannot refresh submissions",
                    "Failed to fetch or process submissions from LeetCode. Please try again later.");
        }

        return user;
    }
}
