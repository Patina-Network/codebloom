package org.patinanetwork.codebloom.utilities;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.patinanetwork.codebloom.common.ff.FeatureFlagManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Overrides index.html with the injected UI FFs. */
@RestController
public class ApplicationShellController {
    private static final String CONFIG_PLACEHOLDER = "__CODEBLOOM_CONFIG__";
    private final byte[] html;

    public ApplicationShellController(
            final FeatureFlagManager featureFlagManager,
            final ObjectMapper objectMapper,
            @Value("${codebloom.frontend.index}") final Resource index)
            throws IOException {
        String template = index.getContentAsString(StandardCharsets.UTF_8);
        if (!template.contains(CONFIG_PLACEHOLDER)) {
            throw new IllegalStateException("Application HTML is missing its runtime configuration placeholder");
        }

        String config = objectMapper
                .writeValueAsString(Map.of("featureFlags", featureFlagManager.getUiFlags()))
                .replace("<", "\\u003c")
                .replace(">", "\\u003e")
                .replace("&", "\\u0026");
        html = template.replace(CONFIG_PLACEHOLDER, config).getBytes(StandardCharsets.UTF_8);
    }

    @GetMapping({"/", "/index.html"})
    public ResponseEntity<byte[]> index() {
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "html", StandardCharsets.UTF_8))
                .cacheControl(CacheControl.noStore())
                .body(html);
    }
}
