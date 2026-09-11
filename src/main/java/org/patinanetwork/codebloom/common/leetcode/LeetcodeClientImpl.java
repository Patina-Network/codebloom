package org.patinanetwork.codebloom.common.leetcode;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.patinanetwork.codebloom.common.db.models.question.QuestionDifficulty;
import org.patinanetwork.codebloom.common.leetcode.models.Lang;
import org.patinanetwork.codebloom.common.leetcode.models.LeetcodeDetailedQuestion;
import org.patinanetwork.codebloom.common.leetcode.models.LeetcodeQuestion;
import org.patinanetwork.codebloom.common.leetcode.models.LeetcodeSubmission;
import org.patinanetwork.codebloom.common.leetcode.models.LeetcodeTopicTag;
import org.patinanetwork.codebloom.common.leetcode.models.POTD;
import org.patinanetwork.codebloom.common.leetcode.models.UserProfile;
import org.patinanetwork.codebloom.common.leetcode.queries.GetAllProblems;
import org.patinanetwork.codebloom.common.leetcode.queries.GetPotd;
import org.patinanetwork.codebloom.common.leetcode.queries.GetSubmissionDetails;
import org.patinanetwork.codebloom.common.leetcode.queries.GetTopics;
import org.patinanetwork.codebloom.common.leetcode.queries.GetUserProfile;
import org.patinanetwork.codebloom.common.leetcode.queries.SelectAcceptedSubmisisonsQuery;
import org.patinanetwork.codebloom.common.leetcode.queries.SelectProblemQuery;
import org.patinanetwork.codebloom.scheduled.auth.LeetcodeAuthStealer;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/** TODO: Add an input to determine whether the request must be processed quickly or not. */
@Component
@Primary
@Slf4j
@Timed(value = "leetcode.client.execution")
public class LeetcodeClientImpl implements LeetcodeClient {

    private static final String GRAPHQL_ENDPOINT = "https://leetcode.com/graphql";
    private static final String ERROR_COUNT_METRIC_NAME = "leetcode.client.exception";

    private final ObjectMapper mapper;

    private final MeterRegistry meterRegistry;

    @VisibleForTesting
    HttpClient client;

    private final LeetcodeAuthStealer leetcodeAuthStealer;

    public LeetcodeClientImpl(final MeterRegistry meterRegistry, final LeetcodeAuthStealer leetcodeAuthStealer) {
        this.meterRegistry = meterRegistry;
        this.client = HttpClient.newHttpClient();
        this.mapper = new ObjectMapper();

        this.leetcodeAuthStealer = leetcodeAuthStealer;
    }

    private Counter errorCounter() {
        var stackFrame = StackWalker.getInstance()
                .walk(frames -> frames.skip(1)
                        // wrapped in lambda, find LeetcodeClientImpl then strip lambda calls.
                        .filter(frame -> frame.getClassName().equals(LeetcodeClientImpl.class.getName()))
                        .filter(frame -> !frame.getMethodName().startsWith("lambda$"))
                        .findFirst())
                .orElseThrow();

        String methodName = stackFrame.getMethodName();
        String className = stackFrame.getClassName();

        return meterRegistry.counter(ERROR_COUNT_METRIC_NAME, "class", className, "method", methodName);
    }

    private String buildCookieHeader() {
        List<String> cookies = new ArrayList<>();

        String session = leetcodeAuthStealer.getCookie();
        cookies.add("LEETCODE_SESSION=" + session);

        String csrf = leetcodeAuthStealer.getCsrf();
        if (csrf != null) {
            cookies.add("csrftoken=" + csrf);
        }

        return String.join("; ", cookies);
    }

    /**
     * leetcode.com does not always throw correct errors, so this is our best guess for what a throttled response status
     * could be.
     */
    private boolean isThrottled(final int statusCode) {
        // 5xx errors
        if (statusCode >= 500) {
            return true;
        }

        // not 100% sure, but thrown consistently from cf
        if (statusCode == 302) {
            return true;
        }

        // cf error
        if (statusCode == 403) {
            return true;
        }

        return false;
    }

    /** Returns {@link HttpRequest.Builder} with some defaults required to interface with leetcode.com */
    private HttpRequest.Builder getGraphQLRequestBuilder() {
        return HttpRequest.newBuilder()
                .uri(URI.create(GRAPHQL_ENDPOINT))
                .header("Content-Type", "application/json")
                .header("Referer", "https://leetcode.com")
                .header("Cookie", buildCookieHeader());
    }

    @Override
    @Retry(name = "leetcodeClient")
    @CircuitBreaker(name = "leetcodeClient")
    public LeetcodeQuestion findQuestionBySlug(final String slug) {
        String requestBody;
        try {
            requestBody = SelectProblemQuery.body(slug);
        } catch (Exception e) {
            throw new LeetcodeClientException("Error building the request body", e);
        }

        try {
            HttpRequest request = getGraphQLRequestBuilder()
                    .POST(BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
            int statusCode = response.statusCode();
            String body = response.body();

            if (statusCode != 200) {
                if (isThrottled(statusCode)) {
                    leetcodeAuthStealer.reloadCookie();
                }
                throw new LeetcodeClientException("API Returned status " + statusCode + ": " + body);
            }

            JsonNode node = mapper.readTree(body);
            JsonNode questionNode = node.path("data").path("question");
            JsonNode premiumNode = questionNode.path("isPaidOnly");
            JsonNode contentNode = questionNode.path("content");
            log.info(
                    "LeetCode question diagnostic: slug={}, isPaidOnlyPresent={}, isPaidOnly={}, descriptionMissing={}",
                    slug,
                    questionNode.has("isPaidOnly"),
                    premiumNode.isMissingNode() ? "missing" : premiumNode.toString(),
                    contentNode.isNull()
                            || contentNode.isMissingNode()
                            || contentNode.asText().isBlank());

            int questionId =
                    node.path("data").path("question").path("questionId").asInt();
            String questionTitle =
                    node.path("data").path("question").path("title").asText();
            String titleSlug =
                    node.path("data").path("question").path("titleSlug").asText();
            String link = "https://leetcode.com/problems/" + titleSlug;
            String difficulty =
                    node.path("data").path("question").path("difficulty").asText();
            String question = node.path("data").path("question").path("content").asText();

            String statsJson = node.path("data").path("question").path("stats").asText();
            JsonNode stats = mapper.readTree(statsJson);
            String acRateString = stats.get("acRate").asText();
            float acRate = Float.parseFloat(acRateString.replace("%", "")) / 100f;

            JsonNode topicTagsNode = node.path("data").path("question").path("topicTags");

            List<LeetcodeTopicTag> tags = new ArrayList<>();

            for (JsonNode el : topicTagsNode) {
                tags.add(LeetcodeTopicTag.builder()
                        .name(el.get("name").asText())
                        .slug(el.get("slug").asText())
                        .build());
            }

            return LeetcodeQuestion.builder()
                    .link(link)
                    .questionId(questionId)
                    .questionTitle(questionTitle)
                    .titleSlug(titleSlug)
                    .difficulty(difficulty)
                    .question(question)
                    .acceptanceRate(acRate)
                    .topics(tags)
                    .build();
        } catch (InterruptedException e) {
            errorCounter().increment();
            Thread.currentThread().interrupt();
            throw new LeetcodeClientException("Thread interrupted", e);
        } catch (Exception e) {
            errorCounter().increment();
            throw new LeetcodeClientException("Error fetching the API", e);
        }
    }

    @Override
    public ArrayList<LeetcodeSubmission> findSubmissionsByUsername(final String username) {
        return findSubmissionsByUsername(username, 20);
    }

    @Retry(name = "leetcodeClient")
    @CircuitBreaker(name = "leetcodeClient")
    @Override
    public ArrayList<LeetcodeSubmission> findSubmissionsByUsername(final String username, final int limit) {
        ArrayList<LeetcodeSubmission> submissions = new ArrayList<>();

        String requestBody;
        try {
            requestBody = SelectAcceptedSubmisisonsQuery.body(username, limit);
        } catch (Exception e) {
            throw new LeetcodeClientException("Error building the request body");
        }

        try {
            HttpRequest request = getGraphQLRequestBuilder()
                    .POST(BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
            int statusCode = response.statusCode();
            String body = response.body();

            if (statusCode != 200) {
                if (isThrottled(statusCode)) {
                    leetcodeAuthStealer.reloadCookie();
                }
                throw new LeetcodeClientException("API Returned status " + statusCode + ": " + body);
            }

            JsonNode node = mapper.readTree(body);
            JsonNode submissionsNode = node.path("data").path("recentAcSubmissionList");

            if (submissionsNode.isArray()) {
                if (submissionsNode.isEmpty() || submissionsNode == null) {
                    return submissions;
                }

                for (JsonNode submission : submissionsNode) {
                    int id = submission.path("id").asInt();
                    String title = submission.path("title").asText();
                    String titleSlug = submission.path("titleSlug").asText();
                    String timestampString = submission.path("timestamp").asText();
                    long epochSeconds = Long.parseLong(timestampString);
                    Instant instant = Instant.ofEpochSecond(epochSeconds);

                    LocalDateTime timestamp = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
                    String statusDisplay = submission.path("statusDisplay").asText();
                    submissions.add(new LeetcodeSubmission(id, title, titleSlug, timestamp, statusDisplay));
                }
            }

            return submissions;
        } catch (InterruptedException e) {
            errorCounter().increment();
            Thread.currentThread().interrupt();
            throw new LeetcodeClientException("Thread interrupted", e);
        } catch (Exception e) {
            errorCounter().increment();
            throw new LeetcodeClientException("Error fetching the API", e);
        }
    }

    @Override
    @Retry(name = "leetcodeClient")
    @CircuitBreaker(name = "leetcodeClient")
    public LeetcodeDetailedQuestion findSubmissionDetailBySubmissionId(final int submissionId) {
        String requestBody;
        try {
            requestBody = GetSubmissionDetails.body(submissionId);
        } catch (Exception e) {
            throw new LeetcodeClientException("Error building the request body");
        }

        try {
            HttpRequest request = getGraphQLRequestBuilder()
                    .POST(BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
            int statusCode = response.statusCode();
            String body = response.body();

            if (statusCode != 200) {
                if (isThrottled(statusCode)) {
                    leetcodeAuthStealer.reloadCookie();
                }
                throw new LeetcodeClientException("API Returned status " + statusCode + ": " + body);
            }

            JsonNode node = mapper.readTree(body);
            JsonNode baseNode = node.path("data").path("submissionDetails");

            int runtime = baseNode.path("runtime").asInt();
            String runtimeDisplay = baseNode.path("runtimeDisplay").asText();
            float runtimePercentile = (float) baseNode.path("runtimePercentile").asDouble();
            int memory = baseNode.path("memory").asInt();
            String memoryDisplay = baseNode.path("memoryDisplay").asText();
            float memoryPercentile = (float) baseNode.path("memoryPercentile").asDouble();
            String code = baseNode.path("code").asText();
            String langName = baseNode.path("lang").path("name").asText();
            String langVerboseName = baseNode.path("lang").path("verboseName").asText();
            Lang lang = (Strings.isNullOrEmpty(langName) || Strings.isNullOrEmpty(langVerboseName))
                    ? null
                    : new Lang(langName, langVerboseName);

            // if any of these are empty, then extremely likely that we're throttled.
            if (Strings.isNullOrEmpty(runtimeDisplay) || Strings.isNullOrEmpty(memoryDisplay)) {
                leetcodeAuthStealer.reloadCookie();
            }

            LeetcodeDetailedQuestion question = new LeetcodeDetailedQuestion(
                    runtime, runtimeDisplay, runtimePercentile, memory, memoryDisplay, memoryPercentile, code, lang);

            return question;
        } catch (InterruptedException e) {
            errorCounter().increment();
            Thread.currentThread().interrupt();
            throw new LeetcodeClientException("Thread interrupted", e);
        } catch (Exception e) {
            errorCounter().increment();
            throw new LeetcodeClientException("Error fetching the API", e);
        }
    }

    @Override
    @Retry(name = "leetcodeClient")
    @CircuitBreaker(name = "leetcodeClient")
    public POTD getPotd() {
        String requestBody;
        try {
            requestBody = GetPotd.body();
        } catch (Exception e) {
            throw new LeetcodeClientException("Error building the request body");
        }

        try {
            HttpRequest request = getGraphQLRequestBuilder()
                    .POST(BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
            int statusCode = response.statusCode();
            String body = response.body();

            if (statusCode != 200) {
                if (isThrottled(statusCode)) {
                    leetcodeAuthStealer.reloadCookie();
                }
                throw new LeetcodeClientException("API Returned status " + statusCode + ": " + body);
            }

            JsonNode node = mapper.readTree(body);
            JsonNode baseNode =
                    node.path("data").path("activeDailyCodingChallengeQuestion").path("question");

            String titleSlug = baseNode.path("titleSlug").asText();
            String title = baseNode.path("title").asText();
            var difficulty =
                    QuestionDifficulty.valueOf(baseNode.path("difficulty").asText());

            return new POTD(title, titleSlug, difficulty);
        } catch (InterruptedException e) {
            errorCounter().increment();
            Thread.currentThread().interrupt();
            throw new LeetcodeClientException("Thread interrupted", e);
        } catch (Exception e) {
            errorCounter().increment();
            throw new LeetcodeClientException("Error fetching the API", e);
        }
    }

    @Override
    @Retry(name = "leetcodeClient")
    @CircuitBreaker(name = "leetcodeClient")
    public UserProfile getUserProfile(final String username) {
        String requestBody;
        try {
            requestBody = GetUserProfile.body(username);
        } catch (Exception e) {
            throw new LeetcodeClientException("Error building the request body", e);
        }

        try {
            HttpRequest request = getGraphQLRequestBuilder()
                    .POST(BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
            int statusCode = response.statusCode();
            String body = response.body();

            if (statusCode != 200) {
                if (isThrottled(statusCode)) {
                    leetcodeAuthStealer.reloadCookie();
                }
                throw new LeetcodeClientException("API Returned status " + statusCode + ": " + body);
            }

            JsonNode node = mapper.readTree(body);
            JsonNode baseNode = node.path("data").path("matchedUser");

            var returnedUsername = baseNode.path("username").asText();
            var ranking = baseNode.path("profile").path("ranking").asText();
            var userAvatar = baseNode.path("profile").path("userAvatar").asText();
            var realName = baseNode.path("profile").path("realName").asText();
            var aboutMe = baseNode.path("profile").path("aboutMe").asText().trim();

            return new UserProfile(returnedUsername, ranking, userAvatar, realName, aboutMe);
        } catch (InterruptedException e) {
            errorCounter().increment();
            Thread.currentThread().interrupt();
            throw new LeetcodeClientException("Thread interrupted", e);
        } catch (Exception e) {
            errorCounter().increment();
            throw new LeetcodeClientException("Error fetching the API", e);
        }
    }

    @Override
    @Retry(name = "leetcodeClient")
    @CircuitBreaker(name = "leetcodeClient")
    public Set<LeetcodeTopicTag> getAllTopicTags() {
        try {
            HttpRequest request = getGraphQLRequestBuilder()
                    .POST(BodyPublishers.ofString(GetTopics.body()))
                    .build();

            HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
            int statusCode = response.statusCode();
            String body = response.body();

            if (statusCode != 200) {
                if (isThrottled(statusCode)) {
                    leetcodeAuthStealer.reloadCookie();
                }
                throw new LeetcodeClientException(
                        "Non-successful response getting topics from Leetcode API. Status code: " + statusCode);
            }

            JsonNode json = mapper.readTree(body);
            JsonNode edges = json.path("data").path("questionTopicTags").path("edges");

            if (!edges.isArray()) {
                throw new LeetcodeClientException(
                        "The expected shape of getting topics did not match the received body");
            }

            Set<LeetcodeTopicTag> result = new HashSet<>();

            for (JsonNode edge : edges) {
                JsonNode node = edge.path("node");
                result.add(LeetcodeTopicTag.builder()
                        .name(node.get("name").asText())
                        .slug(node.get("slug").asText())
                        .build());
            }

            return result;
        } catch (InterruptedException e) {
            errorCounter().increment();
            Thread.currentThread().interrupt();
            throw new LeetcodeClientException("Thread interrupted", e);
        } catch (Exception e) {
            errorCounter().increment();
            throw new LeetcodeClientException("Error getting topics from Leetcode API", e);
        }
    }

    @Override
    @Retry(name = "leetcodeClient")
    @CircuitBreaker(name = "leetcodeClient")
    public List<LeetcodeQuestion> getAllProblems() {
        try {
            HttpRequest request = getGraphQLRequestBuilder()
                    .POST(BodyPublishers.ofString(GetAllProblems.body()))
                    .build();
            HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
            int statusCode = response.statusCode();
            String body = response.body();
            if (statusCode != 200) {
                if (isThrottled(statusCode)) {
                    leetcodeAuthStealer.reloadCookie();
                }
                throw new LeetcodeClientException(
                        "Non-successful response getting all questions from Leetcode API. Status code: " + statusCode);
            }

            JsonNode json = mapper.readTree(body);
            JsonNode allQuestions =
                    json.path("data").path("problemsetQuestionListV2").path("questions");

            if (!allQuestions.isArray()) {
                throw new LeetcodeClientException(
                        "The expected shape of getting topics did not match the received body");
            }

            List<LeetcodeQuestion> result = new ArrayList<>();
            for (JsonNode question : allQuestions) {
                JsonNode topicTags = question.get("topicTags");

                List<LeetcodeTopicTag> tags = new ArrayList<>();
                for (JsonNode tag : topicTags) {
                    tags.add(LeetcodeTopicTag.builder()
                            .name(tag.get("name").asText())
                            .slug(tag.get("slug").asText())
                            .build());
                }

                result.add(LeetcodeQuestion.builder()
                        .link("https://leetcode.com/problems/"
                                + question.get("titleSlug").asText())
                        .questionId(question.get("questionFrontendId").asInt())
                        .questionTitle(question.get("title").asText())
                        .titleSlug(question.get("titleSlug").asText())
                        .difficulty(question.get("difficulty").asText())
                        .acceptanceRate((float) question.get("acRate").asDouble())
                        .topics(tags)
                        .build());
            }
            return result;
        } catch (InterruptedException e) {
            errorCounter().increment();
            Thread.currentThread().interrupt();
            throw new LeetcodeClientException("Thread interrupted", e);
        } catch (Exception e) {
            errorCounter().increment();
            throw new LeetcodeClientException("Error getting all problems from Leetcode API", e);
        }
    }
}
