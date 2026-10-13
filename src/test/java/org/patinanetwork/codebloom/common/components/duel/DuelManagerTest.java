package org.patinanetwork.codebloom.common.components.duel;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.github.javafaker.Faker;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.logging.log4j.util.Strings;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.patinanetwork.codebloom.common.db.models.lobby.Lobby;
import org.patinanetwork.codebloom.common.db.models.lobby.LobbyQuestion;
import org.patinanetwork.codebloom.common.db.models.lobby.LobbyStatus;
import org.patinanetwork.codebloom.common.db.models.lobby.player.LobbyPlayer;
import org.patinanetwork.codebloom.common.db.models.question.bank.QuestionBank;
import org.patinanetwork.codebloom.common.db.models.user.User;
import org.patinanetwork.codebloom.common.db.repos.lobby.LobbyQuestionRepository;
import org.patinanetwork.codebloom.common.db.repos.lobby.LobbyRepository;
import org.patinanetwork.codebloom.common.db.repos.lobby.player.LobbyPlayerRepository;
import org.patinanetwork.codebloom.common.db.repos.lobby.player.question.LobbyPlayerQuestionRepository;
import org.patinanetwork.codebloom.common.db.repos.question.QuestionRepository;
import org.patinanetwork.codebloom.common.db.repos.question.questionbank.QuestionBankRepository;
import org.patinanetwork.codebloom.common.db.repos.user.UserRepository;
import org.patinanetwork.codebloom.common.dto.lobby.DuelData;
import org.patinanetwork.codebloom.common.dto.lobby.LobbyDto;
import org.patinanetwork.codebloom.common.dto.user.UserDto;
import org.patinanetwork.codebloom.common.leetcode.models.LeetcodeSubmission;
import org.patinanetwork.codebloom.common.leetcode.throttled.ThrottledLeetcodeClient;
import org.patinanetwork.codebloom.common.submissions.SubmissionsHandler;
import org.patinanetwork.codebloom.common.submissions.object.AcceptedSubmission;
import org.patinanetwork.codebloom.common.time.StandardizedOffsetDateTime;
import org.patinanetwork.codebloom.common.utils.duel.PartyCodeGenerator;
import org.springframework.http.HttpStatus;

public class DuelManagerTest {

    private final DuelManager duelManager;
    private final Faker faker;

    private LobbyRepository lobbyRepository = mock(LobbyRepository.class);
    private LobbyQuestionRepository lobbyQuestionRepository = mock(LobbyQuestionRepository.class);
    private LobbyPlayerRepository lobbyPlayerRepository = mock(LobbyPlayerRepository.class);
    private LobbyPlayerQuestionRepository lobbyPlayerQuestionRepository = mock(LobbyPlayerQuestionRepository.class);
    private QuestionRepository questionRepository = mock(QuestionRepository.class);
    private QuestionBankRepository questionBankRepository = mock(QuestionBankRepository.class);
    private UserRepository userRepository = mock(UserRepository.class);
    private ThrottledLeetcodeClient throttledLeetcodeClient = mock(ThrottledLeetcodeClient.class);
    private SubmissionsHandler submissionsHandler = mock(SubmissionsHandler.class);

    public DuelManagerTest() {
        this.duelManager = new DuelManager(
                lobbyRepository,
                lobbyQuestionRepository,
                lobbyPlayerRepository,
                lobbyPlayerQuestionRepository,
                questionRepository,
                questionBankRepository,
                userRepository,
                throttledLeetcodeClient,
                submissionsHandler);
        this.faker = Faker.instance();
    }

    private User createRandomUser() {
        return User.builder()
                .id(UUID.randomUUID().toString())
                .discordId(String.valueOf(faker.number().randomNumber(18, true)))
                .discordName(faker.name().username())
                .leetcodeUsername(Optional.ofNullable(faker.name().username()))
                .admin(false)
                .verifyKey(faker.crypto().md5())
                .build();
    }

    private Lobby.LobbyBuilder randomPartialLobby() {
        return Lobby.builder()
                .id(java.util.UUID.randomUUID().toString())
                .joinCode(faker.code().isbn10(false).toUpperCase())
                .createdAt(OffsetDateTime.now());
    }

    @Test
    void testGenerateDuelDataSuccess() {
        String lobbyId = java.util.UUID.randomUUID().toString();
        Lobby mockLobby = randomPartialLobby()
                .id(lobbyId)
                .status(LobbyStatus.ACTIVE)
                .expiresAt(Optional.of(OffsetDateTime.now().plusHours(1)))
                .playerCount(2)
                .winnerId(Optional.empty())
                .build();

        when(lobbyRepository.findLobbyById(lobbyId)).thenReturn(Optional.of(mockLobby));

        try {
            DuelData result = duelManager.generateDuelData(lobbyId);

            assertNotNull(result);
            assertNotNull(result.getLobby());
            assertEquals(lobbyId, result.getLobby().getId());
            assertEquals(mockLobby.getJoinCode(), result.getLobby().getJoinCode());
            assertEquals(LobbyStatus.ACTIVE, result.getLobby().getStatus());
            assertEquals(2, result.getLobby().getPlayerCount());
            assertNull(result.getLobby().getWinnerId());
            verify(lobbyRepository, times(1)).findLobbyById(lobbyId);
        } catch (DuelException e) {
            fail(e);
        }
    }

    @Test
    void testGenerateDuelDataWithWinner() {
        String lobbyId = java.util.UUID.randomUUID().toString();
        String winnerId = java.util.UUID.randomUUID().toString();
        Lobby mockLobby = randomPartialLobby()
                .id(lobbyId)
                .status(LobbyStatus.COMPLETED)
                .expiresAt(Optional.of(OffsetDateTime.now()))
                .playerCount(2)
                .winnerId(Optional.of(winnerId))
                .build();

        when(lobbyRepository.findLobbyById(lobbyId)).thenReturn(Optional.of(mockLobby));

        try {
            DuelData result = duelManager.generateDuelData(lobbyId);

            assertNotNull(result);
            assertEquals(winnerId, result.getLobby().getWinnerId());
            assertEquals(LobbyStatus.COMPLETED, result.getLobby().getStatus());
        } catch (DuelException e) {
            fail(e);
        }
    }

    void testGenerateDuelDataCallsRepositoryOnce() {
        String lobbyId = java.util.UUID.randomUUID().toString();
        Lobby mockLobby = randomPartialLobby()
                .id(lobbyId)
                .status(LobbyStatus.ACTIVE)
                .expiresAt(Optional.of(OffsetDateTime.now()))
                .playerCount(1)
                .build();

        when(lobbyRepository.findLobbyById(lobbyId)).thenReturn(Optional.of(mockLobby));

        try {
            duelManager.generateDuelData(lobbyId);
            verify(lobbyRepository, times(1)).findLobbyById(lobbyId);
        } catch (DuelException e) {
            fail(e);
        }
    }

    @Test
    void testGenerateDuelDataMapsAllLobbyFields() {
        String lobbyId = java.util.UUID.randomUUID().toString();
        OffsetDateTime createdAt = OffsetDateTime.now().minusHours(2);
        OffsetDateTime expiresAt = OffsetDateTime.now().plusHours(1);
        String winnerId = java.util.UUID.randomUUID().toString();

        Lobby mockLobby = randomPartialLobby()
                .id(lobbyId)
                .status(LobbyStatus.CLOSED)
                .createdAt(createdAt)
                .expiresAt(Optional.of(expiresAt))
                .playerCount(4)
                .winnerId(Optional.of(winnerId))
                .build();

        when(lobbyRepository.findLobbyById(lobbyId)).thenReturn(Optional.of(mockLobby));

        try {
            DuelData result = duelManager.generateDuelData(lobbyId);
            LobbyDto resultDto = result.getLobby();

            assertEquals(lobbyId, resultDto.getId());
            assertEquals(mockLobby.getJoinCode(), resultDto.getJoinCode());
            assertEquals(LobbyStatus.CLOSED, resultDto.getStatus());
            assertEquals(createdAt, resultDto.getCreatedAt());
            assertEquals(expiresAt, resultDto.getExpiresAt());
            assertEquals(4, resultDto.getPlayerCount());
            assertEquals(winnerId, resultDto.getWinnerId());
        } catch (DuelException e) {
            fail(e);
        }
    }

    @Test
    void testGenerateDuelDataReturnsNewInstanceEachTime() {
        String lobbyId = java.util.UUID.randomUUID().toString();
        Lobby mockLobby = randomPartialLobby()
                .id(lobbyId)
                .status(LobbyStatus.ACTIVE)
                .expiresAt(Optional.of(OffsetDateTime.now()))
                .playerCount(2)
                .build();

        when(lobbyRepository.findLobbyById(lobbyId)).thenReturn(Optional.of(mockLobby));

        try {
            DuelData result1 = duelManager.generateDuelData(lobbyId);
            DuelData result2 = duelManager.generateDuelData(lobbyId);

            assertNotNull(result1);
            assertNotNull(result2);
            assertEquals(result1.getLobby().getId(), result2.getLobby().getId());
            verify(lobbyRepository, times(2)).findLobbyById(lobbyId);
        } catch (DuelException e) {
            fail(e);
        }
    }

    @Test
    void testBuildPlayersInLobbyConvertsUsersToUserDtos() {
        String lobbyId = java.util.UUID.randomUUID().toString();
        String playerId1 = java.util.UUID.randomUUID().toString();
        String playerId2 = java.util.UUID.randomUUID().toString();

        LobbyPlayer player1 = LobbyPlayer.builder()
                .id(java.util.UUID.randomUUID().toString())
                .lobbyId(lobbyId)
                .playerId(playerId1)
                .points(0)
                .build();

        LobbyPlayer player2 = LobbyPlayer.builder()
                .id(java.util.UUID.randomUUID().toString())
                .lobbyId(lobbyId)
                .playerId(playerId2)
                .points(0)
                .build();

        User user1 = User.builder()
                .id(playerId1)
                .discordId("123456789")
                .discordName("TestUser1")
                .admin(false)
                .verifyKey("key1")
                .build();

        User user2 = User.builder()
                .id(playerId2)
                .discordId("987654321")
                .discordName("TestUser2")
                .admin(true)
                .verifyKey("key2")
                .build();

        when(lobbyPlayerRepository.findPlayersByLobbyId(lobbyId)).thenReturn(List.of(player1, player2));
        when(userRepository.getUserById(playerId1)).thenReturn(Optional.of(user1));
        when(userRepository.getUserById(playerId2)).thenReturn(Optional.of(user2));

        when(lobbyRepository.findLobbyById(lobbyId)).thenReturn(Optional.empty());
        when(lobbyQuestionRepository.findLobbyQuestionsByLobbyId(lobbyId)).thenReturn(List.of());

        try {
            DuelData result = duelManager.generateDuelData(lobbyId);

            assertNotNull(result.getPlayers());
            assertEquals(2, result.getPlayers().size());

            List<UserDto> players = result.getPlayers();
            UserDto resultUser1 = players.stream()
                    .filter(u -> u.getId().equals(playerId1))
                    .findFirst()
                    .orElse(null);
            UserDto resultUser2 = players.stream()
                    .filter(u -> u.getId().equals(playerId2))
                    .findFirst()
                    .orElse(null);

            assertNotNull(resultUser1);
            assertEquals(playerId1, resultUser1.getId());
            assertEquals("123456789", resultUser1.getDiscordId());
            assertEquals("TestUser1", resultUser1.getDiscordName());
            assertFalse(resultUser1.isAdmin());

            assertNotNull(resultUser2);
            assertEquals(playerId2, resultUser2.getId());
            assertEquals("987654321", resultUser2.getDiscordId());
            assertEquals("TestUser2", resultUser2.getDiscordName());
            assertTrue(resultUser2.isAdmin());

            verify(lobbyPlayerRepository, times(2)).findPlayersByLobbyId(lobbyId);
            verify(userRepository, times(1)).getUserById(playerId1);
            verify(userRepository, times(1)).getUserById(playerId2);
        } catch (DuelException e) {
            fail(e);
        }
    }

    @Test
    void testGenerateDuelDataThrowsNonDuelException() {
        String lobbyId = java.util.UUID.randomUUID().toString();

        doThrow(new RuntimeException("Simulated db exception"))
                .when(lobbyRepository)
                .findLobbyById(any());

        try {
            duelManager.generateDuelData(lobbyId);
            fail("Expected a duel exception");
        } catch (DuelException e) {
            e.printStackTrace();

            StringWriter writer = new StringWriter();
            PrintWriter printWriter = new PrintWriter(writer);
            e.printStackTrace(printWriter);
            printWriter.flush();

            String stackTrace = writer.toString();
            assertTrue(e.getHttpStatus().isEmpty());
            assertEquals("Duel exception occurred.", e.getMessage());
            assertTrue(stackTrace.contains("Simulated db exception"));
        }

        verify(lobbyRepository, times(1)).findLobbyById(any());
    }

    @Test
    void testEndDuelLobbyNotFound() {
        String lobbyId = "1234567890";

        when(lobbyRepository.findLobbyById(eq(lobbyId))).thenReturn(Optional.empty());

        try {
            duelManager.endDuel(lobbyId, false);
            fail("Expected a duel exception");
        } catch (DuelException e) {
            e.printStackTrace();
            assertEquals(e.getHttpStatus().get(), HttpStatus.NOT_FOUND);
            assertEquals(e.getMessage(), "Duel cannot be found.");
        }

        verify(lobbyRepository, times(1)).findLobbyById(lobbyId);
        verify(lobbyRepository, times(0)).updateLobby(any());
        verify(lobbyPlayerRepository, times(0)).findPlayersByLobbyId(any());
    }

    @Test
    void testEndDuelThrowsANonDuelException() {
        doThrow(new RuntimeException("Simulated db exception"))
                .when(lobbyRepository)
                .findLobbyById(any());

        try {
            duelManager.endDuel(Strings.EMPTY, false);
            fail("Expected a duel exception");
        } catch (DuelException e) {
            e.printStackTrace();

            StringWriter writer = new StringWriter();
            PrintWriter printWriter = new PrintWriter(writer);
            e.printStackTrace(printWriter);
            printWriter.flush();

            String stackTrace = writer.toString();
            assertTrue(e.getHttpStatus().isEmpty());
            assertEquals("Duel exception occurred.", e.getMessage());
            assertTrue(stackTrace.contains("Simulated db exception"));
        }

        verify(lobbyRepository, times(1)).findLobbyById(any());
        verify(lobbyRepository, times(0)).updateLobby(any());
        verify(lobbyPlayerRepository, times(0)).findPlayersByLobbyId(any());
    }

    @Test
    void testEndDuelLobbyIsNotActive() {
        Lobby lobby = Lobby.builder()
                .id("12345467890")
                .createdAt(StandardizedOffsetDateTime.now())
                .expiresAt(Optional.of(StandardizedOffsetDateTime.now().plus(30, ChronoUnit.MINUTES)))
                .joinCode("ABC123")
                .playerCount(2)
                .status(LobbyStatus.AVAILABLE)
                .winnerId(Optional.empty())
                .build();

        when(lobbyRepository.findLobbyById(eq(lobby.getId()))).thenReturn(Optional.of(lobby));

        try {
            duelManager.endDuel(lobby.getId(), false);
            fail("Expected a duel exception");
        } catch (DuelException e) {
            e.printStackTrace();
            assertEquals(e.getHttpStatus().get(), HttpStatus.CONFLICT);
            assertEquals(e.getMessage(), "This duel is not currently active.");
        }

        verify(lobbyRepository, times(1)).findLobbyById(lobby.getId());
        verify(lobbyRepository, times(0)).updateLobby(any());
        verify(lobbyPlayerRepository, times(0)).findPlayersByLobbyId(any());
    }

    @Test
    void testEndDuelLobbyHasNotExpiredYet() {
        Lobby lobby = Lobby.builder()
                .id("12345467890")
                .createdAt(StandardizedOffsetDateTime.now())
                .expiresAt(Optional.of(StandardizedOffsetDateTime.now().plus(30, ChronoUnit.MINUTES)))
                .joinCode("ABC123")
                .playerCount(2)
                .status(LobbyStatus.ACTIVE)
                .winnerId(Optional.empty())
                .build();

        when(lobbyRepository.findLobbyById(eq(lobby.getId()))).thenReturn(Optional.of(lobby));

        try {
            duelManager.endDuel(lobby.getId(), false);
            fail("Expected a duel exception");
        } catch (DuelException e) {
            e.printStackTrace();
            assertEquals(e.getHttpStatus().get(), HttpStatus.CONFLICT);
            assertEquals(e.getMessage(), "This duel is not ready for expiration yet.");
        }

        verify(lobbyRepository, times(1)).findLobbyById(lobby.getId());
        verify(lobbyRepository, times(0)).updateLobby(any());
        verify(lobbyPlayerRepository, times(0)).findPlayersByLobbyId(any());
    }

    @Test
    void testEndDuelLobbyHasNotExpiredYetButAsDuelCleanupAndNoPlayers() {
        Lobby lobby = Lobby.builder()
                .id("12345467890")
                .createdAt(StandardizedOffsetDateTime.now())
                .expiresAt(Optional.of(StandardizedOffsetDateTime.now().plus(30, ChronoUnit.MINUTES)))
                .joinCode("ABC123")
                .playerCount(2)
                .status(LobbyStatus.ACTIVE)
                .winnerId(Optional.empty())
                .build();

        when(lobbyRepository.findLobbyById(eq(lobby.getId()))).thenReturn(Optional.of(lobby));
        when(lobbyPlayerRepository.findPlayersByLobbyId(eq(lobby.getId()))).thenReturn(List.of());

        try {
            duelManager.endDuel(lobby.getId(), true);
            fail("Expected a duel exception");
        } catch (DuelException e) {
            e.printStackTrace();
            assertEquals(e.getHttpStatus().get(), HttpStatus.INTERNAL_SERVER_ERROR);
            assertEquals(
                    e.getMessage(),
                    "No winner can be found because there are no players in the duel. This should not be happening.");
        }

        verify(lobbyRepository, times(1)).findLobbyById(lobby.getId());
        verify(lobbyPlayerRepository, times(1)).findPlayersByLobbyId(lobby.getId());
        verify(lobbyRepository, times(0)).updateLobby(any());
    }

    @Test
    void testEndDuelNoPlayersInDuel() {
        Lobby lobby = Lobby.builder()
                .id("12345467890")
                .createdAt(StandardizedOffsetDateTime.now())
                .expiresAt(Optional.of(StandardizedOffsetDateTime.now().minus(30, ChronoUnit.MINUTES)))
                .joinCode("ABC123")
                .playerCount(2)
                .status(LobbyStatus.ACTIVE)
                .winnerId(Optional.empty())
                .build();

        when(lobbyRepository.findLobbyById(eq(lobby.getId()))).thenReturn(Optional.of(lobby));
        when(lobbyPlayerRepository.findPlayersByLobbyId(eq(lobby.getId()))).thenReturn(List.of());

        try {
            duelManager.endDuel(lobby.getId(), false);
            fail("Expected a duel exception");
        } catch (DuelException e) {
            e.printStackTrace();
            assertEquals(e.getHttpStatus().get(), HttpStatus.INTERNAL_SERVER_ERROR);
            assertEquals(
                    e.getMessage(),
                    "No winner can be found because there are no players in the duel. This should not be happening.");
        }

        verify(lobbyRepository, times(1)).findLobbyById(lobby.getId());
        verify(lobbyRepository, times(0)).updateLobby(any());
        verify(lobbyPlayerRepository, times(1)).findPlayersByLobbyId(any());
    }

    @Test
    void testEndDuelPlayersTied() {
        Lobby lobby = Lobby.builder()
                .id("12345467890")
                .createdAt(StandardizedOffsetDateTime.now())
                .expiresAt(Optional.of(StandardizedOffsetDateTime.now().minus(30, ChronoUnit.MINUTES)))
                .joinCode("ABC123")
                .playerCount(2)
                .status(LobbyStatus.ACTIVE)
                .winnerId(Optional.empty())
                .build();

        List<LobbyPlayer> players = List.of(
                LobbyPlayer.builder()
                        .id(UUID.randomUUID().toString())
                        .lobbyId(lobby.getId())
                        .playerId(UUID.randomUUID().toString())
                        .points(100)
                        .build(),
                LobbyPlayer.builder()
                        .id(UUID.randomUUID().toString())
                        .lobbyId(lobby.getId())
                        .playerId(UUID.randomUUID().toString())
                        .points(100)
                        .build());

        when(lobbyRepository.findLobbyById(eq(lobby.getId()))).thenReturn(Optional.of(lobby));
        when(lobbyPlayerRepository.findPlayersByLobbyId(eq(lobby.getId()))).thenReturn(players);

        try {
            duelManager.endDuel(lobby.getId(), false);
        } catch (DuelException e) {
            fail(e);
        }

        ArgumentCaptor<Lobby> lobbyCaptor = ArgumentCaptor.forClass(Lobby.class);
        verify(lobbyRepository, times(1)).findLobbyById(lobby.getId());
        verify(lobbyRepository, times(1)).updateLobby(lobbyCaptor.capture());
        verify(lobbyPlayerRepository, times(1)).findPlayersByLobbyId(any());

        var updatedLobby = lobbyCaptor.getValue();
        assertEquals(lobby.getId(), updatedLobby.getId());
        assertEquals(LobbyStatus.COMPLETED, updatedLobby.getStatus());
        assertTrue(updatedLobby.isTie());
        assertTrue(updatedLobby.getWinnerId().isEmpty());
    }

    @Test
    void testEndDuelSinglePlayerWins() {
        Lobby lobby = Lobby.builder()
                .id("12345467890")
                .createdAt(StandardizedOffsetDateTime.now())
                .expiresAt(Optional.of(StandardizedOffsetDateTime.now().minus(30, ChronoUnit.MINUTES)))
                .joinCode("ABC123")
                .playerCount(1)
                .status(LobbyStatus.ACTIVE)
                .winnerId(Optional.empty())
                .build();

        List<LobbyPlayer> players = List.of(LobbyPlayer.builder()
                .id(UUID.randomUUID().toString())
                .lobbyId(lobby.getId())
                .playerId(UUID.randomUUID().toString())
                .points(100)
                .build());

        when(lobbyRepository.findLobbyById(eq(lobby.getId()))).thenReturn(Optional.of(lobby));
        when(lobbyPlayerRepository.findPlayersByLobbyId(eq(lobby.getId()))).thenReturn(players);

        try {
            duelManager.endDuel(lobby.getId(), false);
        } catch (DuelException e) {
            fail(e);
        }

        ArgumentCaptor<Lobby> lobbyCaptor = ArgumentCaptor.forClass(Lobby.class);
        verify(lobbyRepository, times(1)).findLobbyById(lobby.getId());
        verify(lobbyRepository, times(1)).updateLobby(lobbyCaptor.capture());
        verify(lobbyPlayerRepository, times(1)).findPlayersByLobbyId(any());

        var updatedLobby = lobbyCaptor.getValue();
        assertEquals(lobby.getId(), updatedLobby.getId());
        assertEquals(LobbyStatus.COMPLETED, updatedLobby.getStatus());
        assertNotNull(updatedLobby.getWinnerId());
        assertTrue(updatedLobby.getWinnerId().isPresent());
        assertEquals(players.get(0).getPlayerId(), updatedLobby.getWinnerId().get());
    }

    @Test
    void testEndDuelPlayerOneWins() {
        Lobby lobby = Lobby.builder()
                .id("12345467890")
                .createdAt(StandardizedOffsetDateTime.now())
                .expiresAt(Optional.of(StandardizedOffsetDateTime.now().minus(30, ChronoUnit.MINUTES)))
                .joinCode("ABC123")
                .playerCount(2)
                .status(LobbyStatus.ACTIVE)
                .winnerId(Optional.empty())
                .build();

        List<LobbyPlayer> players = List.of(
                LobbyPlayer.builder()
                        .id(UUID.randomUUID().toString())
                        .lobbyId(lobby.getId())
                        .playerId(UUID.randomUUID().toString())
                        .points(150)
                        .build(),
                LobbyPlayer.builder()
                        .id(UUID.randomUUID().toString())
                        .lobbyId(lobby.getId())
                        .playerId(UUID.randomUUID().toString())
                        .points(50)
                        .build());

        when(lobbyRepository.findLobbyById(eq(lobby.getId()))).thenReturn(Optional.of(lobby));
        when(lobbyPlayerRepository.findPlayersByLobbyId(eq(lobby.getId()))).thenReturn(players);

        try {
            duelManager.endDuel(lobby.getId(), false);
        } catch (DuelException e) {
            fail(e);
        }

        ArgumentCaptor<Lobby> lobbyCaptor = ArgumentCaptor.forClass(Lobby.class);
        verify(lobbyRepository, times(1)).findLobbyById(lobby.getId());
        verify(lobbyRepository, times(1)).updateLobby(lobbyCaptor.capture());
        verify(lobbyPlayerRepository, times(1)).findPlayersByLobbyId(any());

        var updatedLobby = lobbyCaptor.getValue();
        assertEquals(lobby.getId(), updatedLobby.getId());
        assertEquals(LobbyStatus.COMPLETED, updatedLobby.getStatus());
        assertFalse(updatedLobby.isTie());
        assertNotNull(updatedLobby.getWinnerId());
        assertTrue(updatedLobby.getWinnerId().isPresent());
        assertEquals(players.get(0).getPlayerId(), updatedLobby.getWinnerId().get());
    }

    @Test
    void testEndDuelPlayerTwoWins() {
        Lobby lobby = Lobby.builder()
                .id("12345467890")
                .createdAt(StandardizedOffsetDateTime.now())
                .expiresAt(Optional.of(StandardizedOffsetDateTime.now().minus(30, ChronoUnit.MINUTES)))
                .joinCode("ABC123")
                .playerCount(2)
                .status(LobbyStatus.ACTIVE)
                .winnerId(Optional.empty())
                .build();

        List<LobbyPlayer> players = List.of(
                LobbyPlayer.builder()
                        .id(UUID.randomUUID().toString())
                        .lobbyId(lobby.getId())
                        .playerId(UUID.randomUUID().toString())
                        .points(50)
                        .build(),
                LobbyPlayer.builder()
                        .id(UUID.randomUUID().toString())
                        .lobbyId(lobby.getId())
                        .playerId(UUID.randomUUID().toString())
                        .points(150)
                        .build());

        when(lobbyRepository.findLobbyById(eq(lobby.getId()))).thenReturn(Optional.of(lobby));
        when(lobbyPlayerRepository.findPlayersByLobbyId(eq(lobby.getId()))).thenReturn(players);

        try {
            duelManager.endDuel(lobby.getId(), false);
        } catch (DuelException e) {
            fail(e);
        }

        ArgumentCaptor<Lobby> lobbyCaptor = ArgumentCaptor.forClass(Lobby.class);
        verify(lobbyRepository, times(1)).findLobbyById(lobby.getId());
        verify(lobbyRepository, times(1)).updateLobby(lobbyCaptor.capture());
        verify(lobbyPlayerRepository, times(1)).findPlayersByLobbyId(any());

        var updatedLobby = lobbyCaptor.getValue();
        assertEquals(lobby.getId(), updatedLobby.getId());
        assertEquals(LobbyStatus.COMPLETED, updatedLobby.getStatus());
        assertFalse(updatedLobby.isTie());
        assertNotNull(updatedLobby.getWinnerId());
        assertTrue(updatedLobby.getWinnerId().isPresent());
        assertEquals(players.get(1).getPlayerId(), updatedLobby.getWinnerId().get());
    }

    @Test
    void testEndDuelSuccessful() {
        Lobby lobby = Lobby.builder()
                .id("12345467890")
                .createdAt(StandardizedOffsetDateTime.now())
                .expiresAt(Optional.of(StandardizedOffsetDateTime.now().minus(30, ChronoUnit.MINUTES)))
                .joinCode("ABC123")
                .playerCount(2)
                .status(LobbyStatus.ACTIVE)
                .winnerId(Optional.empty())
                .build();

        List<LobbyPlayer> players = List.of(
                LobbyPlayer.builder()
                        .id(UUID.randomUUID().toString())
                        .lobbyId(lobby.getId())
                        .playerId(UUID.randomUUID().toString())
                        .points(100)
                        .build(),
                LobbyPlayer.builder()
                        .id(UUID.randomUUID().toString())
                        .lobbyId(lobby.getId())
                        .playerId(UUID.randomUUID().toString())
                        .points(0)
                        .build());

        when(lobbyRepository.findLobbyById(eq(lobby.getId()))).thenReturn(Optional.of(lobby));
        when(lobbyPlayerRepository.findPlayersByLobbyId(eq(lobby.getId()))).thenReturn(players);

        try {
            duelManager.endDuel(lobby.getId(), false);
        } catch (DuelException e) {
            fail(e);
        }

        ArgumentCaptor<Lobby> lobbyCaptor = ArgumentCaptor.forClass(Lobby.class);
        verify(lobbyRepository, times(1)).findLobbyById(lobby.getId());
        verify(lobbyRepository, times(1)).updateLobby(lobbyCaptor.capture());
        verify(lobbyPlayerRepository, times(1)).findPlayersByLobbyId(any());

        var updatedLobby = lobbyCaptor.getValue();
        assertEquals(lobby.getId(), updatedLobby.getId());
        assertNotNull(updatedLobby.getWinnerId());
        assertTrue(updatedLobby.getWinnerId().isPresent());
        assertEquals(updatedLobby.getWinnerId().get(), players.get(0).getPlayerId());
    }

    @Test
    void testEndDuelThrowsNonDuelExceptionFromLobbyPlayerRepository() {
        Lobby lobby = Lobby.builder()
                .id(UUID.randomUUID().toString())
                .createdAt(StandardizedOffsetDateTime.now())
                .expiresAt(Optional.of(StandardizedOffsetDateTime.now().minus(30, ChronoUnit.MINUTES)))
                .joinCode("ABC123")
                .playerCount(2)
                .status(LobbyStatus.ACTIVE)
                .winnerId(Optional.empty())
                .build();

        when(lobbyRepository.findLobbyById(eq(lobby.getId()))).thenReturn(Optional.of(lobby));
        doThrow(new RuntimeException("Simulated db exception"))
                .when(lobbyPlayerRepository)
                .findPlayersByLobbyId(any());

        try {
            duelManager.endDuel(lobby.getId(), false);
            fail("Expected a duel exception");
        } catch (DuelException e) {
            e.printStackTrace();

            StringWriter writer = new StringWriter();
            PrintWriter printWriter = new PrintWriter(writer);
            e.printStackTrace(printWriter);
            printWriter.flush();

            String stackTrace = writer.toString();
            assertTrue(e.getHttpStatus().isEmpty());
            assertEquals("Duel exception occurred.", e.getMessage());
            assertTrue(stackTrace.contains("Simulated db exception"));
        }

        verify(lobbyRepository, times(1)).findLobbyById(eq(lobby.getId()));
        verify(lobbyPlayerRepository, times(1)).findPlayersByLobbyId(any());
        verify(lobbyRepository, never()).updateLobby(any());
    }

    @Test
    void testStartDuelFailsUserIsNotInAParty() {
        when(lobbyPlayerRepository.findValidLobbyPlayerByPlayerId(any())).thenReturn(Optional.empty());

        DuelException e;
        try {
            duelManager.startDuel(Strings.EMPTY, false);
            fail("Expected a duel exception");
            return;
        } catch (DuelException ex) {
            e = ex;
        }

        assertEquals(HttpStatus.NOT_FOUND, e.getHttpStatus().orElseThrow());
        assertEquals("You are not currently in a party!", e.getMessage());

        verify(lobbyPlayerRepository, times(1)).findValidLobbyPlayerByPlayerId(any());
        verify(lobbyRepository, never()).findLobbyById(any());
        verify(lobbyRepository, never()).updateLobby(any());
        verify(questionBankRepository, never()).getRandomQuestion();
        verify(lobbyQuestionRepository, never()).createLobbyQuestion(any());
    }

    @Test
    void testStartDuelFailsLobbyPlayerFoundButNotLobby() {
        String userId = UUID.randomUUID().toString();

        LobbyPlayer lobbyPlayer = LobbyPlayer.builder()
                .id(UUID.randomUUID().toString())
                .lobbyId(UUID.randomUUID().toString())
                .playerId(userId)
                .build();

        when(lobbyPlayerRepository.findValidLobbyPlayerByPlayerId(eq(userId))).thenReturn(Optional.of(lobbyPlayer));
        when(lobbyRepository.findLobbyById(eq(lobbyPlayer.getLobbyId()))).thenReturn(Optional.empty());

        DuelException e;
        try {
            duelManager.startDuel(userId, false);
            fail("Expected a duel exception");
            return;
        } catch (DuelException ex) {
            e = ex;
        }

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, e.getHttpStatus().orElseThrow());
        assertEquals("Hmm, something went wrong.", e.getMessage());

        verify(lobbyPlayerRepository, times(1)).findValidLobbyPlayerByPlayerId(any());
        verify(lobbyRepository, times(1)).findLobbyById(any());
        verify(lobbyRepository, never()).updateLobby(any());
        verify(questionBankRepository, never()).getRandomQuestion();
        verify(lobbyQuestionRepository, never()).createLobbyQuestion(any());
    }

    @Test
    void testStartDuelFailsLobbyFoundIsNotAvailable() {
        String userId = UUID.randomUUID().toString();

        Lobby lobby = Lobby.builder()
                .id(UUID.randomUUID().toString())
                .createdAt(StandardizedOffsetDateTime.now())
                .expiresAt(Optional.of(StandardizedOffsetDateTime.now().minus(30, ChronoUnit.MINUTES)))
                .joinCode(PartyCodeGenerator.generateCode())
                .playerCount(2)
                .status(LobbyStatus.ACTIVE)
                .winnerId(Optional.empty())
                .build();

        LobbyPlayer lobbyPlayer = LobbyPlayer.builder()
                .id(UUID.randomUUID().toString())
                .lobbyId(lobby.getId())
                .playerId(userId)
                .build();

        when(lobbyPlayerRepository.findValidLobbyPlayerByPlayerId(eq(userId))).thenReturn(Optional.of(lobbyPlayer));
        when(lobbyRepository.findLobbyById(eq(lobbyPlayer.getLobbyId()))).thenReturn(Optional.of(lobby));

        DuelException e;
        try {
            duelManager.startDuel(userId, false);
            fail("Expected a duel exception");
            return;
        } catch (DuelException ex) {
            e = ex;
        }

        assertEquals(HttpStatus.CONFLICT, e.getHttpStatus().orElseThrow());
        assertEquals("Lobby is not available!", e.getMessage());

        verify(lobbyPlayerRepository, times(1)).findValidLobbyPlayerByPlayerId(any());
        verify(lobbyRepository, times(1)).findLobbyById(any());
        verify(lobbyRepository, never()).updateLobby(any());
        verify(questionBankRepository, never()).getRandomQuestion();
        verify(lobbyQuestionRepository, never()).createLobbyQuestion(any());
    }

    @Test
    void testStartDuelFailsLobbyDoesNotHaveEnoughPlayers() {
        String userId = UUID.randomUUID().toString();

        Lobby lobby = Lobby.builder()
                .id(UUID.randomUUID().toString())
                .createdAt(StandardizedOffsetDateTime.now())
                .expiresAt(Optional.of(StandardizedOffsetDateTime.now().minus(30, ChronoUnit.MINUTES)))
                .joinCode(PartyCodeGenerator.generateCode())
                .playerCount(1)
                .status(LobbyStatus.AVAILABLE)
                .winnerId(Optional.empty())
                .build();

        LobbyPlayer lobbyPlayer = LobbyPlayer.builder()
                .id(UUID.randomUUID().toString())
                .lobbyId(lobby.getId())
                .playerId(userId)
                .build();

        when(lobbyPlayerRepository.findValidLobbyPlayerByPlayerId(eq(userId))).thenReturn(Optional.of(lobbyPlayer));
        when(lobbyRepository.findLobbyById(eq(lobbyPlayer.getLobbyId()))).thenReturn(Optional.of(lobby));

        DuelException e;
        try {
            duelManager.startDuel(userId, false);
            fail("Expected a duel exception");
            return;
        } catch (DuelException ex) {
            e = ex;
        }

        assertEquals(HttpStatus.CONFLICT, e.getHttpStatus().orElseThrow());
        assertEquals("You must have at least 2 players!", e.getMessage());

        verify(lobbyPlayerRepository, times(1)).findValidLobbyPlayerByPlayerId(any());
        verify(lobbyRepository, times(1)).findLobbyById(any());
        verify(lobbyRepository, never()).updateLobby(any());
        verify(questionBankRepository, never()).getRandomQuestion();
        verify(lobbyQuestionRepository, never()).createLobbyQuestion(any());
    }

    @Test
    void testStartDuelFailsLobbyDoesNotHaveEnoughPlayersButIsAdminUser() {
        String userId = UUID.randomUUID().toString();

        Lobby lobby = Lobby.builder()
                .id(UUID.randomUUID().toString())
                .createdAt(StandardizedOffsetDateTime.now())
                .expiresAt(Optional.of(StandardizedOffsetDateTime.now().minus(30, ChronoUnit.MINUTES)))
                .joinCode(PartyCodeGenerator.generateCode())
                .playerCount(1)
                .status(LobbyStatus.AVAILABLE)
                .winnerId(Optional.empty())
                .build();

        LobbyPlayer lobbyPlayer = LobbyPlayer.builder()
                .id(UUID.randomUUID().toString())
                .lobbyId(lobby.getId())
                .playerId(userId)
                .build();

        QuestionBank questionBank = QuestionBank.builder()
                .id(UUID.randomUUID().toString())
                .questionSlug("Two Sum")
                .build();

        when(lobbyPlayerRepository.findValidLobbyPlayerByPlayerId(eq(userId))).thenReturn(Optional.of(lobbyPlayer));
        when(lobbyRepository.findLobbyById(eq(lobbyPlayer.getLobbyId()))).thenReturn(Optional.of(lobby));
        when(lobbyRepository.updateLobby(any())).thenReturn(true);
        when(questionBankRepository.getRandomQuestion()).thenReturn(Optional.of(questionBank));
        doNothing().when(lobbyQuestionRepository).createLobbyQuestion(any());

        try {
            duelManager.startDuel(userId, true);
        } catch (DuelException e) {
            fail(e);
        }

        verify(lobbyPlayerRepository, times(1)).findValidLobbyPlayerByPlayerId(any());
        verify(lobbyRepository, times(1)).findLobbyById(any());
        verify(lobbyRepository, times(1)).updateLobby(any());
        verify(questionBankRepository, times(1)).getRandomQuestion();
        verify(lobbyQuestionRepository, times(1)).createLobbyQuestion(any());
    }

    @Test
    void testStartDuelSuccess() {
        String userId = UUID.randomUUID().toString();

        Lobby lobby = Lobby.builder()
                .id(UUID.randomUUID().toString())
                .createdAt(StandardizedOffsetDateTime.now())
                .expiresAt(Optional.of(StandardizedOffsetDateTime.now().minus(30, ChronoUnit.MINUTES)))
                .joinCode(PartyCodeGenerator.generateCode())
                .playerCount(2)
                .status(LobbyStatus.AVAILABLE)
                .winnerId(Optional.empty())
                .build();

        LobbyPlayer lobbyPlayer = LobbyPlayer.builder()
                .id(UUID.randomUUID().toString())
                .lobbyId(lobby.getId())
                .playerId(userId)
                .build();

        QuestionBank questionBank = QuestionBank.builder()
                .id(UUID.randomUUID().toString())
                .questionSlug("Two Sum")
                .build();

        when(lobbyPlayerRepository.findValidLobbyPlayerByPlayerId(eq(userId))).thenReturn(Optional.of(lobbyPlayer));
        when(lobbyRepository.findLobbyById(eq(lobbyPlayer.getLobbyId()))).thenReturn(Optional.of(lobby));
        when(lobbyRepository.updateLobby(any())).thenReturn(true);
        when(questionBankRepository.getRandomQuestion()).thenReturn(Optional.of(questionBank));
        doNothing().when(lobbyQuestionRepository).createLobbyQuestion(any());

        try {
            duelManager.startDuel(userId, false);
        } catch (DuelException e) {
            fail(e);
        }

        verify(lobbyPlayerRepository, times(1)).findValidLobbyPlayerByPlayerId(any());
        verify(lobbyRepository, times(1)).findLobbyById(any());
        verify(lobbyRepository, times(1)).updateLobby(any());
        verify(questionBankRepository, times(1)).getRandomQuestion();
        verify(lobbyQuestionRepository, times(1)).createLobbyQuestion(any());
    }

    @Test
    void testStartDuelFailsRandomDatabaseException() {
        doThrow(new RuntimeException("Simulated db exception"))
                .when(lobbyPlayerRepository)
                .findValidLobbyPlayerByPlayerId(any());

        DuelException e;
        try {
            duelManager.startDuel(Strings.EMPTY, false);
            fail("Expected a duel exception");
            return;
        } catch (DuelException ex) {
            ex.printStackTrace();
            e = ex;
        }

        StringWriter writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);
        e.printStackTrace(printWriter);
        printWriter.flush();

        String stackTrace = writer.toString();
        assertTrue(e.getHttpStatus().isEmpty());
        assertEquals("Duel exception occurred.", e.getMessage());
        assertTrue(stackTrace.contains("Simulated db exception"));

        verify(lobbyPlayerRepository, times(1)).findValidLobbyPlayerByPlayerId(any());
        verify(lobbyRepository, never()).findLobbyById(any());
        verify(lobbyRepository, never()).updateLobby(any());
        verify(questionBankRepository, never()).getRandomQuestion();
        verify(lobbyQuestionRepository, never()).createLobbyQuestion(any());
    }

    @Test
    void testStartDuelThrowsNonDuelExceptionFromLobbyRepository() {
        String userId = UUID.randomUUID().toString();

        LobbyPlayer lobbyPlayer = LobbyPlayer.builder()
                .id(UUID.randomUUID().toString())
                .lobbyId(UUID.randomUUID().toString())
                .playerId(userId)
                .build();

        when(lobbyPlayerRepository.findValidLobbyPlayerByPlayerId(eq(userId))).thenReturn(Optional.of(lobbyPlayer));
        doThrow(new RuntimeException("Simulated db exception"))
                .when(lobbyRepository)
                .findLobbyById(any());

        try {
            duelManager.startDuel(userId, false);
            fail("Expected a duel exception");
        } catch (DuelException e) {
            e.printStackTrace();

            StringWriter writer = new StringWriter();
            PrintWriter printWriter = new PrintWriter(writer);
            e.printStackTrace(printWriter);
            printWriter.flush();

            String stackTrace = writer.toString();
            assertTrue(e.getHttpStatus().isEmpty());
            assertEquals("Duel exception occurred.", e.getMessage());
            assertTrue(stackTrace.contains("Simulated db exception"));
        }

        verify(lobbyPlayerRepository, times(1)).findValidLobbyPlayerByPlayerId(eq(userId));
        verify(lobbyRepository, times(1)).findLobbyById(any());
        verify(lobbyRepository, never()).updateLobby(any());
        verify(questionBankRepository, never()).getRandomQuestion();
        verify(lobbyQuestionRepository, never()).createLobbyQuestion(any());
    }

    @Test
    void testStartDuelThrowsNonDuelExceptionFromQuestionBankRepository() {
        String userId = UUID.randomUUID().toString();

        Lobby lobby = Lobby.builder()
                .id(UUID.randomUUID().toString())
                .createdAt(StandardizedOffsetDateTime.now())
                .expiresAt(null)
                .joinCode(PartyCodeGenerator.generateCode())
                .playerCount(2)
                .status(LobbyStatus.AVAILABLE)
                .winnerId(Optional.empty())
                .build();

        LobbyPlayer lobbyPlayer = LobbyPlayer.builder()
                .id(UUID.randomUUID().toString())
                .lobbyId(lobby.getId())
                .playerId(userId)
                .build();

        when(lobbyPlayerRepository.findValidLobbyPlayerByPlayerId(eq(userId))).thenReturn(Optional.of(lobbyPlayer));
        when(lobbyRepository.findLobbyById(eq(lobbyPlayer.getLobbyId()))).thenReturn(Optional.of(lobby));
        when(lobbyRepository.updateLobby(any())).thenReturn(true);
        doThrow(new RuntimeException("Simulated db exception"))
                .when(questionBankRepository)
                .getRandomQuestion();

        try {
            duelManager.startDuel(userId, false);
            fail("Expected a duel exception");
        } catch (DuelException e) {
            e.printStackTrace();

            StringWriter writer = new StringWriter();
            PrintWriter printWriter = new PrintWriter(writer);
            e.printStackTrace(printWriter);
            printWriter.flush();

            String stackTrace = writer.toString();
            assertTrue(e.getHttpStatus().isEmpty());
            assertEquals("Duel exception occurred.", e.getMessage());
            assertTrue(stackTrace.contains("Simulated db exception"));
        }

        verify(lobbyPlayerRepository, times(1)).findValidLobbyPlayerByPlayerId(eq(userId));
        verify(lobbyRepository, times(1)).findLobbyById(any());
        verify(lobbyRepository, times(1)).updateLobby(any());
        verify(questionBankRepository, times(1)).getRandomQuestion();
        verify(lobbyQuestionRepository, never()).createLobbyQuestion(any());
    }

    @Test
    void testStartDuelThrowsNonDuelExceptionFromLobbyQuestionRepository() {
        String userId = UUID.randomUUID().toString();

        Lobby lobby = Lobby.builder()
                .id(UUID.randomUUID().toString())
                .createdAt(StandardizedOffsetDateTime.now())
                .expiresAt(null)
                .joinCode(PartyCodeGenerator.generateCode())
                .playerCount(2)
                .status(LobbyStatus.AVAILABLE)
                .winnerId(Optional.empty())
                .build();

        LobbyPlayer lobbyPlayer = LobbyPlayer.builder()
                .id(UUID.randomUUID().toString())
                .lobbyId(lobby.getId())
                .playerId(userId)
                .build();

        QuestionBank questionBank = QuestionBank.builder()
                .id(UUID.randomUUID().toString())
                .questionSlug("Two Sum")
                .build();

        when(lobbyPlayerRepository.findValidLobbyPlayerByPlayerId(eq(userId))).thenReturn(Optional.of(lobbyPlayer));
        when(lobbyRepository.findLobbyById(eq(lobbyPlayer.getLobbyId()))).thenReturn(Optional.of(lobby));
        when(lobbyRepository.updateLobby(any())).thenReturn(true);
        when(questionBankRepository.getRandomQuestion()).thenReturn(Optional.of(questionBank));
        doThrow(new RuntimeException("Simulated db exception"))
                .when(lobbyQuestionRepository)
                .createLobbyQuestion(any());

        try {
            duelManager.startDuel(userId, false);
            fail("Expected a duel exception");
        } catch (DuelException e) {
            e.printStackTrace();

            StringWriter writer = new StringWriter();
            PrintWriter printWriter = new PrintWriter(writer);
            e.printStackTrace(printWriter);
            printWriter.flush();

            String stackTrace = writer.toString();
            assertTrue(e.getHttpStatus().isEmpty());
            assertEquals("Duel exception occurred.", e.getMessage());
            assertTrue(stackTrace.contains("Simulated db exception"));
        }

        verify(lobbyPlayerRepository, times(1)).findValidLobbyPlayerByPlayerId(eq(userId));
        verify(lobbyRepository, times(1)).findLobbyById(any());
        verify(lobbyRepository, times(1)).updateLobby(any());
        verify(questionBankRepository, times(1)).getRandomQuestion();
        verify(lobbyQuestionRepository, times(1)).createLobbyQuestion(any());
    }

    @Test
    void testGetLobbyByUserIdNoPartyOrDuelFound() {
        String userId = UUID.randomUUID().toString();

        when(lobbyRepository.findAvailableLobbyByLobbyPlayerPlayerId(eq(userId)))
                .thenReturn(Optional.empty());
        when(lobbyRepository.findActiveLobbyByLobbyPlayerPlayerId(eq(userId))).thenReturn(Optional.empty());

        try {
            duelManager.getLobbyByUserId(userId);
            fail("Expected a duel exception");
        } catch (DuelException e) {
            assertEquals(HttpStatus.NOT_FOUND, e.getHttpStatus().orElseThrow());
            assertEquals("No duel or party found for the given player.", e.getMessage());
        }

        verify(lobbyRepository, times(1)).findAvailableLobbyByLobbyPlayerPlayerId(eq(userId));
        verify(lobbyRepository, times(1)).findActiveLobbyByLobbyPlayerPlayerId(eq(userId));
    }

    @Test
    void testGetLobbyByUserIdNoPartyFoundButDuelFound() {
        String userId = UUID.randomUUID().toString();

        Lobby duel = Lobby.builder()
                .id(UUID.randomUUID().toString())
                .createdAt(StandardizedOffsetDateTime.now())
                .expiresAt(Optional.of(StandardizedOffsetDateTime.now()))
                .joinCode(PartyCodeGenerator.generateCode())
                .playerCount(1)
                .winnerId(Optional.empty())
                .tie(false)
                .status(LobbyStatus.ACTIVE)
                .build();

        when(lobbyRepository.findAvailableLobbyByLobbyPlayerPlayerId(eq(userId)))
                .thenReturn(Optional.empty());
        when(lobbyRepository.findActiveLobbyByLobbyPlayerPlayerId(eq(userId))).thenReturn(Optional.of(duel));

        try {
            var res = duelManager.getLobbyByUserId(userId);
            assertNotNull(res);
            assertEquals(duel, res);
        } catch (DuelException e) {
            fail(e);
        }

        verify(lobbyRepository, times(1)).findAvailableLobbyByLobbyPlayerPlayerId(eq(userId));
        verify(lobbyRepository, times(1)).findActiveLobbyByLobbyPlayerPlayerId(eq(userId));
    }

    @Test
    void testGetLobbyByUserIdPartyFound() {
        String userId = UUID.randomUUID().toString();

        Lobby party = Lobby.builder()
                .id(UUID.randomUUID().toString())
                .createdAt(StandardizedOffsetDateTime.now())
                .expiresAt(Optional.of(StandardizedOffsetDateTime.now()))
                .joinCode(PartyCodeGenerator.generateCode())
                .playerCount(2)
                .winnerId(Optional.empty())
                .tie(false)
                .status(LobbyStatus.AVAILABLE)
                .build();

        when(lobbyRepository.findAvailableLobbyByLobbyPlayerPlayerId(eq(userId)))
                .thenReturn(Optional.of(party));

        try {
            var res = duelManager.getLobbyByUserId(userId);
            assertNotNull(res);
            assertEquals(party, res);
        } catch (DuelException e) {
            fail(e);
        }

        verify(lobbyRepository, times(1)).findAvailableLobbyByLobbyPlayerPlayerId(eq(userId));
        verify(lobbyRepository, never()).findActiveLobbyByLobbyPlayerPlayerId(eq(userId));
    }

    @Test
    void testGetLobbyByUserIdThrowsNonDuelException() {
        String userId = UUID.randomUUID().toString();

        doThrow(new RuntimeException("Simulated db exception"))
                .when(lobbyRepository)
                .findAvailableLobbyByLobbyPlayerPlayerId(any());

        try {
            duelManager.getLobbyByUserId(userId);
            fail("Expected a duel exception");
        } catch (DuelException e) {
            e.printStackTrace();

            StringWriter writer = new StringWriter();
            PrintWriter printWriter = new PrintWriter(writer);
            e.printStackTrace(printWriter);
            printWriter.flush();

            String stackTrace = writer.toString();
            assertTrue(e.getHttpStatus().isEmpty());
            assertEquals("Duel exception occurred.", e.getMessage());
            assertTrue(stackTrace.contains("Simulated db exception"));
        }

        verify(lobbyRepository, times(1)).findAvailableLobbyByLobbyPlayerPlayerId(any());
        verify(lobbyRepository, never()).findActiveLobbyByLobbyPlayerPlayerId(any());
    }

    @Test
    void testProcessSubmissionsLobbyGivenButNoLobbyPlayerInstanceFound() {
        var user = createRandomUser();
        var activeLobby = Lobby.builder()
                .id(UUID.randomUUID().toString())
                .joinCode(PartyCodeGenerator.generateCode())
                .status(LobbyStatus.ACTIVE)
                .createdAt(StandardizedOffsetDateTime.now())
                .expiresAt(Optional.of(StandardizedOffsetDateTime.now().plus(30, ChronoUnit.MINUTES)))
                .build();

        when(lobbyPlayerRepository.findValidLobbyPlayerByPlayerId(eq(user.getId())))
                .thenReturn(Optional.empty());

        DuelException e;
        try {
            duelManager.processSubmissions(user, activeLobby);
            fail("Expected an exception");
            return;
        } catch (DuelException ex) {
            ex.printStackTrace();
            e = ex;
        }

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, e.getHttpStatus().orElseThrow());
        assertEquals("A duel was found but the player instance cannot be found.", e.getMessage());

        verify(lobbyPlayerRepository, times(1)).findValidLobbyPlayerByPlayerId(eq(user.getId()));
        verify(lobbyQuestionRepository, never()).findLobbyQuestionsByLobbyId(any());
        verify(questionBankRepository, never()).getQuestionById(any());
        verify(throttledLeetcodeClient, never()).findSubmissionsByUsername(any(), anyInt());
        verify(submissionsHandler, never()).handleSubmissions(any(), any(), eq(true));
        verify(lobbyPlayerQuestionRepository, never()).createLobbyPlayerQuestion(any());
        verify(lobbyPlayerRepository, never()).updateLobbyPlayer(any());
    }

    @Test
    void testProcessSubmissionsFailsRandomDatabaseException() {
        var user = createRandomUser();
        var activeLobby = Lobby.builder()
                .id(UUID.randomUUID().toString())
                .joinCode(PartyCodeGenerator.generateCode())
                .status(LobbyStatus.ACTIVE)
                .createdAt(StandardizedOffsetDateTime.now())
                .expiresAt(Optional.of(StandardizedOffsetDateTime.now().plus(30, ChronoUnit.MINUTES)))
                .build();

        doThrow(new RuntimeException("Simulated db exception"))
                .when(lobbyPlayerRepository)
                .findValidLobbyPlayerByPlayerId(any());

        DuelException e;
        try {
            duelManager.processSubmissions(user, activeLobby);
            fail("Expected a duel exception");
            return;
        } catch (DuelException ex) {
            ex.printStackTrace();
            e = ex;
        }

        StringWriter writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);
        e.printStackTrace(printWriter);
        printWriter.flush();

        String stackTrace = writer.toString();
        assertTrue(e.getHttpStatus().isEmpty());
        assertEquals("Duel exception occurred.", e.getMessage());
        assertTrue(stackTrace.contains("Simulated db exception"));

        verify(lobbyPlayerRepository, times(1)).findValidLobbyPlayerByPlayerId(any());
        verify(lobbyRepository, never()).findLobbyById(any());
        verify(lobbyRepository, never()).updateLobby(any());
        verify(questionBankRepository, never()).getRandomQuestion();
        verify(lobbyQuestionRepository, never()).createLobbyQuestion(any());
    }

    @Test
    void testProcessSubmissionsSuccessful() {
        var user = createRandomUser();
        var activeLobby = Lobby.builder()
                .id(UUID.randomUUID().toString())
                .joinCode(PartyCodeGenerator.generateCode())
                .status(LobbyStatus.ACTIVE)
                .createdAt(StandardizedOffsetDateTime.now())
                .expiresAt(Optional.of(StandardizedOffsetDateTime.now().plus(30, ChronoUnit.MINUTES)))
                .build();

        var lobbyPlayer = LobbyPlayer.builder()
                .id(UUID.randomUUID().toString())
                .lobbyId(activeLobby.getId())
                .playerId(user.getId())
                .points(0)
                .build();

        var questionBank = QuestionBank.builder()
                .id(UUID.randomUUID().toString())
                .questionSlug("two-sum")
                .questionTitle("Two Sum")
                .build();

        var lobbyQuestion = LobbyQuestion.builder()
                .id(UUID.randomUUID().toString())
                .lobbyId(activeLobby.getId())
                .questionBankId(questionBank.getId())
                .userSolvedCount(0)
                .build();

        var leetcodeSubmission =
                new LeetcodeSubmission(1, "Two Sum", "two-sum", java.time.LocalDateTime.now(), "Accepted");

        var acceptedSubmission = new AcceptedSubmission("Two Sum", "question-id-123", 100);

        when(lobbyPlayerRepository.findValidLobbyPlayerByPlayerId(eq(user.getId())))
                .thenReturn(Optional.of(lobbyPlayer));
        when(lobbyQuestionRepository.findLobbyQuestionsByLobbyId(eq(activeLobby.getId())))
                .thenReturn(List.of(lobbyQuestion));
        when(questionBankRepository.getQuestionById(eq(questionBank.getId()))).thenReturn(Optional.of(questionBank));
        when(throttledLeetcodeClient.findSubmissionsByUsername(
                        eq(user.getLeetcodeUsername().orElse(null)), eq(5)))
                .thenReturn(List.of(leetcodeSubmission));
        when(submissionsHandler.handleSubmissions(any(), eq(user), eq(true))).thenReturn(new ArrayList<>() {
            {
                add(acceptedSubmission);
            }
        });
        doNothing().when(lobbyPlayerQuestionRepository).createLobbyPlayerQuestion(any());
        when(lobbyPlayerRepository.updateLobbyPlayer(any())).thenReturn(true);

        try {
            int result = duelManager.processSubmissions(user, activeLobby);
            assertEquals(1, result);
        } catch (DuelException e) {
            fail(e);
        }

        verify(lobbyPlayerRepository, times(1)).findValidLobbyPlayerByPlayerId(eq(user.getId()));
        verify(lobbyQuestionRepository, times(1)).findLobbyQuestionsByLobbyId(eq(activeLobby.getId()));
        verify(questionBankRepository, times(1)).getQuestionById(eq(questionBank.getId()));
        verify(throttledLeetcodeClient, times(1))
                .findSubmissionsByUsername(eq(user.getLeetcodeUsername().orElse(null)), eq(5));
        verify(submissionsHandler, times(1)).handleSubmissions(any(), eq(user), eq(true));
        verify(lobbyPlayerQuestionRepository, times(1)).createLobbyPlayerQuestion(any());
        verify(lobbyPlayerRepository, times(1)).updateLobbyPlayer(any());
    }

    @Test
    void testProcessSubmissionsThrowsNonDuelExceptionFromLobbyQuestionRepository() {
        var user = createRandomUser();
        var activeLobby = Lobby.builder()
                .id(UUID.randomUUID().toString())
                .joinCode(PartyCodeGenerator.generateCode())
                .status(LobbyStatus.ACTIVE)
                .createdAt(StandardizedOffsetDateTime.now())
                .expiresAt(Optional.of(StandardizedOffsetDateTime.now().plus(30, ChronoUnit.MINUTES)))
                .build();

        var lobbyPlayer = LobbyPlayer.builder()
                .id(UUID.randomUUID().toString())
                .lobbyId(activeLobby.getId())
                .playerId(user.getId())
                .points(0)
                .build();

        when(lobbyPlayerRepository.findValidLobbyPlayerByPlayerId(eq(user.getId())))
                .thenReturn(Optional.of(lobbyPlayer));
        doThrow(new RuntimeException("Simulated db exception"))
                .when(lobbyQuestionRepository)
                .findLobbyQuestionsByLobbyId(any());

        try {
            duelManager.processSubmissions(user, activeLobby);
            fail("Expected a duel exception");
        } catch (DuelException e) {
            e.printStackTrace();

            StringWriter writer = new StringWriter();
            PrintWriter printWriter = new PrintWriter(writer);
            e.printStackTrace(printWriter);
            printWriter.flush();

            String stackTrace = writer.toString();
            assertTrue(e.getHttpStatus().isEmpty());
            assertEquals("Duel exception occurred.", e.getMessage());
            assertTrue(stackTrace.contains("Simulated db exception"));
        }

        verify(lobbyPlayerRepository, times(1)).findValidLobbyPlayerByPlayerId(eq(user.getId()));
        verify(lobbyQuestionRepository, times(1)).findLobbyQuestionsByLobbyId(eq(activeLobby.getId()));
        verify(questionBankRepository, never()).getQuestionById(any());
        verify(throttledLeetcodeClient, never()).findSubmissionsByUsername(any(), anyInt());
        verify(submissionsHandler, never()).handleSubmissions(any(), any(), eq(true));
        verify(lobbyPlayerQuestionRepository, never()).createLobbyPlayerQuestion(any());
        verify(lobbyPlayerRepository, never()).updateLobbyPlayer(any());
    }

    @Test
    void testProcessSubmissionsThrowsNonDuelExceptionFromQuestionBankRepository() {
        var user = createRandomUser();
        var activeLobby = Lobby.builder()
                .id(UUID.randomUUID().toString())
                .joinCode(PartyCodeGenerator.generateCode())
                .status(LobbyStatus.ACTIVE)
                .createdAt(StandardizedOffsetDateTime.now())
                .expiresAt(Optional.of(StandardizedOffsetDateTime.now().plus(30, ChronoUnit.MINUTES)))
                .build();

        var lobbyPlayer = LobbyPlayer.builder()
                .id(UUID.randomUUID().toString())
                .lobbyId(activeLobby.getId())
                .playerId(user.getId())
                .points(0)
                .build();

        var lobbyQuestion = LobbyQuestion.builder()
                .id(UUID.randomUUID().toString())
                .lobbyId(activeLobby.getId())
                .questionBankId(UUID.randomUUID().toString())
                .userSolvedCount(0)
                .build();

        when(lobbyPlayerRepository.findValidLobbyPlayerByPlayerId(eq(user.getId())))
                .thenReturn(Optional.of(lobbyPlayer));
        when(lobbyQuestionRepository.findLobbyQuestionsByLobbyId(eq(activeLobby.getId())))
                .thenReturn(List.of(lobbyQuestion));
        doThrow(new RuntimeException("Simulated db exception"))
                .when(questionBankRepository)
                .getQuestionById(any());

        try {
            duelManager.processSubmissions(user, activeLobby);
            fail("Expected a duel exception");
        } catch (DuelException e) {
            e.printStackTrace();

            StringWriter writer = new StringWriter();
            PrintWriter printWriter = new PrintWriter(writer);
            e.printStackTrace(printWriter);
            printWriter.flush();

            String stackTrace = writer.toString();
            assertTrue(e.getHttpStatus().isEmpty());
            assertEquals("Duel exception occurred.", e.getMessage());
            assertTrue(stackTrace.contains("Simulated db exception"));
        }

        verify(lobbyPlayerRepository, times(1)).findValidLobbyPlayerByPlayerId(eq(user.getId()));
        verify(lobbyQuestionRepository, times(1)).findLobbyQuestionsByLobbyId(eq(activeLobby.getId()));
        verify(questionBankRepository, times(1)).getQuestionById(any());
        verify(throttledLeetcodeClient, never()).findSubmissionsByUsername(any(), anyInt());
        verify(submissionsHandler, never()).handleSubmissions(any(), any(), eq(true));
        verify(lobbyPlayerQuestionRepository, never()).createLobbyPlayerQuestion(any());
        verify(lobbyPlayerRepository, never()).updateLobbyPlayer(any());
    }
}
