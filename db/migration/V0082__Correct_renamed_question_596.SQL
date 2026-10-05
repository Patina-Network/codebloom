DO $$
BEGIN
    CASE current_database()
        WHEN 'codebloom-prod' THEN
            UPDATE
                "Question"
            SET
                "questionTitle" = 'Classes With at Least 5 Students',
                "questionSlug" = 'classes-with-at-least-5-students',
                "questionLink" = 'https://leetcode.com/problems/classes-with-at-least-5-students'
            WHERE
                id = '7ea283ec-2e03-4753-b8f8-406b3a68cbe1'
                AND "questionNumber" = 596
                AND "questionSlug" = 'classes-more-than-5-students';
        ELSE
            RAISE NOTICE 'Skipping prod only migration: Current database is %', current_database();
    END CASE;
END $$;
