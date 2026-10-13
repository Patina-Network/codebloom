package org.patinanetwork.codebloom.common.components.duel;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.github.javafaker.Faker;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.patinanetwork.codebloom.common.db.models.lobby.Lobby;
import org.patinanetwork.codebloom.common.db.models.lobby.LobbyStatus;
import org.patinanetwork.codebloom.common.db.models.lobby.player.LobbyPlayer;
import org.patinanetwork.codebloom.common.db.models.user.User;
import org.patinanetwork.codebloom.common.db.repos.lobby.LobbyRepository;
import org.patinanetwork.codebloom.common.db.repos.lobby.player.LobbyPlayerRepository;
import org.patinanetwork.codebloom.common.time.StandardizedOffsetDateTime;
import org.springframework.http.HttpStatus;

public class PartyManagerTest {
    private final PartyManager partyManager;
    private final Faker faker;

    private final LobbyPlayerRepository lobbyPlayerRepository = mock(LobbyPlayerRepository.class);
    private final LobbyRepository lobbyRepository = mock(LobbyRepository.class);

    public PartyManagerTest() {
        partyManager = new PartyManager(lobbyPlayerRepository, lobbyRepository);
        this.faker = Faker.instance();
    }

    private String randomUUID() {
        return UUID.randomUUID().toString();
    }

    private User createRandomUser() {
        return User.builder()
                .id(randomUUID())
                .discordId(String.valueOf(faker.number().randomNumber(18, true)))
                .discordName(faker.name().username())
                .leetcodeUsername(Optional.ofNullable(faker.name().username()))
                .admin(false)
                .verifyKey(faker.crypto().md5())
                .build();
    }

    @Test
    void testCreatePartySuccessWhenUserNotInParty() throws DuelException {
        User user = createRandomUser();

        when(lobbyPlayerRepository.findValidLobbyPlayerByPlayerId(user.getId())).thenReturn(Optional.empty());

        String joinCode = partyManager.createParty(user.getId());

        assertNotNull(joinCode);
        assertEquals(6, joinCode.length());

        ArgumentCaptor<Lobby> lobbyCaptor = ArgumentCaptor.forClass(Lobby.class);
        ArgumentCaptor<LobbyPlayer> playerCaptor = ArgumentCaptor.forClass(LobbyPlayer.class);

        verify(lobbyRepository, times(1)).createLobby(lobbyCaptor.capture());
        verify(lobbyPlayerRepository, times(1)).createLobbyPlayer(playerCaptor.capture());

        Lobby createdLobby = lobbyCaptor.getValue();
        assertNotNull(createdLobby.getJoinCode());
        assertEquals(6, createdLobby.getJoinCode().length());
        assertEquals(LobbyStatus.AVAILABLE, createdLobby.getStatus());
        assertEquals(1, createdLobby.getPlayerCount());
        assertTrue(createdLobby.getWinnerId().isEmpty());
        assertNull(null);

        LobbyPlayer createdPlayer = playerCaptor.getValue();
        assertEquals(user.getId(), createdPlayer.getPlayerId());
        assertEquals(0, createdPlayer.getPoints());
    }

    @Test
    void testCreatePartyFailureWhenUserAlreadyInParty() {
        User user = createRandomUser();

        LobbyPlayer existingLobbyPlayer = LobbyPlayer.builder()
                .id(randomUUID())
                .lobbyId(randomUUID())
                .playerId(user.getId())
                .points(100)
                .build();

        when(lobbyPlayerRepository.findValidLobbyPlayerByPlayerId(user.getId()))
                .thenReturn(Optional.of(existingLobbyPlayer));

        DuelException exception = assertThrows(DuelException.class, () -> partyManager.createParty(user.getId()));

        assertEquals(HttpStatus.CONFLICT, exception.getHttpStatus().orElseThrow());
        assertEquals(
                "You are already in a lobby. Please leave your current lobby before creating a new one.",
                exception.getMessage());

        verify(lobbyRepository, times(0)).createLobby(any());
        verify(lobbyPlayerRepository, times(0)).createLobbyPlayer(any());
    }

    @Test
    void testMultipleUsersCanCreatePartiesIndependently() throws DuelException {
        User user1 = createRandomUser();
        User user2 = createRandomUser();

        when(lobbyPlayerRepository.findValidLobbyPlayerByPlayerId(user1.getId()))
                .thenReturn(Optional.empty());
        when(lobbyPlayerRepository.findValidLobbyPlayerByPlayerId(user2.getId()))
                .thenReturn(Optional.empty());

        String joinCode1 = partyManager.createParty(user1.getId());
        String joinCode2 = partyManager.createParty(user2.getId());

        assertNotNull(joinCode1);
        assertNotNull(joinCode2);
        assertNotEquals(joinCode1, joinCode2);

        ArgumentCaptor<Lobby> lobbyCaptor = ArgumentCaptor.forClass(Lobby.class);
        ArgumentCaptor<LobbyPlayer> playerCaptor = ArgumentCaptor.forClass(LobbyPlayer.class);

        verify(lobbyRepository, times(2)).createLobby(lobbyCaptor.capture());
        verify(lobbyPlayerRepository, times(2)).createLobbyPlayer(playerCaptor.capture());

        var lobbyPlayers = playerCaptor.getAllValues();
        assertEquals(user1.getId(), lobbyPlayers.get(0).getPlayerId());
        assertEquals(user2.getId(), lobbyPlayers.get(1).getPlayerId());
    }

    @Test
    void testEachPartyHasUniqueJoinCode() throws DuelException {
        User user1 = createRandomUser();
        User user2 = createRandomUser();

        when(lobbyPlayerRepository.findValidLobbyPlayerByPlayerId(user1.getId()))
                .thenReturn(Optional.empty());
        when(lobbyPlayerRepository.findValidLobbyPlayerByPlayerId(user2.getId()))
                .thenReturn(Optional.empty());

        partyManager.createParty(user1.getId());
        partyManager.createParty(user2.getId());

        ArgumentCaptor<Lobby> lobbyCaptor = ArgumentCaptor.forClass(Lobby.class);
        verify(lobbyRepository, times(2)).createLobby(lobbyCaptor.capture());

        var lobbies = lobbyCaptor.getAllValues();
        assertNotEquals(lobbies.get(0).getJoinCode(), lobbies.get(1).getJoinCode());
    }

    @Test
    void testPartyExpirationTimeIsNullOnCreation() throws DuelException {
        User user = createRandomUser();

        when(lobbyPlayerRepository.findValidLobbyPlayerByPlayerId(user.getId())).thenReturn(Optional.empty());

        partyManager.createParty(user.getId());

        ArgumentCaptor<Lobby> lobbyCaptor = ArgumentCaptor.forClass(Lobby.class);
        verify(lobbyRepository, times(1)).createLobby(lobbyCaptor.capture());

        Lobby createdLobby = lobbyCaptor.getValue();
        assertTrue(
                createdLobby.getExpiresAt().isEmpty(),
                "expiresAt should be empty when party is created, only set when duel starts");
    }

    @Test
    void testNewLobbyPlayerStartsWithZeroPoints() throws DuelException {
        User user = createRandomUser();

        when(lobbyPlayerRepository.findValidLobbyPlayerByPlayerId(user.getId())).thenReturn(Optional.empty());

        String joinCode = partyManager.createParty(user.getId());

        assertNotNull(joinCode);

        ArgumentCaptor<LobbyPlayer> playerCaptor = ArgumentCaptor.forClass(LobbyPlayer.class);
        verify(lobbyPlayerRepository, times(1)).createLobbyPlayer(playerCaptor.capture());

        LobbyPlayer createdPlayer = playerCaptor.getValue();
        assertEquals(0, createdPlayer.getPoints());
    }

    @Test
    void testCreatePartyInitializesLobbyWithoutWinner() throws DuelException {
        User user = createRandomUser();

        when(lobbyPlayerRepository.findValidLobbyPlayerByPlayerId(user.getId())).thenReturn(Optional.empty());

        String joinCode = partyManager.createParty(user.getId());
        assertNotNull(joinCode);

        ArgumentCaptor<Lobby> lobbyCaptor = ArgumentCaptor.forClass(Lobby.class);
        verify(lobbyRepository, times(1)).createLobby(lobbyCaptor.capture());

        Lobby createdLobby = lobbyCaptor.getValue();
        assertTrue(createdLobby.getWinnerId().isEmpty());
    }

    @Test
    void testCreatePartyReturnsJoinCode() throws DuelException {
        User user = createRandomUser();

        when(lobbyPlayerRepository.findValidLobbyPlayerByPlayerId(user.getId())).thenReturn(Optional.empty());

        String joinCode = partyManager.createParty(user.getId());

        assertNotNull(joinCode);
        assertEquals(6, joinCode.length());
    }

    @Test
    void testCreatePartyThrowsNonDuelException() {
        User user = createRandomUser();

        doThrow(new RuntimeException("Simulated db exception"))
                .when(lobbyPlayerRepository)
                .findValidLobbyPlayerByPlayerId(any());

        try {
            partyManager.createParty(user.getId());
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

        verify(lobbyPlayerRepository, times(1)).findValidLobbyPlayerByPlayerId(any());
        verify(lobbyRepository, never()).createLobby(any());
        verify(lobbyPlayerRepository, never()).createLobbyPlayer(any());
    }

    @Test
    void testJoinPartyCannotFindLobbyByJoinCode() {
        User user = createRandomUser();
        String partyCode = "ABC123";

        when(lobbyRepository.findAvailableLobbyByJoinCode(eq(partyCode))).thenReturn(Optional.empty());

        DuelException exception =
                assertThrows(DuelException.class, () -> partyManager.joinParty(user.getId(), partyCode));

        assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus().orElseThrow());
        assertEquals("The party with the given code cannot be found.", exception.getMessage());
    }

    @Test
    void testJoinPartySuccess() throws DuelException {
        User user = createRandomUser();
        String partyCode = "ABC123";

        Lobby mockLobby = Lobby.builder()
                .id(randomUUID())
                .joinCode(partyCode)
                .expiresAt(Optional.of(StandardizedOffsetDateTime.now().plusHours(1)))
                .status(LobbyStatus.AVAILABLE)
                .playerCount(1)
                .winnerId(Optional.empty())
                .build();

        when(lobbyRepository.findAvailableLobbyByJoinCode(partyCode)).thenReturn(Optional.of(mockLobby));
        when(lobbyRepository.findAvailableLobbyByLobbyPlayerPlayerId(user.getId()))
                .thenReturn(Optional.empty());
        when(lobbyRepository.findActiveLobbyByLobbyPlayerPlayerId(user.getId())).thenReturn(Optional.empty());
        when(lobbyRepository.updateLobby(any(Lobby.class))).thenReturn(true);

        partyManager.joinParty(user.getId(), partyCode);

        verify(lobbyPlayerRepository, times(1)).createLobbyPlayer(any(LobbyPlayer.class));
        verify(lobbyRepository, times(1)).updateLobby(argThat(lobby -> lobby.getPlayerCount() == 2));
    }

    @Test
    void testJoinPartyLobbyAtMaxCapacity() {
        User user = createRandomUser();
        String partyCode = "ABC123";

        Lobby mockLobby = Lobby.builder()
                .id(randomUUID())
                .joinCode(partyCode)
                .status(LobbyStatus.AVAILABLE)
                .expiresAt(Optional.of(StandardizedOffsetDateTime.now().plusHours(1)))
                .playerCount(2)
                .winnerId(Optional.empty())
                .build();

        when(lobbyRepository.findAvailableLobbyByJoinCode(partyCode)).thenReturn(Optional.of(mockLobby));

        DuelException exception =
                assertThrows(DuelException.class, () -> partyManager.joinParty(user.getId(), partyCode));

        assertEquals(HttpStatus.CONFLICT, exception.getHttpStatus().orElseThrow());
        assertEquals("This lobby already has the maximum number of players", exception.getMessage());

        verify(lobbyPlayerRepository, times(0)).createLobbyPlayer(any());
        verify(lobbyRepository, times(0)).updateLobby(any());
    }

    @Test
    void testJoinPartyPlayerAlreadyInAvailableLobby() {
        User user = createRandomUser();
        String partyCode = "ABC123";

        Lobby mockLobby = Lobby.builder()
                .id(randomUUID())
                .joinCode(partyCode)
                .expiresAt(Optional.of(StandardizedOffsetDateTime.now().plusHours(1)))
                .status(LobbyStatus.AVAILABLE)
                .playerCount(1)
                .winnerId(Optional.empty())
                .build();

        Lobby existingLobby = Lobby.builder()
                .id(randomUUID())
                .expiresAt(Optional.of(StandardizedOffsetDateTime.now().plusHours(1)))
                .status(LobbyStatus.AVAILABLE)
                .winnerId(Optional.empty())
                .build();

        when(lobbyRepository.findAvailableLobbyByJoinCode(partyCode)).thenReturn(Optional.of(mockLobby));
        when(lobbyRepository.findAvailableLobbyByLobbyPlayerPlayerId(user.getId()))
                .thenReturn(Optional.of(existingLobby));

        DuelException exception =
                assertThrows(DuelException.class, () -> partyManager.joinParty(user.getId(), partyCode));

        assertEquals(HttpStatus.CONFLICT, exception.getHttpStatus().orElseThrow());
        assertEquals("You are already in a party. Please leave the party, then try again.", exception.getMessage());

        verify(lobbyPlayerRepository, times(0)).createLobbyPlayer(any());
        verify(lobbyRepository, times(0)).updateLobby(any());
    }

    @Test
    void testJoinPartyPlayerAlreadyInActiveLobby() {
        User user = createRandomUser();
        String partyCode = "ABC123";

        Lobby mockLobby = Lobby.builder()
                .id(randomUUID())
                .joinCode(partyCode)
                .status(LobbyStatus.AVAILABLE)
                .expiresAt(Optional.of(StandardizedOffsetDateTime.now().plusHours(1)))
                .playerCount(1)
                .winnerId(Optional.empty())
                .build();

        Lobby activeLobby = Lobby.builder()
                .id(randomUUID())
                .status(LobbyStatus.ACTIVE)
                .expiresAt(Optional.of(StandardizedOffsetDateTime.now().plusHours(1)))
                .winnerId(Optional.empty())
                .build();

        when(lobbyRepository.findAvailableLobbyByJoinCode(partyCode)).thenReturn(Optional.of(mockLobby));
        when(lobbyRepository.findAvailableLobbyByLobbyPlayerPlayerId(user.getId()))
                .thenReturn(Optional.empty());
        when(lobbyRepository.findActiveLobbyByLobbyPlayerPlayerId(user.getId())).thenReturn(Optional.of(activeLobby));

        DuelException exception =
                assertThrows(DuelException.class, () -> partyManager.joinParty(user.getId(), partyCode));

        assertEquals(HttpStatus.CONFLICT, exception.getHttpStatus().orElseThrow());
        assertEquals("You are currently in a duel. Please forfeit the duel, then try again.", exception.getMessage());

        verify(lobbyPlayerRepository, times(0)).createLobbyPlayer(any());
        verify(lobbyRepository, times(0)).updateLobby(any());
    }

    @Test
    void testJoinPartyLobbyUpdateFailure() {
        User user = createRandomUser();
        String partyCode = "ABC123";

        Lobby mockLobby = Lobby.builder()
                .id(randomUUID())
                .joinCode(partyCode)
                .status(LobbyStatus.AVAILABLE)
                .expiresAt(Optional.of(StandardizedOffsetDateTime.now().plusHours(1)))
                .playerCount(1)
                .winnerId(Optional.empty())
                .build();

        when(lobbyRepository.findAvailableLobbyByJoinCode(partyCode)).thenReturn(Optional.of(mockLobby));
        when(lobbyRepository.findAvailableLobbyByLobbyPlayerPlayerId(user.getId()))
                .thenReturn(Optional.empty());
        when(lobbyRepository.findActiveLobbyByLobbyPlayerPlayerId(user.getId())).thenReturn(Optional.empty());
        when(lobbyRepository.updateLobby(any(Lobby.class))).thenReturn(false);

        DuelException exception =
                assertThrows(DuelException.class, () -> partyManager.joinParty(user.getId(), partyCode));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getHttpStatus().orElseThrow());
        assertEquals("Failed to join party. Please try again later.", exception.getMessage());

        verify(lobbyPlayerRepository, times(1)).createLobbyPlayer(any(LobbyPlayer.class));
        verify(lobbyRepository, times(1)).updateLobby(any(Lobby.class));
    }

    @Test
    void testJoinPartyExpiredLobby() {
        User user = createRandomUser();
        String partyCode = "ABC123";

        OffsetDateTime pastTime = OffsetDateTime.now().minusMinutes(5);
        Lobby expiredLobby = Lobby.builder()
                .id(randomUUID())
                .joinCode(partyCode)
                .status(LobbyStatus.AVAILABLE)
                .playerCount(1)
                .expiresAt(Optional.of(pastTime))
                .winnerId(Optional.empty())
                .build();

        when(lobbyRepository.findAvailableLobbyByJoinCode(partyCode)).thenReturn(Optional.of(expiredLobby));

        DuelException exception =
                assertThrows(DuelException.class, () -> partyManager.joinParty(user.getId(), partyCode));

        assertEquals(HttpStatus.GONE, exception.getHttpStatus().orElseThrow());
        assertEquals("The lobby has expired and cannot be joined.", exception.getMessage());

        verify(lobbyPlayerRepository, times(0)).createLobbyPlayer(any());
        verify(lobbyRepository, times(0)).updateLobby(any());
    }

    @Test
    void testJoinPartyThrowsNonDuelException() {
        User user = createRandomUser();
        String partyCode = "ABC123";

        doThrow(new RuntimeException("Simulated db exception"))
                .when(lobbyRepository)
                .findAvailableLobbyByJoinCode(any());

        try {
            partyManager.joinParty(user.getId(), partyCode);
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

        verify(lobbyRepository, times(1)).findAvailableLobbyByJoinCode(eq(partyCode));
        verify(lobbyPlayerRepository, never()).createLobbyPlayer(any());
        verify(lobbyRepository, never()).updateLobby(any());
    }

    @Test
    void testLeavePartySuccessWhenUserInPartyAndLobbyHasMultiplePlayers() throws DuelException {
        User user = createRandomUser();

        String lobbyId = randomUUID();
        LobbyPlayer existingLobbyPlayer = LobbyPlayer.builder()
                .id(randomUUID())
                .lobbyId(lobbyId)
                .playerId(user.getId())
                .points(50)
                .build();

        Lobby lobby = Lobby.builder()
                .id(lobbyId)
                .joinCode("ABC123")
                .status(LobbyStatus.AVAILABLE)
                .expiresAt(Optional.of(OffsetDateTime.now().plusMinutes(30)))
                .playerCount(3)
                .winnerId(Optional.empty())
                .build();

        when(lobbyPlayerRepository.findValidLobbyPlayerByPlayerId(user.getId()))
                .thenReturn(Optional.of(existingLobbyPlayer));
        when(lobbyPlayerRepository.deleteLobbyPlayerById(existingLobbyPlayer.getId()))
                .thenReturn(true);
        when(lobbyRepository.findLobbyById(lobbyId)).thenReturn(Optional.of(lobby));

        partyManager.leaveParty(user.getId());

        verify(lobbyPlayerRepository, times(1)).deleteLobbyPlayerById(existingLobbyPlayer.getId());

        ArgumentCaptor<Lobby> lobbyCaptor = ArgumentCaptor.forClass(Lobby.class);
        verify(lobbyRepository, times(1)).updateLobby(lobbyCaptor.capture());

        Lobby updatedLobby = lobbyCaptor.getValue();
        assertEquals(2, updatedLobby.getPlayerCount());
        assertEquals(LobbyStatus.AVAILABLE, updatedLobby.getStatus());
    }

    @Test
    void testLeavePartySuccessWhenLobbyDoesNotExist() {
        User user = createRandomUser();

        String lobbyId = randomUUID();
        LobbyPlayer existingLobbyPlayer = LobbyPlayer.builder()
                .id(randomUUID())
                .lobbyId(lobbyId)
                .playerId(user.getId())
                .points(50)
                .build();

        when(lobbyPlayerRepository.findValidLobbyPlayerByPlayerId(user.getId()))
                .thenReturn(Optional.of(existingLobbyPlayer));
        when(lobbyPlayerRepository.deleteLobbyPlayerById(existingLobbyPlayer.getId()))
                .thenReturn(true);
        when(lobbyRepository.findLobbyById(lobbyId)).thenReturn(Optional.empty());

        DuelException exception = assertThrows(DuelException.class, () -> partyManager.leaveParty(user.getId()));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getHttpStatus().orElseThrow());
        assertEquals("Something went wrong.", exception.getMessage());

        verify(lobbyPlayerRepository, times(1)).deleteLobbyPlayerById(existingLobbyPlayer.getId());
        verify(lobbyRepository, times(0)).updateLobby(any());
    }

    @Test
    void testLeavePartyDecreasesPlayerCountCorrectly() throws DuelException {
        User user = createRandomUser();

        String lobbyId = randomUUID();
        LobbyPlayer existingLobbyPlayer = LobbyPlayer.builder()
                .id(randomUUID())
                .lobbyId(lobbyId)
                .playerId(user.getId())
                .points(0)
                .build();

        Lobby lobby = Lobby.builder()
                .id(lobbyId)
                .joinCode("ABC123")
                .status(LobbyStatus.AVAILABLE)
                .expiresAt(Optional.of(OffsetDateTime.now().plusMinutes(30)))
                .playerCount(3)
                .winnerId(Optional.empty())
                .build();

        when(lobbyPlayerRepository.findValidLobbyPlayerByPlayerId(user.getId()))
                .thenReturn(Optional.of(existingLobbyPlayer));
        when(lobbyPlayerRepository.deleteLobbyPlayerById(existingLobbyPlayer.getId()))
                .thenReturn(true);
        when(lobbyRepository.findLobbyById(lobbyId)).thenReturn(Optional.of(lobby));

        partyManager.leaveParty(user.getId());

        ArgumentCaptor<Lobby> lobbyCaptor = ArgumentCaptor.forClass(Lobby.class);
        verify(lobbyRepository, times(1)).updateLobby(lobbyCaptor.capture());

        Lobby updatedLobby = lobbyCaptor.getValue();
        assertEquals(2, updatedLobby.getPlayerCount());
        assertEquals(LobbyStatus.AVAILABLE, updatedLobby.getStatus());
    }

    @Test
    void testLeavePartyFailureWhenUserNotInLobby() {
        User user = createRandomUser();

        when(lobbyPlayerRepository.findValidLobbyPlayerByPlayerId(user.getId())).thenReturn(Optional.empty());

        DuelException exception = assertThrows(DuelException.class, () -> partyManager.leaveParty(user.getId()));

        assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus().orElseThrow());
        assertEquals("You are not currently in a lobby.", exception.getMessage());

        verify(lobbyPlayerRepository, times(0)).deleteLobbyPlayerById(any());
        verify(lobbyRepository, times(0)).updateLobby(any());
    }

    @Test
    void testLeavePartyFailureWhenDeletionFails() {
        User user = createRandomUser();

        String lobbyId = randomUUID();
        LobbyPlayer existingLobbyPlayer = LobbyPlayer.builder()
                .id(randomUUID())
                .lobbyId(lobbyId)
                .playerId(user.getId())
                .points(50)
                .build();

        when(lobbyPlayerRepository.findValidLobbyPlayerByPlayerId(user.getId()))
                .thenReturn(Optional.of(existingLobbyPlayer));
        when(lobbyPlayerRepository.deleteLobbyPlayerById(existingLobbyPlayer.getId()))
                .thenReturn(false);

        DuelException exception = assertThrows(DuelException.class, () -> partyManager.leaveParty(user.getId()));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getHttpStatus().orElseThrow());
        assertEquals("Failed to leave lobby. Please try again.", exception.getMessage());

        verify(lobbyPlayerRepository, times(1)).deleteLobbyPlayerById(existingLobbyPlayer.getId());
        verify(lobbyRepository, times(0)).findLobbyById(any());
        verify(lobbyRepository, times(0)).updateLobby(any());
    }

    @Test
    void testLeavePartySetsLobbyToClosedWhenLastPlayerLeaves() throws DuelException {
        User user = createRandomUser();

        String lobbyId = randomUUID();
        LobbyPlayer existingLobbyPlayer = LobbyPlayer.builder()
                .id(randomUUID())
                .lobbyId(lobbyId)
                .playerId(user.getId())
                .points(0)
                .build();

        Lobby lobby = Lobby.builder()
                .id(lobbyId)
                .joinCode("ABC123")
                .status(LobbyStatus.AVAILABLE)
                .expiresAt(Optional.of(OffsetDateTime.now().plusMinutes(30)))
                .playerCount(1)
                .winnerId(Optional.empty())
                .build();

        when(lobbyPlayerRepository.findValidLobbyPlayerByPlayerId(user.getId()))
                .thenReturn(Optional.of(existingLobbyPlayer));
        when(lobbyPlayerRepository.deleteLobbyPlayerById(existingLobbyPlayer.getId()))
                .thenReturn(true);
        when(lobbyRepository.findLobbyById(lobbyId)).thenReturn(Optional.of(lobby));

        partyManager.leaveParty(user.getId());

        ArgumentCaptor<Lobby> lobbyCaptor = ArgumentCaptor.forClass(Lobby.class);
        verify(lobbyRepository, times(1)).updateLobby(lobbyCaptor.capture());

        Lobby updatedLobby = lobbyCaptor.getValue();
        assertEquals(0, updatedLobby.getPlayerCount());
        assertEquals(LobbyStatus.CLOSED, updatedLobby.getStatus());
    }

    @Test
    void testLeavePartyThrowsNonDuelException() {
        User user = createRandomUser();

        doThrow(new RuntimeException("Simulated db exception"))
                .when(lobbyPlayerRepository)
                .findValidLobbyPlayerByPlayerId(any());

        try {
            partyManager.leaveParty(user.getId());
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

        verify(lobbyPlayerRepository, times(1)).findValidLobbyPlayerByPlayerId(any());
        verify(lobbyPlayerRepository, never()).deleteLobbyPlayerById(any());
        verify(lobbyRepository, never()).updateLobby(any());
    }
}
