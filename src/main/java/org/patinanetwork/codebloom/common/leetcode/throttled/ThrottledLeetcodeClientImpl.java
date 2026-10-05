package org.patinanetwork.codebloom.common.leetcode.throttled;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BlockingBucket;
import io.github.bucket4j.Bucket;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import org.patinanetwork.codebloom.common.leetcode.LeetcodeClient;
import org.patinanetwork.codebloom.common.leetcode.LeetcodeClientImpl;
import org.patinanetwork.codebloom.common.leetcode.models.LeetcodeDetailedQuestion;
import org.patinanetwork.codebloom.common.leetcode.models.LeetcodeQuestion;
import org.patinanetwork.codebloom.common.leetcode.models.LeetcodeSubmission;
import org.patinanetwork.codebloom.common.leetcode.models.LeetcodeTopicTag;
import org.patinanetwork.codebloom.common.leetcode.models.POTD;
import org.patinanetwork.codebloom.common.leetcode.models.UserProfile;
import org.patinanetwork.codebloom.common.utils.lock.QueueLock;
import org.springframework.stereotype.Component;

@Component
public class ThrottledLeetcodeClientImpl implements ThrottledLeetcodeClient {
    private final LeetcodeClient leetcodeClient;

    private static final long REQUESTS_OVER_TIME = 1L;
    private static final long MILLISECONDS_TO_WAIT = 100L;
    private final QueueLock rateLimiter;

    private BlockingBucket initializeBucket() {
        var bandwidth = Bandwidth.builder()
                .capacity(REQUESTS_OVER_TIME)
                .refillIntervally(REQUESTS_OVER_TIME, Duration.ofMillis(MILLISECONDS_TO_WAIT))
                .build();

        return Bucket.builder().addLimit(bandwidth).build().asBlocking();
    }

    private void waitForToken(boolean fast) {
        try {
            if (fast) {
                rateLimiter.acquireFast();
            } else {
                rateLimiter.acquire();
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Failed to consume rate limit bucket token in leetcode client", e);
        }
    }

    public ThrottledLeetcodeClientImpl(final LeetcodeClientImpl leetcodeClientImpl, final ExecutorService virtualPool) {
        this.rateLimiter = new QueueLock(initializeBucket(), virtualPool);
        this.leetcodeClient = leetcodeClientImpl;
    }

    @Override
    public LeetcodeQuestion findQuestionBySlugFast(final String slug) {
        waitForToken(true);
        return leetcodeClient.findQuestionBySlug(slug);
    }

    @Override
    public List<LeetcodeSubmission> findSubmissionsByUsernameFast(final String username) {
        waitForToken(true);
        return leetcodeClient.findSubmissionsByUsername(username);
    }

    @Override
    public List<LeetcodeSubmission> findSubmissionsByUsernameFast(final String username, final int limit) {
        waitForToken(true);
        return leetcodeClient.findSubmissionsByUsername(username, limit);
    }

    @Override
    public LeetcodeDetailedQuestion findSubmissionDetailBySubmissionIdFast(final long submissionId) {
        waitForToken(true);
        return leetcodeClient.findSubmissionDetailBySubmissionId(submissionId);
    }

    @Override
    public POTD getPotdFast() {
        waitForToken(true);
        return leetcodeClient.getPotd();
    }

    @Override
    public UserProfile getUserProfileFast(final String username) {
        waitForToken(true);
        return leetcodeClient.getUserProfile(username);
    }

    @Override
    public Set<LeetcodeTopicTag> getAllTopicTagsFast() {
        waitForToken(true);
        return leetcodeClient.getAllTopicTags();
    }

    @Override
    public List<LeetcodeQuestion> getAllProblemsFast() {
        waitForToken(true);
        return leetcodeClient.getAllProblems();
    }

    @Override
    public LeetcodeQuestion findQuestionBySlug(String slug) {
        waitForToken(false);
        return leetcodeClient.findQuestionBySlug(slug);
    }

    @Override
    public List<LeetcodeSubmission> findSubmissionsByUsername(String username) {
        waitForToken(false);
        return leetcodeClient.findSubmissionsByUsername(username);
    }

    @Override
    public List<LeetcodeSubmission> findSubmissionsByUsername(String username, int limit) {
        waitForToken(false);
        return leetcodeClient.findSubmissionsByUsername(username, limit);
    }

    @Override
    public LeetcodeDetailedQuestion findSubmissionDetailBySubmissionId(long submissionId) {
        waitForToken(false);
        return leetcodeClient.findSubmissionDetailBySubmissionId(submissionId);
    }

    @Override
    public POTD getPotd() {
        waitForToken(false);
        return leetcodeClient.getPotd();
    }

    @Override
    public UserProfile getUserProfile(String username) {
        waitForToken(false);
        return leetcodeClient.getUserProfile(username);
    }

    @Override
    public Set<LeetcodeTopicTag> getAllTopicTags() {
        waitForToken(false);
        return leetcodeClient.getAllTopicTags();
    }

    @Override
    public List<LeetcodeQuestion> getAllProblems() {
        waitForToken(false);
        return leetcodeClient.getAllProblems();
    }
}
