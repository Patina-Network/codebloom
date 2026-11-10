package org.patinanetwork.codebloom.utilities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.patinanetwork.codebloom.common.ff.FeatureFlagManager;
import org.patinanetwork.codebloom.jda.properties.FeatureFlagConfiguration;
import org.springframework.core.io.ByteArrayResource;

class ApplicationShellControllerTest {
    private static final String OPEN = "<script id=\"codebloom-config\" type=\"application/json\">";
    private static final String CLOSE = "</script>";

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static FeatureFlagManager flags() {
        FeatureFlagConfiguration ff = new FeatureFlagConfiguration();
        ff.setDuels(true);
        ff.setUserMetrics(true);
        ff.getUi().putAll(Map.of("duels", false, "userMetrics", false, "notInJava", true));
        return new FeatureFlagManager(ff);
    }

    private static ByteArrayResource html(final String body) {
        return new ByteArrayResource(body.getBytes(StandardCharsets.UTF_8));
    }

    private String render(final String template) throws Exception {
        var controller = new ApplicationShellController(flags(), objectMapper, html(template));
        return new String(controller.index().getBody(), StandardCharsets.UTF_8);
    }

    @Test
    void passesUiFlagsThroughIndependentlyOfBackendFlags() throws Exception {
        String body = render(OPEN + "__CODEBLOOM_CONFIG__" + CLOSE);
        String json = body.substring(OPEN.length(), body.length() - CLOSE.length());

        // Backend duels/userMetrics are on; the browser sees only ff.ui, including keys Java never declares.
        assertEquals(
                objectMapper.readTree("{\"featureFlags\":{\"duels\":false,\"userMetrics\":false,\"notInJava\":true}}"),
                objectMapper.readTree(json));
    }

    @Test
    void refusesTemplateWithoutConfigPlaceholder() {
        assertThrows(
                IllegalStateException.class,
                () -> new ApplicationShellController(flags(), objectMapper, html("<div id=\"root\"></div>")));
    }
}
