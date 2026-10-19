package org.patinanetwork.codebloom.common.leetcode.queries;

import java.util.HashMap;
import java.util.Map;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

public class GetTopics {

    public static final String QUERY = """
        #graphql
        query questionTopicTags {
          questionTopicTags {
            edges {
              node {
                name
                slug
              }
            }
          }
        }
                        """;

    public static String body() throws JacksonException {
        ObjectMapper objectMapper = new ObjectMapper();

        Map<String, Object> requestBodyMap = new HashMap<>();
        requestBodyMap.put("query", QUERY);

        return objectMapper.writeValueAsString(requestBodyMap);
    }
}
