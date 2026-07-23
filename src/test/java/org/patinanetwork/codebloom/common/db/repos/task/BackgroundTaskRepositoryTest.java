package org.patinanetwork.codebloom.common.db.repos.task;

import static org.junit.jupiter.api.Assertions.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.patinanetwork.codebloom.common.db.models.task.BackgroundTask;
import org.patinanetwork.codebloom.common.db.models.task.BackgroundTaskEnum;
import org.patinanetwork.codebloom.common.db.repos.BaseRepositoryTest;
import org.patinanetwork.codebloom.common.time.StandardizedOffsetDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(OrderAnnotation.class)
@Slf4j
public class BackgroundTaskRepositoryTest extends BaseRepositoryTest {

    private BackgroundTaskRepository backgroundTaskRepository;
    private BackgroundTask testTask;
    private BackgroundTask anotherTask;
    private BackgroundTask taskWithNullCompletedAt;

    @Autowired
    public BackgroundTaskRepositoryTest(final BackgroundTaskSqlRepository backgroundTaskSqlRepository) {
        this.backgroundTaskRepository = backgroundTaskSqlRepository;
    }

    @BeforeAll
    void createBackgroundTask() {
        Optional<BackgroundTask> recentTask = backgroundTaskRepository.getMostRecentlyCompletedBackgroundTaskByTaskEnum(
                BackgroundTaskEnum.LEETCODE_QUESTION_BANK);
        assertTrue(recentTask.isEmpty());

        testTask = BackgroundTask.builder()
                .task(BackgroundTaskEnum.LEETCODE_QUESTION_BANK)
                .completedAt(StandardizedOffsetDateTime.now())
                .build();

        backgroundTaskRepository.createBackgroundTask(testTask);
        assertNotNull(testTask, "Test background task should be created");
    }

    @AfterAll
    void cleanUp() {
        for (BackgroundTask task : List.of(testTask, anotherTask, taskWithNullCompletedAt)) {
            if (task != null && task.getId() != null) {
                boolean isSuccessful = backgroundTaskRepository.deleteBackgroundTaskById(task.getId());
                if (!isSuccessful) {
                    throw new RuntimeException(
                            "Test task still exists after test completion: %s".formatted(task.getId()));
                }
            }
        }
    }

    @Test
    @Order(1)
    void testGetBackgroundTaskById() {
        Optional<BackgroundTask> found = backgroundTaskRepository.getBackgroundTaskById(testTask.getId());
        assertTrue(found.isPresent());
        assertEquals(testTask, found.get());
    }

    @Test
    @Order(2)
    void testGetBackgroundTasksByTaskEnum() {
        List<BackgroundTask> tasks =
                backgroundTaskRepository.getBackgroundTasksByTaskEnum(BackgroundTaskEnum.LEETCODE_QUESTION_BANK);
        assertNotNull(tasks);
        assertFalse(tasks.isEmpty());
        assertTrue(tasks.stream().anyMatch(task -> task.getId().equals(testTask.getId())));
        assertTrue(tasks.stream().allMatch(task -> task.getTask() == BackgroundTaskEnum.LEETCODE_QUESTION_BANK));
    }

    @Test
    @Order(3)
    void testGetMostRecentlyCompletedBackgroundTaskByTaskEnum() {
        Optional<BackgroundTask> recentTask = backgroundTaskRepository.getMostRecentlyCompletedBackgroundTaskByTaskEnum(
                BackgroundTaskEnum.LEETCODE_QUESTION_BANK);
        assertTrue(recentTask.isPresent());
        assertEquals(BackgroundTaskEnum.LEETCODE_QUESTION_BANK, recentTask.get().getTask());
        assertNotNull(recentTask.get().getCompletedAt());
    }

    @Test
    @Order(4)
    void testUpdateBackgroundTaskById() {
        OffsetDateTime newCompletedAt = StandardizedOffsetDateTime.now().plusMinutes(30);
        BackgroundTask updatedTask = BackgroundTask.builder()
                .id(testTask.getId())
                .task(BackgroundTaskEnum.LEETCODE_QUESTION_BANK)
                .completedAt(newCompletedAt)
                .build();

        boolean updateResult = backgroundTaskRepository.updateBackgroundTaskById(updatedTask);
        assertTrue(updateResult);

        Optional<BackgroundTask> retrieved = backgroundTaskRepository.getBackgroundTaskById(testTask.getId());
        assertTrue(retrieved.isPresent());
        assertEquals(BackgroundTaskEnum.LEETCODE_QUESTION_BANK, retrieved.get().getTask());
        assertEquals(newCompletedAt, retrieved.get().getCompletedAt());
    }

    @Test
    @Order(5)
    void testCreateAnotherBackgroundTask() {
        anotherTask = BackgroundTask.builder()
                .task(BackgroundTaskEnum.LEETCODE_QUESTION_BANK)
                .completedAt(OffsetDateTime.now().minusHours(1))
                .build();

        backgroundTaskRepository.createBackgroundTask(anotherTask);
        assertNotNull(anotherTask.getId());

        Optional<BackgroundTask> found = backgroundTaskRepository.getBackgroundTaskById(anotherTask.getId());
        assertTrue(found.isPresent());
        assertEquals(anotherTask.getTask(), found.get().getTask());
        assertEquals(anotherTask, found.get());
    }

    @Test
    @Order(6)
    void testGetBackgroundTaskByIdNotFound() {
        Optional<BackgroundTask> notFound =
                backgroundTaskRepository.getBackgroundTaskById(UUID.randomUUID().toString());
        assertTrue(notFound.isEmpty());
    }

    @Test
    @Order(7)
    void testUpdateBackgroundTaskByIdNotFound() {
        BackgroundTask nonExistentTask = BackgroundTask.builder()
                .id(UUID.randomUUID().toString())
                .task(BackgroundTaskEnum.LEETCODE_QUESTION_BANK)
                .completedAt(OffsetDateTime.now())
                .build();

        boolean updateResult = backgroundTaskRepository.updateBackgroundTaskById(nonExistentTask);
        assertFalse(updateResult);
    }

    @Test
    @Order(8)
    void testCreateBackgroundTaskWithNullCompletedAt() {
        taskWithNullCompletedAt = BackgroundTask.builder()
                .task(BackgroundTaskEnum.LEETCODE_QUESTION_BANK)
                .completedAt(null)
                .build();

        backgroundTaskRepository.createBackgroundTask(taskWithNullCompletedAt);
        assertNotNull(taskWithNullCompletedAt.getId());

        Optional<BackgroundTask> retrieved =
                backgroundTaskRepository.getBackgroundTaskById(taskWithNullCompletedAt.getId());
        assertTrue(retrieved.isPresent());
        assertNotNull(retrieved.get().getCompletedAt(), "completedAt should be set to current time when null");
    }

    @Test
    @Order(9)
    void testCreateBackgroundTaskWithNullTaskEnum() {
        BackgroundTask taskWithNullEnum = BackgroundTask.builder()
                .task(null)
                .completedAt(StandardizedOffsetDateTime.now())
                .build();

        assertThrows(
                Exception.class,
                () -> {
                    backgroundTaskRepository.createBackgroundTask(taskWithNullEnum);
                },
                "Creating task with null enum should throw exception");
    }

    @Test
    @Order(10)
    void testGetBackgroundTaskByIdWithNullId() {
        assertThrows(
                Exception.class,
                () -> {
                    backgroundTaskRepository.getBackgroundTaskById(null);
                },
                "Getting task with null ID should throw exception");
    }

    @Test
    @Order(11)
    void testGetBackgroundTasksByTaskEnumWithNull() {
        assertThrows(
                Exception.class,
                () -> {
                    backgroundTaskRepository.getBackgroundTasksByTaskEnum(null);
                },
                "Getting tasks with null enum should throw exception");
    }

    @Test
    @Order(12)
    void testGetMostRecentlyCompletedBackgroundTaskByTaskEnumWithNull() {
        assertThrows(
                Exception.class,
                () -> {
                    backgroundTaskRepository.getMostRecentlyCompletedBackgroundTaskByTaskEnum(null);
                },
                "Getting most recent task with null enum should throw exception");
    }

    @Test
    @Order(13)
    void testUpdateBackgroundTaskByIdWithNull() {
        assertThrows(
                Exception.class,
                () -> {
                    backgroundTaskRepository.updateBackgroundTaskById(null);
                },
                "Updating with null task should throw exception");
    }

    @Test
    @Order(14)
    void testDeleteBackgroundTaskById() {
        BackgroundTask deletableTask = BackgroundTask.builder()
                .task(BackgroundTaskEnum.LEETCODE_QUESTION_BANK)
                .completedAt(StandardizedOffsetDateTime.now())
                .build();

        backgroundTaskRepository.createBackgroundTask(deletableTask);
        assertNotNull(deletableTask.getId());

        Optional<BackgroundTask> found = backgroundTaskRepository.getBackgroundTaskById(deletableTask.getId());
        assertTrue(found.isPresent());
        assertEquals(deletableTask, found.get());

        boolean deleted = backgroundTaskRepository.deleteBackgroundTaskById(deletableTask.getId());
        assertTrue(deleted);

        Optional<BackgroundTask> deletedTask = backgroundTaskRepository.getBackgroundTaskById(deletableTask.getId());
        assertTrue(deletedTask.isEmpty());
    }

    @Test
    @Order(15)
    void testDeleteBackgroundTaskByIdNotFound() {
        boolean deleted = backgroundTaskRepository.deleteBackgroundTaskById(
                UUID.randomUUID().toString());
        assertFalse(deleted);
    }

    @Test
    @Order(16)
    void testDeleteBackgroundTaskByIdWithNull() {
        assertThrows(
                Exception.class,
                () -> {
                    backgroundTaskRepository.deleteBackgroundTaskById(null);
                },
                "Deleting with null ID should throw exception");
    }
}
