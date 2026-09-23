package org.patinanetwork.codebloom.api.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javafaker.Faker;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.patinanetwork.codebloom.api.auth.body.EmailBody;
import org.patinanetwork.codebloom.common.db.models.Session;
import org.patinanetwork.codebloom.common.db.models.user.User;
import org.patinanetwork.codebloom.common.db.models.usertag.UserTag;
import org.patinanetwork.codebloom.common.db.repos.session.SessionRepository;
import org.patinanetwork.codebloom.common.db.repos.user.UserRepository;
import org.patinanetwork.codebloom.common.db.repos.usertag.UserTagRepository;
import org.patinanetwork.codebloom.common.email.client.codebloom.OfficialCodebloomEmailClient;
import org.patinanetwork.codebloom.common.email.error.EmailException;
import org.patinanetwork.codebloom.common.email.options.SendEmailOptions;
import org.patinanetwork.codebloom.common.email.template.ReactEmailTemplater;
import org.patinanetwork.codebloom.common.jwt.JWTClient;
import org.patinanetwork.codebloom.common.reporter.Reporter;
import org.patinanetwork.codebloom.common.schools.SchoolEnum;
import org.patinanetwork.codebloom.common.schools.magic.MagicLink;
import org.patinanetwork.codebloom.common.security.AuthenticationObject;
import org.patinanetwork.codebloom.common.security.Protector;
import org.patinanetwork.codebloom.common.simpleredis.SimpleRedis;
import org.patinanetwork.codebloom.common.simpleredis.SimpleRedisProvider;
import org.patinanetwork.codebloom.common.simpleredis.SimpleRedisSlot;
import org.patinanetwork.codebloom.common.url.ServerUrlUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.view.RedirectView;

public class AuthControllerTest {

    private final SessionRepository sessionRepository = mock(SessionRepository.class);
    private final Protector protector = mock(Protector.class);
    private final JWTClient jwtClient = mock(JWTClient.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final OfficialCodebloomEmailClient emailClient = mock(OfficialCodebloomEmailClient.class);
    private final ServerUrlUtils serverUrlUtils = mock(ServerUrlUtils.class);
    private final UserTagRepository userTagRepository = mock(UserTagRepository.class);
    private final Reporter reporter = mock(Reporter.class);
    private final ReactEmailTemplater reactEmailTemplater = mock(ReactEmailTemplater.class);
    private final SimpleRedis<Long> simpleRedis = mock(SimpleRedis.class);
    private final SimpleRedisProvider simpleRedisProvider = mock(SimpleRedisProvider.class);

    private AuthController authController;
    private Faker faker;

    @BeforeEach
    void setup() {
        when(simpleRedisProvider.select(SimpleRedisSlot.VERIFICATION_EMAIL_SENDING))
                .thenReturn(simpleRedis);
        this.authController = new AuthController(
                sessionRepository,
                protector,
                jwtClient,
                userRepository,
                emailClient,
                serverUrlUtils,
                userTagRepository,
                reporter,
                reactEmailTemplater,
                simpleRedisProvider);
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
                .leetcodeUsername(faker.name().username())
                .admin(false)
                .verifyKey(faker.crypto().md5())
                .build();
    }

    private Session createRandomSession(final String userId) {
        return Session.builder()
                .id(Optional.of(UUID.randomUUID().toString().replace("-", "")))
                .userId(userId)
                .expiresAt(LocalDateTime.now().plusDays(1))
                .build();
    }

    private AuthenticationObject createAuthenticationObject(final User user, final Session session) {
        return new AuthenticationObject(user, session);
    }

    @Test
    @DisplayName("Validate auth - happy path")
    void validateAuthHappyPath() {
        User user = createRandomUser();
        Session session = createRandomSession(user.getId());
        AuthenticationObject authObj = createAuthenticationObject(user, session);

        var response = authController.validateAuth(authObj);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        var apiResponder = response.getBody();
        assertNotNull(apiResponder);
        assertTrue(apiResponder.isSuccess());
        assertEquals("You are authenticated!", apiResponder.getMessage());
        assertNotNull(apiResponder.getPayload());
    }

    @Test
    @DisplayName("Logout - successful logout")
    void logoutHappyPath() {
        User user = createRandomUser();
        Session session = createRandomSession(user.getId());
        AuthenticationObject authObj = createAuthenticationObject(user, session);

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        when(protector.validateSession(request)).thenReturn(authObj);
        when(sessionRepository.deleteSessionById(session.getId().orElseThrow())).thenReturn(true);

        RedirectView redirectView = authController.logout(request, response);

        assertNotNull(redirectView);
        assertEquals("/login?success=true&message=You have been logged out!", redirectView.getUrl());

        verify(protector, times(1)).validateSession(request);
        verify(sessionRepository, times(1)).deleteSessionById(session.getId().orElseThrow());
    }

    @Test
    @DisplayName("Logout - session not found")
    void logoutSessionNotFound() {
        User user = createRandomUser();
        Session session = createRandomSession(user.getId());
        AuthenticationObject authObj = createAuthenticationObject(user, session);

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        when(protector.validateSession(request)).thenReturn(authObj);
        when(sessionRepository.deleteSessionById(session.getId().orElseThrow())).thenReturn(false);

        RedirectView redirectView = authController.logout(request, response);

        assertNotNull(redirectView);
        assertEquals("/login?success=false&message=You are not logged in.", redirectView.getUrl());

        verify(protector, times(1)).validateSession(request);
        verify(sessionRepository, times(1)).deleteSessionById(session.getId().orElseThrow());
    }

    @Test
    @DisplayName("Logout - not authenticated")
    void logoutNotAuthenticated() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        when(protector.validateSession(request)).thenThrow(new RuntimeException("Not authenticated"));

        RedirectView redirectView = authController.logout(request, response);

        assertNotNull(redirectView);
        assertEquals("/login?success=false&message=You are not logged in.", redirectView.getUrl());

        verify(protector, times(1)).validateSession(request);
        verify(sessionRepository, times(0)).deleteSessionById(any());
    }

    @Test
    @DisplayName("Logout all - successful logout from all devices")
    void logoutAllHappyPath() {
        User user = createRandomUser();
        Session session = createRandomSession(user.getId());
        AuthenticationObject authObj = createAuthenticationObject(user, session);

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        when(protector.validateSession(request)).thenReturn(authObj);
        when(sessionRepository.deleteSessionsByUserId(user.getId())).thenReturn(true);

        RedirectView redirectView = authController.logoutAll(request, response);

        assertNotNull(redirectView);
        assertEquals("/login?success=true&message=You have been logged out from all devices!", redirectView.getUrl());

        verify(protector, times(1)).validateSession(request);
        verify(sessionRepository, times(1)).deleteSessionsByUserId(user.getId());
    }

    @Test
    @DisplayName("Logout all - not authenticated")
    void logoutAllNotAuthenticated() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        when(protector.validateSession(request)).thenThrow(new RuntimeException("Not authenticated"));

        RedirectView redirectView = authController.logoutAll(request, response);

        assertNotNull(redirectView);
        assertEquals("/login?success=false&message=You are not logged in.", redirectView.getUrl());

        verify(protector, times(1)).validateSession(request);
        verify(sessionRepository, times(0)).deleteSessionsByUserId(any());
    }

    @Test
    @DisplayName("Enroll school - unsupported email domain")
    void enrollSchoolUnsupportedDomain() {
        User user = createRandomUser();
        Session session = createRandomSession(user.getId());
        AuthenticationObject authObj = createAuthenticationObject(user, session);

        EmailBody emailBody = new EmailBody("test@unsupported.com");

        ResponseStatusException exception =
                assertThrows(ResponseStatusException.class, () -> authController.enrollSchool(emailBody, authObj));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertNotNull(exception.getReason());
        assertTrue(exception.getReason().contains("not part of our supported schools domains"));
    }

    @Test
    @DisplayName("Enroll school - rate limited")
    void enrollSchoolRateLimited() {
        User user = createRandomUser();
        Session session = createRandomSession(user.getId());
        AuthenticationObject authObj = createAuthenticationObject(user, session);

        EmailBody emailBody = new EmailBody("test@stu-mail.hunter.cuny.edu");

        when(simpleRedis.containsKey(user.getId())).thenReturn(true);
        when(simpleRedis.get(user.getId())).thenReturn(System.currentTimeMillis());

        ResponseStatusException exception =
                assertThrows(ResponseStatusException.class, () -> authController.enrollSchool(emailBody, authObj));

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, exception.getStatusCode());
        assertNotNull(exception.getReason());
        assertTrue(exception.getReason().contains("Please try again in"));

        verify(simpleRedis, times(1)).containsKey(user.getId());
    }

    @Test
    @DisplayName("Enroll school - email send failure")
    void enrollSchoolEmailSendFailure() throws Exception {
        User user = createRandomUser();
        Session session = createRandomSession(user.getId());
        AuthenticationObject authObj = createAuthenticationObject(user, session);

        EmailBody emailBody = new EmailBody("test@stu-mail.hunter.cuny.edu");

        when(jwtClient.encode(any(MagicLink.class), any(Duration.class))).thenReturn("mock-token");
        when(serverUrlUtils.getUrl()).thenReturn("http://localhost:8080");
        when(reactEmailTemplater.schoolEmailTemplate(any())).thenReturn("<html>Template</html>");
        doThrow(new EmailException("Failed to send email")).when(emailClient).sendMessage(any(SendEmailOptions.class));

        ResponseStatusException exception =
                assertThrows(ResponseStatusException.class, () -> authController.enrollSchool(emailBody, authObj));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatusCode());
        assertEquals("Failed to send email.", exception.getReason());

        verify(emailClient, times(1)).sendMessage(any(SendEmailOptions.class));
    }

    @Test
    @DisplayName("Enroll school - happy path")
    void enrollSchoolHappyPath() throws Exception {
        User user = createRandomUser();
        Session session = createRandomSession(user.getId());
        AuthenticationObject authObj = createAuthenticationObject(user, session);

        EmailBody emailBody = new EmailBody("test@stu-mail.hunter.cuny.edu");

        when(simpleRedis.containsKey(user.getId())).thenReturn(false);
        when(jwtClient.encode(any(MagicLink.class), any(Duration.class))).thenReturn("mock-token");
        when(serverUrlUtils.getUrl()).thenReturn("http://localhost:8080");
        when(reactEmailTemplater.schoolEmailTemplate(any())).thenReturn("<html>Template</html>");

        var response = authController.enrollSchool(emailBody, authObj);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        var apiResponder = response.getBody();
        assertNotNull(apiResponder);
        assertTrue(apiResponder.isSuccess());
        assertEquals("Magic link sent! Check your school inbox to continue.", apiResponder.getMessage());

        verify(emailClient, times(1)).sendMessage(any(SendEmailOptions.class));
        verify(simpleRedis, times(1)).put(eq(user.getId()), any(Long.class));
    }

    @Test
    @DisplayName("Verify school email - not authenticated")
    void verifySchoolEmailNotAuthenticated() {
        HttpServletRequest request = mock(HttpServletRequest.class);

        when(protector.validateSession(request)).thenThrow(new RuntimeException("Not authenticated"));

        RedirectView redirectView = authController.verifySchoolEmail(request);

        assertNotNull(redirectView);
        assertEquals("/login?success=false&message=You are not logged in.", redirectView.getUrl());

        verify(protector, times(1)).validateSession(request);
    }

    @Test
    @DisplayName("Verify school email - invalid token")
    void verifySchoolEmailInvalidToken() throws Exception {
        User user = createRandomUser();
        Session session = createRandomSession(user.getId());
        AuthenticationObject authObj = createAuthenticationObject(user, session);

        HttpServletRequest request = mock(HttpServletRequest.class);

        when(protector.validateSession(request)).thenReturn(authObj);
        when(request.getParameter("state")).thenReturn("invalid-token");
        when(jwtClient.decode("invalid-token", MagicLink.class)).thenThrow(new RuntimeException("Invalid token"));

        RedirectView redirectView = authController.verifySchoolEmail(request);

        assertNotNull(redirectView);
        assertEquals("/settings?success=false&message=Invalid or expired token", redirectView.getUrl());

        verify(protector, times(1)).validateSession(request);
        verify(jwtClient, times(1)).decode("invalid-token", MagicLink.class);
    }

    @Test
    @DisplayName("Verify school email - user ID mismatch")
    void verifySchoolEmailUserIdMismatch() throws Exception {
        User user = createRandomUser();
        Session session = createRandomSession(user.getId());
        AuthenticationObject authObj = createAuthenticationObject(user, session);

        HttpServletRequest request = mock(HttpServletRequest.class);
        MagicLink magicLink = new MagicLink("test@stu-mail.hunter.cuny.edu", "different-user-id");

        when(protector.validateSession(request)).thenReturn(authObj);
        when(request.getParameter("state")).thenReturn("valid-token");
        when(jwtClient.decode("valid-token", MagicLink.class)).thenReturn(magicLink);

        RedirectView redirectView = authController.verifySchoolEmail(request);

        assertNotNull(redirectView);
        assertEquals("/settings?success=false&message=ID does not match current user", redirectView.getUrl());

        verify(protector, times(1)).validateSession(request);
        verify(jwtClient, times(1)).decode("valid-token", MagicLink.class);
    }

    @Test
    @DisplayName("Verify school email - happy path")
    void verifySchoolEmailHappyPath() throws Exception {
        User user = createRandomUser();
        Session session = createRandomSession(user.getId());
        AuthenticationObject authObj = createAuthenticationObject(user, session);

        HttpServletRequest request = mock(HttpServletRequest.class);
        MagicLink magicLink = new MagicLink("test@stu-mail.hunter.cuny.edu", user.getId());

        when(protector.validateSession(request)).thenReturn(authObj);
        when(request.getParameter("state")).thenReturn("valid-token");
        when(jwtClient.decode("valid-token", MagicLink.class)).thenReturn(magicLink);
        when(userRepository.updateUser(any(User.class))).thenReturn(true);

        RedirectView redirectView = authController.verifySchoolEmail(request);

        assertNotNull(redirectView);
        assertEquals("/settings?success=true&message=The email has been verified!", redirectView.getUrl());

        verify(protector, times(1)).validateSession(request);
        verify(jwtClient, times(1)).decode("valid-token", MagicLink.class);
        verify(userRepository, times(1)).updateUser(any(User.class));
        verify(userTagRepository, times(1)).createTag(any());
    }

    @ParameterizedTest
    @EnumSource(SchoolEnum.class)
    void enrollAndVerifyEverySchoolAlias(final SchoolEnum school) throws Exception {
        User user = createRandomUser();
        Session session = createRandomSession(user.getId());
        AuthenticationObject auth = createAuthenticationObject(user, session);
        String email = "student03" + school.getEmailDomain().toUpperCase(java.util.Locale.ROOT);
        when(jwtClient.encode(any(MagicLink.class), any(Duration.class))).thenReturn("alias-token");
        when(serverUrlUtils.getUrl()).thenReturn("http://localhost:8080");
        when(reactEmailTemplater.schoolEmailTemplate(any())).thenReturn("<html>Verify</html>");

        assertEquals(
                HttpStatus.OK,
                authController.enrollSchool(new EmailBody(email), auth).getStatusCode());
        ArgumentCaptor<SendEmailOptions> sent = ArgumentCaptor.forClass(SendEmailOptions.class);
        verify(emailClient).sendMessage(sent.capture());
        assertEquals(email, sent.getValue().getRecipientEmail());

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(protector.validateSession(request)).thenReturn(auth);
        when(request.getParameter("state")).thenReturn("alias-token");
        when(jwtClient.decode("alias-token", MagicLink.class)).thenReturn(new MagicLink(email, user.getId()));
        when(userRepository.updateUser(any(User.class))).thenReturn(true);
        assertEquals(
                "/settings?success=true&message=The email has been verified!",
                authController.verifySchoolEmail(request).getUrl());
        ArgumentCaptor<UserTag> tag = ArgumentCaptor.forClass(UserTag.class);
        verify(userTagRepository).createTag(tag.capture());
        assertEquals(school.getInternalTag(), tag.getValue().getTag());
    }

    @ParameterizedTest
    @ValueSource(strings = {"student@login.cuny.edu", "student@CUNY.EDU"})
    void sharedCunyAddressesExplainHowToUseAliases(final String email) {
        User user = createRandomUser();
        AuthenticationObject auth = createAuthenticationObject(user, createRandomSession(user.getId()));
        var error = assertThrows(
                ResponseStatusException.class, () -> authController.enrollSchool(new EmailBody(email), auth));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        assertTrue(error.getReason().contains("college email alias"));
        org.mockito.Mockito.verifyNoInteractions(emailClient, userRepository, userTagRepository);
    }

    @ParameterizedTest
    @ValueSource(strings = {"student@stu-mail.hunter.cuny.edu.evil.com", "student@stu-mail.unknown.cuny.edu"})
    void rejectsUnrecognizedAliasDomains(final String email) {
        User user = createRandomUser();
        AuthenticationObject auth = createAuthenticationObject(user, createRandomSession(user.getId()));
        assertThrows(ResponseStatusException.class, () -> authController.enrollSchool(new EmailBody(email), auth));
        org.mockito.Mockito.verifyNoInteractions(emailClient);
    }
}
