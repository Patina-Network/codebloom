package org.patinanetwork.codebloom.jda.properties;

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Feature flags that we supported. Must be defined through application.yml */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "ff")
public class FeatureFlagConfiguration {
    private boolean duels;

    private boolean userMetrics;

    private boolean messageWinners;

    /** no need to track the object, backend just writes this directly to the index.html file */
    private Map<String, Boolean> ui = new HashMap<>();
}
