package org.patinanetwork.codebloom.common.leetcode.queries;

import java.util.HashMap;
import java.util.Map;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

public class SelectAcceptedSubmisisonsQuery {

    public static final String QUERY = """
        #graphql
        query recentAcSubmissions($username: String!, $limit: Int) {
            recentAcSubmissionList(username: $username, limit: $limit) {
                id
                title
                titleSlug
                timestamp
                statusDisplay
                lang
            }
        }
        """;

    public static String body(final String username, final int limit) throws JacksonException {
        // API doesn't let you get more than this amount.
        int submissionsLimit;
        if (limit < 1 || limit > 20) {
            submissionsLimit = 20;
        } else {
            submissionsLimit = limit;
        }

        ObjectMapper objectMapper = new ObjectMapper();

        Map<String, Object> requestBodyMap = new HashMap<>();
        requestBodyMap.put("query", QUERY);

        Map<String, Object> variables = new HashMap<>();
        variables.put("username", username);
        variables.put("limit", submissionsLimit);
        requestBodyMap.put("variables", variables);

        return objectMapper.writeValueAsString(requestBodyMap);
    }
}
