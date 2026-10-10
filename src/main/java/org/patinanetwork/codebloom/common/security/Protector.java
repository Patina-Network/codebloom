package org.patinanetwork.codebloom.common.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Optional;
import org.patinanetwork.codebloom.common.db.models.Session;
import org.patinanetwork.codebloom.common.db.models.user.User;
import org.patinanetwork.codebloom.common.db.repos.session.SessionRepository;
import org.patinanetwork.codebloom.common.db.repos.user.UserRepository;
import org.patinanetwork.codebloom.common.time.StandardizedLocalDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Protector is used to validate whether or not the user is logged in or not.
 *
 * @see <a href= "https://github.com/tahminator/codebloom/blob/main/docs/backend/auth.md">Authentication
 *     Documentation</a>
 */
@Component
public class Protector {

    private final SessionRepository sessionRepository;
    private final UserRepository userRepository;

    public Protector(final SessionRepository sessionRepository, final UserRepository userRepository) {
        this.sessionRepository = sessionRepository;
        this.userRepository = userRepository;
    }

    public AuthenticationObject validateSession(final HttpServletRequest request) {
        if (request.getCookies() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }

        for (Cookie cookie : request.getCookies()) {
            if ("session_token".equals(cookie.getName()) && !cookie.getValue().isEmpty()) {
                String sessionToken = cookie.getValue();

                Optional<Session> optSession = sessionRepository.getSessionById(sessionToken);

                if (optSession.isEmpty()) {
                    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
                }

                Session session = optSession.get();

                LocalDateTime now = StandardizedLocalDateTime.now();

                if (session.getExpiresAt().isBefore(now)) {
                    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
                }

                User user = userRepository
                        .getUserById(session.getUserId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized"));

                return new AuthenticationObject(user, session);
            }
        }

        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
    }

    public AuthenticationObject validateAdminSession(final HttpServletRequest request) {
        AuthenticationObject admin = validateSession(request);
        if (!admin.getUser().isAdmin()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return admin;
    }
}
