package org.patinanetwork.codebloom.common.db.repos;

import org.patinanetwork.codebloom.common.email.client.codebloom.OfficialCodebloomEmailClient;
import org.patinanetwork.codebloom.common.email.client.github.GithubOAuthEmailClient;
import org.patinanetwork.codebloom.common.leetcode.auth.LeetcodeAuthenticationService;
import org.patinanetwork.codebloom.jda.JDAClientManager;
import org.patinanetwork.codebloom.jda.command.JDASlashCommandInitializer;
import org.patinanetwork.codebloom.scheduled.submission.SubmissionScheduler;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Some clients are slow and are not required to be loaded for database integration tests.
 *
 * <p>All database tests must extend this class.
 */
public class BaseRepositoryTest {

    @MockitoBean
    private JDAClientManager jdaClientManager;

    @MockitoBean
    private JDASlashCommandInitializer jdaSlashCommandInitializer;

    @MockitoBean
    private OfficialCodebloomEmailClient codebloomEmailClient;

    @MockitoBean
    private GithubOAuthEmailClient githubOAuthEmailClient;

    @MockitoBean
    private SubmissionScheduler submissionScheduler;

    @MockitoBean
    private LeetcodeAuthenticationService leetcodeAuthenticationService;
}
