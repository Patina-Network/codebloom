package org.patinanetwork.codebloom.common.db.repos.weekly;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.patinanetwork.codebloom.common.db.models.weekly.WeeklyMessage;
import org.patinanetwork.codebloom.common.db.repos.BaseRepositoryTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(OrderAnnotation.class)
@Slf4j
public class WeeklyMessageRepositoryTest extends BaseRepositoryTest {

    private WeeklyMessageRepository weeklyMessageRepository;
    private WeeklyMessage testWeeklyMessage;

    @Autowired
    public WeeklyMessageRepositoryTest(final WeeklyMessageRepository weeklyMessageRepository) {
        this.weeklyMessageRepository = weeklyMessageRepository;
    }

    @BeforeAll
    void createTestWeeklyMessage() {
        testWeeklyMessage = WeeklyMessage.builder()
                .id("Test Weekly Message")
                .createdAt(java.time.LocalDateTime.now())
                .build();
        boolean isSuccessful = weeklyMessageRepository.createLatestWeeklyMessage(testWeeklyMessage);

        if (!isSuccessful) {
            fail("Failed to create test weekly message");
        }
    }

    @AfterAll
    void cleanUp() {
        WeeklyMessage latest = weeklyMessageRepository.getLatestWeeklyMessage().orElse(null);
        boolean isSuccessful = latest != null && weeklyMessageRepository.deleteWeeklyMessageById(latest.getId());

        if (!isSuccessful) {
            fail("Failed to delete test weekly message");
        }
    }

    @Test
    @Order(1)
    void testGetLatestWeeklyMessage() {
        WeeklyMessage possibleTestWeeklyMessage =
                weeklyMessageRepository.getLatestWeeklyMessage().orElse(null);

        assertNotNull(possibleTestWeeklyMessage, "Latest weekly message should not be null");
        log.info("testWeeklyMessage: {}", testWeeklyMessage);
        log.info("possibleTestWeeklyMessage: {}", possibleTestWeeklyMessage);
        assertEquals(testWeeklyMessage.getId(), possibleTestWeeklyMessage.getId(), "IDs do not match");
        assertEquals(
                testWeeklyMessage.getCreatedAt(),
                possibleTestWeeklyMessage.getCreatedAt(),
                "CreatedAt timestamps do not match");
    }

    @Test
    @Order(2)
    void findWeeklyMessageById() {
        WeeklyMessage latest = weeklyMessageRepository.getLatestWeeklyMessage().orElse(null);
        WeeklyMessage possibleTestWeeklyMessage =
                weeklyMessageRepository.getWeeklyMessageById(latest.getId()).orElse(null);

        log.info("testWeeklyMessage: {}", testWeeklyMessage.toString());
        log.info(
                "possibleTestWeeklyMessage: {}",
                possibleTestWeeklyMessage != null ? possibleTestWeeklyMessage.toString() : "null");

        assertNotNull(possibleTestWeeklyMessage, "possibleTestWeeklyMessage should not be null");
        assertEquals(latest.getId(), possibleTestWeeklyMessage.getId(), "IDs do not match");
    }

    @Test
    void createAndDeleteLatestWeeklyMessage() {
        boolean created = weeklyMessageRepository.createLatestWeeklyMessage();
        assertTrue(created, "createLatestWeeklyMessage() should return true");

        boolean deleted = weeklyMessageRepository.deleteLatestWeeklyMessage();
        assertTrue(deleted, "deleteLatestWeeklyMessage() should return true");
    }
}
