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
                AND "userId" = '8d8e7c72-2a8d-482c-baad-68d14ecdd9e5'
                AND "submissionId" = '1611970737'
                AND "questionNumber" = 596
                AND "questionTitle" = 'Classes More Than 5 Students'
                AND "questionSlug" = 'classes-more-than-5-students'
                AND "questionLink" = 'https://leetcode.com/problems/classes-more-than-5-students';
        ELSE
            RAISE NOTICE 'Skipping prod only migration: Current database is %', current_database();
    END CASE;
END $$;
