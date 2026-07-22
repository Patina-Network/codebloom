package org.patinanetwork.codebloom.common.db.repos.user;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.patinanetwork.codebloom.common.db.models.user.User;
import org.patinanetwork.codebloom.common.db.models.user.UserMetrics;
import org.patinanetwork.codebloom.common.db.repos.BaseRepositoryTest;
import org.patinanetwork.codebloom.common.db.repos.user.options.UserMetricsFilterOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(OrderAnnotation.class)
@Slf4j
public class UserMetricsRepositoryTest extends BaseRepositoryTest {

    private UserMetricsRepository userMetricsRepository;
    private UserRepository userRepository;

    private User testUser;
    private UserMetrics testUserMetrics;

    @Autowired
    public UserMetricsRepositoryTest(
            final UserMetricsRepository userMetricsRepository, final UserRepository userRepository) {
        this.userMetricsRepository = userMetricsRepository;
        this.userRepository = userRepository;
    }

    @BeforeAll
    void setUp() {
        testUser = User.builder()
                .discordId("test-metrics-" + System.currentTimeMillis())
                .discordName("TestMetricsUser")
                .leetcodeUsername("testmetricsuser")
                .nickname("TestMetricsNickname")
                .admin(false)
                .schoolEmail("testmetrics@example.com")
                .profileUrl("")
                .tags(new ArrayList<>())
                .build();
        userRepository.createUser(testUser);

        testUserMetrics =
                UserMetrics.builder().userId(testUser.getId()).points(100).build();
        userMetricsRepository.createUserMetrics(testUserMetrics);

        if (testUserMetrics.getId() == null) {
            fail("UserMetrics id was not set after creation");
        }

        if (testUserMetrics.getCreatedAt() == null) {
            fail("UserMetrics createdAt was not set after creation");
        }
    }

    @AfterAll
    void cleanUp() {
        boolean isSuccessful = userRepository.deleteUserById(testUser.getId());
        if (!isSuccessful) {
            fail("Failed to delete test user");
        }
    }

    @Test
    @Order(1)
    void testCreateUserMetrics() {
        assertNotNull(testUserMetrics.getId());
        assertNotNull(testUserMetrics.getCreatedAt());
        assertEquals(testUser.getId(), testUserMetrics.getUserId());
        assertEquals(100, testUserMetrics.getPoints());
    }

    @Test
    @Order(2)
    void testFindUserMetricsById() {
        Optional<UserMetrics> found = userMetricsRepository.findUserMetricsById(testUserMetrics.getId());

        assertTrue(found.isPresent());
        assertEquals(testUserMetrics, found.get());
    }

    @Test
    @Order(3)
    void testFindUserMetrics() {
        List<UserMetrics> results =
                userMetricsRepository.findUserMetrics(testUser.getId(), UserMetricsFilterOptions.DEFAULT);

        assertNotNull(results);
        assertTrue(results.size() > 0);
        assertTrue(results.stream().anyMatch(m -> m.getId().equals(testUserMetrics.getId())));
    }

    @Test
    @Order(4)
    void testDeleteUserMetricsById() {
        UserMetrics metricsToDelete =
                UserMetrics.builder().userId(testUser.getId()).points(50).build();
        userMetricsRepository.createUserMetrics(metricsToDelete);
        assertNotNull(metricsToDelete.getId());

        String idToDelete = metricsToDelete.getId();

        Optional<UserMetrics> existing = userMetricsRepository.findUserMetricsById(idToDelete);
        assertTrue(existing.isPresent());

        boolean deleteSuccess = userMetricsRepository.deleteUserMetricsById(idToDelete);
        assertTrue(deleteSuccess);

        Optional<UserMetrics> deleted = userMetricsRepository.findUserMetricsById(idToDelete);
        assertFalse(deleted.isPresent());

        boolean deleteAgain = userMetricsRepository.deleteUserMetricsById(idToDelete);
        assertFalse(deleteAgain);
    }

    @Test
    @Order(5)
    void testFindUserMetricsExcludesSoftDeleted() {
        UserMetrics metricsToDelete =
                UserMetrics.builder().userId(testUser.getId()).points(75).build();
        userMetricsRepository.createUserMetrics(metricsToDelete);

        userMetricsRepository.deleteUserMetricsById(metricsToDelete.getId());

        List<UserMetrics> results =
                userMetricsRepository.findUserMetrics(testUser.getId(), UserMetricsFilterOptions.DEFAULT);
        assertFalse(results.stream().anyMatch(m -> m.getId().equals(metricsToDelete.getId())));
    }

    @Test
    @Order(6)
    void testFindUserMetricsByIdWithInvalidUUID() {
        assertThrows(RuntimeException.class, () -> userMetricsRepository.findUserMetricsById("invalid-uuid-format"));
    }

    @Test
    @Order(7)
    void testFindUserMetricsWithInvalidUUID() {
        assertThrows(
                RuntimeException.class,
                () -> userMetricsRepository.findUserMetrics("invalid-uuid-format", UserMetricsFilterOptions.DEFAULT));
    }

    @Test
    @Order(8)
    void testDeleteUserMetricsByIdWithInvalidUUID() {
        assertThrows(RuntimeException.class, () -> userMetricsRepository.deleteUserMetricsById("invalid-uuid-format"));
    }

    @Test
    @Order(9)
    void testFindUserMetricsByDateRange() {
        OffsetDateTime before = testUserMetrics.getCreatedAt().minusSeconds(1);
        OffsetDateTime after = testUserMetrics.getCreatedAt().plusSeconds(1);

        List<UserMetrics> results = userMetricsRepository.findUserMetrics(
                testUser.getId(),
                UserMetricsFilterOptions.builder().from(before).to(after).build());

        assertNotNull(results);
        assertTrue(results.stream().anyMatch(m -> m.getId().equals(testUserMetrics.getId())));
    }

    @Test
    @Order(10)
    void testFindUserMetricsByDateRangeExcludesOutOfRange() {
        OffsetDateTime future = testUserMetrics.getCreatedAt().plusDays(1);
        OffsetDateTime furtherFuture = testUserMetrics.getCreatedAt().plusDays(2);

        List<UserMetrics> results = userMetricsRepository.findUserMetrics(
                testUser.getId(),
                UserMetricsFilterOptions.builder()
                        .from(future)
                        .to(furtherFuture)
                        .build());

        assertFalse(results.stream().anyMatch(m -> m.getId().equals(testUserMetrics.getId())));
    }

    @Test
    @Order(11)
    void testFindUserMetricsByDateRangeExcludesSoftDeleted() {
        UserMetrics metricsToDelete =
                UserMetrics.builder().userId(testUser.getId()).points(25).build();
        userMetricsRepository.createUserMetrics(metricsToDelete);
        userMetricsRepository.deleteUserMetricsById(metricsToDelete.getId());

        OffsetDateTime before = metricsToDelete.getCreatedAt().minusSeconds(1);
        OffsetDateTime after = metricsToDelete.getCreatedAt().plusSeconds(1);

        List<UserMetrics> results = userMetricsRepository.findUserMetrics(
                testUser.getId(),
                UserMetricsFilterOptions.builder().from(before).to(after).build());

        assertFalse(results.stream().anyMatch(m -> m.getId().equals(metricsToDelete.getId())));
    }

    @Test
    @Order(12)
    void testFindUserMetricsByDateRangeWithInvalidUUID() {
        assertThrows(
                RuntimeException.class,
                () -> userMetricsRepository.findUserMetrics(
                        "invalid-uuid-format",
                        UserMetricsFilterOptions.builder()
                                .from(OffsetDateTime.now().minusDays(1))
                                .to(OffsetDateTime.now())
                                .build()));
    }

    @Test
    @Order(13)
    void testFindUserMetricsPaginatedReturnsCorrectPage() {
        UserMetrics extra1 =
                UserMetrics.builder().userId(testUser.getId()).points(200).build();
        UserMetrics extra2 =
                UserMetrics.builder().userId(testUser.getId()).points(300).build();
        userMetricsRepository.createUserMetrics(extra1);
        userMetricsRepository.createUserMetrics(extra2);

        List<UserMetrics> page1 = userMetricsRepository.findUserMetrics(
                testUser.getId(),
                UserMetricsFilterOptions.builder().page(1).pageSize(2).build());
        List<UserMetrics> page2 = userMetricsRepository.findUserMetrics(
                testUser.getId(),
                UserMetricsFilterOptions.builder().page(2).pageSize(2).build());

        assertEquals(2, page1.size());
        assertFalse(page2.isEmpty());

        List<String> page1Ids = page1.stream().map(UserMetrics::getId).toList();
        assertFalse(page2.stream().anyMatch(m -> page1Ids.contains(m.getId())));
    }

    @Test
    @Order(14)
    void testFindUserMetricsPaginatedEmptyPage() {
        int total = userMetricsRepository.countUserMetrics(testUser.getId(), UserMetricsFilterOptions.DEFAULT);

        List<UserMetrics> result = userMetricsRepository.findUserMetrics(
                testUser.getId(),
                UserMetricsFilterOptions.builder().page(total + 1).pageSize(10).build());

        assertTrue(result.isEmpty());
    }

    @Test
    @Order(15)
    void testFindUserMetricsPaginatedWithInvalidUUID() {
        assertThrows(
                RuntimeException.class,
                () -> userMetricsRepository.findUserMetrics(
                        "invalid-uuid-format",
                        UserMetricsFilterOptions.builder().page(1).pageSize(10).build()));
    }

    @Test
    @Order(16)
    void testFindUserMetricsByDateRangePaginatedReturnsCorrectPage() {
        OffsetDateTime before = testUserMetrics.getCreatedAt().minusSeconds(1);
        OffsetDateTime after = OffsetDateTime.now().plusSeconds(1);

        int total = userMetricsRepository.countUserMetrics(
                testUser.getId(),
                UserMetricsFilterOptions.builder().from(before).to(after).build());
        assertTrue(total >= 2, "Expected at least 2 records in range for pagination test");

        List<UserMetrics> page1 = userMetricsRepository.findUserMetrics(
                testUser.getId(),
                UserMetricsFilterOptions.builder()
                        .from(before)
                        .to(after)
                        .page(1)
                        .pageSize(1)
                        .build());
        List<UserMetrics> page2 = userMetricsRepository.findUserMetrics(
                testUser.getId(),
                UserMetricsFilterOptions.builder()
                        .from(before)
                        .to(after)
                        .page(2)
                        .pageSize(1)
                        .build());

        assertEquals(1, page1.size());
        assertEquals(1, page2.size());
        assertNotEquals(page1.get(0).getId(), page2.get(0).getId());
    }

    @Test
    @Order(17)
    void testFindUserMetricsByDateRangePaginatedEmptyPage() {
        OffsetDateTime before = testUserMetrics.getCreatedAt().minusSeconds(1);
        OffsetDateTime after = OffsetDateTime.now().plusSeconds(1);

        int total = userMetricsRepository.countUserMetrics(
                testUser.getId(),
                UserMetricsFilterOptions.builder().from(before).to(after).build());

        List<UserMetrics> result = userMetricsRepository.findUserMetrics(
                testUser.getId(),
                UserMetricsFilterOptions.builder()
                        .from(before)
                        .to(after)
                        .page(total + 1)
                        .pageSize(10)
                        .build());

        assertTrue(result.isEmpty());
    }

    @Test
    @Order(18)
    void testFindUserMetricsByDateRangePaginatedWithInvalidUUID() {
        assertThrows(
                RuntimeException.class,
                () -> userMetricsRepository.findUserMetrics(
                        "invalid-uuid-format",
                        UserMetricsFilterOptions.builder()
                                .from(OffsetDateTime.now().minusDays(1))
                                .to(OffsetDateTime.now())
                                .page(1)
                                .pageSize(10)
                                .build()));
    }

    @Test
    @Order(19)
    void testCountUserMetrics() {
        int count = userMetricsRepository.countUserMetrics(testUser.getId(), UserMetricsFilterOptions.DEFAULT);
        List<UserMetrics> all =
                userMetricsRepository.findUserMetrics(testUser.getId(), UserMetricsFilterOptions.DEFAULT);

        assertEquals(all.size(), count);
    }

    @Test
    @Order(20)
    void testCountUserMetricsWithInvalidUUID() {
        assertThrows(
                RuntimeException.class,
                () -> userMetricsRepository.countUserMetrics("invalid-uuid-format", UserMetricsFilterOptions.DEFAULT));
    }

    @Test
    @Order(21)
    void testCountUserMetricsByDateRange() {
        OffsetDateTime before = testUserMetrics.getCreatedAt().minusSeconds(1);
        OffsetDateTime after = OffsetDateTime.now().plusSeconds(1);

        UserMetricsFilterOptions options =
                UserMetricsFilterOptions.builder().from(before).to(after).build();

        int count = userMetricsRepository.countUserMetrics(testUser.getId(), options);
        List<UserMetrics> all = userMetricsRepository.findUserMetrics(testUser.getId(), options);

        assertEquals(all.size(), count);
    }

    @Test
    @Order(22)
    void testFindUserMetricsSqlExceptionWrapped() throws SQLException {
        DataSource ds = mock(DataSource.class);
        when(ds.getConnection()).thenThrow(new SQLException("Connection failed"));
        UserMetricsSqlRepository repo = new UserMetricsSqlRepository(JdbcClient.create(ds));
        assertThrows(
                RuntimeException.class, () -> repo.findUserMetrics(testUser.getId(), UserMetricsFilterOptions.DEFAULT));
    }

    @Test
    @Order(23)
    void testCountUserMetricsSqlExceptionWrapped() throws SQLException {
        DataSource ds = mock(DataSource.class);
        when(ds.getConnection()).thenThrow(new SQLException("Connection failed"));
        UserMetricsSqlRepository repo = new UserMetricsSqlRepository(JdbcClient.create(ds));
        assertThrows(
                RuntimeException.class,
                () -> repo.countUserMetrics(testUser.getId(), UserMetricsFilterOptions.DEFAULT));
    }

    @Test
    @Order(24)
    void testCountUserMetricsReturnsZeroWhenNoRows() throws SQLException {
        DataSource ds = mock(DataSource.class);
        Connection conn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);

        when(ds.getConnection()).thenReturn(conn);
        when(conn.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(false);

        UserMetricsSqlRepository repo = new UserMetricsSqlRepository(JdbcClient.create(ds));
        int count = repo.countUserMetrics(testUser.getId(), UserMetricsFilterOptions.DEFAULT);

        assertEquals(0, count);
    }
}
