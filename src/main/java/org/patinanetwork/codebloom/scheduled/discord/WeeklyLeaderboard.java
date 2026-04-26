package org.patinanetwork.codebloom.scheduled.discord;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.patinanetwork.codebloom.common.components.DiscordClubManager;
import org.patinanetwork.codebloom.common.db.models.weekly.WeeklyMessage;
import org.patinanetwork.codebloom.common.db.repos.weekly.WeeklyMessageRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@Profile("!ci | thread")
public class WeeklyLeaderboard {

    private final DiscordClubManager discordClubManager;
    private final WeeklyMessageRepository weeklyMessageRepository;

    WeeklyLeaderboard(
            final DiscordClubManager discordClubManager, final WeeklyMessageRepository weeklyMessageRepository) {
        this.discordClubManager = discordClubManager;
        this.weeklyMessageRepository = weeklyMessageRepository;
    }

    @Scheduled(initialDelay = 0, fixedDelay = 1, timeUnit = TimeUnit.HOURS)
    public void sendWeeklyLeaderboard() {
        WeeklyMessage weeklyMessage = weeklyMessageRepository.getLatestWeeklyMessage();

        if (weeklyMessage != null
                && !weeklyMessage.getCreatedAt().isBefore(LocalDateTime.now().minusDays(7L))) {
            log.info("WeeklyLeaderboard skipped.");
            return;
        }

        log.info("WeeklyLeaderboard triggered.");
        try {
            discordClubManager.sendWeeklyLeaderboardUpdateDiscordMessageToAllClubs();
        } catch (Exception e) {
            log.error("WeeklyLeaderboard failed", e);
        }
        weeklyMessageRepository.createLatestWeeklyMessage();
    }
}
