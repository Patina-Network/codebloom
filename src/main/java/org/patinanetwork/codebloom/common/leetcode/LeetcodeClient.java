package org.patinanetwork.codebloom.common.leetcode;

import java.util.List;
import java.util.Set;
import org.patinanetwork.codebloom.common.leetcode.models.LeetcodeDetailedQuestion;
import org.patinanetwork.codebloom.common.leetcode.models.LeetcodeQuestion;
import org.patinanetwork.codebloom.common.leetcode.models.LeetcodeSubmission;
import org.patinanetwork.codebloom.common.leetcode.models.LeetcodeTopicTag;
import org.patinanetwork.codebloom.common.leetcode.models.POTD;
import org.patinanetwork.codebloom.common.leetcode.models.UserProfile;

public interface LeetcodeClient {
    LeetcodeQuestion findQuestionBySlug(String slug);

    /**
     * Default method that doesn't require a limit. The default limit will return 20 submissions.
     *
     * @param username
     */
    List<LeetcodeSubmission> findSubmissionsByUsername(String username);

    /**
     * This method specifies a submissions limit to adhere to.
     *
     * @param username
     * @param limit
     */
    List<LeetcodeSubmission> findSubmissionsByUsername(String username, int limit);

    /** @implNote requires authentication */
    LeetcodeDetailedQuestion findSubmissionDetailBySubmissionId(long submissionId);

    POTD getPotd();

    UserProfile getUserProfile(String username);

    Set<LeetcodeTopicTag> getAllTopicTags();

    List<LeetcodeQuestion> getAllProblems();
}
