package org.patinanetwork.codebloom.common.leetcode;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.patinanetwork.codebloom.common.db.models.question.QuestionDifficulty;
import org.patinanetwork.codebloom.common.leetcode.models.LeetcodeDetailedQuestion;
import org.patinanetwork.codebloom.common.leetcode.models.LeetcodeQuestion;
import org.patinanetwork.codebloom.common.leetcode.models.LeetcodeSubmission;
import org.patinanetwork.codebloom.common.leetcode.models.LeetcodeTopicTag;
import org.patinanetwork.codebloom.common.leetcode.models.POTD;
import org.patinanetwork.codebloom.common.leetcode.models.UserProfile;
import org.patinanetwork.codebloom.scheduled.auth.LeetcodeAuthStealer;

public class LeetcodeClientTest {

    private final MeterRegistry meterRegistry = new SimpleMeterRegistry();
    private final LeetcodeAuthStealer leetcodeAuthStealer = mock(LeetcodeAuthStealer.class);
    private final HttpClient httpClient = mock(HttpClient.class);
    private final HttpResponse<String> httpResponse = mock(HttpResponse.class);

    private final LeetcodeClientImpl leetcodeClient;

    public LeetcodeClientTest() throws Exception {
        leetcodeClient = new LeetcodeClientImpl(meterRegistry, leetcodeAuthStealer);
        setupMockHttpClient(leetcodeClient, httpClient);
    }

    void setupMockHttpClient(LeetcodeClientImpl leetcodeClient, HttpClient httpClient) throws Exception {
        leetcodeClient.client = httpClient;
    }

    @BeforeEach
    void setup() {
        when(leetcodeAuthStealer.getCookie()).thenReturn("test-session-cookie");
        when(leetcodeAuthStealer.getCsrf()).thenReturn(null);
    }

    @Test
    void testFindQuestionBySlug() throws Exception {
        String responseJson = """
            {
              "data": {
                "question": {
                  "questionId": "42",
                  "title": "Trapping Rain Water",
                  "titleSlug": "trapping-rain-water",
                  "difficulty": "Hard",
                  "content": "<p>Given n non-negative integers...</p>",
                  "stats": "{\\"acRate\\":\\"49.4%\\"}",
                  "topicTags": [
                    {"name": "Array", "slug": "array"},
                    {"name": "Two Pointers", "slug": "two-pointers"}
                  ]
                }
              }
            }
            """;

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(responseJson);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        LeetcodeQuestion result = leetcodeClient.findQuestionBySlug("trapping-rain-water");

        assertNotNull(result);
        assertEquals(42, result.getQuestionId());
        assertEquals("Trapping Rain Water", result.getQuestionTitle());
        assertEquals("trapping-rain-water", result.getTitleSlug());
        assertEquals("Hard", result.getDifficulty());
        assertEquals("https://leetcode.com/problems/trapping-rain-water", result.getLink());
        assertEquals(0.494f, result.getAcceptanceRate(), 0.001);
        assertNotNull(result.getTopics());
        assertEquals(2, result.getTopics().size());
    }

    @ParameterizedTest
    @ValueSource(ints = {500, 302, 403})
    void testFindQuestionBySlugThrottledAndTriggersReloadCookie(int statusCode) throws Exception {
        String responseJson = """
            {
              "data": {}
            }
            """;

        when(httpResponse.statusCode()).thenReturn(statusCode);
        when(httpResponse.body()).thenReturn(responseJson);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);
        when(httpResponse.headers()).thenReturn(HttpHeaders.of(Map.of(), (a, b) -> true));

        try {
            leetcodeClient.findQuestionBySlug("trapping-rain-water");
            fail("Exception expected");
        } catch (RuntimeException e) {
            assertTrue(e.getCause().getMessage().contains(String.valueOf(statusCode)));
            verify(leetcodeAuthStealer, times(1)).reloadCookie();
        }
    }

    @Test
    void testFindSubmissionsByUsername() throws Exception {
        String responseJson = """
            {
              "data": {
                "recentAcSubmissionList": [
                  {
                    "id": "1234567",
                    "title": "Two Sum",
                    "titleSlug": "two-sum",
                    "timestamp": "1640995200",
                    "statusDisplay": "Accepted"
                  },
                  {
                    "id": "1234568",
                    "title": "Add Two Numbers",
                    "titleSlug": "add-two-numbers",
                    "timestamp": "1640995300",
                    "statusDisplay": "Accepted"
                  }
                ]
              }
            }
            """;

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(responseJson);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        List<LeetcodeSubmission> result = leetcodeClient.findSubmissionsByUsername("testuser");

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Two Sum", result.get(0).getTitle());
        assertEquals("two-sum", result.get(0).getTitleSlug());
    }

    @ParameterizedTest
    @ValueSource(ints = {500, 302, 403})
    void testFindSubmissionsByUsernameThrottledAndTriggersReloadCookie(int statusCode) throws Exception {
        String responseJson = """
            {
              "data": {}
            }
            """;

        when(httpResponse.statusCode()).thenReturn(statusCode);
        when(httpResponse.body()).thenReturn(responseJson);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);
        when(httpResponse.headers()).thenReturn(HttpHeaders.of(Map.of(), (a, b) -> true));

        try {
            leetcodeClient.findSubmissionsByUsername("testuser");
            fail("Exception expected");
        } catch (RuntimeException e) {
            assertTrue(e.getCause().getMessage().contains(String.valueOf(statusCode)));
            verify(leetcodeAuthStealer, times(1)).reloadCookie();
        }
    }

    @Test
    void testFindSubmissionsByUsernameWithLimit() throws Exception {
        String responseJson = """
            {
              "data": {
                "recentAcSubmissionList": [
                  {
                    "id": "1234567",
                    "title": "Two Sum",
                    "titleSlug": "two-sum",
                    "timestamp": "1640995200",
                    "statusDisplay": "Accepted"
                  }
                ]
              }
            }
            """;

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(responseJson);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        List<LeetcodeSubmission> result = leetcodeClient.findSubmissionsByUsername("testuser", 5);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testFindSubmissionDetailBySubmissionId() throws Exception {
        String responseJson = """
            {
              "data": {
                "submissionDetails": {
                  "runtime": 45,
                  "runtimeDisplay": "45 ms",
                  "runtimePercentile": 85.5,
                  "memory": 14000000,
                  "memoryDisplay": "14 MB",
                  "memoryPercentile": 72.3,
                  "code": "class Solution {}",
                  "lang": {
                    "name": "java",
                    "verboseName": "Java"
                  }
                }
              }
            }
            """;

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(responseJson);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        LeetcodeDetailedQuestion result = leetcodeClient.findSubmissionDetailBySubmissionId(1234567);

        assertNotNull(result);
        assertEquals(45, result.getRuntime());
        assertEquals("45 ms", result.getRuntimeDisplay());
        assertEquals(85.5f, result.getRuntimePercentile(), 0.01);
        assertEquals(14000000, result.getMemory());
        assertEquals("14 MB", result.getMemoryDisplay());
        assertEquals(72.3f, result.getMemoryPercentile(), 0.01);
        assertEquals("class Solution {}", result.getCode());
        assertNotNull(result.getLang());
        assertEquals("java", result.getLang().getName());
        assertEquals("Java", result.getLang().getVerboseName());
    }

    @ParameterizedTest
    @ValueSource(ints = {500, 302, 403})
    void testFindSubmissionDetailBySubmissionIdThrottledAndTriggersReloadCookie(int statusCode) throws Exception {
        String responseJson = """
            {
              "data": {}
            }
            """;

        when(httpResponse.statusCode()).thenReturn(statusCode);
        when(httpResponse.body()).thenReturn(responseJson);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);
        when(httpResponse.headers()).thenReturn(HttpHeaders.of(Map.of(), (a, b) -> true));

        try {
            leetcodeClient.findSubmissionDetailBySubmissionId(1234567);
            fail("Exception expected");
        } catch (RuntimeException e) {
            assertTrue(e.getCause().getMessage().contains(String.valueOf(statusCode)));
            verify(leetcodeAuthStealer, times(1)).reloadCookie();
        }
    }

    @Test
    void testGetPotd() throws Exception {
        String responseJson = """
            {
              "data": {
                "activeDailyCodingChallengeQuestion": {
                  "question": {
                    "titleSlug": "two-sum",
                    "title": "Two Sum",
                    "difficulty": "Easy"
                  }
                }
              }
            }
            """;

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(responseJson);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        POTD result = leetcodeClient.getPotd();

        assertNotNull(result);
        assertEquals("Two Sum", result.getTitle());
        assertEquals("two-sum", result.getTitleSlug());
        assertEquals(QuestionDifficulty.Easy, result.getDifficulty());
    }

    @ParameterizedTest
    @ValueSource(ints = {500, 302, 403})
    void testGetPotdThrottledAndTriggersReloadCookie(int statusCode) throws Exception {
        String responseJson = """
            {
              "data": {}
            }
            """;

        when(httpResponse.statusCode()).thenReturn(statusCode);
        when(httpResponse.body()).thenReturn(responseJson);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);
        when(httpResponse.headers()).thenReturn(HttpHeaders.of(Map.of(), (a, b) -> true));

        try {
            leetcodeClient.getPotd();
            fail("Exception expected");
        } catch (RuntimeException e) {
            assertTrue(e.getCause().getMessage().contains(String.valueOf(statusCode)));
            verify(leetcodeAuthStealer, times(1)).reloadCookie();
        }
    }

    @Test
    void testGetUserProfile() throws Exception {
        String responseJson = """
            {
              "data": {
                "matchedUser": {
                  "username": "testuser",
                  "profile": {
                    "ranking": "12345",
                    "userAvatar": "https://example.com/avatar.jpg",
                    "realName": "Test User",
                    "aboutMe": "Passionate coder"
                  }
                }
              }
            }
            """;

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(responseJson);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        UserProfile result = leetcodeClient.getUserProfile("testuser");

        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertEquals("12345", result.getRanking());
        assertEquals("https://example.com/avatar.jpg", result.getUserAvatar());
        assertEquals("Test User", result.getRealName());
        assertEquals("Passionate coder", result.getAboutMe());
    }

    @ParameterizedTest
    @ValueSource(ints = {500, 302, 403})
    void testGetUserProfileThrottledAndTriggersReloadCookie(int statusCode) throws Exception {
        String responseJson = """
            {
              "data": {}
            }
            """;

        when(httpResponse.statusCode()).thenReturn(statusCode);
        when(httpResponse.body()).thenReturn(responseJson);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);
        when(httpResponse.headers()).thenReturn(HttpHeaders.of(Map.of(), (a, b) -> true));

        try {
            leetcodeClient.getUserProfile("testuser");
            fail("Exception expected");
        } catch (RuntimeException e) {
            assertTrue(e.getCause().getMessage().contains(String.valueOf(statusCode)));
            verify(leetcodeAuthStealer, times(1)).reloadCookie();
        }
    }

    @Test
    void testGetAllTopicTags() throws Exception {
        String responseJson = """
            {
              "data": {
                "questionTopicTags": {
                  "edges": [
                    {
                      "node": {
                        "name": "Array",
                        "slug": "array"
                      }
                    },
                    {
                      "node": {
                        "name": "Hash Table",
                        "slug": "hash-table"
                      }
                    }
                  ]
                }
              }
            }
            """;

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(responseJson);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        Set<LeetcodeTopicTag> result = leetcodeClient.getAllTopicTags();

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @ParameterizedTest
    @ValueSource(ints = {500, 302, 403})
    void testGetAllTopicTagsThrottledAndTriggersReloadCookie(int statusCode) throws Exception {
        String responseJson = """
            {
              "data": {}
            }
            """;

        when(httpResponse.statusCode()).thenReturn(statusCode);
        when(httpResponse.body()).thenReturn(responseJson);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);
        when(httpResponse.headers()).thenReturn(HttpHeaders.of(Map.of(), (a, b) -> true));

        try {
            leetcodeClient.getAllTopicTags();
            fail("Exception expected");
        } catch (RuntimeException e) {
            assertTrue(e.getCause().getMessage().contains(String.valueOf(statusCode)));
            verify(leetcodeAuthStealer, times(1)).reloadCookie();
        }
    }

    @Test
    void testGetAllProblems() throws Exception {
        String responseJson = """
            {
              "data": {
                "problemsetQuestionListV2": {
                  "questions": [
                    {
                      "questionFrontendId": 1,
                      "title": "Two Sum",
                      "titleSlug": "two-sum",
                      "difficulty": "Easy",
                      "acRate": 49.4,
                      "topicTags": [
                        {"name": "Array", "slug": "array"},
                        {"name": "Hash Table", "slug": "hash-table"}
                      ]
                    },
                    {
                      "questionFrontendId": 2,
                      "title": "Add Two Numbers",
                      "titleSlug": "add-two-numbers",
                      "difficulty": "Medium",
                      "acRate": 42.1,
                      "topicTags": [
                        {"name": "Linked List", "slug": "linked-list"}
                      ]
                    }
                  ]
                }
              }
            }
            """;

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(responseJson);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        List<LeetcodeQuestion> result = leetcodeClient.getAllProblems();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Two Sum", result.get(0).getQuestionTitle());
        assertEquals("Add Two Numbers", result.get(1).getQuestionTitle());
    }

    @Test
    void testGetAllProblemsQuestionsIsNotArray() throws Exception {
        String responseJson = """
            {
              "data": {
                "problemsetQuestionListV2": {
                  "questions": {
                      "questionFrontendId": 2,
                      "title": "Add Two Numbers",
                      "titleSlug": "add-two-numbers",
                      "difficulty": "Medium",
                      "acRate": 42.1,
                      "topicTags": [
                        {"name": "Linked List", "slug": "linked-list"}
                      ]
                  }
                }
              }
            }
            """;

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(responseJson);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        try {
            leetcodeClient.getAllProblems();
            fail("Expected exceptions");
        } catch (Exception e) {
            assertTrue(e.getCause()
                    .getMessage()
                    .contains("The expected shape of getting topics did not match the received body"));
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {500, 302, 403})
    void testGetAllProblemsThrottledAndTriggersReloadCookie(int statusCode) throws Exception {
        String responseJson = """
            {
              "data": {}
            }
            """;

        when(httpResponse.statusCode()).thenReturn(statusCode);
        when(httpResponse.body()).thenReturn(responseJson);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);
        when(httpResponse.headers()).thenReturn(HttpHeaders.of(Map.of(), (a, b) -> true));

        try {
            leetcodeClient.getAllProblems();
            fail("Exception expected");
        } catch (RuntimeException e) {
            assertTrue(e.getCause().getMessage().contains(String.valueOf(statusCode)));
            verify(leetcodeAuthStealer, times(1)).reloadCookie();
        }
    }

    @Test
    void testFindQuestionBySlugThrowsInterruptedException() throws Exception {
        String responseJson = """
            {
              "data": {
                "question": {
                  "questionId": "42",
                  "title": "Trapping Rain Water",
                  "titleSlug": "trapping-rain-water",
                  "difficulty": "Hard",
                  "content": "<p>Given n non-negative integers...</p>",
                  "stats": "{\\"acRate\\":\\"49.4%\\"}",
                  "topicTags": [
                    {"name": "Array", "slug": "array"},
                    {"name": "Two Pointers", "slug": "two-pointers"}
                  ]
                }
              }
            }
            """;

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(responseJson);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(InterruptedException.class);
        try {
            leetcodeClient.findQuestionBySlug("trapping-rain-water");
            fail("Exception expected");
        } catch (RuntimeException e) {
            assertNotNull(e);
            assertNotNull(e.getCause());
            assertTrue(e.getMessage().contains("Thread interrupted"));
            assertInstanceOf(InterruptedException.class, e.getCause());
            assertTrue(Thread.currentThread().isInterrupted());
            Thread.interrupted();
        }
    }

    @Test
    void testFindSubmissionsByUsernameThrowsInterruptedException() throws Exception {
        String responseJson = """
            {
              "data": {
                "recentAcSubmissionList": [
                  {
                    "id": "1234567",
                    "title": "Two Sum",
                    "titleSlug": "two-sum",
                    "timestamp": "1640995200",
                    "statusDisplay": "Accepted"
                  },
                  {
                    "id": "1234568",
                    "title": "Add Two Numbers",
                    "titleSlug": "add-two-numbers",
                    "timestamp": "1640995300",
                    "statusDisplay": "Accepted"
                  }
                ]
              }
            }
            """;

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(responseJson);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(InterruptedException.class);

        try {
            leetcodeClient.findSubmissionsByUsername("testuser");
            fail("Exception expected");
        } catch (RuntimeException e) {
            assertNotNull(e);
            assertNotNull(e.getCause());
            assertTrue(e.getMessage().contains("Thread interrupted"));
            assertInstanceOf(InterruptedException.class, e.getCause());
            assertTrue(Thread.currentThread().isInterrupted());
            Thread.interrupted();
        }
    }

    @Test
    void testFindSubmissionDetailBySubmissionIdThrowsInterruptedException() throws Exception {
        String responseJson = """
            {
              "data": {
                "submissionDetails": {
                  "runtime": 45,
                  "runtimeDisplay": "45 ms",
                  "runtimePercentile": 85.5,
                  "memory": 14000000,
                  "memoryDisplay": "14 MB",
                  "memoryPercentile": 72.3,
                  "code": "class Solution {}",
                  "lang": {
                    "name": "java",
                    "verboseName": "Java"
                  }
                }
              }
            }
            """;

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(responseJson);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(InterruptedException.class);

        try {
            leetcodeClient.findSubmissionDetailBySubmissionId(1234567);
            fail("Exception expected");
        } catch (RuntimeException e) {
            assertNotNull(e);
            assertNotNull(e.getCause());
            assertTrue(e.getMessage().contains("Thread interrupted"));
            assertInstanceOf(InterruptedException.class, e.getCause());
            assertTrue(Thread.currentThread().isInterrupted());
            Thread.interrupted();
        }
    }

    @Test
    void testGetPotdThrowsInterruptedException() throws Exception {
        String responseJson = """
            {
              "data": {
                "activeDailyCodingChallengeQuestion": {
                  "question": {
                    "titleSlug": "two-sum",
                    "title": "Two Sum",
                    "difficulty": "Easy"
                  }
                }
              }
            }
            """;

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(responseJson);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(InterruptedException.class);

        try {
            leetcodeClient.getPotd();
            fail("Exception expected");
        } catch (RuntimeException e) {
            assertNotNull(e);
            assertNotNull(e.getCause());
            assertTrue(e.getMessage().contains("Thread interrupted"));
            assertInstanceOf(InterruptedException.class, e.getCause());
            assertTrue(Thread.currentThread().isInterrupted());
            Thread.interrupted();
        }
    }

    @Test
    void testGetUserProfileThrowsInterruptedException() throws Exception {
        String responseJson = """
            {
              "data": {
                "matchedUser": {
                  "username": "testuser",
                  "profile": {
                    "ranking": "12345",
                    "userAvatar": "https://example.com/avatar.jpg",
                    "realName": "Test User",
                    "aboutMe": "Passionate coder"
                  }
                }
              }
            }
            """;

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(responseJson);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(InterruptedException.class);

        try {
            leetcodeClient.getUserProfile("testuser");
            fail("Exception expected");
        } catch (RuntimeException e) {
            assertNotNull(e);
            assertNotNull(e.getCause());
            assertTrue(e.getMessage().contains("Thread interrupted"));
            assertInstanceOf(InterruptedException.class, e.getCause());
            assertTrue(Thread.currentThread().isInterrupted());
            Thread.interrupted();
        }
    }

    @Test
    void testGetTopicTagsThrowsInterruptedException() throws Exception {
        String responseJson = """
            {
              "data": {
                "questionTopicTags": {
                  "edges": [
                    {
                      "node": {
                        "name": "Array",
                        "slug": "array"
                      }
                    },
                    {
                      "node": {
                        "name": "Hash Table",
                        "slug": "hash-table"
                      }
                    }
                  ]
                }
              }
            }
            """;

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(responseJson);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(InterruptedException.class);

        try {
            leetcodeClient.getAllTopicTags();
            fail("Exception expected");
        } catch (RuntimeException e) {
            assertNotNull(e);
            assertNotNull(e.getCause());
            assertTrue(e.getMessage().contains("Thread interrupted"));
            assertInstanceOf(InterruptedException.class, e.getCause());
            assertTrue(Thread.currentThread().isInterrupted());
            Thread.interrupted();
        }
    }

    @Test
    void testGetAllProblemsThrowsInterruptedException() throws Exception {
        String responseJson = """
            {
              "data": {
                "problemsetQuestionListV2": {
                  "questions": [
                    {
                      "questionFrontendId": 1,
                      "title": "Two Sum",
                      "titleSlug": "two-sum",
                      "difficulty": "Easy",
                      "acRate": 49.4,
                      "topicTags": [
                        {"name": "Array", "slug": "array"},
                        {"name": "Hash Table", "slug": "hash-table"}
                      ]
                    },
                    {
                      "questionFrontendId": 2,
                      "title": "Add Two Numbers",
                      "titleSlug": "add-two-numbers",
                      "difficulty": "Medium",
                      "acRate": 42.1,
                      "topicTags": [
                        {"name": "Linked List", "slug": "linked-list"}
                      ]
                    }
                  ]
                }
              }
            }
            """;

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(responseJson);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(InterruptedException.class);

        try {
            leetcodeClient.getAllProblems();
            fail("Exception expected");
        } catch (RuntimeException e) {
            assertNotNull(e);
            assertNotNull(e.getCause());
            assertTrue(e.getMessage().contains("Thread interrupted"));
            assertInstanceOf(InterruptedException.class, e.getCause());
            assertTrue(Thread.currentThread().isInterrupted());
            Thread.interrupted();
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void paidOnlyFlagPreservesNullContent(boolean isPaidOnly) throws Exception {
        when(httpResponse.statusCode()).thenReturn(200);
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        var data = new java.util.HashMap<String, Object>();
        data.put("questionId", "1");
        data.put("title", "Premium");
        data.put("titleSlug", "premium");
        data.put("difficulty", "Medium");
        data.put("content", null);
        data.put("isPaidOnly", isPaidOnly);
        data.put("stats", "{\"acRate\":\"50%\"}");
        data.put("topicTags", List.of());
        when(httpResponse.body()).thenReturn(mapper.writeValueAsString(Map.of("data", Map.of("question", data))));
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);
        var question = leetcodeClient.findQuestionBySlug("premium");
        assertEquals(isPaidOnly, question.isPaidOnly());
        assertNull(question.getQuestion());
    }
}
