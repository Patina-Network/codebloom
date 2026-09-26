package org.patinanetwork.codebloom.common.db.repos.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
// import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.patinanetwork.codebloom.common.db.models.Session;
import org.patinanetwork.codebloom.common.db.repos.BaseRepositoryTest;
import org.patinanetwork.codebloom.common.time.StandardizedLocalDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(OrderAnnotation.class)
@Slf4j
public class SessionRepositoryTest extends BaseRepositoryTest {

    private SessionRepository sessionRepository;
    private Session testSession;
    private String mockUserId = "0c9b2e77-74cc-4b9e-b7f9-cfe0fd05e50b";

    @Autowired
    public SessionRepositoryTest(final SessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    @BeforeAll
    void createSession() {
        testSession = Session.builder()
                .userId(mockUserId)
                .expiresAt(StandardizedLocalDateTime.now().plusHours(1))
                .build();

        sessionRepository.createSession(testSession);
        log.info("Created test session with ID: {}", testSession.getId().get());
    }

    @AfterAll
    void deleteSession() {
        String sessionId = testSession.getId().get();
        log.info("The test session to be deleted has an id of {}", sessionId);
        boolean isSuccessful = sessionRepository.deleteSessionById(sessionId);

        if (!isSuccessful) {
            fail("Failed to delete test announcement");
        } else {
            log.info("Deleted test session with ID: {}", sessionId);
        }
    }

    @Test
    void testGetSessionById() {
        String sessionId = testSession.getId().get();
        Session found = sessionRepository.getSessionById(sessionId).get();
        assertNotNull(found);
        assertEquals(testSession.getId().get(), found.getId().get());
        assertEquals(testSession, found);
    }

    @Test
    void testGetSessionsByUserId() {
        List<Session> sessionList = sessionRepository.getSessionsByUserId(mockUserId);
        assertNotNull(sessionList);
        assertFalse(sessionList.isEmpty());
        assertTrue(sessionList.stream().anyMatch(session -> session.getId().equals(testSession.getId())));
    }

    @Test
    void testDeleteSessionById() {
        Session tempSession = Session.builder()
                .userId(mockUserId)
                .expiresAt(StandardizedLocalDateTime.now().plusHours(1))
                .build();

        sessionRepository.createSession(tempSession);

        String sessionId = tempSession.getId().get();
        boolean isSuccessful = sessionRepository.deleteSessionById(sessionId);
        assertTrue(isSuccessful);
        log.info("Deleted session with ID: {}", tempSession.getId().get());
    }

    @Test
    void testDeleteSessionsByUserId() {
        Session tempSession = Session.builder()
                .userId(mockUserId)
                .expiresAt(StandardizedLocalDateTime.now().plusHours(1))
                .build();

        sessionRepository.createSession(tempSession);

        boolean isSuccessful = sessionRepository.deleteSessionsByUserId(mockUserId);
        assertTrue(isSuccessful);
        log.info("Deleted sessions for user ID: {}", mockUserId);

        sessionRepository.createSession(testSession);
    }
}
