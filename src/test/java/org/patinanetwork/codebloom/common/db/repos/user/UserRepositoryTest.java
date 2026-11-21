package org.patinanetwork.codebloom.common.db.repos.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.patinanetwork.codebloom.common.db.models.Session;
import org.patinanetwork.codebloom.common.db.models.user.User;
import org.patinanetwork.codebloom.common.db.repos.BaseRepositoryTest;
import org.patinanetwork.codebloom.common.db.repos.session.SessionRepository;
import org.patinanetwork.codebloom.common.time.StandardizedLocalDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(OrderAnnotation.class)
@Slf4j
public class UserRepositoryTest extends BaseRepositoryTest {

    private UserRepository userRepository;
    private SessionRepository sessionRepository;

    private User testUser;
    private User testAdminUser;

    @Autowired
    public UserRepositoryTest(final UserRepository userRepository, final SessionRepository sessionRepository) {
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
    }

    @BeforeAll
    void setUp() {
        String uniqueDiscordId = "test-" + System.currentTimeMillis();
        String uniqueAdminDiscordId = "test-1" + System.currentTimeMillis();

        testUser = User.builder()
                .discordId(uniqueDiscordId)
                .discordName("TestUser")
                .leetcodeUsername(Optional.of("testuser"))
                .nickname(Optional.of("TestNickname"))
                .admin(false)
                .schoolEmail(Optional.of("test@example.com"))
                .profileUrl(Optional.of(""))
                .tags(new ArrayList<>())
                .build();

        testAdminUser = User.builder()
                .discordId(uniqueAdminDiscordId)
                .discordName("TestAdminUser")
                .leetcodeUsername(Optional.of("testadminuser"))
                .nickname(Optional.of("TestAdminNickname"))
                .admin(true)
                .schoolEmail(Optional.of("test@example.com"))
                .profileUrl(Optional.of(""))
                .tags(new ArrayList<>())
                .build();

        userRepository.createUser(testUser);
        userRepository.createUser(testAdminUser);
    }

    @AfterAll
    void cleanUp() {
        boolean isSuccessful =
                userRepository.deleteUserById(testUser.getId()) && userRepository.deleteUserById(testAdminUser.getId());
        if (!isSuccessful) {
            fail("Failed deleting User by Id.");
        }
    }

    @Test
    @Order(1)
    void testGetId() {
        User found = userRepository.getUserById(testUser.getId()).orElse(null);
        assertNotNull(found);
        assertEquals(testUser, found);
    }

    @Test
    @Order(2)
    void testGetUserByDiscordId() {
        User found = userRepository.getUserByDiscordId(testUser.getDiscordId()).orElse(null);
        assertNotNull(found);
        assertEquals(testUser, found);
    }

    @Test
    @Order(3)
    void testGetUserByLeetcodeUsername() {
        User found = userRepository
                .getUserByLeetcodeUsername(testUser.getLeetcodeUsername().orElse(null))
                .orElse(null);
        assertNotNull(found);
        assertEquals(testUser, found);
    }

    @Test
    @Order(4)
    void testUpdateUser() {
        String newNickname = "Updated Nickname";
        testUser.setNickname(Optional.ofNullable(newNickname));

        boolean updateResult = userRepository.updateUser(testUser);
        assertTrue(updateResult);

        User updatedUser = userRepository.getUserById(testUser.getId()).orElse(null);
        assertNotNull(updatedUser);
        assertEquals(newNickname, updatedUser.getNickname().orElse(null));
    }

    @Test
    @Order(5)
    void testGetUserCount() {
        int count = userRepository.getUserCount();
        assertTrue(count > 0);
    }

    @Test
    @Order(6)
    void testGetUserCountWithQuery() {
        int countWithTestUser = userRepository.getUserCount("TestUser");
        assertTrue(countWithTestUser > 0);
    }

    @Test
    @Order(7)
    void testGetAllUsers() {
        ArrayList<User> users = userRepository.getAllUsers();
        assertNotNull(users);
        assertTrue(users.size() > 0);
        assertTrue(users.contains(testUser));
    }

    @Test
    @Order(8)
    void testGetAllUsersWithPagination() {
        ArrayList<User> users = userRepository.getAllUsers(1, 5, "");
        assertNotNull(users);
        assertTrue(users.size() >= 0);
        ArrayList<User> searchResults = userRepository.getAllUsers(1, 100, "TestUser");
        assertTrue(searchResults.contains(testUser));
    }

    @Test
    @Order(9)
    void testUserExistsByLeetcodeUsername() {
        boolean exists = userRepository.userExistsByLeetcodeUsername(
                testUser.getLeetcodeUsername().orElse(null));
        assertTrue(exists);
    }

    @Test
    @Order(10)
    void testGetUserCountWithQueryAndFilter() {
        int countWithAdminTestUserAndFilter = userRepository.getUserCount("TestAdminUser", true);
        assertTrue(countWithAdminTestUserAndFilter > 0);

        int countWithTestUserAndFilter = userRepository.getUserCount("TestUser", true);
        assertTrue(countWithTestUserAndFilter == 0);
    }

    @Test
    @Order(11)
    void testGetAllUsersWithPaginationAndFilter() {
        ArrayList<User> users = userRepository.getAllUsers(1, 5, "", true);
        assertNotNull(users);
        assertTrue(users.size() >= 0);

        ArrayList<User> searchResults = userRepository.getAllUsers(1, 100, "Test", false);
        assertTrue(searchResults.contains(testAdminUser));
        assertTrue(searchResults.contains(testUser));

        ArrayList<User> searchResultsWithFilter = userRepository.getAllUsers(1, 100, "Test", true);
        assertTrue(searchResultsWithFilter.contains(testAdminUser));
        assertTrue(!searchResultsWithFilter.contains(testUser));
    }

    @Test
    @Order(12)
    void testDeactivateReactivateUser() {
        String id = testUser.getId();

        userRepository.deactivateUserAndDeleteSessions(id);
        assertTrue(userRepository.getUserById(id).isEmpty());
        assertFalse(userRepository.getAllUsers().contains(testUser));

        User deactivated =
                userRepository.getUserByDiscordId(testUser.getDiscordId()).orElse(null);
        assertNotNull(deactivated);
        assertTrue(deactivated.getDeletedAt().isPresent());

        assertTrue(userRepository.reactivateUserById(id));
        User reactivated = userRepository.getUserById(id).orElse(null);
        assertNotNull(reactivated);
        assertTrue(reactivated.getDeletedAt().isEmpty());
    }

    @Test
    @Order(13)
    void testGetAllUsersFiltersDeactivated() {
        int countBefore = userRepository.getUserCount();
        userRepository.deactivateUserAndDeleteSessions(testUser.getId());
        try {
            assertFalse(userRepository.getAllUsers(1, 5, "").contains(testUser));
            assertEquals(countBefore - 1, userRepository.getUserCount());
        } finally {
            userRepository.reactivateUserById(testUser.getId());
        }
    }

    @Test
    @Order(14)
    void testDeactivateUserAndDeleteSessions() {
        String id = testUser.getId();
        Session session = Session.builder()
                .userId(id)
                .expiresAt(StandardizedLocalDateTime.now().plusHours(1))
                .build();
        sessionRepository.createSession(session);

        try {
            userRepository.deactivateUserAndDeleteSessions(id);

            assertTrue(userRepository.getUserById(id).isEmpty());
            assertTrue(sessionRepository.getSessionsByUserId(id).isEmpty());
        } finally {
            userRepository.reactivateUserById(id);
        }
    }
}
