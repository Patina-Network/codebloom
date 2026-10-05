package org.patinanetwork.codebloom.common.leetcode.throttled;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.patinanetwork.codebloom.common.db.models.question.QuestionDifficulty;
import org.patinanetwork.codebloom.common.leetcode.LeetcodeClientImpl;
import org.patinanetwork.codebloom.common.leetcode.models.LeetcodeDetailedQuestion;
import org.patinanetwork.codebloom.common.leetcode.models.LeetcodeQuestion;
import org.patinanetwork.codebloom.common.leetcode.models.LeetcodeSubmission;
import org.patinanetwork.codebloom.common.leetcode.models.LeetcodeTopicTag;
import org.patinanetwork.codebloom.common.leetcode.models.POTD;
import org.patinanetwork.codebloom.common.leetcode.models.UserProfile;

public class ThrottledLeetcodeClientTest {
    private LeetcodeClientImpl leetcodeClientImpl;
    private ThrottledLeetcodeClient throttledLeetcodeClient;
    private ExecutorService executor;

    @BeforeEach
    void init() {
        leetcodeClientImpl = mock(LeetcodeClientImpl.class);
        executor = Executors.newVirtualThreadPerTaskExecutor();
        throttledLeetcodeClient = new ThrottledLeetcodeClientImpl(leetcodeClientImpl, executor);
    }

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
    }

    @Test
    void testFindQuestionBySlugFast() {
        String slug = "two-sum";
        LeetcodeQuestion question = LeetcodeQuestion.builder().titleSlug(slug).build();
        when(leetcodeClientImpl.findQuestionBySlug(slug)).thenReturn(question);

        var res = throttledLeetcodeClient.findQuestionBySlugFast(slug);
        assertEquals(question, res);
    }

    @Test
    void testFindQuestionBySlug() {
        String slug = "two-sum";
        LeetcodeQuestion question = LeetcodeQuestion.builder().titleSlug(slug).build();
        when(leetcodeClientImpl.findQuestionBySlug(slug)).thenReturn(question);

        var res = throttledLeetcodeClient.findQuestionBySlug(slug);
        assertEquals(question, res);
    }

    @Test
    void testFindSubmissionsByUsernameFast() {
        String username = "testuser";
        ArrayList<LeetcodeSubmission> submissions = new ArrayList<>();
        when(leetcodeClientImpl.findSubmissionsByUsername(username)).thenReturn(submissions);

        var res = throttledLeetcodeClient.findSubmissionsByUsernameFast(username);
        assertEquals(submissions, res);
    }

    @Test
    void testFindSubmissionsByUsernameFastWithLimit() {
        String username = "testuser";
        int limit = 10;
        ArrayList<LeetcodeSubmission> submissions = new ArrayList<>();
        when(leetcodeClientImpl.findSubmissionsByUsername(username, limit)).thenReturn(submissions);

        var res = throttledLeetcodeClient.findSubmissionsByUsernameFast(username, limit);
        assertEquals(submissions, res);
    }

    @Test
    void testFindSubmissionsByUsername() {
        String username = "testuser";
        ArrayList<LeetcodeSubmission> submissions = new ArrayList<>();
        when(leetcodeClientImpl.findSubmissionsByUsername(username)).thenReturn(submissions);

        var res = throttledLeetcodeClient.findSubmissionsByUsername(username);
        assertEquals(submissions, res);
    }

    @Test
    void testFindSubmissionsByUsernameWithLimit() {
        String username = "testuser";
        int limit = 10;
        ArrayList<LeetcodeSubmission> submissions = new ArrayList<>();
        when(leetcodeClientImpl.findSubmissionsByUsername(username, limit)).thenReturn(submissions);

        var res = throttledLeetcodeClient.findSubmissionsByUsername(username, limit);
        assertEquals(submissions, res);
    }

    @Test
    void testFindSubmissionDetailBySubmissionIdFast() {
        long submissionId = 123L;
        LeetcodeDetailedQuestion detail = new LeetcodeDetailedQuestion(0, null, 0, 0, null, 0, null, null);
        when(leetcodeClientImpl.findSubmissionDetailBySubmissionId(submissionId))
                .thenReturn(detail);

        var res = throttledLeetcodeClient.findSubmissionDetailBySubmissionIdFast(submissionId);
        assertEquals(detail, res);
    }

    @Test
    void testFindSubmissionDetailBySubmissionId() {
        long submissionId = 123L;
        LeetcodeDetailedQuestion detail = new LeetcodeDetailedQuestion(0, null, 0, 0, null, 0, null, null);
        when(leetcodeClientImpl.findSubmissionDetailBySubmissionId(submissionId))
                .thenReturn(detail);

        var res = throttledLeetcodeClient.findSubmissionDetailBySubmissionId(submissionId);
        assertEquals(detail, res);
    }

    @Test
    void testGetPotdFast() {
        POTD potd = new POTD("title", "slug", QuestionDifficulty.Easy);
        when(leetcodeClientImpl.getPotd()).thenReturn(potd);

        var res = throttledLeetcodeClient.getPotdFast();
        assertEquals(potd, res);
    }

    @Test
    void testGetPotd() {
        POTD potd = new POTD("title", "slug", QuestionDifficulty.Easy);
        when(leetcodeClientImpl.getPotd()).thenReturn(potd);

        var res = throttledLeetcodeClient.getPotd();
        assertEquals(potd, res);
    }

    @Test
    void testGetUserProfileFast() {
        String username = "testuser";
        UserProfile profile = new UserProfile(username, "1", "avatar", "name", "about");
        when(leetcodeClientImpl.getUserProfile(username)).thenReturn(profile);

        var res = throttledLeetcodeClient.getUserProfileFast(username);
        assertEquals(profile, res);
    }

    @Test
    void testGetUserProfile() {
        String username = "testuser";
        UserProfile profile = new UserProfile(username, "1", "avatar", "name", "about");
        when(leetcodeClientImpl.getUserProfile(username)).thenReturn(profile);

        var res = throttledLeetcodeClient.getUserProfile(username);
        assertEquals(profile, res);
    }

    @Test
    void testGetAllTopicTagsFast() {
        Set<LeetcodeTopicTag> tags = Set.of();
        when(leetcodeClientImpl.getAllTopicTags()).thenReturn(tags);

        var res = throttledLeetcodeClient.getAllTopicTagsFast();
        assertEquals(tags, res);
    }

    @Test
    void testGetAllTopicTags() {
        Set<LeetcodeTopicTag> tags = Set.of();
        when(leetcodeClientImpl.getAllTopicTags()).thenReturn(tags);

        var res = throttledLeetcodeClient.getAllTopicTags();
        assertEquals(tags, res);
    }

    @Test
    void testGetAllProblemsFast() {
        List<LeetcodeQuestion> problems = List.of();
        when(leetcodeClientImpl.getAllProblems()).thenReturn(problems);

        var res = throttledLeetcodeClient.getAllProblemsFast();
        assertEquals(problems, res);
    }

    @Test
    void testGetAllProblems() {
        List<LeetcodeQuestion> problems = List.of();
        when(leetcodeClientImpl.getAllProblems()).thenReturn(problems);

        var res = throttledLeetcodeClient.getAllProblems();
        assertEquals(problems, res);
    }
}
