package org.patinanetwork.codebloom.common.leetcode.models;

import java.util.List;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;

@Getter
@Builder
@Jacksonized
@ToString
@EqualsAndHashCode
public class LeetcodeQuestion {

    private String link;
    private int questionId;
    private String questionTitle;
    private String titleSlug;
    private String difficulty;
    private String question;
    private boolean isPaidOnly;

    private float acceptanceRate;
    private List<LeetcodeTopicTag> topics;
}
