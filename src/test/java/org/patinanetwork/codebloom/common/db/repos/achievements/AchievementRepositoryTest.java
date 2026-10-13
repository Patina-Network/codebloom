package org.patinanetwork.codebloom.common.db.repos.achievements;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.patinanetwork.codebloom.common.db.models.achievements.Achievement;
import org.patinanetwork.codebloom.common.db.models.achievements.AchievementPlaceEnum;
import org.patinanetwork.codebloom.common.db.models.usertag.Tag;
import org.patinanetwork.codebloom.common.db.repos.BaseRepositoryTest;
import org.patinanetwork.codebloom.common.time.StandardizedOffsetDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(OrderAnnotation.class)
public class AchievementRepositoryTest extends BaseRepositoryTest {

    private AchievementRepository repo;
    private Achievement testAchievement;
    private Achievement deletableAchievement;
    private String mockUserId = "ed3bfe18-e42a-467f-b4fa-07e8da4d2555";
    // Not expired leaderboard
    private String mockLeaderboardId = "39bc2def-669f-4383-8ea3-7202efd613f2";

    @Autowired
    public AchievementRepositoryTest(final AchievementRepository repo) {
        this.repo = repo;
    }

    @BeforeAll
    void createAchievement() {
        testAchievement = Achievement.builder()
                .userId(mockUserId)
                .place(AchievementPlaceEnum.ONE)
                .leaderboard(Optional.empty())
                .leaderboardId(mockLeaderboardId)
                .title("Test Achievement")
                .description(Optional.of("Integration test achievement"))
                .isActive(true)
                .createdAt(StandardizedOffsetDateTime.now())
                .deletedAt(Optional.empty())
                .build();

        repo.createAchievement(testAchievement);
    }

    @AfterAll
    void cleanUp() {
        boolean isSuccessful = repo.deleteAchievementById(testAchievement.getId());
        if (!isSuccessful) {
            fail("Failed deleting achievement by id.");
        }
    }

    @Test
    @Order(1)
    void testGetAchievementById() {
        Achievement found = repo.getAchievementById(testAchievement.getId()).orElse(null);
        assertNotNull(found);
        assertEquals(testAchievement.getId(), found.getId());
        assertEquals(testAchievement, found);
    }

    @Test
    @Order(2)
    void testGetAchievementsByUserId() {
        List<Achievement> achievementList = repo.getAchievementsByUserId(mockUserId);
        assertNotNull(achievementList);
        assertFalse(achievementList.isEmpty());
        assertTrue(achievementList.stream().anyMatch(a -> a.getId().equals(testAchievement.getId())));
    }

    @Test
    @Order(3)
    void testUpdateAchievement() {
        Achievement updatedAchievement = Achievement.builder()
                .id(testAchievement.getId())
                .userId(testAchievement.getUserId())
                .place(AchievementPlaceEnum.THREE)
                .leaderboard(Optional.of(Tag.Patina))
                .leaderboardId(mockLeaderboardId)
                .title("Updated Title")
                .description(Optional.of("Updated Description"))
                .isActive(false)
                .createdAt(testAchievement.getCreatedAt())
                .deletedAt(testAchievement.getDeletedAt())
                .build();

        Achievement result = repo.updateAchievement(updatedAchievement).orElse(null);

        assertNotNull(result);

        assertEquals("Updated Title", result.getTitle());
        assertEquals(Optional.of("Updated Description"), result.getDescription());
        assertFalse(result.isActive());
    }

    @Test
    @Order(4)
    void testDeleteAchievementById() {
        deletableAchievement = Achievement.builder()
                .userId(mockUserId)
                .place(AchievementPlaceEnum.ONE)
                .leaderboard(Optional.empty())
                .leaderboardId(mockLeaderboardId)
                .title("Deletable Achievement")
                .description(Optional.of("Should be deleted"))
                .isActive(true)
                .createdAt(StandardizedOffsetDateTime.now())
                .deletedAt(Optional.empty())
                .build();

        repo.createAchievement(deletableAchievement);

        Achievement found =
                repo.getAchievementById(deletableAchievement.getId()).orElse(null);
        assertNotNull(found);
        assertEquals(deletableAchievement.getId(), found.getId());

        boolean deleted = repo.deleteAchievementById(deletableAchievement.getId());
        assertTrue(deleted);

        assertTrue(repo.getAchievementById(deletableAchievement.getId()).isEmpty());
    }
}
