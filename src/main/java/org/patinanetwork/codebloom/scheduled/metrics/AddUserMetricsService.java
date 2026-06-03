package org.patinanetwork.codebloom.scheduled.metrics;

import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.patinanetwork.codebloom.common.components.LeaderboardException;
import org.patinanetwork.codebloom.common.db.models.leaderboard.Leaderboard;
import org.patinanetwork.codebloom.common.db.models.task.BackgroundTask;
import org.patinanetwork.codebloom.common.db.models.task.BackgroundTaskEnum;
import org.patinanetwork.codebloom.common.db.models.user.UserMetrics;
import org.patinanetwork.codebloom.common.db.models.user.UserWithScore;
import org.patinanetwork.codebloom.common.db.repos.leaderboard.LeaderboardRepository;
import org.patinanetwork.codebloom.common.db.repos.leaderboard.options.LeaderboardFilterOptions;
import org.patinanetwork.codebloom.common.db.repos.task.BackgroundTaskRepository;
import org.patinanetwork.codebloom.common.db.repos.user.UserMetricsRepository;
import org.patinanetwork.codebloom.common.time.StandardizedOffsetDateTime;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("!ci")
public class AddUserMetricsService {

    private final BackgroundTaskRepository backgroundTaskRepository;
    private final LeaderboardRepository leaderboardRepository;
    private final UserMetricsRepository userMetricsRepository;

    public AddUserMetricsService(
            final BackgroundTaskRepository backgroundTaskRepository,
            final LeaderboardRepository leaderboardRepository,
            final UserMetricsRepository userMetricsRepository) {
        this.backgroundTaskRepository = backgroundTaskRepository;
        this.leaderboardRepository = leaderboardRepository;
        this.userMetricsRepository = userMetricsRepository;
    }

    @Scheduled(initialDelay = 15, fixedDelay = 30, timeUnit = TimeUnit.MINUTES)
    public void congregateUserMetrics() {
        try {
            Optional<BackgroundTask> recentMetricsTask =
                    backgroundTaskRepository.getMostRecentlyCompletedBackgroundTaskByTaskEnum(
                            BackgroundTaskEnum.USER_METRICS);

            var midnight = StandardizedOffsetDateTime.now()
                    .atZoneSameInstant(ZoneOffset.UTC)
                    .toLocalDate()
                    .atStartOfDay(ZoneOffset.UTC)
                    .toOffsetDateTime();

            if (recentMetricsTask.isPresent()
                    && recentMetricsTask.get().getCompletedAt() != null
                    && !recentMetricsTask.get().getCompletedAt().isBefore(midnight)) {
                log.info("Skipping user metrics sync because today's snapshot already exists.");
                return;
            }

            Leaderboard currentLeaderboard = leaderboardRepository
                    .getRecentLeaderboardMetadata()
                    .orElseThrow(() -> new LeaderboardException("No leaderboard", "No recent leaderboard was found"));

            int leaderboardUserCount = leaderboardRepository.getLeaderboardUserCountById(
                    currentLeaderboard.getId(), LeaderboardFilterOptions.DEFAULT);
            List<UserWithScore> leaderboardUsers = leaderboardUserCount == 0
                    ? List.of()
                    : leaderboardRepository.getLeaderboardUsersById(
                            currentLeaderboard.getId(),
                            LeaderboardFilterOptions.builder()
                                    .pageSize(leaderboardUserCount)
                                    .build());

            for (var leaderboardUser : leaderboardUsers) {
                userMetricsRepository.createUserMetrics(UserMetrics.builder()
                        .userId(leaderboardUser.getId())
                        .points(leaderboardUser.getTotalScore())
                        .build());
            }

            backgroundTaskRepository.createBackgroundTask(BackgroundTask.builder()
                    .task(BackgroundTaskEnum.USER_METRICS)
                    .completedAt(StandardizedOffsetDateTime.now())
                    .build());
        } catch (LeaderboardException e) {
            log.warn("Skipping user metric sync {}", e.getMessage());
        } catch (Exception e) {
            log.error("Could not perform user metrics check", e);
        }
    }
}
