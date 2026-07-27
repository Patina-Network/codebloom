package org.patinanetwork.codebloom.common.db.repos.task;

import java.sql.Types;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.patinanetwork.codebloom.common.db.models.task.BackgroundTask;
import org.patinanetwork.codebloom.common.db.models.task.BackgroundTaskEnum;
import org.patinanetwork.codebloom.common.time.StandardizedOffsetDateTime;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
public class BackgroundTaskSqlRepository implements BackgroundTaskRepository {

    private static final RowMapper<BackgroundTask> BACKGROUND_TASK_ROW_MAPPER = (rs, rowNum) -> BackgroundTask.builder()
            .id(rs.getString("id"))
            .completedAt(StandardizedOffsetDateTime.normalize(rs.getObject("completedAt", OffsetDateTime.class)))
            .task(BackgroundTaskEnum.valueOf(rs.getString("task")))
            .build();

    private final JdbcClient jdbcClient;

    public BackgroundTaskSqlRepository(final JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public void createBackgroundTask(final BackgroundTask task) {
        String sql = """
                INSERT INTO "BackgroundTask"
                    (id, task, "completedAt")
                VALUES
                    (:id, :task, :completedAt)
            """;

        task.setId(UUID.randomUUID().toString());
        if (task.getCompletedAt() == null) {
            task.setCompletedAt(StandardizedOffsetDateTime.now());
        }
        task.setCompletedAt(StandardizedOffsetDateTime.normalize(task.getCompletedAt()));

        int rowsAffected = jdbcClient
                .sql(sql)
                .param("id", UUID.fromString(task.getId()))
                .param("task", task.getTask().name(), Types.OTHER)
                .param("completedAt", task.getCompletedAt())
                .update();

        if (rowsAffected != 1) {
            throw new RuntimeException("Failed to create background task: Rows affected != 1");
        }
    }

    @Override
    public Optional<BackgroundTask> getBackgroundTaskById(final String id) {
        String sql = """
                SELECT
                    *
                FROM
                    "BackgroundTask"
                WHERE
                    id = :id
            """;

        return jdbcClient
                .sql(sql)
                .param("id", UUID.fromString(id))
                .query(BACKGROUND_TASK_ROW_MAPPER)
                .optional();
    }

    @Override
    public List<BackgroundTask> getBackgroundTasksByTaskEnum(final BackgroundTaskEnum taskEnum) {
        String sql = """
                SELECT
                    *
                FROM
                    "BackgroundTask"
                WHERE
                    task = :task
            """;

        return jdbcClient
                .sql(sql)
                .param("task", taskEnum.name(), Types.OTHER)
                .query(BACKGROUND_TASK_ROW_MAPPER)
                .list();
    }

    @Override
    public Optional<BackgroundTask> getMostRecentlyCompletedBackgroundTaskByTaskEnum(
            final BackgroundTaskEnum taskEnum) {
        String sql = """
                SELECT
                    *
                FROM
                    "BackgroundTask"
                WHERE
                    task = :task
                ORDER BY
                    "completedAt" DESC
                LIMIT 1
            """;

        return jdbcClient
                .sql(sql)
                .param("task", taskEnum.name(), Types.OTHER)
                .query(BACKGROUND_TASK_ROW_MAPPER)
                .optional();
    }

    @Override
    public boolean updateBackgroundTaskById(final BackgroundTask task) {
        String sql = """
                        UPDATE "BackgroundTask"
                        SET
                            task = :task,
                            "completedAt" = :completedAt
                        WHERE
                            id = :id
            """;

        task.setCompletedAt(StandardizedOffsetDateTime.normalize(task.getCompletedAt()));

        int rowsAffected = jdbcClient
                .sql(sql)
                .param("task", task.getTask().name(), Types.OTHER)
                .param("completedAt", task.getCompletedAt())
                .param("id", UUID.fromString(task.getId()))
                .update();

        return rowsAffected == 1;
    }

    @Override
    public boolean deleteBackgroundTaskById(final String id) {
        String sql = """
                DELETE FROM
                    "BackgroundTask"
                WHERE
                    id = :id
            """;

        int rowsAffected = jdbcClient.sql(sql).param("id", UUID.fromString(id)).update();

        return rowsAffected == 1;
    }
}
