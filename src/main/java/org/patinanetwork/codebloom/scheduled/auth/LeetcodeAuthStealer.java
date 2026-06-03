package org.patinanetwork.codebloom.scheduled.auth;

import com.google.common.annotations.VisibleForTesting;
import io.micrometer.core.annotation.Timed;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import lombok.extern.slf4j.Slf4j;
import org.patinanetwork.codebloom.common.db.models.auth.Auth;
import org.patinanetwork.codebloom.common.db.repos.auth.AuthRepository;
import org.patinanetwork.codebloom.common.env.Env;
import org.patinanetwork.codebloom.common.redis.RedisClient;
import org.patinanetwork.codebloom.common.reporter.Reporter;
import org.patinanetwork.codebloom.common.reporter.report.Report;
import org.patinanetwork.codebloom.common.reporter.report.location.Location;
import org.patinanetwork.codebloom.common.time.StandardizedOffsetDateTime;
import org.patinanetwork.codebloom.playwright.PlaywrightClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@Timed(value = "leetcode.client.execution")
public class LeetcodeAuthStealer {

    @VisibleForTesting
    // CHECKSTYLE:OFF
    final ReentrantReadWriteLock LOCK = new ReentrantReadWriteLock();
    // CHECKSTYLE:ON

    private volatile String cookie;
    private volatile String csrf;

    /** So we don't report that a csrf token is missing more than once per machine lifecycle. */
    private boolean reported = false;

    @Value("${github.username}")
    private String githubUsername;

    @Value("${github.password}")
    private String githubPassword;

    private final RedisClient redisClient;
    private final AuthRepository authRepository;
    private final Reporter reporter;
    private final Env env;
    private final PlaywrightClient playwrightClient;

    public LeetcodeAuthStealer(
            final RedisClient redisClient,
            final AuthRepository authRepository,
            final Reporter reporter,
            final Env env,
            PlaywrightClient playwrightClient) {
        this.redisClient = redisClient;
        this.authRepository = authRepository;
        this.reporter = reporter;
        this.env = env;
        this.playwrightClient = playwrightClient;
    }

    /**
     * <b>DO NOT RETURN THE TOKEN IN ANY API ENDPOINT.</b> <div /> This function utilizes Playwright in order to get an
     * authentication key from Leetcode. That code is stored in the database and can then be used to run authenticated
     * queries such as being used to retrieve code from our user submissions.
     */
    @Scheduled(initialDelay = 0, fixedDelay = 60, timeUnit = TimeUnit.MINUTES)
    public void stealAuthCookie() {
        boolean acquired = LOCK.writeLock().tryLock();
        if (!acquired) {
            log.info("Lock failed to be acquired, bouncing...");
            return;
        }

        try {
            Auth mostRecentAuth = authRepository.getMostRecentAuth();

            // The auth token should be refreshed every 4 hours.
            if (mostRecentAuth != null
                    && mostRecentAuth
                            .getCreatedAt()
                            .isAfter(StandardizedOffsetDateTime.now().minus(4, ChronoUnit.HOURS))) {
                log.info("Auth token already exists, using token from database.");
                cookie = mostRecentAuth.getToken();
                csrf = mostRecentAuth.getCsrf();
                return;
            }

            log.info("falling back to checking redis client...");
            Optional<String> authToken = redisClient.getAuth();

            log.info("auth token in redis = {}", authToken.isPresent());

            if (authToken.isPresent()) {
                log.info("auth token found in redis client");
                cookie = authToken.get();
                csrf = null; // don't care in ci.
                return;
            }

            log.info("auth token not found in redis client");
            log.info("Auth token is missing/expired. Attempting to receive token...");

            stealCookieImpl();
        } finally {
            LOCK.writeLock().unlock();
        }
    }

    /**
     * There are some cases where leetcode.com may not respect the token anymore. If that is the case, it is best to try
     * to steal a new cookie and replace the current one.
     *
     * <p>You may await the `CompletableFuture` and receive the brand new token, or call-and-forget.
     */
    @Async
    public CompletableFuture<Optional<String>> reloadCookie() {
        boolean acquired = LOCK.writeLock().tryLock();
        if (!acquired) {
            log.info("Lock failed to be acquired, bouncing...");
            return CompletableFuture.completedFuture(Optional.empty());
        }

        try {
            return CompletableFuture.completedFuture(Optional.ofNullable(stealCookieImpl()));
        } finally {
            LOCK.writeLock().unlock();
        }
    }

    public String getCookie() {
        LOCK.readLock().lock();
        try {
            return cookie;
        } finally {
            LOCK.readLock().unlock();
        }
    }

    /**
     * It's fine if this is null for some requests; it isn't a requirement to fetch data from the GraphQL layer of
     * leetcode.com
     */
    public String getCsrf() {
        if (csrf == null && !reported) {
            reported = true;
            reporter.log(
                    "getCsrf",
                    Report.builder()
                            .environments(env.getActiveProfiles())
                            .location(Location.BACKEND)
                            .data(
                                    "CSRF token is missing inside of LeetcodeAuthStealer. This may be something to look into.")
                            .build());
        }

        return csrf;
    }

    @VisibleForTesting
    String stealCookieImpl() {
        Optional<Auth> auth = playwrightClient.getLeetcodeCookie(githubUsername, githubPassword);
        if (auth.isPresent()) {
            var a = auth.get();
            this.csrf = a.getCsrf();
            this.cookie = a.getToken();
            redisClient.setAuth(a.getToken(), 4, ChronoUnit.HOURS);
            log.info("auth token stored in redis");
            this.authRepository.createAuth(Auth.builder()
                    .csrf(a.getCsrf())
                    .token(a.getToken())
                    .createdAt(StandardizedOffsetDateTime.now())
                    .build());
            return cookie;
        }
        return null;
    }
}
