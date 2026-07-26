package org.patinanetwork.codebloom.utilities;

import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.patinanetwork.codebloom.utilities.sha.CommitShaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(CommitShaProperties.class)
public class SystemMetricsConfig {

    @Bean
    public MeterBinder applicationInfoMetrics(CommitShaProperties commitShaProperties) {
        return registry -> {
            var tags = Tags.of(Tag.of("sha", commitShaProperties.getSha()));
            registry.gauge("application.info", tags, 1, n -> 1.0);
        };
    }
}
