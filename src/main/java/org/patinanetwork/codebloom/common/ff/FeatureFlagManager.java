package org.patinanetwork.codebloom.common.ff;

import java.beans.Introspector;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.patinanetwork.codebloom.jda.properties.FeatureFlagConfiguration;
import org.patinanetwork.codebloom.shared.TypableObject;
import org.springframework.stereotype.Component;

@Component
public class FeatureFlagManager implements TypableObject {
    private final Map<String, Boolean> cachedFlags;
    private final Map<String, Boolean> cachedUiFlags;

    public FeatureFlagManager(final FeatureFlagConfiguration ff) {
        this.cachedFlags = initializeFlags(ff);
        this.cachedUiFlags = Map.copyOf(ff.getUi());
    }

    private Map<String, Boolean> initializeFlags(final FeatureFlagConfiguration ff) {
        Map<String, Boolean> flags = new HashMap<>();
        for (Method method : ff.getClass().getMethods()) {
            if (method.getName().startsWith("is")
                    && method.getReturnType() == boolean.class
                    && method.getParameterCount() == 0) {
                try {
                    String flagName = Introspector.decapitalize(method.getName().substring(2));
                    flags.put(flagName, (Boolean) method.invoke(ff));
                } catch (Exception e) {
                    throw new IllegalStateException(
                            "Failed to resolve feature flag value from method: " + method.getName(), e);
                }
            }
        }
        return Map.copyOf(flags);
    }

    public Map<String, Boolean> getFlags() {
        return cachedFlags;
    }

    public Map<String, Boolean> getUiFlags() {
        return cachedUiFlags;
    }

    @Override
    public String tsType() {
        return cachedUiFlags.keySet().stream()
                .sorted()
                .map(name -> "  " + name + ": boolean;\n")
                .collect(Collectors.joining("", "export type UiFeatureFlags = Readonly<{\n", "}>;\n\n"));
    }
}
