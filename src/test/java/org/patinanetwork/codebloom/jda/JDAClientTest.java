package org.patinanetwork.codebloom.jda;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.awt.Color;
import java.util.List;
import java.util.function.Function;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.AuditableRestAction;
import net.dv8tion.jda.api.requests.restaction.CacheRestAction;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.utils.FileUpload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.patinanetwork.codebloom.jda.client.JDAClient;
import org.patinanetwork.codebloom.jda.client.options.EmbeddedDirectMessageOptions;
import org.patinanetwork.codebloom.jda.client.options.EmbeddedImagesMessageOptions;
import org.patinanetwork.codebloom.jda.client.options.EmbeddedMessageOptions;
import org.patinanetwork.codebloom.jda.properties.patina.JDAPatinaProperties;
import org.patinanetwork.codebloom.jda.properties.reporting.JDAErrorReportingProperties;
import org.patinanetwork.codebloom.jda.properties.reporting.JDALogReportingProperties;

public class JDAClientTest {
    private JDA jda = mock(JDA.class);
    private JDAPatinaProperties jdaPatinaProperties = mock(JDAPatinaProperties.class);
    private JDAErrorReportingProperties jdaErrorReportingProperties = mock(JDAErrorReportingProperties.class);
    private JDALogReportingProperties jdaLogReportingProperties = mock(JDALogReportingProperties.class);
    private JDAClientManager jdaClientManager = mock(JDAClientManager.class);
    private EmbeddedMessageOptions options = mock(EmbeddedMessageOptions.class);
    private EmbeddedImagesMessageOptions imagesOptions = mock(EmbeddedImagesMessageOptions.class);
    private EmbeddedDirectMessageOptions directOptions = mock(EmbeddedDirectMessageOptions.class);
    private User user = mock(User.class);
    private PrivateChannel privateChannel = mock(PrivateChannel.class);

    private JDAClient client;

    @BeforeEach
    void setUp() {
        when(jdaClientManager.getClient()).thenReturn(jda);
        client = new JDAClient(
                jdaClientManager, jdaPatinaProperties, jdaErrorReportingProperties, jdaLogReportingProperties);
    }

    @Test
    void testGetGuildById() {
        Guild guild1 = mock(Guild.class);
        Guild guild2 = mock(Guild.class);

        when(client.getGuilds()).thenReturn(List.of(guild1, guild2));
        when(guild1.getId()).thenReturn("123456789");

        Guild found = client.getGuildById(123456789);

        assertEquals(found, guild1);
    }

    @Test
    void testGetMemberListByGuildIdHasMember() {
        Guild guild = mock(Guild.class);
        Member m1 = mock(Member.class);
        Member m2 = mock(Member.class);

        when(guild.getId()).thenReturn("123");
        when(guild.getMembers()).thenReturn(List.of(m1, m2));
        when(jda.getGuilds()).thenReturn(List.of(guild));

        List<Member> result = client.getMemberListByGuildId("123");

        assertEquals(2, result.size());
        assertSame(m1, result.get(0));
        assertSame(m2, result.get(1));

        verify(jda).getGuilds();
        verify(guild).getMembers();
    }

    @Test
    void testGetMemberListByGuildIdHasMemberEmpty() {
        Guild guild = mock(Guild.class);

        when(guild.getId()).thenReturn("123");
        when(guild.getMembers()).thenReturn(List.of());
        when(jda.getGuilds()).thenReturn(List.of(guild));

        List<Member> result = client.getMemberListByGuildId("123");

        assertEquals(0, result.size());

        verify(jda).getGuilds();
        verify(guild).getMembers();
    }

    @Test
    void testSendEmbedWithImageNoGuild() {
        setupOptions();
        when(jda.getGuilds()).thenReturn(List.of());

        client.sendEmbedWithImage(options);

        verify(jda).getGuilds();
        verifyNoMoreInteractions(jda);
    }

    @Test
    void testSendEmbedWithImageNoChannel() {
        setupOptions();
        Guild guild = mock(Guild.class);
        when(guild.getId()).thenReturn("123456789");
        when(jda.getGuilds()).thenReturn(List.of(guild));

        when(guild.getTextChannelById(anyLong())).thenReturn(null);

        client.sendEmbedWithImage(options);

        verify(guild).getTextChannelById(987654321L);
    }

    @Test
    void testSendEmbedWithImageSuccess() {
        setupOptions();

        Guild guild = mock(Guild.class);
        TextChannel channel = mock(TextChannel.class);

        when(guild.getId()).thenReturn("123456789");
        when(jda.getGuilds()).thenReturn(List.of(guild));
        when(guild.getTextChannelById(987654321L)).thenReturn(channel);

        MessageCreateAction action = mock(MessageCreateAction.class);
        when(channel.sendFiles(any(FileUpload.class))).thenReturn(action);
        when(action.setEmbeds(any(MessageEmbed.class))).thenReturn(action);

        client.sendEmbedWithImage(options);

        verify(channel).sendFiles(any(FileUpload.class));
        verify(action).setEmbeds(any(MessageEmbed.class));
        verify(action).queue();
    }

    @Test
    void testSendEmbedWithImagesNoGuild() {
        setupImagesOptions();
        when(jda.getGuilds()).thenReturn(List.of());

        client.sendEmbedWithImages(imagesOptions);

        verify(jda).getGuilds();
        verifyNoMoreInteractions(jda);
    }

    @Test
    void testSendEmbedWithImagesNoChannel() {
        setupImagesOptions();
        Guild guild = mock(Guild.class);
        when(guild.getId()).thenReturn("123456789");
        when(jda.getGuilds()).thenReturn(List.of(guild));

        when(guild.getTextChannelById(anyLong())).thenReturn(null);

        client.sendEmbedWithImages(imagesOptions);

        verify(guild).getTextChannelById(987654321L);
    }

    @Test
    void testSendEmbedWithImagesSuccess() {
        setupImagesOptions();

        Guild guild = mock(Guild.class);
        TextChannel channel = mock(TextChannel.class);

        when(guild.getId()).thenReturn("123456789");
        when(jda.getGuilds()).thenReturn(List.of(guild));
        when(guild.getTextChannelById(987654321L)).thenReturn(channel);

        MessageCreateAction action = mock(MessageCreateAction.class);
        when(channel.sendMessageEmbeds(anyList())).thenReturn(action);
        when(action.setFiles(anyList())).thenReturn(action);

        client.sendEmbedWithImages(imagesOptions);

        ArgumentCaptor<List<MessageEmbed>> embedsCaptor = ArgumentCaptor.forClass(List.class);
        verify(channel).sendMessageEmbeds(embedsCaptor.capture());
        List<MessageEmbed> embeds = embedsCaptor.getValue();

        assertNotNull(embeds);

        verify(action).setFiles(anyList());
    }

    @Test
    void testDeleteMessageId() {
        TextChannel mockChannel = mock(TextChannel.class);
        AuditableRestAction<Void> mockDeleteAction = mock(AuditableRestAction.class);

        when(jda.getTextChannelById(anyLong())).thenReturn(mockChannel);
        when(mockChannel.deleteMessageById(anyLong())).thenReturn(mockDeleteAction);
        when(mockDeleteAction.complete()).thenReturn(null);

        boolean result = client.deleteMessageById(987654321L, 1223334444L);
        assertTrue(result);
        verify(mockChannel).deleteMessageById(1223334444L);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testDirectMessageWithEmbedSuccess() {
        setupDirectOptions();

        CacheRestAction<User> action = mock(CacheRestAction.class);
        when(jda.retrieveUserById("user123")).thenReturn(action);
        doReturn(action).when(action).flatMap(any());

        client.sendDirectMessageEmbed("user123", directOptions);

        verify(action).queue(any(), any());
        runFlatMapSteps(action, user, privateChannel);
        verify(user).openPrivateChannel();

        ArgumentCaptor<MessageEmbed> embedCaptor = ArgumentCaptor.forClass(MessageEmbed.class);
        verify(privateChannel).sendMessageEmbeds(embedCaptor.capture());
        MessageEmbed embed = embedCaptor.getValue();

        assertEquals("Title", embed.getTitle());
        assertEquals("Desc", embed.getDescription());
        assertEquals("https://codebloom.patinanetwork.org", embed.getUrl());
        assertEquals(Color.BLACK, embed.getColor());
        assertEquals("Footer", embed.getFooter().getText());
        assertEquals("https://example.com/icon.png", embed.getFooter().getIconUrl());
    }

    @SuppressWarnings("unchecked")
    private static void runFlatMapSteps(RestAction<?> action, Object... inputs) {
        ArgumentCaptor<Function<Object, RestAction<Object>>> steps = ArgumentCaptor.forClass(Function.class);
        verify(action, times(inputs.length)).flatMap(steps.capture());
        for (int i = 0; i < inputs.length; i++) {
            steps.getAllValues().get(i).apply(inputs[i]);
        }
    }

    private void setupOptions() {
        when(options.getGuildId()).thenReturn(123456789L);
        when(options.getChannelId()).thenReturn(987654321L);
        when(options.getTitle()).thenReturn("Title");
        when(options.getDescription()).thenReturn("Desc");
        when(options.getFooterText()).thenReturn("Footer");
        when(options.getFooterIcon()).thenReturn("https://example.com/icon.png");
        when(options.getColor()).thenReturn(Color.BLACK);

        when(options.getFileName()).thenReturn("img.png");
        when(options.getFileBytes()).thenReturn(new byte[] {1, 2, 3});
    }

    private void setupImagesOptions() {
        when(imagesOptions.getGuildId()).thenReturn(123456789L);
        when(imagesOptions.getChannelId()).thenReturn(987654321L);
        when(imagesOptions.getTitle()).thenReturn("Title");
        when(imagesOptions.getDescription()).thenReturn("Desc");
        when(imagesOptions.getFooterText()).thenReturn("Footer");
        when(imagesOptions.getFooterIcon()).thenReturn("https://example.com/icon.png");
        when(imagesOptions.getColor()).thenReturn(Color.BLACK);

        when(imagesOptions.getFilesBytes()).thenReturn(List.of(new byte[] {1}, new byte[] {2}));
        when(imagesOptions.getFileNames()).thenReturn(List.of("a.png", "b.png"));
    }

    private void setupDirectOptions() {
        when(directOptions.getTitle()).thenReturn("Title");
        when(directOptions.getDescription()).thenReturn("Desc");
        when(directOptions.getFooterText()).thenReturn("Footer");
        when(directOptions.getFooterIcon()).thenReturn("https://example.com/icon.png");
        when(directOptions.getColor()).thenReturn(Color.BLACK);
    }
}
