package org.patinanetwork.codebloom.common.db.repos.announcement;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.patinanetwork.codebloom.common.db.models.announcement.Announcement;
import org.patinanetwork.codebloom.common.db.repos.BaseRepositoryTest;
import org.patinanetwork.codebloom.common.time.StandardizedOffsetDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(OrderAnnotation.class)
@Slf4j
public class AnnouncementRepositoryTest extends BaseRepositoryTest {

    private AnnouncementRepository announcementRepository;
    private Announcement testAnnouncement;

    @Autowired
    public AnnouncementRepositoryTest(final AnnouncementRepository announcementRepository) {
        this.announcementRepository = announcementRepository;
    }

    @BeforeAll
    void createTestAnnouncement() {
        testAnnouncement = Announcement.builder()
                // id will be set by announcementRepository
                .id(null)
                .expiresAt(StandardizedOffsetDateTime.normalize(
                        StandardizedOffsetDateTime.now().plusMinutes(5L)))
                .showTimer(Optional.of(true))
                .message("Hi this is a test announcement!")
                .build();
        boolean isSuccessful = announcementRepository.createAnnouncement(testAnnouncement);

        if (!isSuccessful) {
            fail("Failed to create test announcement");
        }
        testAnnouncement = announcementRepository.getRecentAnnouncement().orElse(null);
        if (testAnnouncement == null) {
            fail("Failed to fetch the created test announcement");
        }
    }

    @AfterAll
    void deleteTestAnnouncement() {
        boolean isSuccessful = announcementRepository.deleteAnnouncementById(testAnnouncement.getId());

        if (!isSuccessful) {
            fail("Failed to delete test announcement");
        }
    }

    /**
     * @note this could possibly fail via race condition if you create a new announcement in the few seconds while this
     *     test suite is running, overriding the test announcement. If you do, let the test suite finish and let
     *     `@AfterAll` delete the test announcement and then just run the test suite again.
     *     <p>We run this test first just to make sure that we don't run into that issue.
     */
    @Test
    @Order(1)
    void findMostRecentAnnouncement() {
        Announcement possibleTestAnnouncement =
                announcementRepository.getRecentAnnouncement().orElse(null);

        if (possibleTestAnnouncement == null) {
            fail("most recent announcement doesn't exist, even though we created a test announcement");
        }

        log.info("testAnnouncement: {}", testAnnouncement.toString());
        log.info("possibleTestAnnouncement: {}", possibleTestAnnouncement.toString());

        /** @note - see method note */
        if (!testAnnouncement.equals(possibleTestAnnouncement)) {
            fail("the most recent announcement does not equal to the test announcement");
        }
    }

    @Test
    @Order(2)
    void findAnnouncementById() {
        Announcement possibleTestAnnouncement = announcementRepository
                .getAnnouncementById(testAnnouncement.getId())
                .orElse(null);

        if (possibleTestAnnouncement == null) {
            fail("failed to find announcement by id");
        }

        log.info("testAnnouncement: {}", testAnnouncement.toString());
        log.info("possibleTestAnnouncement: {}", possibleTestAnnouncement.toString());

        if (!testAnnouncement.equals(possibleTestAnnouncement)) {
            fail("the generated test announcement does not match the announcement fetched with get announcement by ID");
        }
    }

    @Test
    @Order(4)
    void updateAnnouncementTest() {
        Announcement updatedAnnouncement = Announcement.builder()
                .id(testAnnouncement.getId())
                .expiresAt(testAnnouncement.getExpiresAt())
                .showTimer(Optional.of(true))
                .message("Hi this is an update announcement!")
                .build();
        boolean result = announcementRepository.updateAnnouncement(updatedAnnouncement);
        if (!result) {
            fail("failure to update announcement");
        }
        Announcement resultAnnouncement = announcementRepository
                .getAnnouncementById(updatedAnnouncement.getId())
                .orElse(null);
        assertNotNull(resultAnnouncement);
        assertEquals(resultAnnouncement.getMessage(), "Hi this is an update announcement!");
        assertEquals(resultAnnouncement.getId(), testAnnouncement.getId());
        assertTrue(resultAnnouncement.getShowTimer().orElse(false));
    }

    @Test
    @Order(3)
    void findAllAnnouncements() {
        List<Announcement> announcementsList = announcementRepository.getAllAnnouncements();

        if (announcementsList == null) {
            fail("list of all announcements is null");
        }

        if (announcementsList.size() == 0) {
            fail("list of all announcements is 0, when it should be atleast 1 from the test announcement");
        }

        log.info(announcementsList.toString());

        if (!announcementsList.contains(testAnnouncement)) {
            fail("test announcement cannot be found in the list of all announcements");
        }
    }
}
