package org.patinanetwork.codebloom.api.duel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javafaker.Faker;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.patinanetwork.codebloom.api.duel.body.JoinLobbyBody;
import org.patinanetwork.codebloom.common.components.duel.DuelException;
import org.patinanetwork.codebloom.common.components.duel.DuelManager;
import org.patinanetwork.codebloom.common.components.duel.PartyManager;
import org.patinanetwork.codebloom.common.db.models.lobby.Lobby;
import org.patinanetwork.codebloom.common.db.models.lobby.LobbyStatus;
import org.patinanetwork.codebloom.common.db.models.user.User;
import org.patinanetwork.codebloom.common.db.repos.lobby.LobbyRepository;
import org.patinanetwork.codebloom.common.dto.ApiResponder;
import org.patinanetwork.codebloom.common.dto.lobby.DuelData;
import org.patinanetwork.codebloom.common.security.AuthenticationObject;
import org.patinanetwork.codebloom.common.time.StandardizedOffsetDateTime;
import org.patinanetwork.codebloom.common.utils.duel.PartyCodeGenerator;
import org.patinanetwork.codebloom.common.utils.sse.SseWrapper;
import org.patinanetwork.codebloom.scheduled.pg.handler.LobbyNotifyHandler;
import org.patinanetwork.codebloom.utilities.exception.ValidationException;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
public class DuelControllerTest {

    private final DuelController duelController;
    private final Faker faker;

    private DuelManager duelManager = mock(DuelManager.class);
    private PartyManager partyManager = mock(PartyManager.class);
    private LobbyRepository lobbyRepository = mock(LobbyRepository.class);
    private LobbyNotifyHandler lobbyNotifyHandler = mock(LobbyNotifyHandler.class);

    public DuelControllerTest() {
        this.duelController = new DuelController(duelManager, partyManager, lobbyRepository, lobbyNotifyHandler);
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

    private AuthenticationObject createAuthenticationObject(final User user) {
        return new AuthenticationObject(user, null);
    }

    @Test
    @DisplayName("Join lobby - invalid code length")
    void joinPartyIncorrectLengthCode() {
        var joinPartyBody = JoinLobbyBody.builder().partyCode("ABC12").build();

        User user = createRandomUser();
        AuthenticationObject authObj = createAuthenticationObject(user);

        var ex = assertThrows(ValidationException.class, () -> {
            duelController.joinParty(authObj, joinPartyBody);
        });

        assertEquals("Lobby code must be exactly 6 characters.", ex.getMessage());
    }

    @Test
    @DisplayName("Join lobby - empty code")
    void joinLobbyEmptyCode() {
        var joinPartyBody = JoinLobbyBody.builder().partyCode("").build();

        User user = createRandomUser();
        AuthenticationObject authObj = createAuthenticationObject(user);

        var ex = assertThrows(ValidationException.class, () -> {
            duelController.joinParty(authObj, joinPartyBody);
        });

        assertEquals("Lobby code may not be null or empty.", ex.getMessage());
    }

    @Test
    @DisplayName("Join lobby - null code")
    void joinLobbyNullCode() {
        var joinPartyBody = JoinLobbyBody.builder().partyCode(null).build();

        User user = createRandomUser();
        AuthenticationObject authObj = createAuthenticationObject(user);

        var ex = assertThrows(ValidationException.class, () -> {
            duelController.joinParty(authObj, joinPartyBody);
        });

        assertEquals("Lobby code may not be null or empty.", ex.getMessage());
    }

    @Test
    void testJoinPartyPartyManagerFailed() {
        var joinPartyBody = JoinLobbyBody.builder().partyCode("ABC123").build();

        User user = createRandomUser();
        AuthenticationObject authObj = createAuthenticationObject(user);

        try {
            doThrow(new DuelException(HttpStatus.NOT_FOUND, "The party with the given code cannot be found."))
                    .when(partyManager)
                    .joinParty(eq(user.getId()), eq("ABC123"));
        } catch (DuelException _) {
        }

        ResponseStatusException exception =
                assertThrows(ResponseStatusException.class, () -> duelController.joinParty(authObj, joinPartyBody));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("The party with the given code cannot be found.", exception.getReason());

        try {
            verify(partyManager, times(1)).joinParty(eq(user.getId()), eq("ABC123"));
        } catch (DuelException e) {
            fail(e);
        }
    }

    @Test
    void testJoinPartyHappyPath() {
        var joinPartyBody = JoinLobbyBody.builder().partyCode("ABC123").build();

        User user = createRandomUser();
        AuthenticationObject authObj = createAuthenticationObject(user);

        try {
            doNothing().when(partyManager).joinParty(eq(user.getId()), eq("ABC123"));
        } catch (DuelException _) {
        }

        var response = duelController.joinParty(authObj, joinPartyBody);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        var apiResponder = response.getBody();
        assertTrue(apiResponder.isSuccess());
        assertEquals("Party successfully joined!", apiResponder.getMessage());

        try {
            verify(partyManager, times(1)).joinParty(eq(user.getId()), eq("ABC123"));
        } catch (DuelException e) {
            fail(e);
        }
    }

    @Test
    void testLeavePartyPartyManagerFailed() {
        User user = createRandomUser();
        AuthenticationObject authObj = createAuthenticationObject(user);

        try {
            doThrow(new DuelException(HttpStatus.NOT_FOUND, "You are not currently in a lobby."))
                    .when(partyManager)
                    .leaveParty(eq(user.getId()));
        } catch (DuelException _) {
        }

        ResponseStatusException exception =
                assertThrows(ResponseStatusException.class, () -> duelController.leaveParty(authObj));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("You are not currently in a lobby.", exception.getReason());

        try {
            verify(partyManager, times(1)).leaveParty(eq(user.getId()));
        } catch (DuelException e) {
            fail(e);
        }
    }

    @Test
    void testLeavePartyHappyPath() {
        User user = createRandomUser();
        AuthenticationObject authObj = createAuthenticationObject(user);

        try {
            doNothing().when(partyManager).leaveParty(eq(user.getId()));
        } catch (DuelException _) {
        }

        var response = duelController.leaveParty(authObj);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        var apiResponder = response.getBody();
        assertTrue(apiResponder.isSuccess());
        assertEquals("Successfully left the party.", apiResponder.getMessage());

        try {
            verify(partyManager, times(1)).leaveParty(eq(user.getId()));
        } catch (DuelException e) {
            fail(e);
        }
    }

    @Test
    void testCreatePartyPartyManagerFailed() {
        User user = createRandomUser();
        AuthenticationObject authObj = createAuthenticationObject(user);

        try {
            doThrow(new DuelException(
                            HttpStatus.CONFLICT,
                            "You are already in a lobby. Please leave your current lobby before creating a new one."))
                    .when(partyManager)
                    .createParty(eq(user.getId()));
        } catch (DuelException _) {
        }

        ResponseStatusException exception =
                assertThrows(ResponseStatusException.class, () -> duelController.createParty(authObj));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals(
                "You are already in a lobby. Please leave your current lobby before creating a new one.",
                exception.getReason());

        try {
            verify(partyManager, times(1)).createParty(eq(user.getId()));
        } catch (DuelException e) {
            fail(e);
        }
    }

    @Test
    void testCreatePartyHappyPath() {
        User user = createRandomUser();
        AuthenticationObject authObj = createAuthenticationObject(user);

        try {
            when(partyManager.createParty(eq(user.getId()))).thenReturn("ABC123");
        } catch (DuelException _) {
        }

        var response = duelController.createParty(authObj);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        var apiResponder = response.getBody();
        assertTrue(apiResponder.isSuccess());
        assertEquals("Lobby created successfully!", apiResponder.getMessage());
        assertNotNull(apiResponder.getPayload());
        assertEquals("ABC123", apiResponder.getPayload().getCode());

        try {
            verify(partyManager, times(1)).createParty(eq(user.getId()));
        } catch (DuelException e) {
            fail(e);
        }
    }

    @Test
    @DisplayName("SSE endpoint - lobby does not exist")
    void getDuelDataLobbyDoesNotExist() throws DuelException {
        when(lobbyRepository.findActiveLobbyByJoinCode(any())).thenReturn(Optional.empty());
        when(lobbyRepository.findAvailableLobbyByJoinCode(any())).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            duelController.getDuelData(null);
        });

        assertEquals(HttpStatus.NOT_FOUND.value(), exception.getStatusCode().value());
        assertEquals("A duel/party with the given code cannot be found.", exception.getReason());

        verify(lobbyRepository, times(1)).findActiveLobbyByJoinCode(any());
        verify(lobbyRepository, times(1)).findAvailableLobbyByJoinCode(any());
        verify(duelManager, times(0)).generateDuelData(any());
        verify(lobbyNotifyHandler, times(0)).register(any(), any());
    }

    @Test
    @DisplayName("SSE endpoint - active lobby exists")
    void getDuelDataPlayerInActiveLobby() {
        String lobbyId = randomUUID();
        Lobby activeLobby = Lobby.builder()
                .id(lobbyId)
                .joinCode(PartyCodeGenerator.generateCode())
                .expiresAt(Optional.of(StandardizedOffsetDateTime.now().plus(1, ChronoUnit.HOURS)))
                .status(LobbyStatus.ACTIVE)
                .playerCount(2)
                .build();

        when(lobbyRepository.findActiveLobbyByJoinCode(eq(activeLobby.getJoinCode())))
                .thenReturn(Optional.of(activeLobby));
        doNothing().when(lobbyNotifyHandler).register(eq(lobbyId), any());

        SseWrapper<ApiResponder<DuelData>> result = duelController.getDuelData(activeLobby.getJoinCode());

        assertNotNull(result);
        verify(lobbyRepository, times(1)).findActiveLobbyByJoinCode(activeLobby.getJoinCode());
        verify(lobbyRepository, times(0)).findAvailableLobbyByJoinCode(activeLobby.getJoinCode());
        verify(lobbyNotifyHandler, times(1)).register(eq(lobbyId), any());
    }

    @Test
    @DisplayName("SSE endpoint - available lobby exists")
    void getDuelDataAvailableLobbyExists() {
        String lobbyId = randomUUID();
        Lobby availableLobby = Lobby.builder()
                .id(lobbyId)
                .joinCode(PartyCodeGenerator.generateCode())
                .expiresAt(Optional.of(StandardizedOffsetDateTime.now().plus(1, ChronoUnit.HOURS)))
                .status(LobbyStatus.AVAILABLE)
                .playerCount(1)
                .build();

        when(lobbyRepository.findActiveLobbyByJoinCode(eq(availableLobby.getJoinCode())))
                .thenReturn(Optional.empty());
        when(lobbyRepository.findAvailableLobbyByJoinCode(eq(availableLobby.getJoinCode())))
                .thenReturn(Optional.of(availableLobby));
        doNothing().when(lobbyNotifyHandler).register(eq(lobbyId), any());

        SseWrapper<ApiResponder<DuelData>> result = duelController.getDuelData(availableLobby.getJoinCode());

        assertNotNull(result);
        verify(lobbyRepository, times(1)).findActiveLobbyByJoinCode(availableLobby.getJoinCode());
        verify(lobbyRepository, times(1)).findAvailableLobbyByJoinCode(availableLobby.getJoinCode());
        verify(lobbyNotifyHandler, times(1)).register(eq(lobbyId), any());
    }

    @Test
    void testStartDuelDuelManagerFailed() {
        User user = createRandomUser();
        AuthenticationObject authObj = createAuthenticationObject(user);

        try {
            doThrow(new DuelException(HttpStatus.INTERNAL_SERVER_ERROR, "This is an example duel exception."))
                    .when(duelManager)
                    .startDuel(eq(user.getId()), eq(user.isAdmin()));
        } catch (DuelException _) {
        }

        ResponseStatusException exception =
                assertThrows(ResponseStatusException.class, () -> duelController.startDuel(authObj));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatusCode());
        assertEquals("This is an example duel exception.", exception.getReason());

        try {
            verify(duelManager, times(1)).startDuel(any(), eq(false));
        } catch (DuelException e) {
            fail(e);
        }
    }

    @Test
    void testStartDuelHappyPath() {
        User user = createRandomUser();
        AuthenticationObject authObj = createAuthenticationObject(user);

        try {
            doNothing().when(duelManager).startDuel(eq(user.getId()), eq(user.isAdmin()));
        } catch (DuelException _) {
        }

        var response = duelController.startDuel(authObj);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        var apiResponder = response.getBody();
        assertTrue(apiResponder.isSuccess());
    }

    @Test
    void testEndDuelActivePartyForPlayerCannotBeFound() {
        User user = createRandomUser();
        AuthenticationObject authObj = createAuthenticationObject(user);
        when(lobbyRepository.findActiveLobbyByLobbyPlayerPlayerId(user.getId())).thenReturn(Optional.empty());

        ResponseStatusException exception =
                assertThrows(ResponseStatusException.class, () -> duelController.endDuel(authObj));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("Player is not currently in a duel.", exception.getReason());

        verify(lobbyRepository, times(1)).findActiveLobbyByLobbyPlayerPlayerId(eq(user.getId()));

        try {
            verify(duelManager, times(0)).endDuel(any(), eq(user.isAdmin()));
        } catch (DuelException e) {
            fail(e);
        }
    }

    @Test
    void testEndDuelDuelManagerFailed() {
        User user = createRandomUser();
        AuthenticationObject authObj = createAuthenticationObject(user);

        Lobby lobby = Lobby.builder()
                .id(UUID.randomUUID().toString())
                .joinCode(PartyCodeGenerator.generateCode())
                .status(LobbyStatus.AVAILABLE)
                .expiresAt(Optional.of(OffsetDateTime.now().plusMinutes(30)))
                .playerCount(3)
                .build();

        when(lobbyRepository.findActiveLobbyByLobbyPlayerPlayerId(user.getId())).thenReturn(Optional.of(lobby));

        try {
            doThrow(new DuelException(HttpStatus.INTERNAL_SERVER_ERROR, "This is an example duel exception."))
                    .when(duelManager)
                    .endDuel(eq(lobby.getId()), eq(user.isAdmin()));
        } catch (DuelException _) {
        }

        ResponseStatusException exception =
                assertThrows(ResponseStatusException.class, () -> duelController.endDuel(authObj));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatusCode());
        assertEquals("This is an example duel exception.", exception.getReason());

        verify(lobbyRepository, times(1)).findActiveLobbyByLobbyPlayerPlayerId(eq(user.getId()));

        try {
            verify(duelManager, times(1)).endDuel(any(), eq(user.isAdmin()));
        } catch (DuelException e) {
            fail(e);
        }
    }

    @Test
    void testGetPartyOrDuelCodeForUserDuelManagerFailed() {
        User user = createRandomUser();
        AuthenticationObject authObj = createAuthenticationObject(user);

        try {
            doThrow(new DuelException(HttpStatus.INTERNAL_SERVER_ERROR, "This is an example duel exception."))
                    .when(duelManager)
                    .getLobbyByUserId(eq(user.getId()));
        } catch (DuelException _) {
        }

        ResponseStatusException exception =
                assertThrows(ResponseStatusException.class, () -> duelController.getPartyOrDuelCodeForUser(authObj));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatusCode());
        assertEquals("This is an example duel exception.", exception.getReason());

        try {
            verify(duelManager, times(1)).getLobbyByUserId(any());
        } catch (DuelException e) {
            fail(e);
        }
    }

    @Test
    void testGetPartyOrDuelCodeForUserHappyPath() {
        User user = createRandomUser();
        AuthenticationObject authObj = createAuthenticationObject(user);

        try {
            when(duelManager.getLobbyByUserId(eq(user.getId())))
                    .thenReturn(Lobby.builder().build());
        } catch (DuelException _) {
        }

        var response = duelController.getPartyOrDuelCodeForUser(authObj);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        var apiResponder = response.getBody();
        assertTrue(apiResponder.isSuccess());

        try {
            verify(duelManager, times(1)).getLobbyByUserId(any());
        } catch (DuelException e) {
            fail(e);
        }
    }

    @Test
    void testProcessSolvedProblemsDuelManagerFailed() {
        User user = createRandomUser();
        AuthenticationObject authObj = createAuthenticationObject(user);

        try {
            when(duelManager.getDuelByUserId(eq(user.getId())))
                    .thenReturn(Lobby.builder().build());
        } catch (DuelException e) {
            fail(e);
        }

        try {
            doThrow(new DuelException(HttpStatus.INTERNAL_SERVER_ERROR, "This is an example duel exception."))
                    .when(duelManager)
                    .processSubmissions(eq(user), any());
        } catch (DuelException _) {
        }

        ResponseStatusException exception =
                assertThrows(ResponseStatusException.class, () -> duelController.processSolvedProblemsInDuel(authObj));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatusCode());
        assertEquals("This is an example duel exception.", exception.getReason());

        try {
            verify(duelManager, times(1)).processSubmissions(eq(user), any());
        } catch (DuelException e) {
            fail(e);
        }
    }

    @Test
    void testProcessSolvedProblemsHappyPath() {
        User user = createRandomUser();
        AuthenticationObject authObj = createAuthenticationObject(user);

        try {
            when(duelManager.getDuelByUserId(eq(user.getId())))
                    .thenReturn(Lobby.builder().build());
        } catch (DuelException e) {
            fail(e);
        }

        var response = duelController.processSolvedProblemsInDuel(authObj);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        var apiResponder = response.getBody();
        assertTrue(apiResponder.isSuccess());

        try {
            verify(duelManager, times(1)).processSubmissions(eq(user), any());
        } catch (DuelException e) {
            fail(e);
        }
    }
}
