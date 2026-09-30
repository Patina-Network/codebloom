package org.patinanetwork.codebloom.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.patinanetwork.codebloom.common.dto.ApiResponder;
import org.patinanetwork.codebloom.utilities.ServerMetadataObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;

@SpringBootTest(classes = ApiController.class, webEnvironment = WebEnvironment.NONE, properties = "VERSION=abc1234")
public class ApiControllerVersionTest {

    @Autowired
    private ApiController apiController;

    @Test
    void testApiVersionUsesRuntimeVersion() {
        ApiResponder<ServerMetadataObject> apiResponder =
                apiController.apiIndex(new MockHttpServletRequest()).getBody();

        assertNotNull(apiResponder, "Expected an API response");
        assertNotNull(apiResponder.getPayload(), "Expected server metadata");
        assertEquals(
                "abc1234", apiResponder.getPayload().getVersion(), "Expected the runtime VERSION in server metadata");
    }
}
