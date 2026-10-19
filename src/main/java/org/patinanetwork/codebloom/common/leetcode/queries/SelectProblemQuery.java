package org.patinanetwork.codebloom.common.leetcode.queries;

import java.util.HashMap;
import java.util.Map;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

public class SelectProblemQuery {

    public static final String QUERY = """
        #graphql
        query selectProblem($titleSlug: String!) {
            question(titleSlug: $titleSlug) {
                questionId
                title
                titleSlug
                isPaidOnly
                content
                difficulty
                stats
                topicTags {
                    id
                    name
                    slug
                }
            }
        }
        """;

    public static String body(final String slug) throws JacksonException {
        ObjectMapper objectMapper = new ObjectMapper();

        Map<String, Object> requestBodyMap = new HashMap<>();
        requestBodyMap.put("query", QUERY);

        Map<String, Object> variables = new HashMap<>();
        variables.put("titleSlug", slug);
        requestBodyMap.put("variables", variables);

        return objectMapper.writeValueAsString(requestBodyMap);
    }
}
