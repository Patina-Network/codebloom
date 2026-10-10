package org.patinanetwork.codebloom.api.admin;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.patinanetwork.codebloom.api.admin.body.DeleteAnnouncementBody;
import org.patinanetwork.codebloom.api.admin.body.EditLeaderboardBody;
import org.patinanetwork.codebloom.api.admin.body.NewLeaderboardBody;
import org.patinanetwork.codebloom.api.admin.body.jda.DeleteMessageBody;
import org.patinanetwork.codebloom.common.components.DiscordClubManager;
import org.patinanetwork.codebloom.common.components.LeaderboardManager;
import org.patinanetwork.codebloom.common.db.models.announcement.Announcement;
import org.patinanetwork.codebloom.common.db.models.discord.DiscordClub;
import org.patinanetwork.codebloom.common.db.models.leaderboard.Leaderboard;
import org.patinanetwork.codebloom.common.db.models.question.QuestionWithUser;
import org.patinanetwork.codebloom.common.db.repos.announcement.AnnouncementRepository;
import org.patinanetwork.codebloom.common.db.repos.discord.club.DiscordClubRepository;
import org.patinanetwork.codebloom.common.db.repos.leaderboard.LeaderboardRepository;
import org.patinanetwork.codebloom.common.db.repos.question.QuestionRepository;
import org.patinanetwork.codebloom.common.db.repos.user.UserRepository;
import org.patinanetwork.codebloom.common.dto.ApiResponder;
import org.patinanetwork.codebloom.common.dto.Empty;
import org.patinanetwork.codebloom.common.dto.question.QuestionWithUserDto;
import org.patinanetwork.codebloom.common.time.StandardizedOffsetDateTime;
import org.patinanetwork.codebloom.utilities.exception.ValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

public class AdminControllerTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final LeaderboardRepository leaderboardRepository = mock(LeaderboardRepository.class);
    private final AnnouncementRepository announcementRepository = mock(AnnouncementRepository.class);
    private final QuestionRepository questionRepository = mock(QuestionRepository.class);
    private final DiscordClubManager discordClubManager = mock(DiscordClubManager.class);
    private final LeaderboardManager leaderboardManager = mock(LeaderboardManager.class);
    private final DiscordClubRepository discordClubRepository = mock(DiscordClubRepository.class);

    private final AdminController adminController;

    public AdminControllerTest() {
        adminController = spy(new AdminController(
                leaderboardRepository,
                userRepository,
                announcementRepository,
                questionRepository,
                discordClubManager,
                leaderboardManager,
                discordClubRepository));
    }

    @BeforeEach
    void setUp() {
        reset(
                userRepository,
                leaderboardRepository,
                announcementRepository,
                questionRepository,
                discordClubManager,
                leaderboardManager);
    }

    @Test
    void testCreateLeaderboardSuccessNoExistingLeaderboard() {
        NewLeaderboardBody body =
                NewLeaderboardBody.builder().name("Spring 2024 Challenge").build();

        when(leaderboardRepository.getRecentLeaderboardMetadata()).thenReturn(Optional.empty());

        ResponseEntity<ApiResponder<Empty>> response = adminController.createLeaderboard(null, body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertEquals("Leaderboard was created successfully.", response.getBody().getMessage());
        assertEquals(Empty.of(), response.getBody().getPayload());
        verify(leaderboardRepository).getRecentLeaderboardMetadata();
        verify(leaderboardRepository).addNewLeaderboard(any(Leaderboard.class));
        verify(leaderboardRepository).addAllUsersToLeaderboard(any());
        verify(discordClubManager, never()).sendLeaderboardCompletedDiscordMessageToAllClubs();
        verify(leaderboardManager, never()).generateAchievementsForAllWinners();
        verify(leaderboardRepository, never()).disableLeaderboardById(anyString());
    }

    @Test
    void testCreateLeaderboardSuccessWithExistingLeaderboard() {
        NewLeaderboardBody body =
                NewLeaderboardBody.builder().name("Fall 2024 Challenge").build();

        Leaderboard existingLeaderboard =
                Leaderboard.builder().id("existing-id").name("Old Leaderboard").build();

        when(leaderboardRepository.getRecentLeaderboardMetadata()).thenReturn(Optional.of(existingLeaderboard));

        ResponseEntity<ApiResponder<Empty>> response = adminController.createLeaderboard(null, body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertEquals("Leaderboard was created successfully.", response.getBody().getMessage());
        assertEquals(Empty.of(), response.getBody().getPayload());
        verify(leaderboardRepository).getRecentLeaderboardMetadata();
        verify(discordClubManager).sendLeaderboardCompletedDiscordMessageToAllClubs();
        verify(leaderboardManager).generateAchievementsForAllWinners();
        verify(leaderboardRepository).disableLeaderboardById("existing-id");
        verify(leaderboardRepository).addNewLeaderboard(any(Leaderboard.class));
        verify(leaderboardRepository).addAllUsersToLeaderboard(any());
    }

    @Test
    void testCreateLeaderboardEmptyName() {
        NewLeaderboardBody body = NewLeaderboardBody.builder().name("").build();

        ResponseEntity<ApiResponder<Empty>> response = adminController.createLeaderboard(null, body);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertEquals(
                "Leaderboard name must be between 1 and 512 characters.",
                response.getBody().getMessage());
        verify(leaderboardRepository, never()).getRecentLeaderboardMetadata();
        verify(leaderboardRepository, never()).addNewLeaderboard(any(Leaderboard.class));
    }

    @Test
    void testCreateLeaderboardWhitespaceOnlyName() {
        NewLeaderboardBody body = NewLeaderboardBody.builder().name("   ").build();

        ResponseEntity<ApiResponder<Empty>> response = adminController.createLeaderboard(null, body);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertEquals(
                "Leaderboard name must be between 1 and 512 characters.",
                response.getBody().getMessage());
        verify(leaderboardRepository, never()).getRecentLeaderboardMetadata();
        verify(leaderboardRepository, never()).addNewLeaderboard(any(Leaderboard.class));
    }

    @Test
    void testCreateLeaderboardNameTooLong() {
        String longName = "a".repeat(513);
        NewLeaderboardBody body = NewLeaderboardBody.builder().name(longName).build();

        ResponseEntity<ApiResponder<Empty>> response = adminController.createLeaderboard(null, body);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertEquals(
                "Leaderboard name must be between 1 and 512 characters.",
                response.getBody().getMessage());
        verify(leaderboardRepository, never()).getRecentLeaderboardMetadata();
        verify(leaderboardRepository, never()).addNewLeaderboard(any(Leaderboard.class));
    }

    @Test
    void testCreateLeaderboardMaxValidName() {
        String maxName = "a".repeat(512);
        NewLeaderboardBody body = NewLeaderboardBody.builder().name(maxName).build();

        when(leaderboardRepository.getRecentLeaderboardMetadata()).thenReturn(Optional.empty());

        ResponseEntity<ApiResponder<Empty>> response = adminController.createLeaderboard(null, body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertEquals("Leaderboard was created successfully.", response.getBody().getMessage());
        verify(leaderboardRepository).getRecentLeaderboardMetadata();
        verify(leaderboardRepository).addNewLeaderboard(any(Leaderboard.class));
        verify(leaderboardRepository).addAllUsersToLeaderboard(any());
    }

    @Test
    void testCreateLeaderboardNameWithLeadingAndTrailingSpaces() {
        NewLeaderboardBody body =
                NewLeaderboardBody.builder().name("  Challenge 2024  ").build();

        when(leaderboardRepository.getRecentLeaderboardMetadata()).thenReturn(Optional.empty());

        ResponseEntity<ApiResponder<Empty>> response = adminController.createLeaderboard(null, body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertEquals("Leaderboard was created successfully.", response.getBody().getMessage());
        verify(leaderboardRepository).getRecentLeaderboardMetadata();
        verify(leaderboardRepository)
                .addNewLeaderboard(argThat(leaderboard -> "Challenge 2024".equals(leaderboard.getName())));
        verify(leaderboardRepository).addAllUsersToLeaderboard(any());
    }

    @Test
    void testCreateLeaderboardWithShouldExpireBy() {
        OffsetDateTime futureDate = OffsetDateTime.now().plusDays(30);

        NewLeaderboardBody body = NewLeaderboardBody.builder()
                .name("Challenge 2024")
                .shouldExpireBy(futureDate)
                .build();

        when(leaderboardRepository.getRecentLeaderboardMetadata()).thenReturn(Optional.empty());

        ResponseEntity<ApiResponder<Empty>> response = adminController.createLeaderboard(null, body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertEquals("Leaderboard was created successfully.", response.getBody().getMessage());
        verify(leaderboardRepository)
                .addNewLeaderboard(
                        argThat(leaderboard -> leaderboard.getShouldExpireBy().isPresent()));
        verify(leaderboardRepository).addAllUsersToLeaderboard(any());
    }

    @Test
    void testCreateLeaderboardWithPastShouldExpireBy() {
        OffsetDateTime pastDate = OffsetDateTime.now().minusDays(30);

        NewLeaderboardBody body = NewLeaderboardBody.builder()
                .name("Challenge 2024")
                .shouldExpireBy(pastDate)
                .build();

        ResponseEntity<ApiResponder<Empty>> response = adminController.createLeaderboard(null, body);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertEquals(
                "The expiration date must be in the future.", response.getBody().getMessage());
        verify(leaderboardRepository, never()).addNewLeaderboard(any(Leaderboard.class));
    }

    @Test
    void testCreateLeaderboardWithCurrentTimeShouldExpireBy() {
        OffsetDateTime current = OffsetDateTime.now();

        NewLeaderboardBody body = NewLeaderboardBody.builder()
                .name("Challenge 2024")
                .shouldExpireBy(current)
                .build();

        ResponseEntity<ApiResponder<Empty>> response = adminController.createLeaderboard(null, body);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertEquals(
                "The expiration date must be in the future.", response.getBody().getMessage());
        verify(leaderboardRepository, never()).addNewLeaderboard(any(Leaderboard.class));
    }

    @Test
    void testCreateLeaderboardWithSyntaxHighlightingLanguage() {
        NewLeaderboardBody body = NewLeaderboardBody.builder()
                .name("Challenge 2024")
                .syntaxHighlightingLanguage("python")
                .build();

        when(leaderboardRepository.getRecentLeaderboardMetadata()).thenReturn(Optional.empty());

        ResponseEntity<ApiResponder<Empty>> response = adminController.createLeaderboard(null, body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertEquals("Leaderboard was created successfully.", response.getBody().getMessage());
        verify(leaderboardRepository).addNewLeaderboard(argThat(leaderboard -> leaderboard
                .getSyntaxHighlightingLanguage()
                .filter("python"::equals)
                .isPresent()));
        verify(leaderboardRepository).addAllUsersToLeaderboard(any());
    }

    @Test
    void testCreateLeaderboardWithAllOptionalFields() {
        OffsetDateTime futureDate = OffsetDateTime.now().plusDays(30);

        NewLeaderboardBody body = NewLeaderboardBody.builder()
                .name("Challenge 2024")
                .shouldExpireBy(futureDate)
                .syntaxHighlightingLanguage("python")
                .build();

        when(leaderboardRepository.getRecentLeaderboardMetadata()).thenReturn(Optional.empty());

        ResponseEntity<ApiResponder<Empty>> response = adminController.createLeaderboard(null, body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertEquals("Leaderboard was created successfully.", response.getBody().getMessage());
        verify(leaderboardRepository)
                .addNewLeaderboard(
                        argThat(leaderboard -> leaderboard.getShouldExpireBy().isPresent()
                                && leaderboard
                                        .getSyntaxHighlightingLanguage()
                                        .filter("python"::equals)
                                        .isPresent()));
        verify(leaderboardRepository).addAllUsersToLeaderboard(any());
    }

    @Test
    void testCreateLeaderboardWithNullOptionalFields() {
        NewLeaderboardBody body = NewLeaderboardBody.builder()
                .name("Challenge 2024")
                .shouldExpireBy(null)
                .syntaxHighlightingLanguage(null)
                .build();

        when(leaderboardRepository.getRecentLeaderboardMetadata()).thenReturn(Optional.empty());

        ResponseEntity<ApiResponder<Empty>> response = adminController.createLeaderboard(null, body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertEquals("Leaderboard was created successfully.", response.getBody().getMessage());
        verify(leaderboardRepository)
                .addNewLeaderboard(
                        argThat(leaderboard -> leaderboard.getShouldExpireBy().isEmpty()
                                && leaderboard.getSyntaxHighlightingLanguage().isEmpty()));
        verify(leaderboardRepository).addAllUsersToLeaderboard(any());
    }

    @Test
    void testDeleteAnnouncementNull() {
        DeleteAnnouncementBody body = DeleteAnnouncementBody.builder()
                .id("4f6bbb9a-0baa-11f1-9607-77d42f1cf060")
                .build();
        when(announcementRepository.getAnnouncementById(anyString())).thenReturn(Optional.empty());

        ResponseStatusException exception =
                assertThrows(ResponseStatusException.class, () -> adminController.deleteAnnouncement(body, null));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Announcement does not exist", exception.getReason());
    }

    @Test
    void testDeleteAnnouncementFailure() {
        DeleteAnnouncementBody body = DeleteAnnouncementBody.builder()
                .id("4f6bbb9a-0baa-11f1-9607-77d42f1cf060")
                .build();
        Announcement mockAnnouncement = mock(Announcement.class);
        when(announcementRepository.getAnnouncementById(anyString())).thenReturn(Optional.of(mockAnnouncement));
        when(announcementRepository.updateAnnouncement(mockAnnouncement)).thenReturn(false);

        ResponseEntity<ApiResponder<Empty>> response = adminController.deleteAnnouncement(body, null);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Hmm, something went wrong.", response.getBody().getMessage());
    }

    @Test
    void testDeleteAnnouncementSuccess() {
        DeleteAnnouncementBody body = DeleteAnnouncementBody.builder()
                .id("4f6bbb9a-0baa-11f1-9607-77d42f1cf060")
                .build();
        Announcement mockAnnouncement = mock(Announcement.class);
        when(announcementRepository.getAnnouncementById(anyString())).thenReturn(Optional.of(mockAnnouncement));
        when(announcementRepository.updateAnnouncement(mockAnnouncement)).thenReturn(true);

        ResponseEntity<ApiResponder<Empty>> response = adminController.deleteAnnouncement(body, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertEquals("Announcement successfully disabled!", response.getBody().getMessage());
    }

    @Test
    void testGetIncompleteQuestionNoQuestions() {
        when(questionRepository.getAllIncompleteQuestionsWithUser()).thenReturn(new ArrayList<QuestionWithUser>());

        ResponseStatusException exception =
                assertThrows(ResponseStatusException.class, () -> adminController.getIncompleteQuestions(null));
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("No Incomplete Questions", exception.getReason());
    }

    @Test
    void testGetIncompleteQuestionSuccess() {
        QuestionWithUser qwu = QuestionWithUser.builder().build();

        when(questionRepository.getAllIncompleteQuestionsWithUser()).thenReturn(new ArrayList<>(List.of(qwu)));

        ResponseEntity<ApiResponder<List<QuestionWithUserDto>>> response = adminController.getIncompleteQuestions(null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertEquals(1, response.getBody().getPayload().size());
        assertEquals("Retrieved 1 incomplete questions.", response.getBody().getMessage());
    }

    @Test
    void testSendDiscordMessageInvalidClub() {
        DiscordClub club = mock(DiscordClub.class);
        when(discordClubManager.sendTestEmbedMessageToClub(club)).thenReturn(false);

        String clubId = "bbf4734a-06b6-11f1-869c-07599d6a11f7";
        ResponseEntity<ApiResponder<Empty>> response = adminController.sendDiscordMessage(clubId, null);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Club not found.", response.getBody().getMessage());
    }

    @Test
    void testSendDiscordMessageFailure() {
        String clubId = "bbf4734a-06b6-11f1-869c-07599d6a11f7";
        DiscordClub club = mock(DiscordClub.class);

        when(discordClubRepository.getDiscordClubById(clubId)).thenReturn(Optional.of(club));

        when(discordClubManager.sendTestEmbedMessageToClub(club)).thenReturn(false);

        ResponseEntity<ApiResponder<Empty>> response = adminController.sendDiscordMessage(clubId, null);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Hmm, something went wrong.", response.getBody().getMessage());
    }

    @Test
    void testSendDiscordMessageSuccess() {
        String clubId = "bbf4734a-06b6-11f1-869c-07599d6a11f7";
        DiscordClub club = mock(DiscordClub.class);

        when(discordClubRepository.getDiscordClubById(clubId)).thenReturn(Optional.of(club));

        when(discordClubManager.sendTestEmbedMessageToClub(club)).thenReturn(true);

        ResponseEntity<ApiResponder<Empty>> response = adminController.sendDiscordMessage(clubId, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Message successfully sent!", response.getBody().getMessage());
    }

    @Test
    void testDeleteDiscordMessageFailure() {
        when(discordClubManager.deleteMessageById(anyLong(), anyLong())).thenReturn(false);
        DeleteMessageBody body =
                DeleteMessageBody.builder().channelId(999L).messageId(123L).build();

        ResponseEntity<ApiResponder<Empty>> response = adminController.deleteDiscordMessage(body, null);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Hmm, something went wrong.", response.getBody().getMessage());
    }

    @Test
    void testDeleteDiscordMessageSuccess() {
        when(discordClubManager.deleteMessageById(anyLong(), anyLong())).thenReturn(true);
        DeleteMessageBody body =
                DeleteMessageBody.builder().channelId(999L).messageId(123L).build();

        ResponseEntity<ApiResponder<Empty>> response = adminController.deleteDiscordMessage(body, null);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertEquals("Discord Message successfully deleted", response.getBody().getMessage());
    }

    @Test
    void testEditCurrentLeaderboardSuccess() {
        OffsetDateTime date = StandardizedOffsetDateTime.normalize(OffsetDateTime.parse("4096-01-01T00:00:00Z"));

        EditLeaderboardBody body = EditLeaderboardBody.builder()
                .name("std::string name = new lb")
                .syntaxHighlightingLanguage("cpp")
                .shouldExpireBy(date)
                .build();

        Leaderboard currentLeaderboard =
                Leaderboard.builder().name("current leaderboard").id("123").build();
        when(leaderboardRepository.getRecentLeaderboardMetadata()).thenReturn(Optional.of(currentLeaderboard));

        ResponseEntity<ApiResponder<Empty>> response = adminController.editCurrentLeaderboard(body, null);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertEquals("Leaderboard updated successfully", response.getBody().getMessage());
    }

    @Test
    void testEditCurrentLeaderboardFailureNoCurrentLeaderboard() {
        OffsetDateTime date = StandardizedOffsetDateTime.normalize(OffsetDateTime.parse("4096-01-01T00:00:00Z"));

        EditLeaderboardBody body = EditLeaderboardBody.builder()
                .name("std::string name = new lb")
                .syntaxHighlightingLanguage("cpp")
                .shouldExpireBy(date)
                .build();

        when(leaderboardRepository.getRecentLeaderboardMetadata()).thenReturn(Optional.empty());

        try {
            adminController.editCurrentLeaderboard(body, null);
            fail("Exception expected");
        } catch (ResponseStatusException e) {
            assertNotNull(e);
            assertTrue(e.getMessage().contains("No current leaderboard found"));
        }
    }

    @Test
    void testEditCurrentLeaderboardFailureNameTooShort() {
        OffsetDateTime date = StandardizedOffsetDateTime.normalize(OffsetDateTime.parse("4096-01-01T00:00:00Z"));
        EditLeaderboardBody body = EditLeaderboardBody.builder()
                .name("1")
                .syntaxHighlightingLanguage("cpp")
                .shouldExpireBy(date)
                .build();

        try {
            adminController.editCurrentLeaderboard(body, null);
            fail("Exception expected");
        } catch (ValidationException e) {
            assertNotNull(e);
            assertTrue(e.getMessage().contains("Leaderboard name cannot have only 1 character"));
            assertInstanceOf(ValidationException.class, e);
        }
    }

    @Test
    void testEditCurrentLeaderboardFailureNameTooLong() {
        OffsetDateTime date = StandardizedOffsetDateTime.normalize(OffsetDateTime.parse("4096-01-01T00:00:00Z"));
        String longName = "a".repeat(513);
        EditLeaderboardBody body = EditLeaderboardBody.builder()
                .name(longName)
                .syntaxHighlightingLanguage("cpp")
                .shouldExpireBy(date)
                .build();

        try {
            adminController.editCurrentLeaderboard(body, null);
            fail("Exception expected");
        } catch (ValidationException e) {
            assertNotNull(e);
            assertTrue(e.getMessage().contains("Leaderboard name cannot have more than 512 characters"));
            assertInstanceOf(ValidationException.class, e);
        }
    }

    @Test
    void testEditCurrentLeaderboardFailurePastDate() {
        OffsetDateTime date = StandardizedOffsetDateTime.normalize(OffsetDateTime.parse("2000-01-01T00:00:00Z"));
        EditLeaderboardBody body = EditLeaderboardBody.builder()
                .name("new lb name")
                .syntaxHighlightingLanguage("cpp")
                .shouldExpireBy(date)
                .build();

        try {
            adminController.editCurrentLeaderboard(body, null);
            fail("Exception expected");
        } catch (ValidationException e) {
            assertNotNull(e);
            assertTrue(e.getMessage().contains("The expiration date must be in the future"));
            assertInstanceOf(ValidationException.class, e);
        }
    }
}
