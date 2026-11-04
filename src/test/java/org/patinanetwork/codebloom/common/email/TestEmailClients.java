package org.patinanetwork.codebloom.common.email;

import org.junit.jupiter.api.Test;
import org.patinanetwork.codebloom.common.email.client.codebloom.OfficialCodebloomEmailClient;
import org.patinanetwork.codebloom.common.email.error.EmailException;
import org.patinanetwork.codebloom.config.NoJdaRequired;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class TestEmailClients extends NoJdaRequired {

    private final OfficialCodebloomEmailClient officialCodebloomEmailClient;

    @Autowired
    public TestEmailClients(final OfficialCodebloomEmailClient officialCodebloomEmailClient) {
        this.officialCodebloomEmailClient = officialCodebloomEmailClient;
    }

    @Test
    void testConnections() throws EmailException {
        officialCodebloomEmailClient.testConnection();
    }
}
