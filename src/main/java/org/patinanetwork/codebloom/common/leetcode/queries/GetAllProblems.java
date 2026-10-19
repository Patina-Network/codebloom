package org.patinanetwork.codebloom.common.leetcode.queries;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

public class GetAllProblems {

    public static final String QUERY = """
            #graphql
            query problemsetQuestionListV2($filters: QuestionFilterInput) {
                problemsetQuestionListV2(filters: $filters) {
                    questions {
                    id
                    titleSlug
                    title
                    questionFrontendId
                    paidOnly
                    difficulty
                    topicTags {
                        name
                        slug
                        nameTranslated
                    }
                    acRate
                    }
                }
            }
        """;

    public static String body() throws JacksonException {
        ObjectMapper objectMapper = new ObjectMapper();

        Map<String, Object> requestBodyMap = new HashMap<>();
        requestBodyMap.put("query", QUERY);

        Map<String, Object> variables = new HashMap<>();

        Map<String, Object> filters = new HashMap<>();
        filters.put("filterCombineType", "ALL");

        Map<String, Object> premiumFilter = new HashMap<>();
        premiumFilter.put("premiumStatus", List.of("NOT_PREMIUM"));
        premiumFilter.put("operator", "IS");

        filters.put("premiumFilter", premiumFilter);

        variables.put("filters", filters);

        requestBodyMap.put("variables", variables);

        return objectMapper.writeValueAsString(requestBodyMap);
    }
}
