# `src/`

This directory holds the source code for the Codebloom backend, which is written in [Java](https://www.java.com/en/) and [Spring Boot](https://spring.io/projects/spring-boot).

The backend is currently running on [`Java 25`](https://www.oracle.com/java/technologies/javase/25-relnote-issues.html#NewFeature) due to the fact that virtual threads would be quite useful for us since:

- our production machine doesn't have many threads (1 vCPU, 2 threads)
- a lot of our backend complexity comes from background processes such as timely leaderboard syncs and long-lived Postgres [`NOTIFY/LISTEN`] logic for pub-sub.

View the public Swagger endpoint in production at [codebloom.patinanetwork.org/swagger-ui/index.html](https://codebloom.patinanetwork.org/swagger-ui/index.html)

View [STYLEGUIDE.md](../STYLEGUIDE.md) to view best practices.

## Requirements

> [!WARNING]
> It is HIGHLY recommended that you follow the setup instructions at [docs/local/README.md](../docs/local/README.md) to get up and running.

- [`openjdk@25`](https://openjdk.org/projects/jdk/25/) (though you may feel free to use any other distribution if you would like)
- [`mvn`](https://maven.apache.org/what-is-maven.html) - you do not need to explicitly download this, just use `./mvnw` from the root directory instead (our `Justfile` commands all use `./mvnw` as well).

## Run

> [!WARNING]
> It is HIGHLY recommended that you follow the setup instructions at [docs/local/README.md](../docs/local/README.md) to get up and running.

## Structure

> [!NOTE]
> The backend is very large, so many classes are merged into a package instead to save space for the high-level structure overview

```
src/main
├── test                                                            # Holds all tests
├── java
│   └── org
│       └── patinanetwork
│           └── codebloom
│               ├── api                                             # holds all api specific classes (controllers, request bodies, custom validators, etc)
│               ├── CodebloomApplication.java                       # spring boot entrypoint
│               ├── common
│               │   ├── components                                  # managers that are responsible for complex behaviors (e.g. DiscordClubManager, DuelManager, etc.)
│               │   ├── db
│               │   │   ├── helper                                  # helper utils
│               │   │   ├── models                                  # db model objects
│               │   │   └── repos                                   # db repository classes
│               │   ├── dto                                         # dtos
│               │   ├── email
│               │   │   ├── client
│               │   │   │   ├── codebloom                           # used to send emails
│               │   │   │   └── github                              # used to read emails during Leetcode OAuth process
│               │   │   ├── Email.java                              # base interface used by ./client
│               │   │   ├── error                                   # shared error used by ./client
│               │   │   ├── Message.java                            # base message object used by ./client
│               │   │   ├── options                                 # shared builder objects used by ./client
│               │   │   └── template                                # react email templater
│               │   ├── jwt                                         # jwt helper class(es)
│               │   ├── leetcode                                    # leetcode client classes
│               │   │   ├── LeetcodeClient.java
│               │   │   ├── LeetcodeClientImpl.java
│               │   │   ├── models                                  # shared objects belonging to leetcode.com
│               │   │   ├── queries                                 # GraphQL queries stored as strings
│               │   │   ├── score                                   # scoring logic that Codebloom uses
│               │   │   └── throttled                               # throttled to help alleviate rate limits
│               │   │       ├── ThrottledLeetcodeClient.java
│               │   │       └── ThrottledLeetcodeClientImpl.java
│               │   ├── redis                                       # WIP: redis
│               │   ├── reporter                                    # classes that can be used to report logs & errors to our private Discord server
│               │   ├── schools                                     # school-specific utils
│               │   ├── security                                    # security/auth helper utils
│               │   ├── simpleredis                                 # in-memory redis
│               │   ├── submissions                                 # leetcode/codebloom submission glue code handler
│               │   ├── time                                        # time utils
│               │   ├── url
│               │   │   └── ServerUrlUtils.java
│               │   └── utils                                       # more utils
│               ├── jda
│               │   ├── client
│               │   │   ├── JDAClient.java                          # external client that can be consumed by Spring Boot
│               │   ├── command                                     # slash command logic
│               │   ├── JDAClientManager.java                       # wires up JDAClient
│               │   ├── JDACommandRegisterHandler.java              # registers slash commands
│               │   └── properties
│               ├── playwright                                      # external playwright client
│               │   └── PlaywrightClient.java
│               ├── scheduled                                       # holds all background processes
│               │   ├── auth                                        # used to "steal" a leetcode token. this is a special class as runs in the bg and can be imported into other classes (this separation should be more distinct in the future)
│               │   ├── discord                                     # used to send weekly leaderboard to discord
│               │   ├── duel                                        # WIP: used to cleanup expired duels
│               │   ├── leetcode                                    # various bg services related to leetcode.com and Codebloom
│               │   ├── pg                                          # postgres NOTIFY/LISTEN handlers
│               │   │   ├── handler
│               │   │   │   ├── JobNotifyHandler.java
│               │   │   │   └── LobbyNotifyHandler.java
│               │   │   ├── NotifyListener.java                     # main class where notifications are checked
│               │   ├── potd                                        # update POTD
│               │   └── submission                                  # routinely check for new submissions
│               ├── shared                                          # WIP: still being worked out but currently used to define complex shared enum logic that can be used in the backend but also auto-generated to the frontend
│               │   └── tag
│               │       └── ParentTags.java
│               └── utilities
│                   ├── exception                                   # exception handlers
│                   ├── generator                                   # complex generator logic, such as automatic typescript file generation, react email generation, etc
│                   ├── OpenApiConfig.java                          # force loads some routes to OpenAPI spec
│                   ├── RateLimitingFilter.java                     # handles rate limiting
│                   ├── ServerMetadataObject.java                   # small object returned when /api called
│                   ├── sha                                         # object that returns the last committed SHA at runtime
│                   ├── StaticContentFilter.java                    # handles static content vs dynamic routes
│                   └── WebConfig.java                              # used to load some custom annotation stuff into Spring Security
└── resources
    ├── application-prod.yml                                        # prod overrides
    ├── application-stg.yml                                         # staging overrides
    ├── application.yml                                             # main yaml file
```

## Authentication

### Custom Routes

- **GET: `/api/auth/validate`** - Verifies whether the user is authenticated based on cookies stored in the browser.
- **GET: `/api/auth/logout`** - Logs out the user by invalidating the session and removing cookies from the browser. It automatically returns back to the frontend with either:
  - `/login?success=false&message=This is the failure message`.
  - `/login?success=true&message=This is the success message!`
  - These should be handled on the frontend route.

### Spring OAuth Routes

- **OAuth Initiation:** `GET: /api/auth/flow/{provider}` - Begins the OAuth authentication process for a specific provider. (Example: `/api/auth/flow/discord` starts the Discord OAuth flow.)
- **OAuth Callback:**`GET: /api/auth/flow/callback/{provider}` - Handles the callback process after the OAuth provider returns data to authenticate the user. (Example: `/api/auth/flow/callback/discord` processes the callback from Discord OAuth.)
  - If successful, the user is automatically redirected to `/dashboard`.
  - If failed, the user is automatically redirected to `/login?success=false&message=This is the failure message`.
    - This should be handled on the frontend route.

### Security Details

- **CSRF Protection** - Disabled in the main OAuth security chain.
- **Auth Validator / Session Token Cookie Setter** - Managed by the `CustomAuthenticationSuccessHandler`.
  - Cookie Settings:
    - Name: `session_token`
    - Max Age: **30 days** (configurable via a private variable).

### Backend Objects

- [`Protector.java`](https://github.com/tahminator/codebloom/tree/main/src/main/java/org/patinanetwork/codebloom/common/security/Protector.java) is used to validate whether the user is logged in or not. It automatically handles unauthorized requests (and any other `ResponseStatusException`) via [ControllerExceptionHandler.java](https://github.com/tahminator/codebloom/tree/main/src/main/java/org/patinanetwork/codebloom/utilities/exception/ControllerExceptionHandler.java)

- [`Protected.java`](https://github.com/tahminator/codebloom/tree/main/src/main/java/org/patinanetwork/codebloom/common/security/annotation/Protected.java) can be applied to a controller method as an annotation. You can find an example inside of the file's Javadoc.
  - [`AuthController.java`](https://github.com/tahminator/codebloom/tree/main/src/main/java/org/patinanetwork/codebloom/api/auth/AuthController.java) contains examples of using Protector.java to protect endpoints.

- [`GlobalExceptionHandler.java`](https://github.com/tahminator/codebloom/blob/main/src/main/java/org/patinanetwork/codebloom/utilities/GlobalExceptionHandler.java) manages exception handling for unauthorized requests

- [`SecurityConfig.java`](https://github.com/tahminator/codebloom/blob/main/src/main/java/org/patinanetwork/codebloom/api/auth/security/SecurityConfig.java) holds the OAuth provider
  - [`CustomAuthenticationSuccessHandler.java`](https://github.com/tahminator/codebloom/tree/main/src/main/java/org/patinanetwork/codebloom/api/auth/security/CustomAuthenticationSuccessHandler.java) actually handles the process of authenticating the user once they have been successfully redirected from the OAuth provider back to our server

### Examples

#### Annotation

You may use the [`Protected`](https://github.com/tahminator/codebloom/tree/main/src/main/java/org/patinanetwork/codebloom/common/security/annotation/Protected.java) annotation to validate requests manually like so:

```java
@RestController
@RequestMapping("/api")
public class ApiController {

    @PostMapping("/any-user")
    public ResponseEntity<ApiResponder<Empty>> apiAnyUser(@Protected final AuthenticationObject authenticationObject) {
        User user = authenticationObject.getUser(); // guaranteed for user to exist at this line.
    }

    @PostMapping("/admin-only")
    public ResponseEntity<ApiResponder<Empty>> apiForAdmins(@Protected(admin = true) final AuthenticationObject authenticationObject) {
        User user = authenticationObject.getUser(); // guaranteed for an admin user to exist at this line.
    }
}
```

#### Imperative

You may use the [`Protector`](https://github.com/tahminator/codebloom/tree/main/src/main/java/org/patinanetwork/codebloom/common/security/Protector.java) class to validate requests manually like so:

```java
@RestController
@RequestMapping("/api")
public class ApiController {

    private final Protector protector;

    public AdminController(final Protector protector) {
        this.protector = protector;
    }

    @PostMapping("/any-user")
    public ResponseEntity<ApiResponder<Empty>> apiAnyUser(final HttpServletRequest request) {
        AuthenticationObject authenticationObject = protector.validateSession(request);
        User user = authenticationObject.getUser(); // guaranteed for user to exist at this line.
    }

    @PostMapping("/admin-only")
    public ResponseEntity<ApiResponder<Empty>> apiForAdmins(final HttpServletRequest request) {
        AuthenticationObject authenticationObject = protector.validateAdminSession(request);
        User user = authenticationObject.getUser(); // guaranteed for an admin user to exist at this line.
    }
}
```

## Clients

### `LeetcodeClient`

We have a Leetcode client that interfaces with leetcode.com's GraphQL API to fetch question data, user submissions, and other information. It handles authentication by automatically stealing session cookies via Playwright.

The main client can be found at [`LeetcodeClientImpl.java`](https://github.com/tahminator/codebloom/blob/main/src/main/java/org/patinanetwork/codebloom/common/leetcode/LeetcodeClientImpl.java), though we only use [`ThrottledLeetcodeClient.java`](https://github.com/tahminator/codebloom/blob/main/src/main/java/org/patinanetwork/codebloom/common/leetcode/throttled/ThrottledLeetcodeClientImpl.java) to avoid rate-limiting.

#### Client Features

The [`LeetcodeClient.java`](https://github.com/tahminator/codebloom/blob/main/src/main/java/org/patinanetwork/codebloom/common/leetcode/LeetcodeClient.java) interface provides these methods:

- `findQuestionBySlug` - Gets full question details including title, difficulty, acceptance rate, and topic tags
- `findSubmissionsByUsername` - Fetches recent accepted submissions for a user
- `findSubmissionDetailBySubmissionId` - Gets detailed submission info (runtime, memory, code)
- `getPotd` - Gets the Problem of the Day
- `getUserProfile` - Fetches user profile data (ranking, avatar, bio)
- `getAllTopicTags` - Returns all available question topic tags

> [!WARNING]
> This list of methods may not be up to date. Instead, check [`LeetcodeClient.java`](https://github.com/tahminator/codebloom/blob/main/src/main/java/org/patinanetwork/codebloom/common/leetcode/LeetcodeClient.java)

#### Authentication

We have a scheduled task at [`LeetcodeAuthStealer.java`](https://github.com/tahminator/codebloom/blob/main/src/main/java/org/patinanetwork/codebloom/scheduled/auth/LeetcodeAuthStealer.java) that:

- Runs hourly and reuses database session cookies less than four hours old
- Uses Playwright to automate GitHub OAuth login flow
- Stores the `LEETCODE_SESSION` cookie in our database
- Supports asynchronous cookie reloads when authentication needs refreshing

This is necessary because leetcode.com requires authenticated requests for most API calls.

#### Rate Limiting

We have a class called [`ThrottledLeetcodeClient.java`](https://github.com/tahminator/codebloom/blob/main/src/main/java/org/patinanetwork/codebloom/common/leetcode/throttled/ThrottledLeetcodeClient.java) ([impl](https://github.com/tahminator/codebloom/blob/main/src/main/java/org/patinanetwork/codebloom/common/leetcode/throttled/ThrottledLeetcodeClientImpl.java)) that wraps the main client with rate limiting:

- Uses bucket4j for token bucket rate limiting
- Configured for 1 request per 100ms to avoid getting blocked
- Automatically queues requests when rate limit is hit

#### GraphQL Queries

All API calls use GraphQL queries stored in the `queries` package:

- `SelectProblemQuery` - Get question details by slug
- `SelectAcceptedSubmisisonsQuery` - Get user's accepted submissions
- `GetSubmissionDetails` - Get submission runtime/memory details
- `GetPotd` - Get daily challenge question
- `GetUserProfile` - Get user profile info
- `GetTopics` - Get all topic tags

> [!WARNING]
> This list of methods may not be up to date. Instead, check [`queries/`](https://github.com/tahminator/codebloom/tree/main/src/main/java/org/patinanetwork/codebloom/common/leetcode/queries)

#### Error Handling

The client throws `RuntimeException` for:

- HTTP status codes other than 200
- JSON parsing errors
- Network timeouts
- Authentication failures

All errors bubble up to the calling services which handle them appropriately.

#### Usage in Codebase

The client is used throughout the app for:

- Fetching question data when users submit solutions
- Getting user profiles for leaderboards
- Syncing topic tags with our database
- Calculating scores based on submission details
- Daily challenge notifications

### `Reporter`

Codebloom has a hand-rolled error reporter that will report certain logs and errors to Discord channels in our private Discord guild.

#### Implementation Detail

- [`Reporter.java`](https://github.com/tahminator/codebloom/blob/main/src/main/java/org/patinanetwork/codebloom/common/reporter/Reporter.java) is the abstracted reporter class that exposes the following methods:
  - [`.log`](https://github.com/tahminator/codebloom/blob/main/src/main/java/org/patinanetwork/codebloom/common/reporter/Reporter.java#L73-L100) is used for non-errors that need to be reported back to the server. We mainly use this for tracking odd behaviors or suspicious activity that may not be an error.
  - [`.error`](https://github.com/tahminator/codebloom/blob/main/src/main/java/org/patinanetwork/codebloom/common/reporter/Reporter.java#L44-L71) is used to report any errors back to the server. We use this to raise an indicator to the dev team that something is wrong when it likely shouldn't be. We have logic that will automatically call `.error` on any [controller exceptions](https://github.com/tahminator/codebloom/blob/main/src/main/java/org/patinanetwork/codebloom/utilities/exception/ControllerExceptionHandler.java#L30-L43) or [task scheduler](https://github.com/tahminator/codebloom/blob/main/src/main/java/org/patinanetwork/codebloom/utilities/exception/ScheduledTaskExceptionHandler.java#L29-L48) (aka any background services).
- [`ReporterController.java`](https://github.com/tahminator/codebloom/blob/main/src/main/java/org/patinanetwork/codebloom/api/reporter/ReporterController.java) allows us to ingest errors from our endpoint. The endpoints have basic CSRF protections via checking the `Origin` header.
  - [`/api/reporter/error`](https://github.com/tahminator/codebloom/blob/main/src/main/java/org/patinanetwork/codebloom/api/reporter/ReporterController.java#L57-L70) is the endpoint used to ingest errors from the frontend which will be sent to the server.
  - [`/api/reporter/log`](https://github.com/tahminator/codebloom/blob/main/src/main/java/org/patinanetwork/codebloom/api/reporter/ReporterController.java#L57-L70) is the endpoint used to ingest logs from the frontend which will be sent to the server.
- [`ThrottledReporter.java`](https://github.com/tahminator/codebloom/blob/main/src/main/java/org/patinanetwork/codebloom/common/reporter/throttled/ThrottledReporter.java) is a rate-limited version of the Reporter class, which exposes the same functions but limits how often they can be called. Used for randomly-selected high-traffic logging.

#### Examples

[Link to Reporter example](https://github.com/tahminator/codebloom/blob/main/src/main/java/org/patinanetwork/codebloom/scheduled/auth/LeetcodeAuthStealer.java#L244-L259)

```java
    public synchronized String getCsrf() {
        if (csrf == null && !reported) {
            reported = true;
            reporter.log(Report.builder()
                            .environments(env.getActiveProfiles())
                            .location(Location.BACKEND)
                            .data("CSRF token is missing inside of LeetcodeAuthStealer. This may be something to look into.")
                            .build());
        }

        return csrf;
    }
```

[Link to ThrottledReporter example](https://github.com/tahminator/codebloom/blob/main/src/main/java/org/patinanetwork/codebloom/common/submissions/SubmissionsHandler.java#L131-L148)

```java
            throttledReporter.log(Report.builder()
                                            .data(String.format("""
                                                Score Distribution Report

                                                Leetcode Username: %s
                                                Difficulty: %s (%d pts)
                                                Acceptance Rate: %.2f
                                                Question Multiplier: %.2f
                                                Total: %d
                                                """,
                                                user.getLeetcodeUsername(),
                                                leetcodeQuestion.getDifficulty(),
                                                basePoints,
                                                leetcodeQuestion.getAcceptanceRate(),
                                                multiplier,
                                                points
                                                ))
                                            .build());
```
