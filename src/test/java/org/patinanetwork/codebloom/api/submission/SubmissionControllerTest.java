package org.patinanetwork.codebloom.api.submission;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.patinanetwork.codebloom.api.submission.body.LeetcodeUsernameObject;
import org.patinanetwork.codebloom.common.db.models.potd.POTD;
import org.patinanetwork.codebloom.common.db.models.question.QuestionWithUser;
import org.patinanetwork.codebloom.common.db.models.user.User;
import org.patinanetwork.codebloom.common.db.repos.potd.POTDRepository;
import org.patinanetwork.codebloom.common.db.repos.question.QuestionRepository;
import org.patinanetwork.codebloom.common.db.repos.user.UserRepository;
import org.patinanetwork.codebloom.common.dto.ApiResponder;
import org.patinanetwork.codebloom.common.dto.Empty;
import org.patinanetwork.codebloom.common.dto.potd.PotdDto;
import org.patinanetwork.codebloom.common.dto.question.QuestionWithUserDto;
import org.patinanetwork.codebloom.common.leetcode.models.LeetcodeSubmission;
import org.patinanetwork.codebloom.common.leetcode.models.UserProfile;
import org.patinanetwork.codebloom.common.leetcode.throttled.ThrottledLeetcodeClient;
import org.patinanetwork.codebloom.common.security.AuthenticationObject;
import org.patinanetwork.codebloom.common.simpleredis.SimpleRedis;
import org.patinanetwork.codebloom.common.simpleredis.SimpleRedisProvider;
import org.patinanetwork.codebloom.common.simpleredis.SimpleRedisSlot;
import org.patinanetwork.codebloom.common.submissions.SubmissionsHandler;
import org.patinanetwork.codebloom.common.submissions.object.AcceptedSubmission;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

public class SubmissionControllerTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final SimpleRedis<Long> simpleRedis = mock(SimpleRedis.class);
    private final SimpleRedisProvider simpleRedisProvider = mock(SimpleRedisProvider.class);
    private final ThrottledLeetcodeClient leetcodeClient = mock(ThrottledLeetcodeClient.class);
    private final SubmissionsHandler submissionsHandler = mock(SubmissionsHandler.class);
    private final QuestionRepository questionRepository = mock(QuestionRepository.class);
    private final POTDRepository potdRepository = mock(POTDRepository.class);

    private final HttpServletRequest request = mock(HttpServletRequest.class);

    private SubmissionController submissionController;

    @BeforeEach
    void setUp() {
        when(simpleRedisProvider.select(SimpleRedisSlot.SUBMISSION_REFRESH)).thenReturn(simpleRedis);

        this.submissionController = new SubmissionController(
                userRepository,
                simpleRedisProvider,
                leetcodeClient,
                submissionsHandler,
                questionRepository,
                potdRepository);
    }

    @Test
    void testGetVerificationKeySuccess() {
        AuthenticationObject auth = mock(AuthenticationObject.class);
        User user = mock(User.class);

        when(auth.getUser()).thenReturn(user);
        when(user.getVerifyKey()).thenReturn("verify-123");

        ResponseEntity<ApiResponder<String>> response = submissionController.getVerificationKey(auth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertEquals(
                "Successfully retreived authentication key", response.getBody().getMessage());
        assertEquals("verify-123", response.getBody().getPayload());

        verify(auth).getUser();
        verify(user).getVerifyKey();
    }

    @Test
    void testSetLeetcodeUsernameSuccess() {
        AuthenticationObject auth = mock(AuthenticationObject.class);
        User user = mock(User.class);
        LeetcodeUsernameObject body = mock(LeetcodeUsernameObject.class);
        UserProfile profile = mock(UserProfile.class);

        when(auth.getUser()).thenReturn(user);

        when(user.getLeetcodeUsername()).thenReturn(null);
        when(user.getVerifyKey()).thenReturn("verify-123");

        when(body.getLeetcodeUsername()).thenReturn("leetcodeUser");

        when(leetcodeClient.getUserProfileFast("leetcodeUser")).thenReturn(profile);
        when(profile.getAboutMe()).thenReturn("verify-123");
        when(profile.getUserAvatar()).thenReturn("avatar-url");

        when(userRepository.userExistsByLeetcodeUsername("leetcodeUser")).thenReturn(false);

        ResponseEntity<ApiResponder<Empty>> response = submissionController.setLeetcodeUsername(auth, body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertEquals("Leetcode username has been set!", response.getBody().getMessage());

        verify(user).setLeetcodeUsername("leetcodeUser");
        verify(user).setProfileUrl("avatar-url");
        verify(userRepository).updateUser(user);
    }

    @Test
    void testSetLeetcodeUsernameFailure() {
        AuthenticationObject auth = mock(AuthenticationObject.class);
        User user = mock(User.class);
        LeetcodeUsernameObject body = mock(LeetcodeUsernameObject.class);

        when(auth.getUser()).thenReturn(user);
        when(user.getLeetcodeUsername()).thenReturn("existing");

        ResponseStatusException ex =
                assertThrows(ResponseStatusException.class, () -> submissionController.setLeetcodeUsername(auth, body));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    void testCheckLatestSubmissionsSuccess() {
        AuthenticationObject auth = mock(AuthenticationObject.class);
        User user = mock(User.class);

        List<LeetcodeSubmission> leetcodeSubs = List.of(mock(LeetcodeSubmission.class));
        ArrayList<AcceptedSubmission> acceptedSubs = new ArrayList<>(List.of(mock(AcceptedSubmission.class)));

        when(auth.getUser()).thenReturn(user);

        when(user.getLeetcodeUsername()).thenReturn("leetcodeUser");
        when(user.getId()).thenReturn("abcdefg123456");

        when(simpleRedis.containsKey("abcdefg123456")).thenReturn(false);

        when(leetcodeClient.findSubmissionsByUsernameFast("leetcodeUser", 20)).thenReturn(leetcodeSubs);

        when(submissionsHandler.handleSubmissions(leetcodeSubs, user, true)).thenReturn(acceptedSubs);

        ResponseEntity<ApiResponder<ArrayList<AcceptedSubmission>>> response =
                submissionController.checkLatestSubmissions(auth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertEquals(acceptedSubs, response.getBody().getPayload());
        assertEquals(
                "Successfully checked all recent submissions!",
                response.getBody().getMessage());

        verify(leetcodeClient).findSubmissionsByUsernameFast("leetcodeUser", 20);
        verify(submissionsHandler).handleSubmissions(leetcodeSubs, user, true);
    }

    @Test
    void testCheckLatestSubmissionsFailure() {
        AuthenticationObject auth = mock(AuthenticationObject.class);
        User user = mock(User.class);

        when(auth.getUser()).thenReturn(user);
        when(user.getLeetcodeUsername()).thenReturn(null);

        ResponseStatusException ex =
                assertThrows(ResponseStatusException.class, () -> submissionController.checkLatestSubmissions(auth));

        assertEquals(HttpStatus.PRECONDITION_FAILED, ex.getStatusCode());
    }

    @Test
    void testGetCurrentPotdSuccess() {
        AuthenticationObject auth = mock(AuthenticationObject.class);
        User user = mock(User.class);
        POTD potd = mock(POTD.class);

        when(auth.getUser()).thenReturn(user);
        when(user.getId()).thenReturn("abcdefg123456");

        when(potdRepository.getCurrentPOTD()).thenReturn(Optional.of(potd));
        when(potd.getCreatedAt()).thenReturn(LocalDateTime.now());
        when(potd.getSlug()).thenReturn("two-sum");

        when(questionRepository.getQuestionBySlugAndUserId("two-sum", "abcdefg123456"))
                .thenReturn(Optional.empty());

        ResponseEntity<ApiResponder<PotdDto>> response = submissionController.getCurrentPotd(auth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
        assertEquals("Problem of the day has been fetched!", response.getBody().getMessage());
        assertNotNull(response.getBody().getPayload());
    }

    @Test
    void testGetCurrentPotdFailure() {
        AuthenticationObject auth = mock(AuthenticationObject.class);
        User user = mock(User.class);

        when(auth.getUser()).thenReturn(user);
        when(potdRepository.getCurrentPOTD()).thenReturn(Optional.empty());

        ResponseStatusException ex =
                assertThrows(ResponseStatusException.class, () -> submissionController.getCurrentPotd(auth));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void testGetCurrentPotdEmbedSuccess() {
        POTD potd = mock(POTD.class);

        when(potdRepository.getCurrentPOTD()).thenReturn(Optional.of(potd));
        when(potd.getCreatedAt()).thenReturn(LocalDateTime.now());

        ResponseEntity<ApiResponder<PotdDto>> response = submissionController.getCurrentPotdEmbed();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
        assertEquals("Problem of the day has been fetched!", response.getBody().getMessage());
        assertNotNull(response.getBody().getPayload());
    }

    @Test
    void testGetCurrentPotdEmbedFailure() {
        when(potdRepository.getCurrentPOTD()).thenReturn(Optional.empty());

        ResponseStatusException ex =
                assertThrows(ResponseStatusException.class, () -> submissionController.getCurrentPotdEmbed());

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void testGetSubmissionBySubmissionIdSuccess() {
        QuestionWithUser question = mock(QuestionWithUser.class);

        when(questionRepository.getQuestionWithUserById("abc123")).thenReturn(Optional.of(question));

        ResponseEntity<ApiResponder<QuestionWithUserDto>> response =
                submissionController.getSubmissionBySubmissionId(request, "abc123");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
        assertEquals("Problem of the day has been fetched!", response.getBody().getMessage());
        assertNotNull(response.getBody().getPayload());

        verify(questionRepository).getQuestionWithUserById("abc123");
    }

    @Test
    void testGetSubmissionBySubmissionIdFailure() {
        when(questionRepository.getQuestionWithUserById("missing")).thenReturn(Optional.empty());

        ResponseEntity<ApiResponder<QuestionWithUserDto>> response =
                submissionController.getSubmissionBySubmissionId(request, "missing");

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
    }
}
