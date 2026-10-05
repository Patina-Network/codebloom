ALTER TABLE "Question"
ADD COLUMN "skipTopicLookup" BOOLEAN NOT NULL DEFAULT FALSE;

DO $$
BEGIN
    CASE current_database()
        WHEN 'codebloom-prod' THEN
            UPDATE
                "Question"
            SET
                "skipTopicLookup" = TRUE
            WHERE
                id = '7ea283ec-2e03-4753-b8f8-406b3a68cbe1'
                AND "questionNumber" = 596
                AND "questionSlug" = 'classes-more-than-5-students';
        ELSE
            RAISE NOTICE 'Skipping prod only update: Current database is %', current_database();
    END CASE;
END $$;
