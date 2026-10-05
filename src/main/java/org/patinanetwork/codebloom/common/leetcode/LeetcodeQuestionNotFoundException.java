package org.patinanetwork.codebloom.common.leetcode;

public class LeetcodeQuestionNotFoundException extends LeetcodeClientException {

    public LeetcodeQuestionNotFoundException(final String slug) {
        super("LeetCode returned no question for slug " + slug);
    }
}
