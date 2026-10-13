package org.patinanetwork.codebloom.api.auth.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.patinanetwork.codebloom.common.db.models.Session;
import org.patinanetwork.codebloom.common.db.models.discord.DiscordClub;
import org.patinanetwork.codebloom.common.db.models.discord.DiscordClubMetadata;
import org.patinanetwork.codebloom.common.db.models.leaderboard.Leaderboard;
import org.patinanetwork.codebloom.common.db.models.user.User;
import org.patinanetwork.codebloom.common.db.models.usertag.Tag;
import org.patinanetwork.codebloom.common.db.models.usertag.UserTag;
import org.patinanetwork.codebloom.common.db.repos.discord.club.DiscordClubRepository;
import org.patinanetwork.codebloom.common.db.repos.leaderboard.LeaderboardRepository;
import org.patinanetwork.codebloom.common.db.repos.session.SessionRepository;
import org.patinanetwork.codebloom.common.db.repos.user.UserRepository;
import org.patinanetwork.codebloom.common.db.repos.usertag.UserTagRepository;
import org.patinanetwork.codebloom.common.leetcode.models.UserProfile;
import org.patinanetwork.codebloom.common.leetcode.throttled.ThrottledLeetcodeClient;
import org.patinanetwork.codebloom.jda.client.JDAClient;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.core.user.OAuth2User;

@DisplayName("CustomAuthenticationSuccessHandler")
class CustomAuthenticationSuccessHandlerTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final SessionRepository sessionRepository = mock(SessionRepository.class);
    private final LeaderboardRepository leaderboardRepository = mock(LeaderboardRepository.class);
    private final UserTagRepository userTagRepository = mock(UserTagRepository.class);
    private final DiscordClubRepository discordClubRepository = mock(DiscordClubRepository.class);
    private final JDAClient jdaClient = mock(JDAClient.class);
    private final ThrottledLeetcodeClient leetcodeClient = mock(ThrottledLeetcodeClient.class);

    private final HttpServletRequest request = mock(HttpServletRequest.class);
    private final HttpServletResponse response = mock(HttpServletResponse.class);
    private final Authentication authentication = mock(Authentication.class);

    private final CustomAuthenticationSuccessHandler handler = new CustomAuthenticationSuccessHandler(
            userRepository,
            sessionRepository,
            leaderboardRepository,
            jdaClient,
            userTagRepository,
            discordClubRepository,
            leetcodeClient);

    private static final String DISCORD_ID = "123456789";
    private static final String DISCORD_NAME = "testuser";

    private final OAuth2User oAuth2User = mock(OAuth2User.class);

    {
        when(oAuth2User.getAttributes()).thenReturn(Map.of("id", DISCORD_ID, "username", DISCORD_NAME));
    }

    @BeforeEach
    void commonStubs() {
        when(jdaClient.getGuilds()).thenReturn(List.of());
        when(discordClubRepository.getAllActiveDiscordClubs()).thenReturn(List.of());
    }

    @Test
    @DisplayName("updates the discord name and creates a session cookie")
    void updatesNameAndSetsCookie() throws Exception {
        User existingUser = User.builder()
                .id("user-1")
                .discordId(DISCORD_ID)
                .discordName("old-name")
                .build();
        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        when(userRepository.getUserByDiscordId(DISCORD_ID)).thenReturn(Optional.of(existingUser));
        doAnswer(inv -> {
                    Session s = inv.getArgument(0);
                    s.setId(Optional.of("session-abc"));
                    return null;
                })
                .when(sessionRepository)
                .createSession(any(Session.class));

        handler.onAuthenticationSuccess(request, response, authentication);

        assertEquals(DISCORD_NAME, existingUser.getDiscordName());
        verify(userRepository).updateUser(existingUser);

        ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);
        verify(response).addCookie(cookieCaptor.capture());
        Cookie cookie = cookieCaptor.getValue();
        assertEquals("session_token", cookie.getName());
        assertTrue(cookie.isHttpOnly());
        assertTrue(cookie.getSecure());

        verify(response).sendRedirect("/dashboard");
    }

    @Test
    @DisplayName("updates profile URL when the user has a leetcode username")
    void updatesProfileUrl() throws Exception {
        User existingUser = User.builder()
                .id("user-1")
                .discordId(DISCORD_ID)
                .discordName("old-name")
                .leetcodeUsername(Optional.of("leet_user"))
                .build();
        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        when(userRepository.getUserByDiscordId(DISCORD_ID)).thenReturn(Optional.of(existingUser));
        UserProfile profile = new UserProfile("leet_user", "1000", "https://avatar.url", null, null);
        when(leetcodeClient.getUserProfile("leet_user")).thenReturn(profile);
        doAnswer(inv -> {
                    Session s = inv.getArgument(0);
                    s.setId(Optional.of("session-xyz"));
                    return null;
                })
                .when(sessionRepository)
                .createSession(any(Session.class));

        handler.onAuthenticationSuccess(request, response, authentication);

        assertEquals("https://avatar.url", existingUser.getProfileUrl().orElse(null));
    }

    @Test
    @DisplayName("still succeeds if LeetCode profile lookup throws")
    void survivesLeetcodeLookupFailure() throws Exception {
        User existingUser = User.builder()
                .id("user-1")
                .discordId(DISCORD_ID)
                .discordName("old-name")
                .leetcodeUsername(Optional.of("bad_user"))
                .build();
        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        when(userRepository.getUserByDiscordId(DISCORD_ID)).thenReturn(Optional.of(existingUser));
        when(leetcodeClient.getUserProfile("bad_user")).thenThrow(new RuntimeException("API down"));
        doAnswer(inv -> {
                    Session s = inv.getArgument(0);
                    s.setId(Optional.of("session-fail-safe"));
                    return null;
                })
                .when(sessionRepository)
                .createSession(any(Session.class));

        assertDoesNotThrow(() -> handler.onAuthenticationSuccess(request, response, authentication));
        verify(response).sendRedirect("/dashboard");
    }

    @Test
    @DisplayName("creates a user, adds them to the leaderboard, and sets session cookie")
    void createsUserAndAddsToLeaderboard() throws Exception {
        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        when(userRepository.getUserByDiscordId(DISCORD_ID)).thenReturn(Optional.empty());
        Leaderboard lb = Leaderboard.builder().id("lb-1").build();
        when(leaderboardRepository.getRecentLeaderboardMetadata()).thenReturn(Optional.of(lb));
        doAnswer(inv -> {
                    Session s = inv.getArgument(0);
                    s.setId(Optional.of("new-session-id"));
                    return null;
                })
                .when(sessionRepository)
                .createSession(any(Session.class));

        handler.onAuthenticationSuccess(request, response, authentication);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).createUser(userCaptor.capture());
        var createdUser = userCaptor.getValue();
        assertEquals(DISCORD_ID, createdUser.getDiscordId());
        assertEquals(DISCORD_NAME, createdUser.getDiscordName());
        verify(leaderboardRepository).addUserToLeaderboard(any(), eq("lb-1"));
        verify(response).sendRedirect("/dashboard");
    }

    @Test
    @DisplayName("creates a tag when the user is a member of a guild linked to a club")
    void assignsTagForGuildMember() throws Exception {
        User existingUser = User.builder()
                .id("user-club")
                .discordId(DISCORD_ID)
                .discordName(DISCORD_NAME)
                .build();
        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        when(userRepository.getUserByDiscordId(DISCORD_ID)).thenReturn(Optional.of(existingUser));
        doAnswer(inv -> {
                    Session s = inv.getArgument(0);
                    s.setId(Optional.of("s-club"));
                    return null;
                })
                .when(sessionRepository)
                .createSession(any(Session.class));

        Guild guild = mock(Guild.class);
        when(guild.getId()).thenReturn("guild-1");
        Member member = mock(Member.class);
        when(member.getId()).thenReturn(DISCORD_ID);
        when(guild.getMembers()).thenReturn(List.of(member));
        when(jdaClient.getGuilds()).thenReturn(List.of(guild));

        DiscordClub club = DiscordClub.builder()
                .name("Hunter College")
                .tag(Tag.Hunter)
                .discordClubMetadata(Optional.of(DiscordClubMetadata.builder()
                        .guildId(Optional.of("guild-1"))
                        .build()))
                .build();
        when(discordClubRepository.getAllActiveDiscordClubs()).thenReturn(List.of(club));
        when(userTagRepository.findTagByUserIdAndTag("user-club", Tag.Hunter)).thenReturn(Optional.empty());

        handler.onAuthenticationSuccess(request, response, authentication);

        ArgumentCaptor<UserTag> tagCaptor = ArgumentCaptor.forClass(UserTag.class);
        verify(userTagRepository).createTag(tagCaptor.capture());
        assertEquals(Tag.Hunter, tagCaptor.getValue().getTag());
    }

    @Test
    @DisplayName("skips tag creation when user already has the tag")
    void skipsExistingTag() throws Exception {
        User existingUser = User.builder()
                .id("user-club")
                .discordId(DISCORD_ID)
                .discordName(DISCORD_NAME)
                .build();
        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        when(userRepository.getUserByDiscordId(DISCORD_ID)).thenReturn(Optional.of(existingUser));
        doAnswer(inv -> {
                    Session s = inv.getArgument(0);
                    s.setId(Optional.of("s-club"));
                    return null;
                })
                .when(sessionRepository)
                .createSession(any(Session.class));

        Guild guild = mock(Guild.class);
        when(guild.getId()).thenReturn("guild-1");
        Member member = mock(Member.class);
        when(member.getId()).thenReturn(DISCORD_ID);
        when(guild.getMembers()).thenReturn(List.of(member));
        when(jdaClient.getGuilds()).thenReturn(List.of(guild));

        DiscordClub club = DiscordClub.builder()
                .name("Hunter College")
                .tag(Tag.Hunter)
                .discordClubMetadata(Optional.of(DiscordClubMetadata.builder()
                        .guildId(Optional.of("guild-1"))
                        .build()))
                .build();
        when(discordClubRepository.getAllActiveDiscordClubs()).thenReturn(List.of(club));
        when(userTagRepository.findTagByUserIdAndTag("user-club", Tag.Hunter))
                .thenReturn(Optional.of(UserTag.builder().build()));

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(userTagRepository, never()).createTag(any());
    }

    @Test
    @DisplayName("sets nickname from guild member when club is Patina Network")
    void setsNicknameForPatinaClub() throws Exception {
        User existingUser = User.builder()
                .id("user-club")
                .discordId(DISCORD_ID)
                .discordName(DISCORD_NAME)
                .build();
        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        when(userRepository.getUserByDiscordId(DISCORD_ID)).thenReturn(Optional.of(existingUser));
        doAnswer(inv -> {
                    Session s = inv.getArgument(0);
                    s.setId(Optional.of("s-club"));
                    return null;
                })
                .when(sessionRepository)
                .createSession(any(Session.class));

        Guild guild = mock(Guild.class);
        when(guild.getId()).thenReturn("guild-patina");
        Member member = mock(Member.class);
        when(member.getId()).thenReturn(DISCORD_ID);
        when(member.getNickname()).thenReturn("Cool Nickname");
        when(guild.getMembers()).thenReturn(List.of(member));
        when(jdaClient.getGuilds()).thenReturn(List.of(guild));

        DiscordClub patinaClub = DiscordClub.builder()
                .name("Patina Network")
                .tag(Tag.Patina)
                .discordClubMetadata(Optional.of(DiscordClubMetadata.builder()
                        .guildId(Optional.of("guild-patina"))
                        .build()))
                .build();
        when(discordClubRepository.getAllActiveDiscordClubs()).thenReturn(List.of(patinaClub));
        when(userTagRepository.findTagByUserIdAndTag("user-club", Tag.Patina)).thenReturn(Optional.empty());

        handler.onAuthenticationSuccess(request, response, authentication);

        assertEquals("Cool Nickname", existingUser.getNickname().orElse(null));
    }

    @Test
    @DisplayName("falls back to global name when guild nickname is null for Patina Network")
    void fallsBackToGlobalNameForPatina() throws Exception {
        User existingUser = User.builder()
                .id("user-club")
                .discordId(DISCORD_ID)
                .discordName(DISCORD_NAME)
                .build();
        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        when(userRepository.getUserByDiscordId(DISCORD_ID)).thenReturn(Optional.of(existingUser));
        doAnswer(inv -> {
                    Session s = inv.getArgument(0);
                    s.setId(Optional.of("s-club"));
                    return null;
                })
                .when(sessionRepository)
                .createSession(any(Session.class));

        Guild guild = mock(Guild.class);
        when(guild.getId()).thenReturn("guild-patina");
        Member member = mock(Member.class);
        when(member.getId()).thenReturn(DISCORD_ID);
        when(member.getNickname()).thenReturn(null);
        net.dv8tion.jda.api.entities.User discordUser = mock(net.dv8tion.jda.api.entities.User.class);
        when(discordUser.getGlobalName()).thenReturn("Global Name");
        when(member.getUser()).thenReturn(discordUser);
        when(guild.getMembers()).thenReturn(List.of(member));
        when(jdaClient.getGuilds()).thenReturn(List.of(guild));

        DiscordClub patinaClub = DiscordClub.builder()
                .name("Patina Network")
                .tag(Tag.Patina)
                .discordClubMetadata(Optional.of(DiscordClubMetadata.builder()
                        .guildId(Optional.of("guild-patina"))
                        .build()))
                .build();
        when(discordClubRepository.getAllActiveDiscordClubs()).thenReturn(List.of(patinaClub));
        when(userTagRepository.findTagByUserIdAndTag("user-club", Tag.Patina)).thenReturn(Optional.empty());

        handler.onAuthenticationSuccess(request, response, authentication);

        assertEquals("Global Name", existingUser.getNickname().orElse(null));
    }

    @Test
    @DisplayName("skips clubs whose metadata has no guildId")
    void skipsClubWithNoGuildId() throws Exception {
        User existingUser = User.builder()
                .id("user-club")
                .discordId(DISCORD_ID)
                .discordName(DISCORD_NAME)
                .build();
        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        when(userRepository.getUserByDiscordId(DISCORD_ID)).thenReturn(Optional.of(existingUser));
        doAnswer(inv -> {
                    Session s = inv.getArgument(0);
                    s.setId(Optional.of("s-club"));
                    return null;
                })
                .when(sessionRepository)
                .createSession(any(Session.class));

        DiscordClub clubNoGuild = DiscordClub.builder()
                .name("No Guild Club")
                .tag(Tag.Nyu)
                .discordClubMetadata(Optional.of(
                        DiscordClubMetadata.builder().guildId(Optional.empty()).build()))
                .build();
        when(discordClubRepository.getAllActiveDiscordClubs()).thenReturn(List.of(clubNoGuild));

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(userTagRepository, never()).createTag(any());
    }

    @Test
    @DisplayName("redirects and throws when the principal is not an OAuth2User")
    void nonOAuthPrincipalFails() {
        when(authentication.getPrincipal()).thenReturn("not-an-oauth-user");

        assertThrows(
                UsernameNotFoundException.class,
                () -> handler.onAuthenticationSuccess(request, response, authentication));
    }

    @Test
    @DisplayName("redirects and throws when session creation returns a null ID")
    void sessionCreationFailureRedirects() throws Exception {
        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        var existingUser = User.builder()
                .id("user-fail")
                .discordId(DISCORD_ID)
                .discordName(DISCORD_NAME)
                .build();
        when(userRepository.getUserByDiscordId(DISCORD_ID)).thenReturn(Optional.of(existingUser));

        doNothing().when(sessionRepository).createSession(any(Session.class));

        assertThrows(RuntimeException.class, () -> handler.onAuthenticationSuccess(request, response, authentication));
    }
}
