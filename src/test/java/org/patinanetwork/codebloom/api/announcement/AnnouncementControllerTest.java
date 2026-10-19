package org.patinanetwork.codebloom.api.announcement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.restassured.RestAssured;
import io.restassured.common.mapper.TypeRef;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.patinanetwork.codebloom.api.admin.body.CreateAnnouncementBody;
import org.patinanetwork.codebloom.common.db.repos.announcement.AnnouncementRepository;
import org.patinanetwork.codebloom.common.dto.ApiResponder;
import org.patinanetwork.codebloom.common.dto.announcement.AnnouncementDto;
import org.patinanetwork.codebloom.common.time.StandardizedOffsetDateTime;
import org.patinanetwork.codebloom.config.NoJdaRequired;
import org.patinanetwork.codebloom.config.TestProtector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Import(TestProtector.class)
@Slf4j
public class AnnouncementControllerTest extends NoJdaRequired {

    @LocalServerPort
    private int port;

    private CreateAnnouncementBody createAnnouncementBody = CreateAnnouncementBody.builder()
            .message("Hi this is a test message!")
            .showTimer(true)
            .expiresAt(StandardizedOffsetDateTime.now().plusHours(24))
            .build();
    private AnnouncementDto testAnnouncement;

    /** only used for cleanup after all tests are done. */
    @Autowired
    private AnnouncementRepository announcementRepository;

    @BeforeEach
    void setUpPort() {
        RestAssured.port = port;
    }

    @BeforeAll
    static void setUpUri() {
        RestAssured.baseURI = "http://localhost";
    }

    @AfterAll
    void deleteTestUrl() {
        if (testAnnouncement == null) {
            return;
        }

        announcementRepository.deleteAnnouncementById(testAnnouncement.getId());
    }

    /**
     * Use builder pattern to pass in input.
     *
     * @throws JacksonException
     */
    private String builtTestCreateAnnouncementPayload(final CreateAnnouncementBody createAnnouncementBody)
            throws JacksonException {
        Map<String, Object> body = new HashMap<>();
        body.put("message", createAnnouncementBody.getMessage());
        body.put("showTimer", createAnnouncementBody.isShowTimer());
        body.put("expiresAt", createAnnouncementBody.getExpiresAt().toString());
        return new ObjectMapper().writeValueAsString(body);
    }

    @Test
    @Order(1)
    void createNewTestAnnouncementAsAdmin() throws JacksonException {
        ApiResponder<AnnouncementDto> apiResponder = RestAssured.given()
                .when()
                .header("Content-Type", "application/json")
                .body(builtTestCreateAnnouncementPayload(createAnnouncementBody))
                .post("/api/admin/announcement/create")
                .then()
                .statusCode(200)
                .extract()
                .as(new TypeRef<ApiResponder<AnnouncementDto>>() {});

        assertTrue(apiResponder != null, "Expected apiResponder to not be equal to null");
        assertTrue(apiResponder.isSuccess(), "Testing apiResponder success is true");
        assertTrue(apiResponder.getMessage() != null, "Testing apiResponder message is not null");

        testAnnouncement = apiResponder.getPayload();

        assertTrue(testAnnouncement != null, "Expected announcement to not be equal to null");

        assertEquals(
                testAnnouncement.getExpiresAt(),
                StandardizedOffsetDateTime.normalize(createAnnouncementBody.getExpiresAt()),
                "Expected announcement response and announcement request body expiresAt to be equal");
        assertTrue(
                testAnnouncement.getMessage().equals(createAnnouncementBody.getMessage()),
                "Expected announcement response and announcement request body message to be equal");
        assertTrue(
                testAnnouncement.isShowTimer() == createAnnouncementBody.isShowTimer(),
                "Expected announcement response and announcement request body showTimer to be equal");
    }

    @Test
    @Order(2)
    void getTestAnnouncement() {
        ApiResponder<AnnouncementDto> apiResponder = RestAssured.given()
                .when()
                .get("/api/announcement")
                .then()
                .statusCode(200)
                .extract()
                .as(new TypeRef<ApiResponder<AnnouncementDto>>() {});

        assertTrue(apiResponder != null, "Expected apiResponder to not be equal to null");
        assertTrue(apiResponder.isSuccess(), "Testing apiResponder success is true");
        assertTrue(apiResponder.getMessage() != null, "Testing apiResponder message is not null");
        AnnouncementDto newlyFetchedAnnouncement = apiResponder.getPayload();

        assertTrue(newlyFetchedAnnouncement != null, "expected newlyFetchedAnnouncement to not be null");

        log.info("testAnnouncement: {}", testAnnouncement.toString());
        log.info("possibleTestAnnouncement: {}", newlyFetchedAnnouncement.toString());

        assertTrue(
                testAnnouncement.equals(newlyFetchedAnnouncement),
                "Expected the previously created announcement to be equal to the newly fetched announcement");
    }
}
