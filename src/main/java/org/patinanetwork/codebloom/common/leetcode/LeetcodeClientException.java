package org.patinanetwork.codebloom.common.leetcode;

import lombok.Getter;

@Getter
public class LeetcodeClientException extends RuntimeException {

    private final boolean notFound;

    public LeetcodeClientException(final String message) {
        this(message, false);
    }

    public LeetcodeClientException(final String message, final boolean notFound) {
        super(message);
        this.notFound = notFound;
    }

    public LeetcodeClientException(final String message, final Throwable e) {
        super(message, e);
        this.notFound = false;
    }
}
