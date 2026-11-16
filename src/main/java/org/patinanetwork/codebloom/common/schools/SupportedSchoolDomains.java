package org.patinanetwork.codebloom.common.schools;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.patinanetwork.codebloom.common.db.models.usertag.Tag;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@EnableConfigurationProperties(SchoolProperties.class)
public class SupportedSchoolDomains {

    static final String GMAIL_DOMAIN = "@gmail.com";

    private final Map<String, Tag> domainToTag;

    public SupportedSchoolDomains(final SchoolProperties schoolProperties) {
        Map<String, Tag> domains = new HashMap<>();

        for (SchoolEnum school : SchoolEnum.values()) {
            domains.put(school.getEmailDomain(), school.getInternalTag());
        }

        if (schoolProperties.isAllowGmail()) {
            domains.put(GMAIL_DOMAIN, Tag.Nyu);
        }

        this.domainToTag = Collections.unmodifiableMap(domains);
    }

    public Set<String> getDomains() {
        return domainToTag.keySet();
    }

    public Optional<Tag> findTag(final String domain) {
        return Optional.ofNullable(domainToTag.get(domain));
    }
}
