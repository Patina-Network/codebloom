package org.patinanetwork.codebloom.utilities;

import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.patinanetwork.codebloom.utilities.sha.CommitShaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for system metrics exposed to Prometheus through Spring Actuator.
 * Prometheus ingests the metrics from Actuator through a Kubernetes object defined in the k8s-manifest.
 *
 * @see <a href= "https://github.com/Patina-Network/k8s-manifests/blob/main/base/production/codebloom/servicemonitor.yaml">Service Monitor</a>
 */
@Configuration
@EnableConfigurationProperties(CommitShaProperties.class)
public class SystemMetricsConfig {


    /**
     * Add commit sha to metrics so that the deployed version of the code being run on the instance is known.
     */
    @Bean
    public MeterBinder applicationInfoMetrics(CommitShaProperties commitShaProperties) {
        return registry -> {
            var tags = Tags.of(Tag.of("sha", commitShaProperties.getSha()));
            registry.gauge("application.info", tags, 1, n -> 1.0);
        };
    }
}
