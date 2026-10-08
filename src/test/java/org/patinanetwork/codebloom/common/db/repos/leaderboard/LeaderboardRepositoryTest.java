package org.patinanetwork.codebloom.common.db.repos.leaderboard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.patinanetwork.codebloom.common.db.models.leaderboard.Leaderboard;
import org.patinanetwork.codebloom.common.db.models.user.User;
import org.patinanetwork.codebloom.common.db.models.user.UserWithScore;
import org.patinanetwork.codebloom.common.db.repos.BaseRepositoryTest;
import org.patinanetwork.codebloom.common.db.repos.leaderboard.options.LeaderboardFilterOptions;
import org.patinanetwork.codebloom.common.db.repos.user.UserRepository;
import org.patinanetwork.codebloom.common.db.repos.user.options.UserFilterOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.AopTestUtils;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Slf4j
public class LeaderboardRepositoryTest extends BaseRepositoryTest {

    private Leaderboard previousLeaderboard;
    private final LeaderboardRepository leaderboardRepository;
    private final UserRepository userRepository;
    private final String superUserId = "ed3bfe18-e42a-467f-b4fa-07e8da4d2555";

    static final int PAGE_SIZE = 9999999;
    private Leaderboard mockLeaderboard;

    @Autowired
    public LeaderboardRepositoryTest(
            final LeaderboardRepository leaderboardRepository, final UserRepository userRepository) {
        this.leaderboardRepository = leaderboardRepository;
        this.userRepository = userRepository;
    }

    @BeforeAll
    void createMockLeaderboard() {
        // will bring this back after
        previousLeaderboard =
                leaderboardRepository.getRecentLeaderboardMetadata().get();
        mockLeaderboard = Leaderboard.builder()
                .name("Mock Leaderboard")
                .deletedAt(Optional.empty())
                .shouldExpireBy(Optional.empty())
                .syntaxHighlightingLanguage(Optional.empty())
                .build();
        leaderboardRepository.addNewLeaderboard(mockLeaderboard);
    }

    @AfterAll
    void deleteMockLeaderboard() throws Exception {
        // repository is proxied (e.g. @Timed), so private methods only exist on the target
        LeaderboardRepository target = AopTestUtils.getTargetObject(leaderboardRepository);

        Method deleteLeaderboardMethod = target.getClass().getDeclaredMethod("deleteLeaderboardById", String.class);
        Method enableLeaderboardMethod = target.getClass().getDeclaredMethod("enableLeaderboardById", String.class);

        deleteLeaderboardMethod.setAccessible(true);
        enableLeaderboardMethod.setAccessible(true);

        boolean deleteLeaderboardSuccessful = (Boolean) deleteLeaderboardMethod.invoke(target, mockLeaderboard.getId());

        assertTrue(deleteLeaderboardSuccessful, "failed to delete mock leaderboard");

        boolean enableLeaderboardSuccessful =
                (Boolean) enableLeaderboardMethod.invoke(target, previousLeaderboard.getId());

        assertTrue(enableLeaderboardSuccessful, "failed to restore previous leaderboard");
    }

    @Order(1)
    @Test
    void testIfMostRecentLeaderboardMetadataValid() {
        Leaderboard possiblyMockLeaderboard =
                leaderboardRepository.getRecentLeaderboardMetadata().get();

        if (possiblyMockLeaderboard == null || !mockLeaderboard.equals(possiblyMockLeaderboard)) {
            log.info("[DEBUG] - mockLeaderboard: {}", mockLeaderboard.toString());
            log.info(
                    "[DEBUG] - possiblyMockLeaderboard: {}",
                    possiblyMockLeaderboard == null ? "null" : possiblyMockLeaderboard.toString());
            fail("most recent leaderboard metadata did not return mock leaderboard");
        }
    }

    @Order(2)
    @Test
    void testGetLeaderboardMetadataById() {
        Leaderboard possiblyMockLeaderboard = leaderboardRepository
                .getLeaderboardMetadataById(mockLeaderboard.getId())
                .get();

        if (possiblyMockLeaderboard == null || !mockLeaderboard.equals(possiblyMockLeaderboard)) {
            log.info("[DEBUG] - mockLeaderboard: {}", mockLeaderboard.toString());
            log.info(
                    "[DEBUG] - possiblyMockLeaderboard: {}",
                    possiblyMockLeaderboard == null ? "null" : possiblyMockLeaderboard.toString());
            fail("most recent leaderboard metadata did not return mock leaderboard");
        }
    }

    @Order(3)
    @Test
    void testUpdateLeaderboardName() {
        mockLeaderboard.setName("New test name");
        leaderboardRepository.updateLeaderboard(mockLeaderboard);

        // ensure that they are still equal

        Leaderboard possiblyMockLeaderboard = leaderboardRepository
                .getLeaderboardMetadataById(mockLeaderboard.getId())
                .get();

        if (possiblyMockLeaderboard == null || !mockLeaderboard.equals(possiblyMockLeaderboard)) {
            log.info("[DEBUG] - mockLeaderboard: {}", mockLeaderboard.toString());
            log.info(
                    "[DEBUG] - possiblyMockLeaderboard: {}",
                    possiblyMockLeaderboard == null ? "null" : possiblyMockLeaderboard.toString());
            fail("most recent leaderboard metadata did not return mock leaderboard");
        }
    }

    @Order(4)
    @Test
    void testAddAllUsersToMockLeaderboard() {
        boolean isSuccessful = leaderboardRepository.addAllUsersToLeaderboard(mockLeaderboard.getId());

        assertTrue(isSuccessful, "failed to add all users to mock leaderboard");
    }

    @Order(5)
    @Test
    void testGetRecentLeaderboardUserCount() {
        LeaderboardFilterOptions options =
                LeaderboardFilterOptions.builder().query("").patina(false).build();

        int leaderboardUsersCount = leaderboardRepository.getRecentLeaderboardUserCount(options);
        assertTrue(leaderboardUsersCount > 0);
    }

    @Order(6)
    @Test
    void testLeaderboardUsersByIdEqualsRecentLeaderboardUsers() {
        LeaderboardFilterOptions options = LeaderboardFilterOptions.builder()
                .page(1)
                .pageSize(PAGE_SIZE)
                .query("")
                .patina(false)
                .build();
        List<UserWithScore> allLeaderboardUsersById =
                leaderboardRepository.getLeaderboardUsersById(mockLeaderboard.getId(), options);
        List<UserWithScore> allRecentLeaderboardUsers = leaderboardRepository.getRecentLeaderboardUsers(options);

        IntStream.range(0, allLeaderboardUsersById.size()).forEach(i -> {
            var user = allLeaderboardUsersById.get(i);
            assertTrue(allRecentLeaderboardUsers.stream().anyMatch(leaderboardUser -> {
                return leaderboardUser.getId().equals(user.getId());
            }));
        });
    }

    @Order(7)
    @Test
    void testUsersInMockLeaderboardToAllUsers() {
        List<User> allUsers = userRepository.getAllUsers();

        LeaderboardFilterOptions options = LeaderboardFilterOptions.builder()
                .page(1)
                .pageSize(allUsers.size())
                .query("")
                .patina(false)
                .build();

        List<UserWithScore> allLeaderboardUsers =
                leaderboardRepository.getLeaderboardUsersById(mockLeaderboard.getId(), options);

        assertTrue(allUsers != null);
        assertTrue(allLeaderboardUsers != null);
        assertTrue(allUsers.size() > 0);
        assertTrue(allLeaderboardUsers.size() > 0);
        assertTrue(allUsers.size() == allLeaderboardUsers.size());

        IntStream.range(0, allUsers.size()).forEach(i -> {
            var user = allUsers.get(i);

            assertTrue(allLeaderboardUsers.stream().anyMatch(leaderboardUser -> {
                return leaderboardUser.getId().equals(user.getId());
            }));
        });
    }

    @Order(8)
    @Test
    void testAddingANewUserToMockLeaderboard() {
        User newMockUser = User.builder()
                .discordId("3021234402183490")
                .discordName("mockUserName" + ThreadLocalRandom.current().nextInt())
                .build();
        userRepository.createUser(newMockUser);

        boolean isAddUserSuccessful =
                leaderboardRepository.addUserToLeaderboard(newMockUser.getId(), mockLeaderboard.getId());

        assertTrue(isAddUserSuccessful, "failed to add new user to mock leaderboard");

        // cleanup
        boolean isDeleteUserSuccessful = userRepository.deleteUserById(newMockUser.getId());

        assertTrue(isDeleteUserSuccessful, "failed to delete new user");

        // leaderboard should be purged of user via cascade deleting
    }

    @Order(9)
    @Test
    void testGivingPointsToSuperUserOnMockLeaderboard() {
        int newPoints = 99999;
        boolean isUserUpdatePointsSuccessful =
                leaderboardRepository.updateUserPointsFromLeaderboard(mockLeaderboard.getId(), superUserId, newPoints);

        assertTrue(isUserUpdatePointsSuccessful, "failed to update points of super user on mock leaderboard");

        // TODO - Write tests for pointOfTime
        UserWithScore superUser = userRepository.getUserWithScoreByIdAndLeaderboardId(
                superUserId,
                mockLeaderboard.getId(),
                UserFilterOptions.builder().build());

        assertTrue(superUser != null);
        assertEquals(newPoints, superUser.getTotalScore());
    }

    @Order(10)
    @Test
    void testGettingLeaderboardRanks() {
        LeaderboardFilterOptions options = LeaderboardFilterOptions.builder()
                .page(1)
                .pageSize(PAGE_SIZE)
                .query("")
                .patina(false)
                .build();

        try {
            leaderboardRepository.getGlobalRankedIndexedLeaderboardUsersById(mockLeaderboard.getId(), options);
        } catch (Exception e) {
            fail(e);
        }

        // TODO - Write more tests for this function.
    }

    @Test
    @Order(12)
    void testLeaderboardUserCountById() {
        LeaderboardFilterOptions options =
                LeaderboardFilterOptions.builder().query("").patina(false).build();
        int leaderboardUsersCount = leaderboardRepository.getLeaderboardUserCountById(mockLeaderboard.getId(), options);
        assertTrue(leaderboardUsersCount > 0);
    }

    @Test
    @Order(13)
    void testLeaderboardCount() {
        int leaderboardCount = leaderboardRepository.getLeaderboardCount();
        assertTrue(leaderboardCount > 0);
    }

    @Test
    @Order(14)
    void testGetAllLeaderboards() {
        LeaderboardFilterOptions options = LeaderboardFilterOptions.builder()
                .page(1)
                .pageSize(PAGE_SIZE)
                .query("")
                .build();
        var leaderboards = leaderboardRepository.getAllLeaderboardsShallow(options);
        assertTrue(leaderboards != null);
        assertTrue(leaderboards.size() > 0);
    }

    @Test
    @Order(15)
    void testDisableLeaderboardById() {
        boolean isSuccessful = leaderboardRepository.disableLeaderboardById(mockLeaderboard.getId());
        assertTrue(isSuccessful);
    }

    @Test
    @Order(16)
    void testDisableLeaderboardByIdAgain() {
        boolean isSuccessful = leaderboardRepository.disableLeaderboardById(mockLeaderboard.getId());
        assertFalse(isSuccessful);
    }
}
