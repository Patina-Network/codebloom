package org.patinanetwork.codebloom.common.json;

import tools.jackson.databind.ObjectMapper;

/** Output a pretty JSON string. */
public class JsonPrinter {

    /** Output a pretty JSON string. */
    public static String prettify(final String jsonString) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readTree(jsonString).toPrettyString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to prettify JSON string", e);
        }
    }
}
