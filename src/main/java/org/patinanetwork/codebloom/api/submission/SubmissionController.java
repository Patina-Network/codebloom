package org.patinanetwork.codebloom.api.submission;

import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.patinanetwork.codebloom.api.submission.body.LeetcodeUsernameObject;
import org.patinanetwork.codebloom.common.db.models.potd.POTD;
import org.patinanetwork.codebloom.common.db.models.user.User;
import org.patinanetwork.codebloom.common.db.repos.potd.POTDRepository;
import org.patinanetwork.codebloom.common.db.repos.question.QuestionRepository;
import org.patinanetwork.codebloom.common.db.repos.user.UserRepository;
import org.patinanetwork.codebloom.common.dto.ApiResponder;
import org.patinanetwork.codebloom.common.dto.Empty;
import org.patinanetwork.codebloom.common.dto.autogen.UnsafeEmptySuccessResponse;
import org.patinanetwork.codebloom.common.dto.autogen.UnsafeGenericFailureResponse;
import org.patinanetwork.codebloom.common.dto.autogen.UnsafeRateLimitResponse;
import org.patinanetwork.codebloom.common.dto.potd.PotdDto;
import org.patinanetwork.codebloom.common.dto.question.QuestionWithUserDto;
import org.patinanetwork.codebloom.common.leetcode.models.LeetcodeSubmission;
import org.patinanetwork.codebloom.common.leetcode.models.UserProfile;
import org.patinanetwork.codebloom.common.leetcode.throttled.ThrottledLeetcodeClient;
import org.patinanetwork.codebloom.common.security.AuthenticationObject;
import org.patinanetwork.codebloom.common.security.annotation.Protected;
import org.patinanetwork.codebloom.common.simpleredis.SimpleRedis;
import org.patinanetwork.codebloom.common.simpleredis.SimpleRedisProvider;
import org.patinanetwork.codebloom.common.simpleredis.SimpleRedisSlot;
import org.patinanetwork.codebloom.common.submissions.SubmissionsHandler;
import org.patinanetwork.codebloom.common.submissions.object.AcceptedSubmission;
import org.patinanetwork.codebloom.common.time.StandardizedLocalDateTime;
import org.patinanetwork.codebloom.common.utils.log.LogExecutionTime;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@Tag(name = "LeetCode Routes", description = """
    This route is responsible for the actual submission logic,
    such as setting LeetCode username, fetching the problem of the day, retreiving submission data, and more.
    """)
@RequestMapping("/api/leetcode")
@Timed(value = "controller.execution")
public class SubmissionController {

    // 5 Minute rate limit to avoid abuse.
    private static final double SECONDS_TO_WAIT = 5 * 60;

    private final UserRepository userRepository;
    private final SimpleRedis<Long> simpleRedis;
    private final ThrottledLeetcodeClient leetcodeClient;
    private final SubmissionsHandler submissionsHandler;
    private final QuestionRepository questionRepository;
    private final POTDRepository potdRepository;

    /**
     * This checks if the different is 24 hours, instead of checking whether they are actually part of the "same day".
     */
    private boolean isSameDay(final LocalDateTime createdAt) {
        LocalDateTime now = StandardizedLocalDateTime.now();
        Duration duration = Duration.between(createdAt, now);

        return duration.toHours() < 24;
    }

    public SubmissionController(
            final UserRepository userRepository,
            final SimpleRedisProvider simpleRedisProvider,
            final ThrottledLeetcodeClient throttledLeetcodeClient,
            final SubmissionsHandler submissionsHandler,
            final QuestionRepository questionRepository,
            final POTDRepository potdRepository) {
        this.userRepository = userRepository;
        this.simpleRedis = simpleRedisProvider.select(SimpleRedisSlot.SUBMISSION_REFRESH);
        this.leetcodeClient = throttledLeetcodeClient;
        this.submissionsHandler = submissionsHandler;
        this.questionRepository = questionRepository;
        this.potdRepository = potdRepository;
    }

    @Operation(
            summary = "Returns the currently authenticated user's verification key.",
            description = """
                Protected endpoint that returns the currently authenticated user's verification key. In order to set their Leetcode username,
                users must add the verification key to their About Me in order to pass validation.
        """,
            responses = {
                @ApiResponse(
                        responseCode = "401",
                        description = "Not authenticated",
                        content = @Content(schema = @Schema(implementation = UnsafeGenericFailureResponse.class))),
                @ApiResponse(responseCode = "200", description = "Successfully retrieved key"),
            })
    @GetMapping("/key")
    public ResponseEntity<ApiResponder<String>> getVerificationKey(
            @Protected final AuthenticationObject authenticationObject) {

        User user = authenticationObject.getUser();

        return ResponseEntity.ok()
                .body(ApiResponder.success("Successfully retreived authentication key", user.getVerifyKey()));
    }

    @Operation(
            summary = "Set a Leetcode username for the current user",
            description = """
        Protected endpoint that allows a user to submit a JSON with the leetcode username they would like to add.
        Cannot re-use this endpoint once a name is set.
        """,
            responses = {
                @ApiResponse(
                        responseCode = "401",
                        description = "Not authenticated",
                        content = @Content(schema = @Schema(implementation = UnsafeGenericFailureResponse.class))),
                @ApiResponse(
                        responseCode = "200",
                        description = "Name has been set successfully",
                        content = @Content(schema = @Schema(implementation = UnsafeEmptySuccessResponse.class))),
                @ApiResponse(
                        responseCode = "409",
                        description = "Attempt to set name that's already been set",
                        content = @Content(schema = @Schema(implementation = UnsafeGenericFailureResponse.class))),
                @ApiResponse(
                        responseCode = "400",
                        description = "Invalid username",
                        content = @Content(schema = @Schema(implementation = UnsafeGenericFailureResponse.class))),
            })
    @PostMapping("/set")
    public ResponseEntity<ApiResponder<Empty>> setLeetcodeUsername(
            @Protected final AuthenticationObject authenticationObject,
            @Valid @RequestBody final LeetcodeUsernameObject leetcodeUsernameObject) {

        User user = authenticationObject.getUser();

        if (user.getLeetcodeUsername() != null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "User has already set a username previously. You cannot change your name anymore. Please contact support if there are any issues.");
        }

        UserProfile leetcodeUserProfile =
                leetcodeClient.getUserProfileFast(leetcodeUsernameObject.getLeetcodeUsername());
        String aboutMe = leetcodeUserProfile.getAboutMe();

        if (aboutMe == null || !aboutMe.contains(user.getVerifyKey())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "The verification key is not present in the user's about me or the about me did not exist.");
        }

        if (userRepository.userExistsByLeetcodeUsername(leetcodeUsernameObject.getLeetcodeUsername())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "This username has already been taken. If this is a mistake, please get in touch with us so we can attempt to rectify it.");
        }

        user.setLeetcodeUsername(leetcodeUsernameObject.getLeetcodeUsername());
        user.setProfileUrl(leetcodeUserProfile.getUserAvatar());
        userRepository.updateUser(user);

        return ResponseEntity.ok().body(ApiResponder.success("Leetcode username has been set!", Empty.of()));
    }

    @Operation(
            summary = "Check the current user's LeetCode submissions and update leaderboard",
            description = """
        Protected endpoint that handles the logic of checking the most recent submissions,
        as well as updating the current leaderboard with any new points the user has accumulated.
        There is a rate limit on the route to prevent abuse (currently: 5 minutes).
        """,
            responses = {
                @ApiResponse(
                        responseCode = "401",
                        description = "Not authenticated",
                        content = @Content(schema = @Schema(implementation = UnsafeGenericFailureResponse.class))),
                @ApiResponse(responseCode = "200", description = "The check was completed successfuly"),
                @ApiResponse(
                        responseCode = "412",
                        description = "Leetcode username hasn't been set",
                        content = @Content(schema = @Schema(implementation = UnsafeGenericFailureResponse.class))),
                @ApiResponse(
                        responseCode = "429",
                        description = "Rate limited",
                        content = @Content(schema = @Schema(implementation = UnsafeRateLimitResponse.class))),
                @ApiResponse(
                        responseCode = "400",
                        description = "Invalid username",
                        content = @Content(schema = @Schema(implementation = UnsafeGenericFailureResponse.class))),
            })
    @PostMapping("/check")
    @LogExecutionTime
    public ResponseEntity<ApiResponder<ArrayList<AcceptedSubmission>>> checkLatestSubmissions(
            @Protected final AuthenticationObject authenticationObject) {
        User user = authenticationObject.getUser();

        if (user.getLeetcodeUsername() == null) {
            throw new ResponseStatusException(
                    HttpStatus.PRECONDITION_FAILED,
                    "You cannot access this resource without setting a Leetcode username first.");
        }

        if (simpleRedis.containsKey(user.getId())) {
            long timeThen = simpleRedis.get(user.getId());
            long timeNow = System.currentTimeMillis();
            long difference = (timeNow - timeThen) / 1000;

            if (difference < SECONDS_TO_WAIT) {
                long remainingTime = (long) SECONDS_TO_WAIT - difference;
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, Long.toString(remainingTime));
            }
        }

        simpleRedis.put(user.getId(), System.currentTimeMillis());

        List<LeetcodeSubmission> leetcodeSubmissions =
                leetcodeClient.findSubmissionsByUsernameFast(user.getLeetcodeUsername(), 20);

        return ResponseEntity.ok()
                .body(ApiResponder.success(
                        "Successfully checked all recent submissions!",
                        submissionsHandler.handleSubmissions(leetcodeSubmissions, user, true)));
    }

    @Operation(
            summary = "Returns current problem of the day.",
            description = """
        Returns the current problem of the day, as long as there is a problem of the day set and the user hasn't completed the problem already.
        """,
            responses = {
                @ApiResponse(responseCode = "200", description = "POTD found"),
                @ApiResponse(
                        responseCode = "404",
                        description = "POTD not found",
                        content = @Content(schema = @Schema(implementation = UnsafeGenericFailureResponse.class))),
                @ApiResponse(
                        responseCode = "401",
                        description = "Not authenticated",
                        content = @Content(schema = @Schema(implementation = UnsafeGenericFailureResponse.class))),
            })
    @GetMapping("/potd")
    public ResponseEntity<ApiResponder<PotdDto>> getCurrentPotd(
            @Protected final AuthenticationObject authenticationObject) {

        User user = authenticationObject.getUser();

        POTD potd = potdRepository
                .getCurrentPOTD()
                .filter(p -> isSameDay(p.getCreatedAt()))
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sorry, no problem of the day today!"));

        boolean alreadyCompleted = questionRepository
                .getQuestionBySlugAndUserId(potd.getSlug(), user.getId())
                .isPresent();

        if (alreadyCompleted) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(
                            ApiResponder.failure(
                                    "Nice, you have already completed the problem of the day! Come back tomorrow for a new one!"));
        }

        return ResponseEntity.ok()
                .body(ApiResponder.success("Problem of the day has been fetched!", PotdDto.fromPOTD(potd)));
    }

    @Operation(
            summary = "Returns current problem of the day for embedding.",
            description = """
        Returns the current problem of the day for use in embeds, as long as there is a problem of the day set for today.
        This endpoint does not require user authentication and does not check if the user has already completed the problem.
        """,
            responses = {
                @ApiResponse(responseCode = "200", description = "POTD found"),
                @ApiResponse(
                        responseCode = "404",
                        description = "POTD not found",
                        content = @Content(schema = @Schema(implementation = UnsafeGenericFailureResponse.class))),
            })
    @GetMapping("/potd/embed")
    public ResponseEntity<ApiResponder<PotdDto>> getCurrentPotdEmbed() {
        POTD potd = potdRepository
                .getCurrentPOTD()
                .filter(p -> isSameDay(p.getCreatedAt()))
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sorry, no problem of the day today!"));

        return ResponseEntity.ok()
                .body(ApiResponder.success("Problem of the day has been fetched!", PotdDto.fromPOTD(potd)));
    }

    @Operation(
            summary = "Returns submission data.",
            description = """
        Returns the submission data from any user, as long as the user making the request is authenticated.
        This includes the scraped LeetCode description, which is HTML that has been sanitized by the server,
        so it is safe to use on the frontend.
        """,
            responses = {
                @ApiResponse(responseCode = "200", description = "Question found"),
                @ApiResponse(
                        responseCode = "404",
                        description = "Question not found",
                        content = @Content(schema = @Schema(implementation = UnsafeGenericFailureResponse.class))),
                @ApiResponse(
                        responseCode = "401",
                        description = "Not authenticated",
                        content = @Content(schema = @Schema(implementation = UnsafeGenericFailureResponse.class))),
            })
    @GetMapping("/submission/{submissionId}")
    public ResponseEntity<ApiResponder<QuestionWithUserDto>> getSubmissionBySubmissionId(
            final HttpServletRequest request, @PathVariable final String submissionId) {

        var question = questionRepository.getQuestionWithUserById(submissionId);

        if (question.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponder.failure("Sorry, submission could not be found."));
        }

        return ResponseEntity.ok()
                .body(ApiResponder.success(
                        "Problem of the day has been fetched!",
                        QuestionWithUserDto.fromQuestionWithUser(question.get())));
    }
}
