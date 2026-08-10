ALTER TABLE "Achievement"
ADD COLUMN "leaderboardId" UUID,
ADD CONSTRAINT "fk_leaderboard" FOREIGN KEY ("leaderboardId") REFERENCES "Leaderboard"(id);

-- backfill
-- all current `Achievement.name` is either
--   "<leaderboard name> - <school> - <Nth> Place"  (tag leaderboards)
--   "<leaderboard name> - <Nth> Place"             (global leaderboard)
DO $$
BEGIN
    CASE current_database()
        WHEN 'codebloom-prod' THEN
            UPDATE "Achievement" a
            SET "leaderboardId" = l.id
            FROM "Leaderboard" l
            WHERE l.name = split_part(a.title, ' - ', 1);
        ELSE
            RAISE NOTICE 'Skipping prod only backfill: Current database is %', current_database();
    END CASE;
END $$;

ALTER TABLE "Achievement"
ALTER COLUMN "leaderboardId" SET NOT NULL;
