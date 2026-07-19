package org.patinanetwork.codebloom.api.auth;

import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.time.Duration;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.patinanetwork.codebloom.api.auth.body.EmailBody;
import org.patinanetwork.codebloom.common.db.models.Session;
import org.patinanetwork.codebloom.common.db.models.user.User;
import org.patinanetwork.codebloom.common.db.models.usertag.UserTag;
import org.patinanetwork.codebloom.common.db.repos.session.SessionRepository;
import org.patinanetwork.codebloom.common.db.repos.user.UserRepository;
import org.patinanetwork.codebloom.common.db.repos.usertag.UserTagRepository;
import org.patinanetwork.codebloom.common.dto.ApiResponder;
import org.patinanetwork.codebloom.common.dto.Empty;
import org.patinanetwork.codebloom.common.dto.autogen.UnsafeGenericFailureResponse;
import org.patinanetwork.codebloom.common.dto.security.AuthenticationObjectDto;
import org.patinanetwork.codebloom.common.email.client.codebloom.OfficialCodebloomEmailClient;
import org.patinanetwork.codebloom.common.email.error.EmailException;
import org.patinanetwork.codebloom.common.email.options.SendEmailOptions;
import org.patinanetwork.codebloom.common.email.template.ReactEmailTemplater;
import org.patinanetwork.codebloom.common.jwt.JWTClient;
import org.patinanetwork.codebloom.common.reporter.Reporter;
import org.patinanetwork.codebloom.common.reporter.report.Report;
import org.patinanetwork.codebloom.common.schools.SchoolEnum;
import org.patinanetwork.codebloom.common.schools.magic.MagicLink;
import org.patinanetwork.codebloom.common.security.AuthenticationObject;
import org.patinanetwork.codebloom.common.security.Protector;
import org.patinanetwork.codebloom.common.security.annotation.Protected;
import org.patinanetwork.codebloom.common.simpleredis.SimpleRedis;
import org.patinanetwork.codebloom.common.simpleredis.SimpleRedisProvider;
import org.patinanetwork.codebloom.common.simpleredis.SimpleRedisSlot;
import org.patinanetwork.codebloom.common.url.ServerUrlUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.view.RedirectView;

@RestController
@Tag(name = "Authentication Routes")
@RequestMapping("/api/auth")
@Timed(value = "controller.execution")
public class AuthController {

    private static final double SECONDS_TO_WAIT = 10;

    private final SessionRepository sessionRepository;
    private final Protector protector;
    private final JWTClient jwtClient;
    private final UserRepository userRepository;
    private final OfficialCodebloomEmailClient emailClient;
    private final ServerUrlUtils serverUrlUtils;
    private final UserTagRepository userTagRepository;
    private final Reporter reporter;
    private final ReactEmailTemplater reactEmailTemplater;
    private final SimpleRedis<Long> simpleRedis;

    public AuthController(
            final SessionRepository sessionRepository,
            final Protector protector,
            final JWTClient jwtClient,
            final UserRepository userRepository,
            final OfficialCodebloomEmailClient emailClient,
            final ServerUrlUtils serverUrlUtils,
            final UserTagRepository userTagRepository,
            final Reporter reporter,
            final ReactEmailTemplater reactEmailTemplater,
            final SimpleRedisProvider simpleRedisProvider) {
        this.sessionRepository = sessionRepository;
        this.protector = protector;
        this.userRepository = userRepository;
        this.jwtClient = jwtClient;
        this.emailClient = emailClient;
        this.serverUrlUtils = serverUrlUtils;
        this.userTagRepository = userTagRepository;
        this.reporter = reporter;
        this.reactEmailTemplater = reactEmailTemplater;
        this.simpleRedis = simpleRedisProvider.select(SimpleRedisSlot.VERIFICATION_EMAIL_SENDING);
    }

    @Operation(
            summary = "Validate if the user is authenticated or not.",
            responses = {
                @ApiResponse(responseCode = "200", description = "Authenticated"),
                @ApiResponse(
                        responseCode = "401",
                        description = "Not authenticated",
                        content = @Content(schema = @Schema(implementation = UnsafeGenericFailureResponse.class))),
            })
    @GetMapping("/validate")
    public ResponseEntity<ApiResponder<AuthenticationObjectDto>> validateAuth(
            @Protected final AuthenticationObject authenticationObject) {

        return ResponseEntity.ok()
                .body(ApiResponder.success(
                        "You are authenticated!",
                        AuthenticationObjectDto.fromAuthenticationObject(authenticationObject)));
    }

    // Decided to make this redirect to routes, with a message query if needed,
    // keeping it inline with the logic of the authentication handler.
    @Operation(
            summary = "Logs user out",
            description =
                    "Logs the user out if currently authenticated. This is a Redirect route that does redirects as responses.",
            responses = {
                @ApiResponse(
                        responseCode = "302",
                        description =
                                "Redirect to `/login?success=true&message=\"Successful logout message here.\"` on successful authentication.",
                        content = @Content),
            })
    @GetMapping("/logout")
    public RedirectView logout(final HttpServletRequest request, final HttpServletResponse response) {
        try {
            AuthenticationObject authenticationObject = protector.validateSession(request);

            Session session = authenticationObject.getSession();

            String sessionId =
                    session.getId().orElseThrow(() -> new IllegalStateException("Authenticated session has no id"));
            boolean sessionDeleted = sessionRepository.deleteSessionById(sessionId);

            if (!sessionDeleted) {
                return new RedirectView("/login?success=false&message=You are not logged in.");
            }

            ResponseCookie strippedCookie = ResponseCookie.from("session_token", "")
                    .path("/")
                    .httpOnly(true)
                    .maxAge(0)
                    .build();
            response.addHeader(HttpHeaders.SET_COOKIE, strippedCookie.toString());

            return new RedirectView("/login?success=true&message=You have been logged out!");
        } catch (Exception e) {
            return new RedirectView("/login?success=false&message=You are not logged in.");
        }
    }

    @Operation(
            summary = "Logs user out from all sessions",
            description =
                    "Logs the user out from all authenticated sessions across all devices. This is a Redirect route that does redirects as responses.",
            responses = {
                @ApiResponse(
                        responseCode = "302",
                        description =
                                "Redirect to `/login?success=true&message=\"Successful logout message here.\"` on successful authentication.",
                        content = @Content),
            })
    @GetMapping("/logout/all")
    public RedirectView logoutAll(final HttpServletRequest request, final HttpServletResponse response) {
        try {
            AuthenticationObject authenticationObject = protector.validateSession(request);

            String userId = authenticationObject.getUser().getId();

            sessionRepository.deleteSessionsByUserId(userId);

            ResponseCookie strippedCookie = ResponseCookie.from("session_token", "")
                    .path("/")
                    .httpOnly(true)
                    .maxAge(0)
                    .build();
            response.addHeader(HttpHeaders.SET_COOKIE, strippedCookie.toString());

            return new RedirectView("/login?success=true&message=You have been logged out from all devices!");
        } catch (Exception e) {
            return new RedirectView("/login?success=false&message=You are not logged in.");
        }
    }

    @Operation(
            summary = "Enroll with a school email (if supported)",
            description = """
            Allows users to submit a school-specific email if supported. Emails will be verified with a magic link sent to their email.
                """,
            responses = {
                @ApiResponse(responseCode = "200", description = "email send successfully"),
                @ApiResponse(responseCode = "500", description = "not implemented"),
            })
    @PostMapping("/school/enroll")
    public ResponseEntity<ApiResponder<Empty>> enrollSchool(
            @Valid @RequestBody final EmailBody emailBody, @Protected final AuthenticationObject authenticationObject) {
        User user = authenticationObject.getUser();
        String userId = user.getId();

        String email = emailBody.getEmail();
        String domain = email.substring(email.indexOf("@")).toLowerCase();
        Set<String> supportedDomains = Stream.of(SchoolEnum.values())
                .map(school -> school.getEmailDomain())
                .collect(Collectors.toSet());

        if (!supportedDomains.contains(domain)) {
            String supportedSchools = String.join(", ", supportedDomains);
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "The email is not part of our supported schools domains: " + supportedSchools);
        }

        if (simpleRedis.containsKey(userId)) {
            long timeThen = simpleRedis.get(userId);
            long timeNow = System.currentTimeMillis();
            long difference = (timeNow - timeThen) / 1000;

            if (difference < SECONDS_TO_WAIT) {
                long remainingTime = (long) SECONDS_TO_WAIT - difference;
                throw new ResponseStatusException(
                        HttpStatus.TOO_MANY_REQUESTS,
                        "Please try again in " + Long.toString(remainingTime) + " seconds.");
            }
        }

        simpleRedis.put(userId, System.currentTimeMillis());

        MagicLink magicLink = new MagicLink(email, userId);
        try {
            String token = jwtClient.encode(magicLink, Duration.ofHours(1));
            String verificationLink = serverUrlUtils.getUrl() + "/api/auth/school/verify?state=" + token;
            emailClient.sendMessage(SendEmailOptions.builder()
                    .recipientEmail(email)
                    .subject("Hello from Codebloom!")
                    .body(reactEmailTemplater.schoolEmailTemplate(verificationLink))
                    .build());
            return ResponseEntity.ok()
                    .body(ApiResponder.success("Magic link sent! Check your school inbox to continue.", Empty.of()));
        } catch (EmailException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to send email.");
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Error processing request: not implemented");
        }
    }

    @Operation(
            summary = "Verifies the JWT",
            description =
                    "Verifies the magic link sent to the user's email. If successful, the user will be enrolled with the school tag.",
            responses = {
                @ApiResponse(responseCode = "302", description = "Redirect to /settings with success or error message"),
            })
    @GetMapping("/school/verify")
    public RedirectView verifySchoolEmail(final HttpServletRequest request) {
        AuthenticationObject authenticationObject;
        Session session;
        User user;
        try {
            authenticationObject = protector.validateSession(request);
            session = authenticationObject.getSession();
            user = authenticationObject.getUser();
        } catch (Exception e) {
            return new RedirectView("/login?success=false&message=You are not logged in.");
        }
        if (session == null) {
            return new RedirectView("/settings?success=false&message=You are not logged in");
        }

        String token = request.getParameter("state");
        MagicLink magicLink;
        try {
            magicLink = jwtClient.decode(token, MagicLink.class);
        } catch (Exception e) {
            return new RedirectView("/settings?success=false&message=Invalid or expired token");
        }

        String magicLinkId = magicLink.getUserId();
        String currentUserId = authenticationObject.getUser().getId();
        if (!magicLinkId.equals(currentUserId)) {
            return new RedirectView("/settings?success=false&message=ID does not match current user");
        }

        user.setSchoolEmail(magicLink.getEmail());
        boolean isSuccessful = userRepository.updateUser(user);

        if (!isSuccessful) {
            throw new RuntimeException("User repository failed to update user and add school email.");
        }

        String emailDomain = magicLink
                .getEmail()
                .substring(magicLink.getEmail().indexOf("@"))
                .toLowerCase();

        SchoolEnum schoolEnum = Stream.of(SchoolEnum.values())
                .filter(school -> school.getEmailDomain().equals(emailDomain))
                .findFirst()
                .orElse(null);
        if (schoolEnum == null) {
            return new RedirectView("/settings?success=false&message=This email is not supported");
        }

        UserTag schoolTag = UserTag.builder()
                .userId(user.getId())
                .tag(schoolEnum.getInternalTag())
                .build();

        if (user.getTags().stream()
                .anyMatch(tag ->
                        tag.getTag().name().equals(schoolEnum.getInternalTag().name()))) {
            reporter.log(
                    "auth",
                    Report.builder()
                            .data(String.format(
                                    "User %s\nAlready has tag %s",
                                    user.getNickname() != null ? user.getNickname() : user.getDiscordName(),
                                    schoolEnum.getInternalTag().name()))
                            .build());
        } else {
            userTagRepository.createTag(schoolTag);
        }

        return new RedirectView("/settings?success=true&message=The email has been verified!");
    }
}
