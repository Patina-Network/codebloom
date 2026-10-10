package org.patinanetwork.codebloom.api.auth.security;

import io.micrometer.core.annotation.Timed;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import org.patinanetwork.codebloom.common.db.models.Session;
import org.patinanetwork.codebloom.common.db.models.discord.DiscordClubMetadata;
import org.patinanetwork.codebloom.common.db.models.user.User;
import org.patinanetwork.codebloom.common.db.models.usertag.UserTag;
import org.patinanetwork.codebloom.common.db.repos.discord.club.DiscordClubRepository;
import org.patinanetwork.codebloom.common.db.repos.leaderboard.LeaderboardRepository;
import org.patinanetwork.codebloom.common.db.repos.session.SessionRepository;
import org.patinanetwork.codebloom.common.db.repos.user.UserRepository;
import org.patinanetwork.codebloom.common.db.repos.usertag.UserTagRepository;
import org.patinanetwork.codebloom.common.leetcode.LeetcodeClient;
import org.patinanetwork.codebloom.common.leetcode.models.UserProfile;
import org.patinanetwork.codebloom.common.leetcode.throttled.ThrottledLeetcodeClient;
import org.patinanetwork.codebloom.common.time.StandardizedLocalDateTime;
import org.patinanetwork.codebloom.common.utils.pair.Pair;
import org.patinanetwork.codebloom.jda.client.JDAClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

/**
 * Handles successful OAuth2 login by either creating a new user or referencing an old user and generating a cookie
 * storing the session ID.
 *
 * @see <a href= "https://github.com/tahminator/codebloom/tree/main/docs/auth.md">Authentication Documentation</a>
 */
@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(CustomAuthenticationSuccessHandler.class);

    // 30 days expiration
    private final int maxAgeSeconds = 60 * 60 * 24 * 30;

    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final LeaderboardRepository leaderboardRepository;
    private final UserTagRepository userTagRepository;
    private final DiscordClubRepository discordClubRepository;
    private final JDAClient jdaClient;
    private final LeetcodeClient leetcodeClient;

    public CustomAuthenticationSuccessHandler(
            final UserRepository userRepository,
            final SessionRepository sessionRepository,
            final LeaderboardRepository leaderboardRepository,
            final JDAClient jdaClient,
            final UserTagRepository userTagRepository,
            final DiscordClubRepository discordClubRepository,
            final ThrottledLeetcodeClient throttledLeetcodeClient) {
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.leaderboardRepository = leaderboardRepository;
        this.discordClubRepository = discordClubRepository;
        this.jdaClient = jdaClient;
        this.userTagRepository = userTagRepository;
        this.leetcodeClient = throttledLeetcodeClient;
    }

    @Override
    @Timed(value = "controller.execution")
    public void onAuthenticationSuccess(
            final HttpServletRequest request, final HttpServletResponse response, final Authentication authentication)
            throws IOException, ServletException {
        Object principal = authentication.getPrincipal();
        String discordId = null;
        String discordName = null;

        if (principal instanceof OAuth2User) {
            OAuth2User oAuth2User = (OAuth2User) principal;
            Map<String, Object> attributes = oAuth2User.getAttributes();
            discordId = attributes.get("id").toString();
            discordName = attributes.get("username").toString();

            var existingUserResult = userRepository.getUserByDiscordId(discordId);
            User existingUser;

            if (existingUserResult.isPresent()) {
                existingUser = existingUserResult.get();
                existingUser.setDiscordName(discordName);
                if (existingUser.getLeetcodeUsername().isPresent()) {
                    try {
                        UserProfile profile = leetcodeClient.getUserProfile(
                                existingUser.getLeetcodeUsername().orElseThrow());
                        if (profile != null && profile.getUserAvatar() != null) {
                            existingUser.setProfileUrl(Optional.ofNullable(profile.getUserAvatar()));
                        }
                    } catch (RuntimeException ex) {
                        LOGGER.warn(
                                "LeetCode lookup failed for {}",
                                existingUser.getLeetcodeUsername().orElseThrow(),
                                ex);
                    }
                }
                userRepository.updateUser(existingUser);
            } else {
                User newUser = User.builder()
                        .discordId(discordId)
                        .discordName(discordName)
                        .build();
                userRepository.createUser(newUser);
                leaderboardRepository
                        .getRecentLeaderboardMetadata()
                        .ifPresent(lb -> leaderboardRepository.addUserToLeaderboard(newUser.getId(), lb.getId()));
                existingUser = newUser;
            }

            final String userDiscordId = existingUser.getDiscordId();
            final String userId = existingUser.getId();

            List<Guild> guilds = jdaClient.getGuilds();
            Map<String, List<Member>> guildIdToMembersMap = guilds.parallelStream()
                    .map(g -> Pair.of(g, g.getMembers()))
                    .collect(Collectors.toMap(p -> p.getLeft().getId(), p -> p.getRight()));

            var clubs = discordClubRepository.getAllActiveDiscordClubs();

            for (var club : clubs) {
                var metadata = club.getDiscordClubMetadata();
                var guildId = metadata.flatMap(DiscordClubMetadata::getGuildId);
                var tag = club.getTag();

                if (guildId.isEmpty()) {
                    continue;
                }

                var memberList = guildIdToMembersMap.get(guildId.get());
                if (memberList == null) {
                    continue;
                }
                var member = memberList.stream()
                        .filter(m -> m.getId().equals(userDiscordId))
                        .findFirst();

                if (member.isEmpty()) {
                    continue;
                }

                userTagRepository
                        .findTagByUserIdAndTag(userId, tag)
                        .ifPresentOrElse(
                                existing -> {},
                                () -> userTagRepository.createTag(UserTag.builder()
                                        .userId(userId)
                                        .tag(tag)
                                        .build()));

                // override to handle nicknames
                // TODO: Abstract this logic into `DiscordClub`
                if ("Patina Network".equals(club.getName())) {
                    if (member.get().getNickname() != null) {
                        existingUser.setNickname(
                                Optional.ofNullable(member.get().getNickname()));
                    } else if (member.get().getUser().getGlobalName() != null) {
                        existingUser.setNickname(
                                Optional.ofNullable(member.get().getUser().getGlobalName()));
                    } else {
                        existingUser.setNickname(Optional.ofNullable(existingUser.getDiscordName()));
                    }
                    userRepository.updateUser(existingUser);
                }
            }

            LocalDateTime expirationTime = StandardizedLocalDateTime.now().plusSeconds(maxAgeSeconds);

            Session session = Session.builder()
                    .userId(existingUser.getId())
                    .expiresAt(expirationTime)
                    .build();
            sessionRepository.createSession(session);

            if (session.getId().isEmpty()) {
                response.sendRedirect("/login?success=false&message=Failed to log in.");
                throw new RuntimeException("Failed to create new session.");
            }

            Cookie cookie = new Cookie("session_token", session.getId().get());
            cookie.setMaxAge(maxAgeSeconds);

            cookie.setHttpOnly(true);
            cookie.setSecure(true);
            cookie.setPath("/");

            response.addCookie(cookie);
            response.sendRedirect("/dashboard");
        } else {
            response.sendRedirect("/login?success=false&message=Failed to log in.");
            throw new UsernameNotFoundException("User not found");
        }
    }
}
