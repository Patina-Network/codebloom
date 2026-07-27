package org.patinanetwork.codebloom.common.db.repos.job;

import java.sql.Types;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.patinanetwork.codebloom.common.db.models.job.Job;
import org.patinanetwork.codebloom.common.db.models.job.JobStatus;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
public class JobSqlRepository implements JobRepository {

    private static final RowMapper<Job> JOB_ROW_MAPPER = (rs, rowNum) -> Job.builder()
            .id(rs.getString("id"))
            .createdAt(rs.getObject("createdAt", OffsetDateTime.class))
            .processedAt(rs.getObject("processedAt", OffsetDateTime.class))
            .completedAt(rs.getObject("completedAt", OffsetDateTime.class))
            .nextAttemptAt(rs.getObject("nextAttemptAt", OffsetDateTime.class))
            .status(JobStatus.valueOf(rs.getString("status")))
            .questionId(rs.getString("questionId"))
            .attempts(rs.getInt("attempts"))
            .build();

    private final JdbcClient jdbcClient;

    public JobSqlRepository(final JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public void createJob(final Job job) {
        String sql = """
            INSERT INTO "Job"
                (id, "questionId", status)
            VALUES
                (?, ?, ?)
            RETURNING
                "createdAt", "nextAttemptAt"
            """;

        job.setId(UUID.randomUUID().toString());

        OffsetDateTime[] timestamps = jdbcClient
                .sql(sql)
                .param(1, UUID.fromString(job.getId()))
                .param(2, job.getQuestionId())
                .param(3, job.getStatus().name(), Types.OTHER)
                .query((rs, rowNum) -> new OffsetDateTime[] {
                    rs.getObject("createdAt", OffsetDateTime.class), rs.getObject("nextAttemptAt", OffsetDateTime.class)
                })
                .optional()
                .orElse(null);

        if (timestamps != null) {
            job.setCreatedAt(timestamps[0]);
            job.setNextAttemptAt(timestamps[1]);
        }
    }

    @Override
    public Job findJobById(final String id) {
        String sql = """
            SELECT
                id,
                "createdAt",
                "processedAt",
                "completedAt",
                "nextAttemptAt",
                status,
                "questionId",
                "attempts"
            FROM
                "Job"
            WHERE
                id = ?
            """;

        return jdbcClient
                .sql(sql)
                .param(1, UUID.fromString(id))
                .query(JOB_ROW_MAPPER)
                .optional()
                .orElse(null);
    }

    @Override
    public List<Job> findJobsByQuestionId(String id) {
        String sql = """
            SELECT
                id,
                "createdAt",
                "processedAt",
                "completedAt",
                "nextAttemptAt",
                status,
                "questionId",
                "attempts"
            FROM
                "Job"
            WHERE
                "questionId" = ?
            ORDER BY
                "nextAttemptAt" ASC
            """;

        return jdbcClient.sql(sql).param(1, id).query(JOB_ROW_MAPPER).list();
    }

    @Override
    public List<Job> findIncompleteJobs(final int maxJobs) {
        String sql = """
            SELECT
                id,
                "createdAt",
                "processedAt",
                "completedAt",
                "nextAttemptAt",
                status,
                "questionId",
                "attempts"
            FROM
                "Job"
            WHERE
                status = ?
                AND "nextAttemptAt" <= NOW()
            ORDER BY
                "nextAttemptAt" ASC
            LIMIT ?
            """;

        return jdbcClient
                .sql(sql)
                .param(1, JobStatus.INCOMPLETE.name(), Types.OTHER)
                .param(2, maxJobs)
                .query(JOB_ROW_MAPPER)
                .list();
    }

    @Override
    public boolean updateJob(final Job job) {
        String sql = """
            UPDATE "Job"
            SET
                "processedAt" = ?,
                "completedAt" = ?,
                "nextAttemptAt" = ?,
                status = ?,
                "attempts" = ?
            WHERE
                id = ?
            """;

        int rowsAffected = jdbcClient
                .sql(sql)
                .param(1, job.getProcessedAt())
                .param(2, job.getCompletedAt())
                .param(3, job.getNextAttemptAt())
                .param(4, job.getStatus().name(), Types.OTHER)
                .param(5, job.getAttempts())
                .param(6, UUID.fromString(job.getId()))
                .update();

        return rowsAffected == 1;
    }

    @Override
    public boolean deleteJobById(final String id) {
        String sql = """
            DELETE FROM "Job"
            WHERE id = ?
            """;

        int rowsAffected = jdbcClient.sql(sql).param(1, UUID.fromString(id)).update();

        return rowsAffected == 1;
    }

    @Override
    public boolean deleteAllJobs() {
        String sql = """
            DELETE FROM "Job"
            """;

        int rowsAffected = jdbcClient.sql(sql).update();
        return rowsAffected == 1;
    }
}
