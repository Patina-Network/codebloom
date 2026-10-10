package org.patinanetwork.codebloom.common.db.repos.auth;

import static org.junit.jupiter.api.Assertions.fail;

import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.patinanetwork.codebloom.common.db.models.auth.Auth;
import org.patinanetwork.codebloom.common.db.repos.BaseRepositoryTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Slf4j
public class AuthRepositoryTest extends BaseRepositoryTest {

    private AuthRepository authRepository;
    private Auth testAuth;

    @Autowired
    public AuthRepositoryTest(final AuthRepository authRepository) {
        this.authRepository = authRepository;
    }

    @BeforeAll
    void createTestAuth() {
        testAuth = Auth.builder()
                .token(UUID.randomUUID().toString())
                .csrf(Optional.ofNullable(UUID.randomUUID().toString()))
                .build();

        authRepository.createAuth(testAuth);
    }

    @Test
    @Order(1)
    void findMostRecentAuth() {
        Auth possibleAuth = authRepository.getMostRecentAuth().orElse(null);

        if (possibleAuth == null) {
            fail("most recent auth not found");
        }

        if (!testAuth.equals(possibleAuth)) {
            log.info("[DEBUG] - possibleAuth: {}", possibleAuth.toString());
            log.info("[DEBUG] - testAuth: {}", testAuth.toString());
            fail("most recent auth does not equal to test auth");
        }
    }

    @Test
    void findAuthById() {
        Auth possibleAuth = authRepository.getAuthById(testAuth.getId()).orElse(null);

        if (possibleAuth == null) {
            fail("most recent auth not found");
        }

        if (!testAuth.equals(possibleAuth)) {
            log.info("[DEBUG] - possibleAuth: {}", possibleAuth.toString());
            log.info("[DEBUG] - testAuth: {}", testAuth.toString());
            fail("most recent auth does not equal to test auth");
        }
    }

    @Test
    void updateAuth() {
        testAuth.setToken("yoooo");
        boolean isSuccessful = authRepository.updateAuthById(testAuth);

        if (!isSuccessful) {
            fail("updating test auth has failed");
        }
    }

    @AfterAll
    void deleteTestAuth() {
        boolean isSuccessful = authRepository.deleteAuthById(testAuth.getId());

        if (!isSuccessful) {
            fail("failed to delete test auth");
        }
    }
}
