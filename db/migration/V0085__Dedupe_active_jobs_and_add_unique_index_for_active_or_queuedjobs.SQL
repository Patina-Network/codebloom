-- at most one active (INCOMPLETE/PROCESSING) job may exist per question.
-- existing duplicates must be resolved first, otherwise the index creation fails.
WITH ranked AS (
    SELECT
        id,
        ROW_NUMBER() OVER (
            PARTITION BY "questionId"
            ORDER BY
                (status = 'PROCESSING') DESC,
                "processedAt" DESC NULLS LAST,
                "createdAt" DESC
        ) AS rn
    FROM "Job"
    WHERE status IN ('INCOMPLETE', 'PROCESSING')
)
UPDATE "Job" j
SET
    status = 'COMPLETE',
    "completedAt" = NOW()
FROM ranked r
WHERE j.id = r.id
  AND r.rn > 1;

CREATE UNIQUE INDEX IF NOT EXISTS "Job_questionId_active_unique"
    ON "Job" ("questionId")
    WHERE status IN ('INCOMPLETE', 'PROCESSING');
