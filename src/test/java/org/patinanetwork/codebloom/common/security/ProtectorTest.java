package org.patinanetwork.codebloom.common.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.patinanetwork.codebloom.common.db.models.Session;
import org.patinanetwork.codebloom.common.db.models.user.User;
import org.patinanetwork.codebloom.common.db.repos.session.SessionRepository;
import org.patinanetwork.codebloom.common.db.repos.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class ProtectorTest {

    private final SessionRepository sessionRepository = mock(SessionRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final HttpServletRequest request = mock(HttpServletRequest.class);

    private Protector protector;

    @BeforeEach
    void setUp() {
        protector = new Protector(sessionRepository, userRepository);
    }

    @Test
    @DisplayName("validateSession rejects null cookies")
    void rejectsNullCookies() {
        when(request.getCookies()).thenReturn(null);

        ResponseStatusException exception =
                assertThrows(ResponseStatusException.class, () -> protector.validateSession(request));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        verify(sessionRepository, never()).getSessionById(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    @DisplayName("validateSession rejects when session_token cookie is missing")
    void rejectsMissingSessionTokenCookie() {
        when(request.getCookies()).thenReturn(new Cookie[] {new Cookie("other_cookie", "value")});

        ResponseStatusException exception =
                assertThrows(ResponseStatusException.class, () -> protector.validateSession(request));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
    }

    @Test
    @DisplayName("validateSession rejects empty session token")
    void rejectsEmptySessionToken() {
        when(request.getCookies()).thenReturn(new Cookie[] {new Cookie("session_token", "")});

        ResponseStatusException exception =
                assertThrows(ResponseStatusException.class, () -> protector.validateSession(request));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        verify(sessionRepository, never()).getSessionById(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    @DisplayName("validateSession rejects unknown session")
    void rejectsUnknownSession() {
        when(request.getCookies()).thenReturn(new Cookie[] {new Cookie("session_token", "missing-session")});
        when(sessionRepository.getSessionById("missing-session")).thenReturn(Optional.empty());

        ResponseStatusException exception =
                assertThrows(ResponseStatusException.class, () -> protector.validateSession(request));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
    }

    @Test
    @DisplayName("validateSession rejects expired session")
    void rejectsExpiredSession() {
        Session expiredSession = Session.builder()
                .id(Optional.of("expired-session"))
                .userId("user-1")
                .expiresAt(LocalDateTime.now().minusMinutes(1))
                .build();

        when(request.getCookies()).thenReturn(new Cookie[] {new Cookie("session_token", "expired-session")});
        when(sessionRepository.getSessionById("expired-session")).thenReturn(Optional.of(expiredSession));

        ResponseStatusException exception =
                assertThrows(ResponseStatusException.class, () -> protector.validateSession(request));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
    }

    @Test
    @DisplayName("validateSession rejects when user does not exist")
    void rejectsMissingUser() {
        Session validSession = Session.builder()
                .id(Optional.of("session-1"))
                .userId("missing-user")
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .build();

        when(request.getCookies()).thenReturn(new Cookie[] {new Cookie("session_token", "session-1")});
        when(sessionRepository.getSessionById("session-1")).thenReturn(Optional.of(validSession));
        when(userRepository.getUserById("missing-user")).thenReturn(Optional.empty());

        ResponseStatusException exception =
                assertThrows(ResponseStatusException.class, () -> protector.validateSession(request));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
    }

    @Test
    @DisplayName("validateSession returns auth object for valid session")
    void returnsAuthForValidSession() {
        Session validSession = Session.builder()
                .id(Optional.of("session-2"))
                .userId("user-2")
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .build();

        User user = User.builder()
                .id("user-2")
                .discordId("discord-2")
                .discordName("alice")
                .verifyKey("verify")
                .admin(false)
                .build();

        when(request.getCookies()).thenReturn(new Cookie[] {new Cookie("session_token", "session-2")});
        when(sessionRepository.getSessionById("session-2")).thenReturn(Optional.of(validSession));
        when(userRepository.getUserById("user-2")).thenReturn(Optional.of(user));

        AuthenticationObject auth = protector.validateSession(request);

        assertSame(user, auth.getUser());
        assertSame(validSession, auth.getSession());
    }

    @Test
    @DisplayName("validateAdminSession rejects non-admin user")
    void rejectsNonAdmin() {
        Session validSession = Session.builder()
                .id(Optional.of("session-3"))
                .userId("user-3")
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .build();

        User nonAdminUser = User.builder()
                .id("user-3")
                .discordId("discord-3")
                .discordName("bob")
                .verifyKey("verify")
                .admin(false)
                .build();

        when(request.getCookies()).thenReturn(new Cookie[] {new Cookie("session_token", "session-3")});
        when(sessionRepository.getSessionById("session-3")).thenReturn(Optional.of(validSession));
        when(userRepository.getUserById("user-3")).thenReturn(Optional.of(nonAdminUser));

        ResponseStatusException exception =
                assertThrows(ResponseStatusException.class, () -> protector.validateAdminSession(request));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
    }

    @Test
    @DisplayName("validateAdminSession returns auth object for admin user")
    void returnsAuthForAdmin() {
        Session validSession = Session.builder()
                .id(Optional.of("session-4"))
                .userId("user-4")
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .build();

        User adminUser = User.builder()
                .id("user-4")
                .discordId("discord-4")
                .discordName("carol")
                .verifyKey("verify")
                .admin(true)
                .build();

        when(request.getCookies()).thenReturn(new Cookie[] {new Cookie("session_token", "session-4")});
        when(sessionRepository.getSessionById("session-4")).thenReturn(Optional.of(validSession));
        when(userRepository.getUserById("user-4")).thenReturn(Optional.of(adminUser));

        AuthenticationObject auth = protector.validateAdminSession(request);

        assertSame(adminUser, auth.getUser());
        assertSame(validSession, auth.getSession());
    }
}
