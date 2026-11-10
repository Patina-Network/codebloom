package org.patinanetwork.codebloom.common.ff;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.patinanetwork.codebloom.jda.properties.FeatureFlagConfiguration;

class FeatureFlagManagerTest {

    private FeatureFlagManager featureFlagManager;

    @BeforeEach
    void setUp() {
        FeatureFlagConfiguration ff = new FeatureFlagConfiguration();
        ff.setDuels(true);
        ff.setUserMetrics(false);
        featureFlagManager = new FeatureFlagManager(ff);
    }

    @Test
    void getAllFlagsReturnsValuesFromConfiguration() {
        var flags = featureFlagManager.getFlags();

        assertEquals(2, flags.size());
        assertEquals(Boolean.TRUE, flags.get("duels"));
        assertEquals(Boolean.FALSE, flags.get("userMetrics"));
    }

    @Test
    void getAllFlagsReturnsImmutableView() {
        var flags = featureFlagManager.getFlags();

        assertThrows(UnsupportedOperationException.class, () -> flags.put("newFlag", true));
        assertFalse(flags.containsKey("newFlag"));
    }
}
