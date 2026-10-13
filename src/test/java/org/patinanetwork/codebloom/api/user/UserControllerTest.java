package org.patinanetwork.codebloom.api.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javafaker.Faker;
import jakarta.servlet.http.HttpServletRequest;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.patinanetwork.codebloom.common.db.models.question.Question;
import org.patinanetwork.codebloom.common.db.models.user.User;
import org.patinanetwork.codebloom.common.db.models.user.UserMetrics;
import org.patinanetwork.codebloom.common.db.repos.question.QuestionRepository;
import org.patinanetwork.codebloom.common.db.repos.question.topic.service.QuestionTopicService;
import org.patinanetwork.codebloom.common.db.repos.user.UserMetricsRepository;
import org.patinanetwork.codebloom.common.db.repos.user.UserRepository;
import org.patinanetwork.codebloom.common.dto.question.QuestionDto;
import org.patinanetwork.codebloom.common.dto.user.UserDto;
import org.patinanetwork.codebloom.common.dto.user.metrics.MetricsDto;
import org.patinanetwork.codebloom.common.page.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class UserControllerTest {

    private final UserController userController;
    private final Faker faker;

    private QuestionRepository questionRepository = mock(QuestionRepository.class);
    private UserRepository userRepository = mock(UserRepository.class);
    private QuestionTopicService questionTopicService = mock(QuestionTopicService.class);
    private UserMetricsRepository userMetricsRepository = mock(UserMetricsRepository.class);
    private HttpServletRequest request = mock(HttpServletRequest.class);

    public UserControllerTest() {
        this.userController =
                new UserController(questionRepository, userRepository, questionTopicService, userMetricsRepository);
        this.faker = Faker.instance();
    }

    private String randomUUID() {
        return UUID.randomUUID().toString();
    }

    private User createRandomUser() {
        return User.builder()
                .id(randomUUID())
                .discordId(String.valueOf(faker.number().randomNumber(18, true)))
                .discordName(faker.name().username())
                .leetcodeUsername(Optional.ofNullable(faker.name().username()))
                .admin(false)
                .verifyKey(faker.crypto().md5())
                .build();
    }

    private Question createRandomQuestion(final String userId) {
        return Question.builder()
                .id(randomUUID())
                .userId(userId)
                .questionTitle(faker.lorem().sentence())
                .questionSlug(faker.lorem().word())
                .questionDifficulty(org.patinanetwork.codebloom.common.db.models.question.QuestionDifficulty.Medium)
                .pointsAwarded(Optional.of(10))
                .topics(new ArrayList<>())
                .build();
    }

    @Test
    @DisplayName("Get user profile - user not found")
    void getUserProfileUserNotFound() {
        String userId = randomUUID();

        when(userRepository.getUserById(eq(userId))).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            userController.getUserProfileByUserId(request, userId);
        });

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("Failed to find user profile.", exception.getReason());

        verify(userRepository, times(1)).getUserById(eq(userId));
    }

    @Test
    @DisplayName("Get user profile - returns user profile successfully")
    void getUserProfileReturnsUserProfileSuccessfully() {
        User user = createRandomUser();

        when(userRepository.getUserById(eq(user.getId()))).thenReturn(Optional.of(user));

        var response = userController.getUserProfileByUserId(request, user.getId());

        assertEquals(HttpStatus.OK, response.getStatusCode());

        var apiResponder = response.getBody();
        assertNotNull(apiResponder);
        assertTrue(apiResponder.isSuccess());
        assertEquals("User profile found!", apiResponder.getMessage());
        assertNotNull(apiResponder.getPayload());

        UserDto payload = apiResponder.getPayload();
        assertEquals(user.getId(), payload.getId());
        assertEquals(user.getDiscordId(), payload.getDiscordId());
        assertEquals(user.getDiscordName(), payload.getDiscordName());
        assertEquals(user.getLeetcodeUsername().orElse(null), payload.getLeetcodeUsername());

        verify(userRepository, times(1)).getUserById(eq(user.getId()));
    }

    @Test
    @DisplayName("Get submissions - startDate after endDate returns bad request")
    void getSubmissionsInvalidDateRange() {
        String userId = randomUUID();
        OffsetDateTime startDate = OffsetDateTime.now();
        OffsetDateTime endDate = startDate.minusDays(1);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            userController.getAllQuestionsForUser(
                    request, 1, "", 20, false, Collections.emptySet(), startDate, endDate, userId);
        });

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("startDate cannot be after endDate.", exception.getReason());
    }

    @Test
    @DisplayName("Get submissions - returns submissions successfully with no filters")
    void getSubmissionsReturnsSuccessfullyNoFilters() {
        String userId = randomUUID();

        ArrayList<Question> questions = new ArrayList<>();
        questions.add(createRandomQuestion(userId));
        questions.add(createRandomQuestion(userId));

        when(questionTopicService.stringsToEnums(any()))
                .thenReturn(new org.patinanetwork.codebloom.common.db.models.question.topic.LeetcodeTopicEnum[0]);
        when(questionRepository.getQuestionsByUserId(eq(userId), eq(1), eq(20), eq(""), eq(false), any(), any(), any()))
                .thenReturn(questions);
        when(questionRepository.getQuestionCountByUserId(eq(userId), eq(""), eq(false), any(), any(), any()))
                .thenReturn(2);

        var response = userController.getAllQuestionsForUser(
                request, 1, "", 20, false, Collections.emptySet(), null, null, userId);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        var apiResponder = response.getBody();
        assertNotNull(apiResponder);
        assertTrue(apiResponder.isSuccess());
        assertEquals("All questions have been fetched!", apiResponder.getMessage());

        Page<QuestionDto> page = apiResponder.getPayload();
        assertNotNull(page);
        assertEquals(2, page.getItems().size());
        assertEquals(1, page.getPages());
        assertEquals(20, page.getPageSize());

        verify(questionRepository, times(1))
                .getQuestionsByUserId(eq(userId), eq(1), eq(20), eq(""), eq(false), any(), any(), any());
        verify(questionRepository, times(1))
                .getQuestionCountByUserId(eq(userId), eq(""), eq(false), any(), any(), any());
    }

    @Test
    @DisplayName("Get submissions - page size capped at maximum")
    void getSubmissionsPageSizeCapped() {
        String userId = randomUUID();

        when(questionTopicService.stringsToEnums(any()))
                .thenReturn(new org.patinanetwork.codebloom.common.db.models.question.topic.LeetcodeTopicEnum[0]);
        when(questionRepository.getQuestionsByUserId(
                        anyString(), anyInt(), eq(20), anyString(), anyBoolean(), any(), any(), any()))
                .thenReturn(new ArrayList<>());
        when(questionRepository.getQuestionCountByUserId(anyString(), anyString(), anyBoolean(), any(), any(), any()))
                .thenReturn(0);

        var response = userController.getAllQuestionsForUser(
                request, 1, "", 100, false, Collections.emptySet(), null, null, userId);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        var apiResponder = response.getBody();
        assertNotNull(apiResponder);

        Page<QuestionDto> page = apiResponder.getPayload();
        assertEquals(20, page.getPageSize());

        verify(questionRepository, times(1))
                .getQuestionsByUserId(eq(userId), eq(1), eq(20), eq(""), eq(false), any(), any(), any());
    }

    @Test
    @DisplayName("Get submissions - empty results")
    void getSubmissionsEmptyResults() {
        String userId = randomUUID();

        when(questionTopicService.stringsToEnums(any()))
                .thenReturn(new org.patinanetwork.codebloom.common.db.models.question.topic.LeetcodeTopicEnum[0]);
        when(questionRepository.getQuestionsByUserId(
                        anyString(), anyInt(), anyInt(), anyString(), anyBoolean(), any(), any(), any()))
                .thenReturn(new ArrayList<>());
        when(questionRepository.getQuestionCountByUserId(anyString(), anyString(), anyBoolean(), any(), any(), any()))
                .thenReturn(0);

        var response = userController.getAllQuestionsForUser(
                request, 1, "", 20, false, Collections.emptySet(), null, null, userId);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        var apiResponder = response.getBody();
        assertNotNull(apiResponder);
        assertTrue(apiResponder.isSuccess());

        Page<QuestionDto> page = apiResponder.getPayload();
        assertNotNull(page);
        assertEquals(0, page.getItems().size());
        assertEquals(0, page.getPages());
    }

    @Test
    @DisplayName("Get submissions - with date filters")
    void getSubmissionsWithDateFilters() {
        String userId = randomUUID();
        OffsetDateTime startDate = OffsetDateTime.now().minusDays(7);
        OffsetDateTime endDate = OffsetDateTime.now();

        ArrayList<Question> questions = new ArrayList<>();
        questions.add(createRandomQuestion(userId));

        when(questionTopicService.stringsToEnums(any()))
                .thenReturn(new org.patinanetwork.codebloom.common.db.models.question.topic.LeetcodeTopicEnum[0]);
        when(questionRepository.getQuestionsByUserId(eq(userId), eq(1), eq(20), eq(""), eq(false), any(), any(), any()))
                .thenReturn(questions);
        when(questionRepository.getQuestionCountByUserId(eq(userId), eq(""), eq(false), any(), any(), any()))
                .thenReturn(1);

        var response = userController.getAllQuestionsForUser(
                request, 1, "", 20, false, Collections.emptySet(), startDate, endDate, userId);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        var apiResponder = response.getBody();
        assertNotNull(apiResponder);
        assertTrue(apiResponder.isSuccess());

        Page<QuestionDto> page = apiResponder.getPayload();
        assertNotNull(page);
        assertEquals(1, page.getItems().size());
    }

    @Test
    @DisplayName("Get all users - returns users successfully")
    void getAllUsersReturnsSuccessfully() {
        ArrayList<User> users = new ArrayList<>();
        users.add(createRandomUser());
        users.add(createRandomUser());
        users.add(createRandomUser());

        when(userRepository.getAllUsers(eq(1), eq(20), eq(""))).thenReturn(users);
        when(userRepository.getUserCount(eq(""))).thenReturn(3);

        var response = userController.getAllUsers(request, 1, "", 20);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        var apiResponder = response.getBody();
        assertNotNull(apiResponder);
        assertTrue(apiResponder.isSuccess());
        assertEquals("All users have been successfully fetched!", apiResponder.getMessage());

        Page<UserDto> page = apiResponder.getPayload();
        assertNotNull(page);
        assertEquals(3, page.getItems().size());
        assertEquals(1, page.getPages());
        assertEquals(20, page.getPageSize());

        verify(userRepository, times(1)).getAllUsers(eq(1), eq(20), eq(""));
        verify(userRepository, times(1)).getUserCount(eq(""));
    }

    @Test
    @DisplayName("Get all users - with search query")
    void getAllUsersWithSearchQuery() {
        String searchQuery = "testuser";

        ArrayList<User> users = new ArrayList<>();
        User matchingUser = createRandomUser();
        users.add(matchingUser);

        when(userRepository.getAllUsers(eq(1), eq(20), eq(searchQuery))).thenReturn(users);
        when(userRepository.getUserCount(eq(searchQuery))).thenReturn(1);

        var response = userController.getAllUsers(request, 1, searchQuery, 20);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        var apiResponder = response.getBody();
        assertNotNull(apiResponder);
        assertTrue(apiResponder.isSuccess());

        Page<UserDto> page = apiResponder.getPayload();
        assertNotNull(page);
        assertEquals(1, page.getItems().size());

        verify(userRepository, times(1)).getAllUsers(eq(1), eq(20), eq(searchQuery));
        verify(userRepository, times(1)).getUserCount(eq(searchQuery));
    }

    @Test
    @DisplayName("Get all users - page size capped at maximum")
    void getAllUsersPageSizeCapped() {
        when(userRepository.getAllUsers(eq(1), eq(20), eq(""))).thenReturn(new ArrayList<>());
        when(userRepository.getUserCount(eq(""))).thenReturn(0);

        var response = userController.getAllUsers(request, 1, "", 100);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        var apiResponder = response.getBody();
        assertNotNull(apiResponder);

        Page<UserDto> page = apiResponder.getPayload();
        assertEquals(20, page.getPageSize());

        verify(userRepository, times(1)).getAllUsers(eq(1), eq(20), eq(""));
    }

    @Test
    @DisplayName("Get all users - empty results")
    void getAllUsersEmptyResults() {
        when(userRepository.getAllUsers(eq(1), eq(20), eq(""))).thenReturn(new ArrayList<>());
        when(userRepository.getUserCount(eq(""))).thenReturn(0);

        var response = userController.getAllUsers(request, 1, "", 20);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        var apiResponder = response.getBody();
        assertNotNull(apiResponder);
        assertTrue(apiResponder.isSuccess());

        Page<UserDto> page = apiResponder.getPayload();
        assertNotNull(page);
        assertEquals(0, page.getItems().size());
        assertEquals(0, page.getPages());
    }

    private UserMetrics createRandomUserMetrics(final String userId) {
        return UserMetrics.builder()
                .id(randomUUID())
                .userId(userId)
                .points(faker.number().numberBetween(1, 100))
                .createdAt(OffsetDateTime.now().minusDays(faker.number().numberBetween(1, 6)))
                .build();
    }

    @Test
    @DisplayName("Get user metrics - startDate after endDate returns 400")
    void getUserMetricsInvalidDateRangeReturnsBadRequest() {
        String userId = randomUUID();
        OffsetDateTime startDate = OffsetDateTime.now();
        OffsetDateTime endDate = startDate.minusDays(1);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            userController.getUserMetrics(request, userId, 1, 20, startDate, endDate);
        });

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("startDate cannot be after endDate.", exception.getReason());
    }

    @Test
    @DisplayName("Get user metrics - returns metrics successfully with explicit date range")
    void getUserMetricsReturnsSuccessfullyWithDateRange() {
        String userId = randomUUID();
        OffsetDateTime startDate = OffsetDateTime.now().minusDays(7);
        OffsetDateTime endDate = OffsetDateTime.now();

        List<UserMetrics> metricsList = List.of(createRandomUserMetrics(userId), createRandomUserMetrics(userId));

        when(userMetricsRepository.findUserMetrics(eq(userId), any())).thenReturn(metricsList);
        when(userMetricsRepository.countUserMetrics(eq(userId), any())).thenReturn(2);

        var response = userController.getUserMetrics(request, userId, 1, 20, startDate, endDate);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        var apiResponder = response.getBody();
        assertNotNull(apiResponder);
        assertTrue(apiResponder.isSuccess());
        assertEquals("Metrics fetched!", apiResponder.getMessage());

        Page<MetricsDto> page = apiResponder.getPayload();
        assertNotNull(page);
        assertEquals(2, page.getItems().size());
        assertEquals(1, page.getPages());
        assertEquals(20, page.getPageSize());

        verify(userMetricsRepository, times(1)).findUserMetrics(eq(userId), any());
        verify(userMetricsRepository, times(1)).countUserMetrics(eq(userId), any());
    }

    @Test
    @DisplayName("Get user metrics - defaults date range to last 7 days when not provided")
    void getUserMetricsDefaultsToLastSevenDays() {
        String userId = randomUUID();

        when(userMetricsRepository.findUserMetrics(eq(userId), any())).thenReturn(List.of());
        when(userMetricsRepository.countUserMetrics(eq(userId), any())).thenReturn(0);

        var response = userController.getUserMetrics(request, userId, 1, 20, null, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        verify(userMetricsRepository, times(1)).findUserMetrics(eq(userId), any());
    }

    @Test
    @DisplayName("Get user metrics - empty results")
    void getUserMetricsEmptyResults() {
        String userId = randomUUID();

        when(userMetricsRepository.findUserMetrics(eq(userId), any())).thenReturn(List.of());
        when(userMetricsRepository.countUserMetrics(eq(userId), any())).thenReturn(0);

        var response = userController.getUserMetrics(request, userId, 1, 20, null, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        Page<MetricsDto> page = response.getBody().getPayload();
        assertNotNull(page);
        assertEquals(0, page.getItems().size());
        assertEquals(0, page.getPages());
    }

    @Test
    @DisplayName("Get user metrics - page size capped at maximum")
    void getUserMetricsPageSizeCapped() {
        String userId = randomUUID();

        when(userMetricsRepository.findUserMetrics(eq(userId), any())).thenReturn(List.of());
        when(userMetricsRepository.countUserMetrics(eq(userId), any())).thenReturn(0);

        var response = userController.getUserMetrics(request, userId, 1, 100, null, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        Page<MetricsDto> page = response.getBody().getPayload();
        assertEquals(20, page.getPageSize());
    }

    @Test
    @DisplayName("Get user metrics - pagination with multiple pages")
    void getUserMetricsPaginationMultiplePages() {
        String userId = randomUUID();

        List<UserMetrics> metricsList = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            metricsList.add(createRandomUserMetrics(userId));
        }

        when(userMetricsRepository.findUserMetrics(eq(userId), any())).thenReturn(metricsList);
        when(userMetricsRepository.countUserMetrics(eq(userId), any())).thenReturn(45);

        var response = userController.getUserMetrics(request, userId, 1, 20, null, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        Page<MetricsDto> page = response.getBody().getPayload();
        assertNotNull(page);
        assertEquals(20, page.getItems().size());
        assertEquals(3, page.getPages());
        assertTrue(page.isHasNextPage());
    }

    @Test
    @DisplayName("Get user metrics - dto maps all fields correctly")
    void getUserMetricsDtoMapsFieldsCorrectly() {
        String userId = randomUUID();
        UserMetrics metric = createRandomUserMetrics(userId);

        when(userMetricsRepository.findUserMetrics(eq(userId), any())).thenReturn(List.of(metric));
        when(userMetricsRepository.countUserMetrics(eq(userId), any())).thenReturn(1);

        var response = userController.getUserMetrics(request, userId, 1, 20, null, null);

        MetricsDto dto = response.getBody().getPayload().getItems().get(0);
        assertEquals(metric.getId(), dto.getId());
        assertEquals(metric.getUserId(), dto.getUserId());
        assertEquals(metric.getPoints(), dto.getPoints());
        assertEquals(metric.getCreatedAt(), dto.getCreatedAt());
    }

    @Test
    @DisplayName("Get all users - pagination with multiple pages")
    void getAllUsersPaginationMultiplePages() {
        ArrayList<User> users = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            users.add(createRandomUser());
        }

        when(userRepository.getAllUsers(eq(1), eq(20), eq(""))).thenReturn(users);
        when(userRepository.getUserCount(eq(""))).thenReturn(45);

        var response = userController.getAllUsers(request, 1, "", 20);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        var apiResponder = response.getBody();
        assertNotNull(apiResponder);
        assertTrue(apiResponder.isSuccess());

        Page<UserDto> page = apiResponder.getPayload();
        assertNotNull(page);
        assertEquals(20, page.getItems().size());
        assertEquals(3, page.getPages());
        assertTrue(page.isHasNextPage());

        verify(userRepository, times(1)).getAllUsers(eq(1), eq(20), eq(""));
        verify(userRepository, times(1)).getUserCount(eq(""));
    }
}
