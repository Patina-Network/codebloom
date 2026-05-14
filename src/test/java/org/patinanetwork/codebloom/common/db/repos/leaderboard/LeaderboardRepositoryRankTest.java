package org.patinanetwork.codebloom.common.db.repos.leaderboard;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.patinanetwork.codebloom.common.db.models.leaderboard.Leaderboard;
import org.patinanetwork.codebloom.common.db.models.user.UserWithScore;
import org.patinanetwork.codebloom.common.db.repos.BaseRepositoryTest;
import org.patinanetwork.codebloom.common.db.repos.leaderboard.options.LeaderboardFilterOptions;
import org.patinanetwork.codebloom.common.page.Indexed;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Slf4j
public class LeaderboardRepositoryRankTest extends BaseRepositoryTest {

    private final LeaderboardRepository leaderboardRepository;

    private final Leaderboard currentLeaderboard;
    private static final String EXPIRED_LEADERBOARD_ID = "8fc363b2-b5be-4f1a-9e0e-ea4844fc919c";

    @Autowired
    public LeaderboardRepositoryRankTest(final LeaderboardRepository leaderboardRepository) {
        this.leaderboardRepository = leaderboardRepository;
        this.currentLeaderboard =
                this.leaderboardRepository.getRecentLeaderboardMetadata().get();
    }

    @Test
    @DisplayName("Assert that ranked behaviors are correct between users list and single user - Filtered")
    void assertRankedConsistencyBetweenUserListAndSingleUserFiltered() {
        var opts = LeaderboardFilterOptions.builder().patina(true).build();

        List<Indexed<UserWithScore>> users =
                leaderboardRepository.getRankedIndexedLeaderboardUsersById(currentLeaderboard.getId(), opts);

        users.forEach(i -> {
            var indexed = leaderboardRepository.getFilteredRankedUserById(
                    currentLeaderboard.getId(), i.getItem().getId(), opts);
            var user = indexed.get().getItem();
            assertEquals(
                    indexed.get().getIndex(),
                    users.stream()
                            .filter(u -> {
                                var possibleUser = u.getItem();
                                return Objects.equals(user.getId(), possibleUser.getId());
                            })
                            .findFirst()
                            .map(Indexed::getIndex)
                            .orElse(-1));
        });
    }

    @Test
    @DisplayName("Assert that ranked behaviors are correct between users list and single user - Global")
    void assertRankedConsistencyBetweenUserListAndSingleUserGlobal() {
        var opts = LeaderboardFilterOptions.DEFAULT;
        List<Indexed<UserWithScore>> users =
                leaderboardRepository.getGlobalRankedIndexedLeaderboardUsersById(currentLeaderboard.getId(), opts);

        users.forEach(i -> {
            var indexed = leaderboardRepository.getGlobalRankedUserById(
                    currentLeaderboard.getId(), i.getItem().getId());
            var user = indexed.get().getItem();
            assertEquals(
                    indexed.get().getIndex(),
                    users.stream()
                            .filter(u -> {
                                var possibleUser = u.getItem();
                                return Objects.equals(user.getId(), possibleUser.getId());
                            })
                            .findFirst()
                            .map(Indexed::getIndex)
                            .orElse(-1));
        });
    }

    @Test
    @DisplayName("Assert that getFilteredRankedUserById fails if user is not in the search criteria")
    void assertGetFilteredRankedUserByIdFailsIfNotInSearchCriteria() {
        String gwcUserId = "a1a1a1a1-a2d2-e3e3-f4f4-a5a5a5a5a5a5";
        var opts = LeaderboardFilterOptions.builder().patina(true).build();

        var result = leaderboardRepository.getFilteredRankedUserById(currentLeaderboard.getId(), gwcUserId, opts);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Assert ranking handles tied scores correctly")
    void assertTiedScoresRankingCorrectly() {
        // Test users with identical scores are ranked by leetcodeUsername presence and
        // createdAt
        var opts = LeaderboardFilterOptions.DEFAULT;
        var indexedUserList =
                leaderboardRepository.getGlobalRankedIndexedLeaderboardUsersById(currentLeaderboard.getId(), opts);

        // Validate ranking rules by checking each adjacent pair
        for (int i = 0; i < indexedUserList.size() - 1; i++) {
            var current = indexedUserList.get(i).getItem();
            var next = indexedUserList.get(i + 1).getItem();

            // Rule 1: Higher scores rank first
            if (current.getTotalScore() != next.getTotalScore()) {
                assertTrue(
                        current.getTotalScore() > next.getTotalScore(),
                        String.format(
                                "User %s (score: %d) should rank before user %s (score: %d)",
                                current.getId(), current.getTotalScore(), next.getId(), next.getTotalScore()));
            } else {
                // Rule 2: For tied scores, check zero-score leetcode username logic
                if (current.getTotalScore() == 0) {
                    boolean currentHasLeetcode = current.getLeetcodeUsername() != null;
                    boolean nextHasLeetcode = next.getLeetcodeUsername() != null;

                    if (currentHasLeetcode != nextHasLeetcode) {
                        assertTrue(
                                currentHasLeetcode,
                                String.format(
                                        "Zero-score user %s with leetcode should rank before user %s without",
                                        current.getId(), next.getId()));
                    }
                }

                // TODO: Add logic to fetch Metadata.
            }
        }
    }

    @Test
    @DisplayName("Assert filtered ranking when user has multiple tags")
    void assertMultipleTagsFiltering() {
        String multiTagUserId = "6b8579ca-2d67-42ab-89e9-e75656493f3e"; // Has both Patina and Baruch tags

        var patinaOpts = LeaderboardFilterOptions.builder().patina(true).build();
        var baruchOpts = LeaderboardFilterOptions.builder().baruch(true).build();

        var patinaResult =
                leaderboardRepository.getFilteredRankedUserById(EXPIRED_LEADERBOARD_ID, multiTagUserId, patinaOpts);
        var baruchResult =
                leaderboardRepository.getFilteredRankedUserById(EXPIRED_LEADERBOARD_ID, multiTagUserId, baruchOpts);

        assertNotNull(patinaResult, "User with Patina tag should appear in Patina filter");
        assertNotNull(baruchResult, "User with Baruch tag should appear in Baruch filter");
        assertEquals(
                patinaResult.get().getItem(),
                baruchResult.get().getItem(),
                "Same user should be returned in both filters");
    }

    @Test
    @DisplayName("Assert filtered ranking excludes users created after leaderboard deletion")
    void assertUserTagCreationDateFiltering() {
        String userWithOldTag = "6b8579ca-2d67-42ab-89e9-e75656493f3e"; // Tag created before leaderboard deletion
        String userWithNewTag = "71bbe3f2-2f09-4726-8f79-04554ce561e0"; // Tag created after leaderboard deletion

        var hunterOpts =
                LeaderboardFilterOptions.builder().patina(true).hunter(true).build();

        var oldTagResult =
                leaderboardRepository.getFilteredRankedUserById(EXPIRED_LEADERBOARD_ID, userWithOldTag, hunterOpts);
        var newTagResult =
                leaderboardRepository.getFilteredRankedUserById(EXPIRED_LEADERBOARD_ID, userWithNewTag, hunterOpts);

        assertTrue(oldTagResult.isPresent(), "User with tag created before leaderboard deletion should be included");
        assertTrue(newTagResult.isEmpty(), "User with tag created after leaderboard deletion should be excluded");
    }

    @Test
    @DisplayName("Assert ranking consistency with all filter options disabled")
    void assertAllFiltersDisabledBehavior() {
        // Test that when all tag filters are false, all users are included (OR
        // condition at end of SQL)
        var allFalseOpts = LeaderboardFilterOptions.builder()
                .patina(false)
                .hunter(false)
                .nyu(false)
                .baruch(false)
                .rpi(false)
                .gwc(false)
                .sbu(false)
                .ccny(false)
                .columbia(false)
                .cornell(false)
                .bmcc(false)
                .build();

        List<Indexed<UserWithScore>> filteredUsers =
                leaderboardRepository.getRankedIndexedLeaderboardUsersById(currentLeaderboard.getId(), allFalseOpts);
        List<Indexed<UserWithScore>> globalUsers = leaderboardRepository.getGlobalRankedIndexedLeaderboardUsersById(
                currentLeaderboard.getId(), LeaderboardFilterOptions.DEFAULT);

        assertEquals(globalUsers.size(), filteredUsers.size(), "When all filters are false, should include all users");
    }
}
