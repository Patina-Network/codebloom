package org.patinanetwork.codebloom.api.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.patinanetwork.codebloom.common.leetcode.LeetcodeClient;
import org.patinanetwork.codebloom.common.leetcode.models.Lang;
import org.patinanetwork.codebloom.common.leetcode.models.LeetcodeDetailedQuestion;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

public class E2EControllerTest {

    private final LeetcodeClient leetcodeClient = mock(LeetcodeClient.class);

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean(LeetcodeClient.class, () -> leetcodeClient)
            .withUserConfiguration(E2EController.class);

    @Test
    void returnsSubmissionDetailsFromLeetcodeClient() {
        LeetcodeDetailedQuestion details = new LeetcodeDetailedQuestion(
                1, "1 ms", 99.5f, 20780000, "20.8 MB", 75.0f, "print(1)", new Lang("python3", "Python3"));
        when(leetcodeClient.findSubmissionDetailBySubmissionId(2196928698L)).thenReturn(details);

        var response = new E2EController(leetcodeClient).getLeetcodeSubmission(2196928698L);

        assertTrue(response.isSuccess());
        assertEquals("LeetCode submission fetched", response.getMessage());
        assertSame(details, response.getPayload());
    }

    @Test
    void loadsUnderE2eProfile() {
        contextRunner.withPropertyValues("spring.profiles.active=ci,e2e").run(context -> assertThat(context)
                .hasSingleBean(E2EController.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "ci", "prod", "e2e,prod"})
    void neverLoadsOutsideE2eOrWithProd(final String profiles) {
        contextRunner.withPropertyValues("spring.profiles.active=" + profiles).run(context -> assertThat(context)
                .doesNotHaveBean(E2EController.class));
    }
}
