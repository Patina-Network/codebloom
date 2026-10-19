package org.patinanetwork.codebloom.common.leetcode.queries;

import java.util.HashMap;
import java.util.Map;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

public class GetUserProfile {

    public static final String QUERY = """
        #graphql
        query userPublicProfile($username: String!) {
          matchedUser(username: $username) {
            username
            profile {
              ranking
              userAvatar
              realName
              aboutMe
            }
          }
        }
        """;

    public static String body(final String username) throws JacksonException {
        ObjectMapper objectMapper = new ObjectMapper();

        Map<String, Object> requestBodyMap = new HashMap<>();
        requestBodyMap.put("query", QUERY);

        Map<String, Object> variables = new HashMap<>();
        variables.put("username", username);
        requestBodyMap.put("variables", variables);

        return objectMapper.writeValueAsString(requestBodyMap);
    }
}
