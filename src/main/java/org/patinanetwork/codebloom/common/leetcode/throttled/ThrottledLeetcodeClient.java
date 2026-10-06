package org.patinanetwork.codebloom.common.leetcode.throttled;

import java.util.List;
import java.util.Set;
import org.patinanetwork.codebloom.common.leetcode.LeetcodeClient;
import org.patinanetwork.codebloom.common.leetcode.models.LeetcodeDetailedQuestion;
import org.patinanetwork.codebloom.common.leetcode.models.LeetcodeQuestion;
import org.patinanetwork.codebloom.common.leetcode.models.LeetcodeSubmission;
import org.patinanetwork.codebloom.common.leetcode.models.LeetcodeTopicTag;
import org.patinanetwork.codebloom.common.leetcode.models.POTD;
import org.patinanetwork.codebloom.common.leetcode.models.UserProfile;

/**
 * Attaches a rate limiter over {@link LeetcodeClient} to avoid getting rate-limited from leetcode.com
 *
 * <p>Methods that end in {@Fast} skip the rate-limiter. <b>You should ONLY use this if the latency will directly affect
 * the user and/or any other latency-sensitive concerns.</b>
 */
public interface ThrottledLeetcodeClient extends LeetcodeClient {
    LeetcodeQuestion findQuestionBySlugFast(String slug);

    /**
     * Default method that doesn't require a limit. The default limit will return 20 submissions.
     *
     * @param username
     */
    List<LeetcodeSubmission> findSubmissionsByUsernameFast(String username);

    /**
     * This method specifies a submissions limit to adhere to.
     *
     * @param username
     * @param limit
     */
    List<LeetcodeSubmission> findSubmissionsByUsernameFast(String username, int limit);

    /** @implNote requires authentication */
    LeetcodeDetailedQuestion findSubmissionDetailBySubmissionIdFast(long submissionId);

    POTD getPotdFast();

    UserProfile getUserProfileFast(String username);

    Set<LeetcodeTopicTag> getAllTopicTagsFast();

    List<LeetcodeQuestion> getAllProblemsFast();
}
