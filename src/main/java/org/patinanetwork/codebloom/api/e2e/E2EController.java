package org.patinanetwork.codebloom.api.e2e;

import org.patinanetwork.codebloom.common.dto.ApiResponder;
import org.patinanetwork.codebloom.common.leetcode.LeetcodeClient;
import org.patinanetwork.codebloom.common.leetcode.models.LeetcodeDetailedQuestion;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Profile("e2e & !prod")
@RestController
@RequestMapping("/api/e2e")
public class E2EController {

    private final LeetcodeClient leetcodeClient;

    public E2EController(final LeetcodeClient leetcodeClient) {
        this.leetcodeClient = leetcodeClient;
    }

    @GetMapping("/leetcode/submission/{submissionId}")
    public ApiResponder<LeetcodeDetailedQuestion> getLeetcodeSubmission(@PathVariable final long submissionId) {
        return ApiResponder.success(
                "LeetCode submission fetched", leetcodeClient.findSubmissionDetailBySubmissionId(submissionId));
    }
}
