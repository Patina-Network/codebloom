package org.patinanetwork.codebloom.common.db.repos.club;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.patinanetwork.codebloom.common.db.models.club.Club;
import org.patinanetwork.codebloom.common.db.models.usertag.Tag;
import org.patinanetwork.codebloom.common.db.repos.BaseRepositoryTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(OrderAnnotation.class)
public class ClubRepositoryTest extends BaseRepositoryTest {

    private ClubRepository repo;
    private Club testClub;
    private Club deletableClub;

    @Autowired
    public ClubRepositoryTest(final ClubRepository repo) {
        this.repo = repo;
    }

    @BeforeAll
    void createClub() {
        long timestamp = System.currentTimeMillis();
        testClub = Club.builder()
                .name("Test Club")
                .description("Integration test club")
                .slug("test-club-" + timestamp)
                .splashIconUrl("")
                .password("testpassword123")
                .tag(Tag.Gwc)
                .build();

        repo.createClub(testClub);
        testClub = repo.getClubBySlug(testClub.getSlug()).orElse(null);
        assertNotNull(testClub, "Test club should be created and retrievable");
    }

    @AfterAll
    void cleanUp() {
        if (testClub != null && testClub.getId() != null) {
            boolean isSuccessful = repo.deleteClubById(testClub.getId());
            if (!isSuccessful) {
                fail("Failed deleting club by id.");
            }
        }
    }

    @Test
    @Order(1)
    void testGetClubById() {
        Club found = repo.getClubById(testClub.getId()).orElse(null);
        assertNotNull(found);
        assertEquals(testClub.getId(), found.getId());
        assertEquals(testClub, found);
    }

    @Test
    @Order(2)
    void testGetClubBySlug() {
        Club found = repo.getClubBySlug(testClub.getSlug()).orElse(null);
        assertNotNull(found);
        assertEquals(testClub.getSlug(), found.getSlug());
        assertEquals(testClub, found);
    }

    @Test
    @Order(3)
    void testUpdateClub() {
        Club updatedClub = Club.builder()
                .id(testClub.getId())
                .name("Updated Club Name")
                .description("Updated Description")
                .slug(testClub.getSlug())
                .splashIconUrl("")
                .password("newpassword456")
                .tag(Tag.Gwc)
                .build();

        Club result = repo.updateClub(updatedClub).orElse(null);

        assertNotNull(result);
        assertEquals("Updated Club Name", result.getName());
        assertEquals("Updated Description", result.getDescription());
        assertEquals("", result.getSplashIconUrl());
        assertEquals("newpassword456", result.getPassword());
        assertEquals(Tag.Gwc, result.getTag());
    }

    @Test
    @Order(4)
    void testDeleteClubById() {
        long timestamp = System.currentTimeMillis();
        deletableClub = Club.builder()
                .name("Deletable Club")
                .description("Should be deleted")
                .slug("deletable-club-" + timestamp)
                .splashIconUrl("")
                .password("deletepassword")
                .tag(Tag.Gwc)
                .build();

        repo.createClub(deletableClub);
        deletableClub = repo.getClubBySlug(deletableClub.getSlug()).orElse(null);

        Club found = repo.getClubById(deletableClub.getId()).orElse(null);
        assertNotNull(found);
        assertEquals(deletableClub.getId(), found.getId());
        assertEquals(deletableClub.getName(), found.getName());

        boolean deleted = repo.deleteClubById(deletableClub.getId());
        assertTrue(deleted);

        assertTrue(repo.getClubById(deletableClub.getId()).isEmpty());
    }

    @Test
    @Order(5)
    void testDeleteClubBySlug() {
        long timestamp = System.currentTimeMillis();
        Club deletableBySlugClub = Club.builder()
                .name("Deletable By Slug Club")
                .description("Should be deleted by slug")
                .slug("deletable-by-slug-club-" + timestamp)
                .splashIconUrl("")
                .password("deleteslugpassword")
                .tag(Tag.Gwc)
                .build();

        repo.createClub(deletableBySlugClub);
        deletableBySlugClub = repo.getClubBySlug(deletableBySlugClub.getSlug()).orElse(null);

        Club found = repo.getClubBySlug(deletableBySlugClub.getSlug()).orElse(null);
        assertNotNull(found);
        assertEquals(deletableBySlugClub.getSlug(), found.getSlug());
        assertEquals(deletableBySlugClub.getName(), found.getName());

        boolean deleted = repo.deleteClubBySlug(deletableBySlugClub.getSlug());
        assertTrue(deleted);

        assertTrue(repo.getClubBySlug(deletableBySlugClub.getSlug()).isEmpty());
    }
}
