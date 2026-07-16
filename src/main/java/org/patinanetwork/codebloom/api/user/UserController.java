package org.patinanetwork.codebloom.api.user;

import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.patinanetwork.codebloom.common.db.models.question.Question;
import org.patinanetwork.codebloom.common.db.models.question.topic.LeetcodeTopicEnum;
import org.patinanetwork.codebloom.common.db.models.user.User;
import org.patinanetwork.codebloom.common.db.models.user.UserMetrics;
import org.patinanetwork.codebloom.common.db.repos.question.QuestionRepository;
import org.patinanetwork.codebloom.common.db.repos.question.topic.service.QuestionTopicService;
import org.patinanetwork.codebloom.common.db.repos.user.UserMetricsRepository;
import org.patinanetwork.codebloom.common.db.repos.user.UserRepository;
import org.patinanetwork.codebloom.common.db.repos.user.options.UserMetricsFilterOptions;
import org.patinanetwork.codebloom.common.dto.ApiResponder;
import org.patinanetwork.codebloom.common.dto.autogen.UnsafeGenericFailureResponse;
import org.patinanetwork.codebloom.common.dto.question.QuestionDto;
import org.patinanetwork.codebloom.common.dto.user.UserDto;
import org.patinanetwork.codebloom.common.dto.user.metrics.MetricsDto;
import org.patinanetwork.codebloom.common.ff.annotation.FF;
import org.patinanetwork.codebloom.common.page.Page;
import org.patinanetwork.codebloom.common.time.StandardizedOffsetDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@Tag(
        name = "General user routes",
        description =
                "This controller is responsible for handling general user data, such as user profile, user submissions, and more.")
@RequestMapping("/api/user")
@Timed(value = "controller.execution")
/** simulated be change */
public class UserController {

    /* Page size for submissions */
    private static final int SUBMISSIONS_PAGE_SIZE = 20;

    private static final int METRICS_PAGE_SIZE = 20;

    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;
    private final QuestionTopicService questionTopicService;
    private final UserMetricsRepository userMetricsRepository;

    public UserController(
            final QuestionRepository questionRepository,
            final UserRepository userRepository,
            final QuestionTopicService questionTopicService,
            final UserMetricsRepository userMetricsRepository) {
        this.questionRepository = questionRepository;
        this.userRepository = userRepository;
        this.questionTopicService = questionTopicService;
        this.userMetricsRepository = userMetricsRepository;
    }

    @Operation(
            summary = "Public route that returns the given user's profile",
            description = """
        Unprotected endpoint that returns the user profile of the user ID that is passed to the endpoint's path.
        """,
            responses = {
                @ApiResponse(
                        responseCode = "404",
                        description = "User profile has not been found",
                        content = @Content(schema = @Schema(implementation = UnsafeGenericFailureResponse.class))),
                @ApiResponse(responseCode = "200", description = "User profile has been found"),
            })
    @GetMapping("{userId}/profile")
    public ResponseEntity<ApiResponder<UserDto>> getUserProfileByUserId(
            final HttpServletRequest request, @PathVariable final String userId) {

        User user = userRepository.getUserById(userId);

        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Failed to find user profile.");
        }

        return ResponseEntity.ok().body(ApiResponder.success("User profile found!", UserDto.fromUser(user)));
    }

    @Operation(
            summary = "Returns a list of the questions successfully submitted by the user.",
            description = """
        Protected endpoint that returns the list of questions completed by the user.
        These questions are guaranteed to be completed by the user.
        """,
            responses = {
                @ApiResponse(responseCode = "200", description = "Successful"),
                @ApiResponse(
                        responseCode = "400",
                        description = "Invalid date range (startDate is after endDate)",
                        content = @Content(schema = @Schema(implementation = UnsafeGenericFailureResponse.class))),
                @ApiResponse(
                        responseCode = "401",
                        description = "Not authenticated",
                        content = @Content(schema = @Schema(implementation = UnsafeGenericFailureResponse.class))),
            })
    @GetMapping("{userId}/submissions")
    public ResponseEntity<ApiResponder<Page<QuestionDto>>> getAllQuestionsForUser(
            final HttpServletRequest request,
            @Parameter(description = "Page index", example = "1") @RequestParam(required = false, defaultValue = "1")
                    final int page,
            @Parameter(description = "Question Title", example = "Two")
                    @RequestParam(required = false, defaultValue = "")
                    final String query,
            @Parameter(description = "Page size (maximum of " + SUBMISSIONS_PAGE_SIZE)
                    @RequestParam(required = false, defaultValue = "" + SUBMISSIONS_PAGE_SIZE)
                    final int pageSize,
            @Parameter(description = "Filter to hide questions with 0 points awarded")
                    @RequestParam(required = false, defaultValue = "false")
                    final boolean pointFilter,
            @Parameter(description = "Filter for questions with at least one of the topics provided")
                    @RequestParam(required = false, defaultValue = "")
                    final Set<String> topics,
            @Parameter(description = "Start date to filter submissions by createdAt (inclusive)")
                    @RequestParam(required = false)
                    final OffsetDateTime startDate,
            @Parameter(description = "End date to filter submissions by createdAt (inclusive)")
                    @RequestParam(required = false)
                    final OffsetDateTime endDate,
            @PathVariable final String userId) {

        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startDate cannot be after endDate.");
        }

        final OffsetDateTime normalizedStartDate = StandardizedOffsetDateTime.normalize(startDate);
        final OffsetDateTime normalizedEndDate = StandardizedOffsetDateTime.normalize(endDate);

        final int parsedPageSize = Math.min(pageSize, SUBMISSIONS_PAGE_SIZE);

        LeetcodeTopicEnum[] topicEnums = questionTopicService.stringsToEnums(topics);

        ArrayList<Question> questions = questionRepository.getQuestionsByUserId(
                userId, page, parsedPageSize, query, pointFilter, topicEnums, normalizedStartDate, normalizedEndDate);

        int totalQuestions = questionRepository.getQuestionCountByUserId(
                userId, query, pointFilter, topics, normalizedStartDate, normalizedEndDate);
        int totalPages = (int) Math.ceil((double) totalQuestions / parsedPageSize);
        boolean hasNextPage = page < totalPages;

        List<QuestionDto> questionDtos =
                questions.stream().map(q -> QuestionDto.fromQuestion(q)).toList();

        Page<QuestionDto> createdPage = new Page<>(hasNextPage, questionDtos, totalPages, parsedPageSize);

        return ResponseEntity.ok().body(ApiResponder.success("All questions have been fetched!", createdPage));
    }

    @Operation(
            summary = "Public route that returns a list of all the users' metadata.",
            description = """
            Unprotected endpoint that returns basic metadata for all users.
        """,
            responses = {
                @ApiResponse(
                        responseCode = "404",
                        description = "All users' metadata has not been found.",
                        content = @Content(schema = @Schema(implementation = UnsafeGenericFailureResponse.class))),
                @ApiResponse(responseCode = "200", description = "All users' metadata has been found."),
            })
    @GetMapping("/all")
    public ResponseEntity<ApiResponder<Page<UserDto>>> getAllUsers(
            final HttpServletRequest request,
            @Parameter(description = "Page index", example = "1") @RequestParam(required = false, defaultValue = "1")
                    final int page,
            @Parameter(description = "Question Title", example = "Two")
                    @RequestParam(required = false, defaultValue = "")
                    final String query,
            @Parameter(description = "Page size (maximum of " + SUBMISSIONS_PAGE_SIZE)
                    @RequestParam(required = false, defaultValue = "" + SUBMISSIONS_PAGE_SIZE)
                    final int pageSize) {

        final int parsedPageSize = Math.min(pageSize, SUBMISSIONS_PAGE_SIZE);

        List<User> users = userRepository.getAllUsers(page, parsedPageSize, query);

        int totalUsers = userRepository.getUserCount(query);
        int totalPages = (int) Math.ceil((double) totalUsers / parsedPageSize);
        boolean hasNextPage = page < totalPages;

        List<UserDto> userDtos = users.stream().map(UserDto::fromUser).toList();

        Page<UserDto> createdPage = new Page<>(hasNextPage, userDtos, totalPages, parsedPageSize);

        return ResponseEntity.ok().body(ApiResponder.success("All users have been successfully fetched!", createdPage));
    }

    @Operation(
            summary = "Staging-only route that returns paginated metrics for a given user.",
            description = """
            Returns a paginated list of collected metrics points for the given user within a date range.
            Defaults to the last 7 days if no dates are provided. Only available in staging.
            """,
            responses = {
                @ApiResponse(responseCode = "200", description = "Metrics fetched successfully"),
                @ApiResponse(
                        responseCode = "400",
                        description = "Invalid date range (startDate is after endDate)",
                        content = @Content(schema = @Schema(implementation = UnsafeGenericFailureResponse.class))),
                @ApiResponse(
                        responseCode = "403",
                        description = "Endpoint is not available in this environment",
                        content = @Content(schema = @Schema(implementation = UnsafeGenericFailureResponse.class))),
            })
    @GetMapping("{userId}/metrics")
    @FF("userMetrics")
    public ResponseEntity<ApiResponder<Page<MetricsDto>>> getUserMetrics(
            final HttpServletRequest request,
            @PathVariable final String userId,
            @Parameter(description = "Page index", example = "1") @RequestParam(required = false, defaultValue = "1")
                    final int page,
            @Parameter(description = "Page size (maximum of " + METRICS_PAGE_SIZE)
                    @RequestParam(required = false, defaultValue = "" + METRICS_PAGE_SIZE)
                    final int pageSize,
            @Parameter(description = "Start date to filter metrics by createdAt (inclusive)")
                    @RequestParam(required = false)
                    final OffsetDateTime startDate,
            @Parameter(description = "End date to filter metrics by createdAt (inclusive)")
                    @RequestParam(required = false)
                    final OffsetDateTime endDate) {

        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startDate cannot be after endDate.");
        }

        final OffsetDateTime resolvedEnd = endDate == null
                ? StandardizedOffsetDateTime.normalize(OffsetDateTime.now())
                : StandardizedOffsetDateTime.normalize(endDate);
        final OffsetDateTime resolvedStart = startDate == null
                ? StandardizedOffsetDateTime.normalize(OffsetDateTime.now().minusWeeks(1))
                : StandardizedOffsetDateTime.normalize(startDate);

        final int parsedPageSize = Math.min(pageSize, METRICS_PAGE_SIZE);

        final UserMetricsFilterOptions options = UserMetricsFilterOptions.builder()
                .page(page)
                .pageSize(parsedPageSize)
                .from(resolvedStart)
                .to(resolvedEnd)
                .build();

        List<UserMetrics> metrics = userMetricsRepository.findUserMetrics(userId, options);
        int totalMetrics = userMetricsRepository.countUserMetrics(userId, options);
        int totalPages = (int) Math.ceil((double) totalMetrics / parsedPageSize);
        boolean hasNextPage = page < totalPages;

        List<MetricsDto> metricsDtos =
                metrics.stream().map(MetricsDto::fromUserMetrics).toList();
        Page<MetricsDto> createdPage = new Page<>(hasNextPage, metricsDtos, totalPages, parsedPageSize);

        return ResponseEntity.ok().body(ApiResponder.success("Metrics fetched!", createdPage));
    }
}
