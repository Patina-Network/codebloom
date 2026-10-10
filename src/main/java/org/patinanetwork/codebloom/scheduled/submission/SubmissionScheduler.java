package org.patinanetwork.codebloom.scheduled.submission;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.patinanetwork.codebloom.common.db.models.user.User;
import org.patinanetwork.codebloom.common.db.repos.user.UserRepository;
import org.patinanetwork.codebloom.common.leetcode.LeetcodeClient;
import org.patinanetwork.codebloom.common.leetcode.models.LeetcodeSubmission;
import org.patinanetwork.codebloom.common.leetcode.throttled.ThrottledLeetcodeClient;
import org.patinanetwork.codebloom.common.submissions.SubmissionsHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Profile("!ci")
public class SubmissionScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubmissionScheduler.class);

    private final UserRepository userRepository;
    private final LeetcodeClient leetcodeClient;
    private final SubmissionsHandler submissionsHandler;

    private volatile boolean shuttingDown = false;

    public SubmissionScheduler(
            final UserRepository userRepository,
            final ThrottledLeetcodeClient throttledLeetcodeClient,
            final SubmissionsHandler submissionsHandler) {
        this.userRepository = userRepository;
        this.leetcodeClient = throttledLeetcodeClient;
        this.submissionsHandler = submissionsHandler;
    }

    // handleAllUserSubmissions is a long running async function.
    // This EventListener is used to early terminate the Submission Scheduler thread
    // as it doesn't stop when the main
    // thread is terminated.
    @EventListener(ContextClosedEvent.class)
    public void onShutdown() {
        LOGGER.info("Shutdown detected. Terminating scheduled task.");
        shuttingDown = true;
    }

    // Cron runs every 30 minutes
    @Scheduled(fixedDelay = 30, timeUnit = TimeUnit.MINUTES)
    public void handleAllUserSubmissions() {
        LOGGER.info("Beginning the scheduled task to handle all user submissions now:");
        ArrayList<User> users = userRepository.getAllUsers();

        for (User user : users) {
            if (shuttingDown) {
                LOGGER.info("Skipping scheduled run because shutdown has started.");
                return;
            }

            if (user.getLeetcodeUsername().isEmpty()) {
                continue;
            }

            List<LeetcodeSubmission> leetcodeSubmissions = leetcodeClient.findSubmissionsByUsername(
                    user.getLeetcodeUsername().orElse(null));

            submissionsHandler.handleSubmissions(leetcodeSubmissions, user, false);
        }

        LOGGER.info("Scheduled task complete");
    }
}
