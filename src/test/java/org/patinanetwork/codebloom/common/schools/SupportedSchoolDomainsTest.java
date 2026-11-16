package org.patinanetwork.codebloom.common.schools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.patinanetwork.codebloom.common.db.models.usertag.Tag;

public class SupportedSchoolDomainsTest {

    private static SupportedSchoolDomains withGmail(final boolean allowGmail) {
        SchoolProperties properties = new SchoolProperties();
        properties.setAllowGmail(allowGmail);

        return new SupportedSchoolDomains(properties);
    }

    @Test
    void gmailIsRejectedUnlessExplicitlyAllowed() {
        SupportedSchoolDomains domains = withGmail(false);

        assertTrue(domains.findTag("@gmail.com").isEmpty());
        assertTrue(!domains.getDomains().contains("@gmail.com"));
    }

    @Test
    void allowedGmailVerifiesAsNyuWithoutDroppingRealSchools() {
        SupportedSchoolDomains domains = withGmail(true);

        assertEquals(Optional.of(Tag.Nyu), domains.findTag("@gmail.com"));

        for (SchoolEnum school : SchoolEnum.values()) {
            assertEquals(Optional.of(school.getInternalTag()), domains.findTag(school.getEmailDomain()));
        }
    }
}
