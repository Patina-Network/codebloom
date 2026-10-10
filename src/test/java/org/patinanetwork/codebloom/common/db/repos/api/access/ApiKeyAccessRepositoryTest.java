package org.patinanetwork.codebloom.common.db.repos.api.access;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.patinanetwork.codebloom.common.db.models.api.ApiKeyAccessEnum;
import org.patinanetwork.codebloom.common.db.models.api.access.ApiKeyAccess;
import org.patinanetwork.codebloom.common.db.repos.BaseRepositoryTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(OrderAnnotation.class)
@Slf4j
public class ApiKeyAccessRepositoryTest extends BaseRepositoryTest {

    private final ApiKeyAccessRepository apiKeyAccessRepository;
    private ApiKeyAccess testApiKeyAccess;
    private ApiKeyAccess deletableApiKeyAccess;
    private final String mockedApiKeyId = "48fa0072-2d3c-4c94-9003-0234efcf0209";
    private final ApiKeyAccessEnum access = ApiKeyAccessEnum.GWC_READ_BY_USER;
    private final ApiKeyAccessEnum testAccess = ApiKeyAccessEnum.TEST_VALUE;

    @Autowired
    public ApiKeyAccessRepositoryTest(final ApiKeyAccessRepository apiKeyAccessRepository) {
        this.apiKeyAccessRepository = apiKeyAccessRepository;
    }

    @BeforeAll
    void createTestApiKeyAccess() {
        testApiKeyAccess = ApiKeyAccess.builder()
                .id(UUID.randomUUID().toString())
                .apiKeyId(mockedApiKeyId)
                .access(access)
                .build();

        apiKeyAccessRepository.createApiKeyAccess(testApiKeyAccess);
    }

    @AfterAll
    void testDeleteApiKeyAccessesByApiKeyId() {
        deletableApiKeyAccess = ApiKeyAccess.builder()
                .id(UUID.randomUUID().toString())
                .apiKeyId(mockedApiKeyId)
                .access(testAccess)
                .build();

        apiKeyAccessRepository.createApiKeyAccess(deletableApiKeyAccess);

        List<ApiKeyAccess> found =
                apiKeyAccessRepository.getApiKeyAccessesByApiKeyId(deletableApiKeyAccess.getApiKeyId());
        assertNotNull(found);
        assertTrue(found.size() > 0);
        assertTrue(found.stream().anyMatch(k -> k.getId().equals(deletableApiKeyAccess.getId())));

        boolean deleted = apiKeyAccessRepository.deleteApiKeyAccessesByApiKeyId(deletableApiKeyAccess.getApiKeyId());
        assertTrue(deleted);

        assertTrue(apiKeyAccessRepository
                .getApiKeyAccessById(deletableApiKeyAccess.getId())
                .isEmpty());
    }

    /**
     * @note this could possibly fail via race condition if you create a new apiKeyAccess in the few seconds while this
     *     test suite is running, overriding the test apiKeyAccess. If you do, let the test suite finish and let
     *     `@AfterAll` delete the test apiKey and then just run the test suite again.
     *     <p>We run this test first just to make sure that we don't run into that issue.
     */
    @Test
    @Order(1)
    void findApiKeyAccessById() {
        ApiKeyAccess possibleTestApiKeyAccess = apiKeyAccessRepository
                .getApiKeyAccessById(testApiKeyAccess.getId())
                .orElse(null);

        if (possibleTestApiKeyAccess == null) {
            fail("most recent apiKeyAccess doesn't exist, even though we created a test apiKeyAccess");
        }

        /** @note - see method note */
        if (!testApiKeyAccess.equals(possibleTestApiKeyAccess)) {
            fail("the most recent apiKeyAccess does not equal to the test apiKeyAccess");
        }
    }

    @Test
    @Order(2)
    void findApiKeyAccessesByApiKeyId() {
        List<ApiKeyAccess> apiKeysAccessList =
                apiKeyAccessRepository.getApiKeyAccessesByApiKeyId(testApiKeyAccess.getApiKeyId());

        if (apiKeysAccessList == null) {
            fail("list of all apiKeysAccess is null");
        }

        if (apiKeysAccessList.size() == 0) {
            fail("list of all apiKeysAccess is 0, when it should be at least 1 from the test apiKeyAccess");
        }

        if (!apiKeysAccessList.contains(testApiKeyAccess)) {
            fail("test apiKeyAccess cannot be found in the list of all apiKeysAccess");
        }
    }

    @Test
    @Order(3)
    void updateApiKeyAccessByIdTest() {
        ApiKeyAccess updatedApiKeyAccess = ApiKeyAccess.builder()
                .id(testApiKeyAccess.getId())
                .apiKeyId(testApiKeyAccess.getApiKeyId())
                .access(testApiKeyAccess.getAccess())
                .build();

        boolean result = apiKeyAccessRepository.updateApiKeyAccessById(updatedApiKeyAccess);

        if (!result) {
            fail("failure to update updateApiKeyAccessById");
        }

        ApiKeyAccess resultApiKeyAccess = apiKeyAccessRepository
                .getApiKeyAccessById(testApiKeyAccess.getId())
                .orElse(null);

        assertNotNull(resultApiKeyAccess);
        assertEquals(resultApiKeyAccess.getId(), testApiKeyAccess.getId());
        assertEquals(resultApiKeyAccess.getApiKeyId(), testApiKeyAccess.getApiKeyId());
        assertEquals(resultApiKeyAccess.getAccess(), testApiKeyAccess.getAccess());
    }

    @Test
    @Order(4)
    void deleteTestApiKeyAccess() {
        boolean isSuccessful = apiKeyAccessRepository.deleteApiKeyAccessById(testApiKeyAccess.getId());

        if (!isSuccessful) {
            fail("Failed to delete test apiKeyAccess");
        }
    }
}
