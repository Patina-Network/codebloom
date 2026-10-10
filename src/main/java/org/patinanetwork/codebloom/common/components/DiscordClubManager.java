package org.patinanetwork.codebloom.common.components;

import java.awt.Color;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.patinanetwork.codebloom.common.db.models.discord.DiscordClub;
import org.patinanetwork.codebloom.common.db.models.discord.DiscordClubMetadata;
import org.patinanetwork.codebloom.common.db.models.leaderboard.Leaderboard;
import org.patinanetwork.codebloom.common.db.models.user.User;
import org.patinanetwork.codebloom.common.db.models.user.UserWithScore;
import org.patinanetwork.codebloom.common.db.repos.discord.club.DiscordClubRepository;
import org.patinanetwork.codebloom.common.db.repos.leaderboard.LeaderboardRepository;
import org.patinanetwork.codebloom.common.db.repos.leaderboard.options.LeaderboardFilterGenerator;
import org.patinanetwork.codebloom.common.db.repos.leaderboard.options.LeaderboardFilterOptions;
import org.patinanetwork.codebloom.common.db.repos.user.UserRepository;
import org.patinanetwork.codebloom.common.db.repos.user.options.UserFilterOptions;
import org.patinanetwork.codebloom.common.dto.refresh.RefreshResultDto;
import org.patinanetwork.codebloom.common.page.Indexed;
import org.patinanetwork.codebloom.common.time.StandardizedLocalDateTime;
import org.patinanetwork.codebloom.common.url.ServerUrlUtils;
import org.patinanetwork.codebloom.common.utils.leaderboard.LeaderboardUtils;
import org.patinanetwork.codebloom.jda.client.JDAClient;
import org.patinanetwork.codebloom.jda.client.options.EmbeddedImagesMessageOptions;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DiscordClubManager {

    private final JDAClient jdaClient;
    private final LeaderboardRepository leaderboardRepository;
    private final DiscordClubRepository discordClubRepository;
    private final UserRepository userRepository;
    private final ServerUrlUtils serverUrlUtils;
    private final LeaderboardManager leaderboardManager;

    public DiscordClubManager(
            final ServerUrlUtils serverUrlUtils,
            final JDAClient jdaClient,
            final LeaderboardRepository leaderboardRepository,
            final DiscordClubRepository discordClubRepository,
            final UserRepository userRepository,
            final LeaderboardManager leaderboardManager) {
        this.serverUrlUtils = serverUrlUtils;
        this.jdaClient = jdaClient;
        this.leaderboardRepository = leaderboardRepository;
        this.discordClubRepository = discordClubRepository;
        this.userRepository = userRepository;
        this.leaderboardManager = leaderboardManager;
    }

    private static final String[] MEDAL_EMOJIS = {"🥇", "🥈", "🥉"};

    private String buildTopUsersSection(final List<UserWithScore> users, final boolean isFinal) {
        if (users.isEmpty()) {
            if (isFinal) {
                return "Keep solving problems and climb your way to the top of the next one!";
            }
            return "There are no scores yet, but the leaderboard is still open. Get in there and start solving!";
        }

        var sb = new StringBuilder();
        for (int i = 0; i < Math.min(users.size(), 3); i++) {
            if (i > 0) {
                sb.append("\n");
            }
            sb.append("%s- <@%s> - %s pts"
                    .formatted(
                            MEDAL_EMOJIS[i],
                            users.get(i).getDiscordId(),
                            users.get(i).getTotalScore()));
        }
        return sb.toString();
    }

    /**
     * Loads the page for each discord club's tag, takes a screenshot, and sends a final Discord message with the
     * winners of the leaderboard.
     *
     * <p>CANNOT BE ASYNC, WILL BREAK THE FLOW.
     *
     * <p>TODO:
     * https://codebloom.notion.site/Refactor-create-new-leaderboard-to-load-new-leaderboard-first-then-take-screenshots-of-the-old-page-2a87c85563aa809a9ca6dc23a31b0ab2?pvs=74
     */
    private void sendLeaderboardCompletedDiscordMessage(final DiscordClub club) {
        try {
            Optional<Leaderboard> currentLeaderboard = leaderboardRepository.getRecentLeaderboardMetadata();

            if (currentLeaderboard.isEmpty()) {
                return;
            }

            LeaderboardFilterOptions options = LeaderboardFilterGenerator.builderWithTag(club.getTag())
                    .page(1)
                    .pageSize(5)
                    .build();

            List<UserWithScore> users =
                    LeaderboardUtils.filterUsersWithPoints(leaderboardRepository.getLeaderboardUsersById(
                            currentLeaderboard.get().getId(), options));

            String topUsersSection = buildTopUsersSection(users, true);
            String headerText = users.isEmpty()
                    ? "No one claimed a spot on this leaderboard. "
                    : "CONGRATS TO THE WINNERS FROM THIS LEADERBOARD!";

            String description = String.format(
                    """
                Dear %s users,

                %s

                %s

                The new leaderboard just started, so best of luck to everyone!

                View the full leaderboard for %s users at %s/leaderboard/%s?%s=true

                Happy LeetCoding,
                Codebloom
                <%s>
                """,
                    club.getName(),
                    headerText,
                    topUsersSection,
                    club.getName(),
                    serverUrlUtils.getUrl(),
                    currentLeaderboard.get().getId(),
                    club.getTag().name().toLowerCase(),
                    serverUrlUtils.getUrl());

            var guildId = club.getDiscordClubMetadata().flatMap(DiscordClubMetadata::getGuildId);
            var channelId = club.getDiscordClubMetadata().flatMap(DiscordClubMetadata::getLeaderboardChannelId);

            if (guildId.isEmpty() || channelId.isEmpty()) {
                log.error("club {} is skipped because of missing metadata", club.getName());
                return;
            }

            jdaClient.sendEmbedWithImages(EmbeddedImagesMessageOptions.builder()
                    .guildId(Long.valueOf(guildId.get()))
                    .channelId(Long.valueOf(channelId.get()))
                    .description(description)
                    .title("🏆🏆🏆 %s - Final Leaderboard Score for %s"
                            .formatted(currentLeaderboard.get().getName(), club.getName()))
                    .footerText("Codebloom - LeetCode Leaderboard for %s".formatted(club.getName()))
                    .footerIcon("%s/favicon.ico".formatted(serverUrlUtils.getUrl()))
                    .color(new Color(69, 129, 103))
                    .build());
        } catch (Exception e) {
            log.error("Exception thrown in DiscordClubManager", e);
        }
    }

    private void sendWeeklyLeaderboardUpdateDiscordMessage(final DiscordClub club) {
        try {
            var guildIdOpt = club.getDiscordClubMetadata().flatMap(DiscordClubMetadata::getGuildId);
            var channelIdOpt = club.getDiscordClubMetadata().flatMap(DiscordClubMetadata::getLeaderboardChannelId);

            if (guildIdOpt.isEmpty() || channelIdOpt.isEmpty()) {
                log.error("club {} is skipped because of missing metadata", club.getName());
                return;
            }

            String guildId = guildIdOpt.get();
            String channelId = channelIdOpt.get();

            MessageCreateData messageData = buildLeaderboardMessageForClub(guildId, true);

            if (messageData == null
                    || messageData.getEmbeds() == null
                    || messageData.getEmbeds().isEmpty()) {
                log.error("No embed returned from buildLeaderboardMessageForClub for guildId={}", guildId);
                return;
            }

            MessageEmbed embed = messageData.getEmbeds().get(0);

            jdaClient.sendEmbedWithImages(EmbeddedImagesMessageOptions.builder()
                    .guildId(Long.valueOf(guildId))
                    .channelId(Long.valueOf(channelId))
                    .description(defaultIfNull(embed.getDescription()))
                    .title(defaultIfNull(embed.getTitle()))
                    .footerText(
                            embed.getFooter() == null
                                    ? null
                                    : defaultIfNull(embed.getFooter().getText()))
                    .footerIcon(
                            embed.getFooter() == null
                                    ? null
                                    : defaultIfNull(embed.getFooter().getIconUrl()))
                    .color(embed.getColor() == null ? new Color(69, 129, 103) : embed.getColor())
                    .build());
        } catch (Exception e) {
            log.error("Error in DiscordClubManager when sending weekly leaderboard", e);
        }
    }

    private static String defaultIfNull(final String s) {
        return s == null ? "" : s;
    }

    public void sendLeaderboardCompletedDiscordMessageToAllClubs() {
        var discordClubs = discordClubRepository.getAllActiveDiscordClubs();
        discordClubs.forEach(this::sendLeaderboardCompletedDiscordMessage);
    }

    public void sendWeeklyLeaderboardUpdateDiscordMessageToAllClubs() {
        var discordClubs = discordClubRepository.getAllActiveDiscordClubs();
        discordClubs.forEach(this::sendWeeklyLeaderboardUpdateDiscordMessage);
    }

    public MessageCreateData buildLeaderboardMessageForClub(String guildId, boolean isWeekly) {
        DiscordClub club =
                discordClubRepository.getDiscordClubByGuildId(guildId).orElseThrow();

        Optional<Leaderboard> currentLeaderboard = leaderboardRepository.getRecentLeaderboardMetadata();

        if (currentLeaderboard.isEmpty()) {
            throw new RuntimeException("No recent leaderboard found.");
        }

        LeaderboardFilterOptions options = LeaderboardFilterGenerator.builderWithTag(club.getTag())
                .page(1)
                .pageSize(5)
                .build();

        List<UserWithScore> users =
                LeaderboardUtils.filterUsersWithPoints(leaderboardRepository.getLeaderboardUsersById(
                        currentLeaderboard.get().getId(), options));

        Optional<LocalDateTime> shouldExpireByTime = currentLeaderboard.get().getShouldExpireBy();

        long daysLeft = 0;
        long hoursLeft = 0;
        long minutesLeft = 0;

        if (shouldExpireByTime.isPresent()) {
            Duration remaining = Duration.between(StandardizedLocalDateTime.now(), shouldExpireByTime.get());
            daysLeft = remaining.toDays();
            hoursLeft = remaining.toHours() % 24;
            minutesLeft = remaining.toMinutes() % 60;
        }

        String topUsersSection = buildTopUsersSection(users, false);
        String headerText = "Here is %s on the LeetCode leaderboard for our very own members!"
                .formatted(isWeekly ? "a weekly update" : "an update");

        String description = String.format(
                """
            Dear %s users,

            %s

            %s

            Just as a reminder, there's %d day(s), %d hour(s), and %d minute(s) left until the leaderboard closes, so keep grinding!

            View the full leaderboard for %s users at %s/leaderboard?%s=true

            %s

            Beep boop,
            Codebloom
            <%s>
            """,
                club.getName(),
                headerText,
                topUsersSection,
                daysLeft,
                hoursLeft,
                minutesLeft,
                club.getName(),
                serverUrlUtils.getUrl(),
                club.getTag().name().toLowerCase(),
                isWeekly ? "See you next week!" : "",
                serverUrlUtils.getUrl());

        MessageEmbed embed = new EmbedBuilder()
                .setTitle("%s - %s Leaderboard Update for %s"
                        .formatted(currentLeaderboard.get().getName(), isWeekly ? "Weekly" : "", club.getName()))
                .setDescription(description)
                .setFooter(
                        "Codebloom - LeetCode Leaderboard for %s".formatted(club.getName()),
                        "%s/favicon.ico".formatted(serverUrlUtils.getUrl()))
                .setColor(new Color(69, 129, 103))
                .build();

        return MessageCreateData.fromEmbeds(embed);
    }

    public RefreshResultDto refreshSubmissions(String guildId, String discordId) throws LeaderboardException {
        DiscordClub club = discordClubRepository
                .getDiscordClubByGuildId(guildId)
                .orElseThrow(() -> new LeaderboardException("Club does not exist", "This club does not exist!"));

        User user = leaderboardManager.refreshUserSubmissions(discordId);
        Leaderboard currentLeaderboard = leaderboardRepository
                .getRecentLeaderboardMetadata()
                .orElseThrow(() -> new LeaderboardException(
                        "Could not find Recent Leaderboard", "Recent leaderboard does not exist"));

        LeaderboardFilterOptions options =
                LeaderboardFilterGenerator.builderWithTag(club.getTag()).build();

        String userId = user.getId();

        UserWithScore scoredUser = userRepository
                .getUserWithScoreByIdAndLeaderboardId(userId, currentLeaderboard.getId(), UserFilterOptions.DEFAULT)
                .orElse(null);

        int score = scoredUser.getTotalScore();
        Indexed<UserWithScore> globalIndex = leaderboardRepository
                .getGlobalRankedUserById(currentLeaderboard.getId(), userId)
                .orElseThrow(() ->
                        new LeaderboardException("Could not find global rank", "Error finding global rank by userId."));
        Indexed<UserWithScore> clubIndex = leaderboardRepository
                .getFilteredRankedUserById(currentLeaderboard.getId(), userId, options)
                .orElseThrow(() -> new LeaderboardException(
                        "Could not find filtered rank", "Error finding filtered rank by userId."));

        return RefreshResultDto.builder()
                .score(score)
                .globalRank(globalIndex.getIndex())
                .clubRank(clubIndex.getIndex())
                .leaderboardName(currentLeaderboard.getName())
                .clubName(club.getName())
                .build();
    }

    public boolean sendTestEmbedMessageToClub(DiscordClub club) {
        try {
            String description = """
                    This is a test message ensuring that the integration is working as expected. Please ignore.
                """;
            var guildIdOpt = club.getDiscordClubMetadata().flatMap(DiscordClubMetadata::getGuildId);
            var channelIdOpt = club.getDiscordClubMetadata().flatMap(DiscordClubMetadata::getLeaderboardChannelId);
            if (guildIdOpt.isEmpty() || channelIdOpt.isEmpty()) {
                log.warn("Missing guildId or leaderboardChannelId for club {}", club.getId());
                return false;
            }
            jdaClient.sendEmbedWithImages(EmbeddedImagesMessageOptions.builder()
                    .guildId(Long.valueOf(guildIdOpt.get()))
                    .channelId(Long.valueOf(channelIdOpt.get()))
                    .description(description)
                    .title("Message for %s".formatted(club.getName()))
                    .footerText("Codebloom - LeetCode Leaderboard for %s".formatted(club.getName()))
                    .footerIcon("%s/favicon.ico".formatted(serverUrlUtils.getUrl()))
                    .color(new Color(69, 129, 103))
                    .build());
            return true;
        } catch (Exception e) {
            log.error("Error in DiscordClubManager when sending test message", e);
            return false;
        }
    }

    public boolean deleteMessageById(Long channelId, Long messageId) {
        return jdaClient.deleteMessageById(channelId, messageId);
    }
}
