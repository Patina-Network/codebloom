package org.patinanetwork.codebloom.jda.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.utils.FileUpload;
import org.patinanetwork.codebloom.jda.JDAClientManager;
import org.patinanetwork.codebloom.jda.client.options.EmbeddedDirectMessageOptions;
import org.patinanetwork.codebloom.jda.client.options.EmbeddedImagesMessageOptions;
import org.patinanetwork.codebloom.jda.client.options.EmbeddedMessageOptions;
import org.patinanetwork.codebloom.jda.properties.patina.JDAPatinaProperties;
import org.patinanetwork.codebloom.jda.properties.reporting.JDAErrorReportingProperties;
import org.patinanetwork.codebloom.jda.properties.reporting.JDALogReportingProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

/** Use this client to interface with any required Discord bot logic. */
@Component
@Slf4j
@EnableConfigurationProperties({
    JDAPatinaProperties.class,
    JDAErrorReportingProperties.class,
    JDALogReportingProperties.class,
})
public class JDAClient {

    private final JDA jda;

    @Getter
    private final JDAPatinaProperties jdaPatinaProperties;

    @Getter
    private final JDAErrorReportingProperties jdaErrorReportingProperties;

    @Getter
    private final JDALogReportingProperties jdaLogReportingProperties;

    public JDAClient(
            final JDAClientManager jdaClientManager,
            final JDAPatinaProperties jdaPatinaProperties,
            final JDAErrorReportingProperties jdaReportingProperties,
            final JDALogReportingProperties jdaLogReportingProperties) {
        this.jda = jdaClientManager.getClient();
        this.jdaPatinaProperties = jdaPatinaProperties;
        this.jdaErrorReportingProperties = jdaReportingProperties;
        this.jdaLogReportingProperties = jdaLogReportingProperties;
    }

    public List<Guild> getGuilds() {
        return jda.getGuilds();
    }

    public Guild getGuildById(final long guildId) {
        String guildIdString = String.valueOf(guildId);
        return jda.getGuilds().stream()
                .filter(g -> g.getId().equals(guildIdString))
                .findFirst()
                .orElse(null);
    }

    public List<Member> getMemberListByGuildId(final String guildId) {
        List<Guild> guilds = jda.getGuilds();

        Optional<Guild> optionalGuild =
                guilds.stream().filter(g -> g.getId().equals(guildId)).findFirst();

        if (optionalGuild.isEmpty()) {
            return List.of();
        }

        return optionalGuild.get().getMembers();
    }

    /**
     * Send a Rich Embed message with a file to the guild ID and channel ID of your choosing.
     *
     * <p>Check EmbeddedMessageOptions for details on what is supported.
     */
    public void sendEmbedWithImage(final EmbeddedMessageOptions options) {
        Guild guild = getGuildById(options.getGuildId());
        if (guild == null) {
            log.error("Guild does not exist.");
            return;
        }
        TextChannel channel = guild.getTextChannelById(options.getChannelId());
        if (channel == null) {
            log.error("Channel does not exist on the given guild.");
            return;
        }

        EmbedBuilder embedBuilder = new EmbedBuilder()
                .setTitle(options.getTitle())
                .setDescription(options.getDescription())
                .setFooter(options.getFooterText(), options.getFooterIcon())
                .setColor(options.getColor());

        if (options.getFileName().endsWith(".png") || options.getFileName().endsWith(".jpg")) {
            embedBuilder.setImage(String.format("attachment://%s", options.getFileName()));
        }

        MessageEmbed embed = embedBuilder.build();

        log.info("Message has been built, ready to send...");

        channel.sendFiles(FileUpload.fromData(options.getFileBytes(), options.getFileName()))
                .setEmbeds(embed)
                .queue();

        log.info("Message has been queued");
    }

    public void sendEmbedWithImages(final EmbeddedImagesMessageOptions options) {
        Guild guild = getGuildById(options.getGuildId());
        if (guild == null) {
            log.error("Guild does not exist.");
            return;
        }
        TextChannel channel = guild.getTextChannelById(options.getChannelId());
        if (channel == null) {
            log.error("Channel does not exist on the given guild.");
            return;
        }

        List<byte[]> filesBytes = options.getFilesBytes();
        List<String> fileNames = options.getFileNames();
        List<FileUpload> uploads = new ArrayList<>();
        List<MessageEmbed> embeds = new ArrayList<>();

        EmbedBuilder baseEmbed = new EmbedBuilder()
                .setColor(options.getColor())
                .setTitle(options.getTitle())
                .setUrl("https://codebloom.patinanetwork.org")
                .setDescription(options.getDescription())
                .setFooter(options.getFooterText(), options.getFooterIcon());

        embeds.add(baseEmbed.build());

        if (!CollectionUtils.isEmpty(fileNames) && !CollectionUtils.isEmpty(filesBytes)) {
            for (int i = 0; i < filesBytes.size(); i++) {
                String name = (fileNames != null && i < fileNames.size()) ? fileNames.get(i) : "image" + i + ".png";
                uploads.add(FileUpload.fromData(filesBytes.get(i), name));
            }

            for (int i = 0; i < fileNames.size(); i++) {
                if (i == 0) {
                    baseEmbed.setImage("attachment://" + fileNames.get(0));
                } else {

                    EmbedBuilder additionalEmbed = new EmbedBuilder()
                            .setUrl("https://codebloom.patinanetwork.org")
                            .setImage("attachment://" + fileNames.get(i));
                    embeds.add(additionalEmbed.build());
                }
            }
        }

        var messageCreationAction = channel.sendMessageEmbeds(embeds);

        if (!uploads.isEmpty()) {
            messageCreationAction.setFiles(uploads);
        }

        messageCreationAction.queue();
    }

    public boolean deleteMessageById(long channelId, long messageId) {
        TextChannel channel = jda.getTextChannelById(channelId);

        if (channel == null) {
            return false;
        }

        try {
            channel.deleteMessageById(messageId).complete();
            return true;
        } catch (ErrorResponseException e) {
            log.error("Failed to delete message.", e);
            return false;
        }
    }

    public void sendDirectMessageEmbed(final String userId, final EmbeddedDirectMessageOptions options) {
        EmbedBuilder embed = new EmbedBuilder()
                .setColor(options.getColor())
                .setTitle(options.getTitle())
                .setUrl("https://codebloom.patinanetwork.org")
                .setDescription(options.getDescription())
                .setFooter(options.getFooterText(), options.getFooterIcon());

        jda.retrieveUserById(userId)
                .flatMap(user -> user.openPrivateChannel())
                .flatMap(channel -> channel.sendMessageEmbeds(embed.build()))
                .queue(
                        msg -> log.info("DM sent to user {}", userId),
                        // User may have DMs disabled
                        throwable -> log.warn("Failed to DM user {}", userId, throwable));
    }
}
