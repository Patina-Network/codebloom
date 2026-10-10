DO $$
BEGIN
    CASE current_database()
        WHEN 'codebloom-prod', 'codebloom-stg' THEN
            UPDATE
                "Question"
            SET
                -- https://leetcode.com/submissions/detail/2147752091/
                "submissionId" = '2147752091'
            WHERE
                id = '0ac6adc6-56af-418c-9c9d-cbee7d3b15c8'
                AND "submissionId" = '0';
        ELSE
            RAISE NOTICE 'Skipping prod/staging only migration: Current database is %', current_database();
    END CASE;
END $$;
