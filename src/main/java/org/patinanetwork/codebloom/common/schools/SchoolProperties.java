package org.patinanetwork.codebloom.common.schools;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "codebloom.schools")
@Getter
@Setter
public class SchoolProperties {
    private boolean allowGmail;
}
